package models.quests;

import Data.database.DataBaseManager;
import Data.database.PlantRepository;
import Data.database.QuestDatabaseMigration;
import Data.database.QuestsRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.User;
import models.games.ChapterTheme;
import models.games.GameState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class QuestService {
    private static final QuestService INSTANCE =
            new QuestService(new QuestsRepository(), new Random());
    private final QuestsRepository repository;
    private final Random random;
    QuestService(QuestsRepository repository, Random random) {
        this.repository = repository;
        this.random = random;
    }
    public static QuestService getInstance() {
        return INSTANCE;
    }
    public void initializeForUser(int userId) {
        LocalDate today = LocalDate.now();
        for (Quest quest : repository.getAllQuests()) {
            Optional<UserQuest> existing = repository.getUserQuest(userId, quest.getId());
            boolean dailyExpired = quest.getType() == QuestType.DAILY
                    && (existing.isEmpty()
                    || existing.get().getResetDate() == null
                    || !today.equals(existing.get().getResetDate()));
            if (existing.isEmpty() || dailyExpired) {
                repository.saveAssignment(createAssignment(userId, quest, today));
            } else if (assignmentNeedsRepair(quest, existing.get())) {
                repository.saveAssignment(repairAssignment(userId, quest, existing.get()));
            }
        }
    }
    private UserQuest createAssignment(int userId, Quest quest, LocalDate today) {
        String parameter = chooseParameter(userId, quest);
        int target = assignmentTarget(quest, parameter);
        int reward = assignmentReward(quest, parameter);
        return new UserQuest(
                userId, quest.getId(), 0, target, reward, false, false,
                quest.getType() == QuestType.DAILY ? today : null, parameter);
    }
    private boolean assignmentNeedsRepair(Quest quest, UserQuest assignment) {
        if (parameterNeedsRepair(quest, assignment.getParameter())) {
            return true;
        }
        return assignment.getTargetAmount() != assignmentTarget(quest, assignment.getParameter())
                || assignment.getRewardAmount() != assignmentReward(
                quest, assignment.getParameter());
    }
    private UserQuest repairAssignment(int userId, Quest quest, UserQuest old) {
        String parameter = old.getParameter();
        if (parameterNeedsRepair(quest, parameter)) {
            parameter = chooseParameter(userId, quest);
        }
        int target = assignmentTarget(quest, parameter);
        int reward = assignmentReward(quest, parameter);
        int progress = Math.min(Math.max(0, old.getProgress()), target);
        return new UserQuest(
                userId, quest.getId(), progress, target, reward,
                old.isCompleted() || progress >= target, old.isClaimed(),
                old.getResetDate(), parameter);
    }
    private int assignmentTarget(Quest quest, String parameter) {
        return switch (quest.getEventType()) {
            case SUN_COLLECTED, MOWER_KILLS ->
                    Math.max(1, intParameter(parameter, quest.getTargetAmount()));
            default -> Math.max(1, quest.getTargetAmount());
        };
    }
    private int assignmentReward(Quest quest, String parameter) {
        int value = intParameter(parameter, 0);
        return switch (quest.getEventType()) {
            case SUN_COLLECTED -> Math.max(0, value / 100);
            case WIN_MAX_PLANTS_LOST -> Math.max(0, quest.getRewardAmount() - value);
            case MOWER_KILLS -> Math.max(0, value);
            default -> Math.max(0, quest.getRewardAmount());
        };
    }
    public List<QuestsRepository.QuestEntry> getPage(User user, QuestType type) {
        if (user == null) {
            throw new IllegalStateException("A logged-in user is required.");
        }
        initializeForUser(user.getId());
        return repository.getQuestEntries(user.getId(), type);
    }
    public void recordSunCollected(User user, int amount) {
        if (user == null || amount <= 0) {
            return;
        }
        initializeForUser(user.getId());
        for (Quest quest : repository.getAllQuests()) {
            if (quest.getEventType() != QuestEventType.SUN_COLLECTED) {
                continue;
            }
            UserQuest assignment = repository.getUserQuest(user.getId(), quest.getId())
                    .orElse(null);
            if (canProgress(assignment)) {
                boolean newlyCompleted = repository.addProgress(user.getId(), quest.getId(), amount, quest.getType()
                );

                if (newlyCompleted) {
                    incrementQuestCounter(user, quest.getType());
                }
            }
        }
    }
    public void evaluateAdventureRun(
            User user,
            GameState state,
            ChapterTheme chapter,
            int difficultyLevel,
            boolean won
    ) {
        if (user == null || state == null || chapter == null
                || chapter == ChapterTheme.MINIGAME) {
            return;
        }
        initializeForUser(user.getId());
        QuestRunTracker tracker = state.getQuestTracker();
        for (Quest quest : repository.getAllQuests()) {
            UserQuest assignment = repository.getUserQuest(user.getId(), quest.getId())
                    .orElse(null);
            if (!canProgress(assignment)) {
                continue;
            }
            if (quest.getEventType() == QuestEventType.MAX_DIFFICULTY_WIN_STREAK) {
                int next = won && difficultyLevel == 5 ? assignment.getProgress() + 1 : 0;
                boolean newlyCompleted = repository.setProgress(user.getId(), quest.getId(), next, quest.getType());
                if (newlyCompleted) {
                    incrementQuestCounter(user, quest.getType());
                }
                continue;
            }
            int amount = evaluate(quest, assignment, tracker, state, chapter, won);
            if (amount > 0) {
                boolean newlyCompleted = repository.addProgress(user.getId(), quest.getId(), amount, quest.getType());
                if (newlyCompleted) {
                    incrementQuestCounter(user, quest.getType());
                }
            }
        }
    }
    private void incrementQuestCounter(User user, QuestType questType) {
        if (questType == QuestType.DAILY) {
            user.setQuestDailyNum(user.getQuestDailyNum() + 1);
        } else {
            user.setQuestNonDailyNum(user.getQuestNonDailyNum() + 1);
        }
    }
    private boolean canProgress(UserQuest assignment) {
        return assignment != null && !assignment.isClaimed() && !assignment.isCompleted();
    }
    private int evaluate(
            Quest quest,
            UserQuest assignment,
            QuestRunTracker tracker,
            GameState state,
            ChapterTheme chapter,
            boolean won
    ) {
        String parameter = assignment.getParameter();
        int target = assignment.getTargetAmount();
        return switch (quest.getEventType()) {
            case SUN_COLLECTED -> 0;
            case CHAPTER_ZOMBIE_KILLS -> parameterMatchesChapter(parameter, chapter)
                    ? tracker.getTotalKills() : 0;
            case PLANT_ONLY_KILLS -> tracker.getNonPlantKills() == 0
                    ? Math.min(target, tracker.getPlantKills()) : 0;
            case SPECIFIC_PLANT_KILLS -> tracker.getNonPlantKills() == 0
                    && tracker.onlyPlantKillersByName(parameter)
                    ? Math.min(target, tracker.getKillsByPlantName(parameter)) : 0;
            case WIN_MAX_PLANTS_LOST -> won
                    && tracker.getPlantsLost() <= intParameter(parameter, 0) ? target : 0;
            case WIN_EXACT_SUN -> won
                    && state.getSun() == intParameter(parameter, 0) ? target : 0;
            case FAST_KILLS -> tracker.getFastKills(
                    intParameter(parameter, 30), state.getTicksPerSecond()) >= target
                    ? target : 0;
            case EXPLOSIVE_PLANTS_USED -> tracker.getExplosivePlantsUsed() >= target
                    ? target : 0;
            case FINISH_SYMMETRIC -> tracker.isSymmetric(state.getBoard()) ? target : 0;
            case ONLY_FAMILY_KILLS -> tracker.getNonPlantKills() == 0
                    && tracker.onlyPlantKillersFromFamily(parameter)
                    ? target : 0;
            case WIN_WITHOUT_FAMILY -> won && !tracker.usedFamily(parameter) ? target : 0;
            case WIN_DAY_WITH_NIGHT_PLANTS -> won && tracker.isDayLevel(chapter)
                    && tracker.usedOnlyNightPlants() ? target : 0;
            case MAX_DIFFICULTY_WIN_STREAK -> 0;
            case FIRST_COLUMN_KILLS_NO_MOWER ->
                    tracker.getFirstColumnKillsWithoutMower() >= target ? target : 0;
            case WIN_ASYMMETRIC_EXCEPT_MIDDLE -> won
                    && tracker.isAsymmetricExceptMiddle(state.getBoard()) ? target : 0;
            case WIN_ONLY_SUN_PRODUCERS_EXACT_COUNT -> won
                    && tracker.usedOnlySunProducersExactly(intParameter(parameter, 3))
                    ? target : 0;
            case WIN_EMPTY_COLUMN -> won
                    && tracker.neverUsedColumn(intParameter(parameter, 1)) ? target : 0;
            case WIN_EMPTY_ROW -> won
                    && tracker.neverUsedRow(intParameter(parameter, 1)) ? target : 0;
            case WIN_EMPTY_CROSS -> {
                int[] cross = crossParameter(parameter);
                yield won && tracker.neverUsedRow(cross[0])
                        && tracker.neverUsedColumn(cross[1]) ? target : 0;
            }
            case MOWER_KILLS -> Math.min(target, tracker.getMowerKills());
        };
    }
    public String claimReward(User user, int questId) {
        if (user == null) {
            throw new IllegalStateException("A logged-in user is required.");
        }
        initializeForUser(user.getId());
        Quest quest = repository.getQuest(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found."));
        UserQuest assignment = repository.getUserQuest(user.getId(), questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest is not assigned."));
        if (!assignment.isCompleted()) {
            throw new IllegalStateException("Quest is not complete yet.");
        }
        if (assignment.isClaimed()) {
            throw new IllegalStateException("Quest reward was already claimed.");
        }
        int oldCoins = user.getCoins();
        int oldGems = user.getGems();
        int oldDaily = user.getQuestDailyNum();
        int oldNonDaily = user.getQuestNonDailyNum();
        try (Connection connection = DataBaseManager.getConnection()) {
            QuestDatabaseMigration.migrate(connection);
            connection.setAutoCommit(false);
            try {
                String reward = applyReward(connection, user, quest, assignment);
                if (!repository.markClaimed(connection, user.getId(), questId)) {
                    throw new IllegalStateException("Quest reward was already claimed.");
                }
                connection.commit();
                connection.commit();
                return reward;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                restoreUser(user, oldCoins, oldGems, oldDaily, oldNonDaily);
                throw exception;
            }
        } catch (SQLException exception) {
            restoreUser(user, oldCoins, oldGems, oldDaily, oldNonDaily);
            throw new IllegalStateException("Could not claim quest reward.", exception);
        }
    }
    public String resolvedCondition(Quest quest, UserQuest assignment) {
        String parameter = assignment.getParameter();
        if (parameter == null || parameter.isBlank()) {
            return quest.getCondition();
        }
        int[] cross = crossParameter(parameter);
        return quest.getCondition()
                .replace("{parameter}", parameter)
                .replace("{plant}", parameter)
                .replace("{family}", displayFamily(parameter))
                .replace("{chapter}", displayChapter(parameter))
                .replace("{row}", Integer.toString(cross[0]))
                .replace("{column}", Integer.toString(cross[1]));
    }
    private String applyReward(
            Connection connection, User user, Quest quest, UserQuest assignment
    ) throws SQLException {
        int amount = assignment.getRewardAmount();
        return switch (quest.getRewardType()) {
            case CURRENCY_COINS -> {
                updateCurrency(connection, user.getId(), "coins", amount);
                user.setCoins(user.getCoins() + amount);
                yield amount + " coins";
            }
            case CURRENCY_GEMS -> {
                updateCurrency(connection, user.getId(), "gems", amount);
                user.setGems(user.getGems() + amount);
                yield amount + " gems";
            }
            case INVENTORY -> {
                int plantId = resolveInventoryPlant(user.getId(), quest.getUnlockableId());
                addSeedPackets(connection, user.getId(), plantId, amount);
                yield amount + " seed packets for " + plantLabel(plantId);
            }
            case UNLOCKABLE -> {
                int plantId = resolveUnlockPlant(user.getId(), quest.getUnlockableId());
                unlockPlant(connection, user.getId(), plantId);
                yield plantLabel(plantId) + " unlocked";
            }
        };
    }
    private String chooseParameter(int userId, Quest quest) {
        String options = quest.getParameterOptions();
        if (options == null || options.isBlank()) {
            return null;
        }
        return switch (options.trim().toUpperCase(Locale.ROOT)) {
            case "@CHAPTER" -> randomChapter();
            case "@FAMILY" -> randomFamily(
                    userId, quest.getEventType() == QuestEventType.ONLY_FAMILY_KILLS);
            case "@KILLING_PLANT" -> randomKillingPlant(userId);
            case "@ROW" -> Integer.toString(random.nextInt(5) + 1);
            case "@COLUMN" -> Integer.toString(random.nextInt(9) + 1);
            case "@CROSS" -> Integer.toString(random.nextInt(5) + 1);
            default -> randomOption(options);
        };
    }
    private String randomChapter() {
        List<ChapterTheme> chapters = List.of(
                ChapterTheme.ANCIENT_EGYPT,
                ChapterTheme.FROSTBITE_CAVES,
                ChapterTheme.BIG_WAVE_BEACH,
                ChapterTheme.DARK_AGES);
        return chapters.get(random.nextInt(chapters.size())).name();
    }
    private String randomKillingPlant(int userId) {
        List<PlantData> allKillingPlants = PlantRegistry.getAll().stream()
                .filter(plant -> plant.damage() > 0)
                .sorted((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(
                        first.name(), second.name()))
                .toList();
        if (allKillingPlants.isEmpty()) {
            throw new IllegalStateException("No killing plant exists for this quest.");
        }

        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(userId);
        List<PlantData> unlockedKillingPlants = allKillingPlants.stream()
                .filter(plant -> unlocked.contains(plant.id()))
                .toList();

        if (!unlockedKillingPlants.isEmpty()) {
            return unlockedKillingPlants.get(
                    random.nextInt(unlockedKillingPlants.size())).name();
        }
        PlantData starterKiller = PlantRegistry.getById(1);
        if (starterKiller != null && starterKiller.damage() > 0) {
            return starterKiller.name();
        }
        return allKillingPlants.get(random.nextInt(allKillingPlants.size())).name();
    }

    private boolean parameterNeedsRepair(Quest quest, String parameter) {
        String options = quest.getParameterOptions();
        if (options == null || options.isBlank()) {
            return false;
        }
        if (parameter == null || parameter.isBlank()) {
            return true;
        }

        return switch (options.trim().toUpperCase(Locale.ROOT)) {
            case "@CROSS" -> !parameter.trim().matches("[1-5]");
            case "@KILLING_PLANT" -> {
                PlantData plant = PlantRegistry.getByName(parameter);
                yield plant == null || plant.damage() <= 0;
            }
            default -> false;
        };
    }
    private String randomFamily(int userId, boolean killingOnly) {
        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(userId);
        Set<String> families = collectFamilies(unlocked, killingOnly);
        if (families.isEmpty()) {
            families = collectFamilies(Set.of(), killingOnly);
        }
        if (families.isEmpty()) {
            return "Shooter";
        }
        List<String> values = new ArrayList<>(families);
        return values.get(random.nextInt(values.size()));
    }
    private Set<String> collectFamilies(Set<Integer> allowedIds, boolean killingOnly) {
        Set<String> families = new LinkedHashSet<>();
        PlantRegistry.getAll().stream()
                .filter(plant -> allowedIds.isEmpty() || allowedIds.contains(plant.id()))
                .filter(plant -> !killingOnly || plant.damage() > 0)
                .map(PlantData::category)
                .filter(value -> value != null && !value.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(families::add);
        return families;
    }
    private String randomOption(String options) {
        List<String> values = new ArrayList<>();
        for (String value : options.split("\\|")) {
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }
        return values.isEmpty() ? null : values.get(random.nextInt(values.size()));
    }
    private int resolveInventoryPlant(int userId, String target) {
        Integer explicit = resolveExplicitPlantId(target);
        if (explicit != null) {
            if (!PlantRepository.loadUnlockedPlants(userId).contains(explicit)) {
                throw new IllegalStateException("The reward plant is not unlocked.");
            }
            return explicit;
        }
        List<Integer> candidates = PlantRepository.loadUnlockedPlants(userId).stream()
                .filter(PlantRegistry::contains)
                .sorted()
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No unlocked plant exists for this reward.");
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
    private int resolveUnlockPlant(int userId, String target) {
        Integer explicit = resolveExplicitPlantId(target);
        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(userId);
        if (explicit != null) {
            if (unlocked.contains(explicit)) {
                throw new IllegalStateException("That plant is already unlocked.");
            }
            return explicit;
        }
        List<Integer> candidates = PlantRegistry.getAll().stream()
                .map(PlantData::id)
                .filter(id -> !unlocked.contains(id))
                .sorted()
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("All plants are already unlocked.");
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
    private Integer resolveExplicitPlantId(String target) {
        if (target == null || target.isBlank()
                || target.equalsIgnoreCase("NONE")
                || target.equalsIgnoreCase("any_plant")
                || target.equalsIgnoreCase("RANDOM_UNLOCKED_PLANT")
                || target.equalsIgnoreCase("RANDOM_LOCKED_PLANT")) {
            return null;
        }
        if (target.matches("\\d+")) {
            int id = Integer.parseInt(target);
            if (!PlantRegistry.contains(id)) {
                throw new IllegalStateException("Unknown reward plant #" + id + ".");
            }
            return id;
        }
        PlantData plant = PlantRegistry.getByName(target);
        if (plant == null) {
            throw new IllegalStateException("Unknown reward plant: " + target + ".");
        }
        return plant.id();
    }
    private void updateCurrency(Connection connection, int userId, String column, int amount)
            throws SQLException {
        if (!column.equals("coins") && !column.equals("gems")) {
            throw new IllegalArgumentException("Unsupported currency.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET " + column + " = " + column + " + ? WHERE id = ?")) {
            statement.setInt(1, amount);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }
    private void addSeedPackets(Connection connection, int userId, int plantId, int amount)
            throws SQLException {
        String sql = """
                INSERT INTO user_plants(user_id, plant_id, plant_level, seed_packets)
                VALUES (?, ?, 1, ?)
                ON CONFLICT(user_id, plant_id)
                DO UPDATE SET seed_packets = seed_packets + excluded.seed_packets
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, plantId);
            statement.setInt(3, amount);
            statement.executeUpdate();
        }
    }
    private void unlockPlant(Connection connection, int userId, int plantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO user_unlocked_plants(user_id, plant_id) VALUES (?, ?)")) {
            statement.setInt(1, userId);
            statement.setInt(2, plantId);
            statement.executeUpdate();
        }
    }

    private void restoreUser(User user, int coins, int gems, int daily, int nonDaily) {
        user.setCoins(coins);
        user.setGems(gems);
        user.setQuestDailyNum(daily);
        user.setQuestNonDailyNum(nonDaily);
    }
    private boolean parameterMatchesChapter(String parameter, ChapterTheme chapter) {
        if (parameter == null || parameter.equalsIgnoreCase("ANY")) {return true;}
        return chapter.name().equalsIgnoreCase(parameter)
                || chapter.getName().equalsIgnoreCase(parameter.replace('_', ' '));
    }
    private int intParameter(String parameter, int fallback) {
        if (parameter == null || parameter.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(parameter.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
    private int[] crossParameter(String parameter) {
        if (parameter != null && parameter.contains(",")) {
            String[] values = parameter.split(",", 2);
            return new int[]{intParameter(values[0], 1), intParameter(values[1], 1)};
        }
        int value = intParameter(parameter, 1);
        return new int[]{value, value};
    }
    private String displayFamily(String family) {
        return family.replaceAll("([a-z])([A-Z])", "$1 $2");
    }
    private String displayChapter(String chapter) {
        try {
            return ChapterTheme.valueOf(chapter).getName();
        } catch (IllegalArgumentException exception) {
            return chapter.replace('_', ' ');
        }
    }
    private String plantLabel(int plantId) {
        PlantData plant = PlantRegistry.getById(plantId);
        return plant == null ? "plant #" + plantId : plant.name();
    }
}
