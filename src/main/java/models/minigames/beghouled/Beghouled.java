package models.minigames.beghouled;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import lombok.Getter;
import lombok.Setter;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantFactory;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;
import models.games.ZombieWaveManager;
import models.minigames.MinigameStage;
import models.minigames.MinigameType;
import models.quests.QuestKillSourceType;

import java.util.*;
@Getter
@Setter
public class Beghouled extends Game {
    private static final int TICKS_PER_SECOND = 10;
    private static final int FIRST_WAVE_DELAY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int MAX_WAVE_BUDGET = 3000;
    private static final int MAX_BOARD_CREATING_ATTEMPTS = 100;
    private static final int MAX_NEW_CASCADE_CHECK= 50;
    private static final int SUN_PER_UNIT = 50;
    private final MinigameStage stage;
    private final StageConfig config;
    private final Random random;
    private int completedMatches;
    private boolean zombieReachedHouse;

    public Beghouled(int stageNumber) {
        this(stageNumber, new Random());
    }

    public Beghouled(int stageNumber, Random random) {
        this.stage = findStage(stageNumber);
        this.config = StageConfig.forStage(stageNumber);
        this.random = Objects.requireNonNull(random, "Random cannot be null.");
    }

    private static MinigameStage findStage(int stageNumber) {
        return MinigameStage.getStages(MinigameType.BEGHOULDED).stream()
                .filter(candidate -> candidate.getStageNumber() == stageNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Beghouled stage must be 1, 2, or 3."
                ));
    }

    @Override
    public void loadLevel() {
        registriesReady();
        Board board = new Board();
        GameState state = new GameState(board, ChapterTheme.MINIGAME, true);
        state.setSun(0);
        setGameState(state);
        setSkySunSpawner(null);
        ZombieWaveManager waveManager =
        ZombieWaveManager.endless(state, config.zombieTypes(), config.baseWaveBudget(), MAX_WAVE_BUDGET, random);
        waveManager.setFirstWaveDelayTicks(FIRST_WAVE_DELAY_TICKS);
        state.setZombieWaveManager(waveManager);
        completedMatches = 0;
        zombieReachedHouse = false;
        if (!generatePlayableBoard()) {
            throw new IllegalStateException(
                    "Could not generate a Beghouled board with at least one proper move."
            );
        }
    }

    private void registriesReady() {
        if (PlantRegistry.getAll().isEmpty()) {
            throw new IllegalStateException("PlantRegistry is empty.");
        }
        if (ZombieRegistry.getTemplates().isEmpty()) {
            throw new IllegalStateException("ZombieRegistry is empty.");
        }
        for (String plantName : config.basePlantNames()) {
            if (PlantRegistry.getByName(plantName) == null) {
                throw new IllegalStateException(" Beghouled plant is missing: " + plantName);
            }
        }
        for (UpgradeRule rule : config.upgrades()) {
            if (PlantRegistry.getByName(rule.fromPlant()) == null
                    || PlantRegistry.getByName(rule.toPlant()) == null) {
                throw new IllegalStateException(
                        "Beghouled upgrade plant is missing: " + rule.fromPlant() + " -> " + rule.toPlant());
            }
        }
        for (ZombieType zombieType : config.zombieTypes()) {
            String alias = zombieType.getAlias();
            if (ZombieRegistry.getTemplate(alias) == null) {
                throw new IllegalStateException("Beghouled zombie is missing: " + alias);
            }
        }
    }

    @Override
    public void start() {}

    @Override
    public void onTick() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) {
            return;
        }
        state.addTick(1);
        state.getZombieWaveManager().onTick();
        state.getBoard().tickPlants(state);
        state.getBoard().tickProjectiles(state);
        tickZombiesAndCreateCraters();
        state.getBoard().tickLoots(state);
        state.tickMowers();
        state.getBoard().tickSuns(state);
        if (state.checkLoseCondition()) {
            zombieReachedHouse = true;
            finishAsLoss();
            return;
        }

        if (!hasAnyLegalSwap() && findMatchGroups().isEmpty()) {
            resetBoardBecauseNoMoveExists();
        }
    }

    @Override
    public void forward(int requestedTicks) {
        if (requestedTicks < 0) {
            throw new IllegalArgumentException("Tick count cannot be negative.");
        }
        for (int i = 0;
             i < requestedTicks && !getGameState().isFinished();
             i++) {
            onTick();
        }
    }

    public SwapOutcome swapPlants(int firstX, int firstY, int secondX, int secondY) {
        ensureRunning();
        Tile first = userTile(firstX, firstY);
        Tile second = userTile(secondX, secondY);

        if (first.isCrater() || second.isCrater()) {
            throw new IllegalStateException("A crater cannot contain or swap a plant.");
        }
        if (!first.hasTopPlant() || !second.hasTopPlant()) {
            throw new IllegalStateException("Both selected tiles must contain a plant.");
        }
        int distance = Math.abs(first.getLane() - second.getLane())
                + Math.abs(first.getColumn() - second.getColumn());
        if (distance != 1) {
            throw new IllegalArgumentException(
                    "Only orthogonally adjacent plants can be swapped."
            );
        }

        swapPlantsInternal(first, second);
        List<MatchGroup> initialGroups = findMatchGroups();
        if (!groupsTouchEitherTile(initialGroups, first, second)) {
            swapPlantsInternal(first, second);
            throw new IllegalStateException(
                    "That swap does not create a match of three or more."
            );
        }

        return resolveMatches(initialGroups);
    }

    public UpgradeOutcome upgradePlants(String fromPlant, String toPlant) {
        ensureRunning();
        PlantData from = resolvePlant(fromPlant);
        PlantData to = resolvePlant(toPlant);
        UpgradeRule rule = findUpgradeRule(from, to);
        if (rule == null) {
            throw new IllegalArgumentException("This upgrade is not available in Beghouled stage "
                            + stage.getStageNumber() + ".");}
        GameState state = getGameState();
        if (state.getSun() < rule.cost()) {
            throw new IllegalStateException(
                    "Not enough sun. This upgrade costs " + rule.cost() + " sun, but you have " + state.getSun() + ".");
        }
        List<Tile> targets = new ArrayList<>();
        Board board = state.getBoard();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                Plant plant = tile.getTopPlant();
                if (plant != null && plant.getId() == from.id()) {
                    targets.add(tile);
                }
            }
        }
        if (targets.isEmpty()) {
            throw new IllegalStateException(
                    "There are no " + from.name() + " plants on the board.");
        }
        state.decreaseSunBalance(rule.cost());
        for (Tile tile : targets) {
            Plant oldPlant = tile.getTopPlant();
            if (oldPlant != null) {
                tile.removeSpecificPlant(oldPlant);
                oldPlant.setMarkedForRemoval(true);
            }
            placeFreshPlant(tile, to.name());
        }
        state.logEvent(
        "Upgraded " + targets.size() + " " + from.name() + " plant(s) to " + to.name() + " for " + rule.cost() +
                " sun.\n"
        );
        List<MatchGroup> groups = findMatchGroups();
        if (!groups.isEmpty()) {
            resolveMatches(groups);
        } else if (!hasAnyLegalSwap()) {
            resetBoardBecauseNoMoveExists();}
        return new UpgradeOutcome(from.name(), to.name(), targets.size(), rule.cost(), state.getSun());
    }

    private SwapOutcome resolveMatches(List<MatchGroup> firstGroups) {
        int directMatches = 0;
        int cascadeMatches = 0;
        int totalSunGained = 0;
        boolean cascade = false;
        int rounds = 0;
        List<MatchGroup> groups = firstGroups;

        while (!groups.isEmpty() && rounds < MAX_NEW_CASCADE_CHECK) {
            rounds++;
            int gainedThisRound = scoreAndLog(groups, cascade);
            totalSunGained += gainedThisRound;
            getGameState().increaseSunBalance(gainedThisRound);
            completedMatches += groups.size();

            if (cascade) {
                cascadeMatches += groups.size();
            } else {
                directMatches += groups.size();
            }

            removeMatchedPlants(groups);
            collapseAndRefill();
            cascade = true;
            groups = findMatchGroups();
        }

        if (rounds >= MAX_NEW_CASCADE_CHECK && !groups.isEmpty()) {
            getGameState().logEvent(
                    "Cascade safety limit reached; the board was reshuffled.\n"
            );
            generatePlayableBoard();
        }

        checkWinCondition();

        boolean boardReset = false;
        if (!getGameState().isFinished() && !hasAnyLegalSwap()) {
            boardReset = resetBoardBecauseNoMoveExists();
        }

        return new SwapOutcome(
                directMatches,
                cascadeMatches,
                totalSunGained,
                completedMatches,
                boardReset
        );
    }

    private int scoreAndLog(List<MatchGroup> groups, boolean cascade) {
        int total = 0;
        StringBuilder message = new StringBuilder();
        for (MatchGroup group : groups) {
            Tile firstTile = group.tiles().get(0);
            Plant plant = firstTile.getTopPlant();
            String plantName = plant == null ? "plant" : plant.getName();
            int units = group.tiles().size() - 2;
            if (cascade) {
                units++;
            }
            int gained = units * SUN_PER_UNIT;
            total += gained;
            message.append(cascade ? "Cascade " : "Match ")
                    .append(group.tiles().size())
                    .append(" x ")
                    .append(plantName)
                    .append(" gave ")
                    .append(gained)
                    .append(" sun.\n");
        }
        getGameState().logEvent(message.toString());
        return total;
    }

    private void removeMatchedPlants(List<MatchGroup> groups) {
        Set<Tile> uniqueTiles = new LinkedHashSet<>();
        for (MatchGroup group : groups) {
            uniqueTiles.addAll(group.tiles());
        }
        for (Tile tile : uniqueTiles) {
            Plant plant = tile.getTopPlant();
            if (plant != null) {
                tile.removeSpecificPlant(plant);
                plant.setMarkedForRemoval(true);
            }
        }
    }


    private void collapseAndRefill() {
        Board board = getGameState().getBoard();
        for (int column = 0; column < board.getColumnCount(); column++) {
            List<Tile> playableBottomToTop = new ArrayList<>();
            List<Plant> survivorsBottomToTop = new ArrayList<>();

            for (int lane = board.getLaneCount() - 1; lane >= 0; lane--) {
                Tile tile = board.getTile(lane, column);
                if (tile.isCrater()) {
                    continue;
                }
                playableBottomToTop.add(tile);
                if (tile.getTopPlant() != null) {
                    survivorsBottomToTop.add(tile.getTopPlant());
                }
            }

            for (Tile tile : playableBottomToTop) {
                Plant current = tile.getTopPlant();
                if (current != null) {
                    tile.removeSpecificPlant(current);
                }
            }

            int index = 0;
            for (; index < survivorsBottomToTop.size(); index++) {
                placeExistingPlant(
                        playableBottomToTop.get(index),
                        survivorsBottomToTop.get(index)
                );
            }
            for (; index < playableBottomToTop.size(); index++) {
                placeFreshPlant(
                        playableBottomToTop.get(index),
                        randomBasePlantName()
                );
            }
        }
    }

    private boolean generatePlayableBoard() {
        for (int attempt = 0;
             attempt < MAX_BOARD_CREATING_ATTEMPTS;
             attempt++) {
            clearTopPlants();
            if (!fillWithoutImmediateMatches()) {
                continue;
            }
            if (findMatchGroups().isEmpty() && hasAnyLegalSwap()) {
                return true;
            }
        }
        return false;
    }

    private boolean fillWithoutImmediateMatches() {
        Board board = getGameState().getBoard();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                if (tile.isCrater()) {
                    continue;
                }
                List<String> candidates = new ArrayList<>(config.basePlantNames());
                Collections.shuffle(candidates, random);
                String chosen = null;
                for (String candidate : candidates) {
                    PlantData data = PlantRegistry.getByName(candidate);
                    if (data != null
                            && !doesCreateImmediateMatch(lane, column, data.id())) {
                        chosen = data.name();
                        break;
                    }
                }
                if (chosen == null) {
                    return false;
                }
                placeFreshPlant(tile, chosen);
            }
        }
        return true;
    }

    private boolean doesCreateImmediateMatch(int lane, int column, int plantId) {
        Board board = getGameState().getBoard();
        if (column >= 2) {
            Plant leftOne = board.getTile(lane, column - 1).getTopPlant();
            Plant leftTwo = board.getTile(lane, column - 2).getTopPlant();
            if (leftOne != null && leftTwo != null
                    && leftOne.getId() == plantId
                    && leftTwo.getId() == plantId) {
                return true;
            }
        }
        if (lane >= 2) {
            Plant upOne = board.getTile(lane - 1, column).getTopPlant();
            Plant upTwo = board.getTile(lane - 2, column).getTopPlant();
            return upOne != null && upTwo != null
                    && upOne.getId() == plantId
                    && upTwo.getId() == plantId;
        }
        return false;
    }

    private void clearTopPlants() {
        Board board = getGameState().getBoard();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                Plant plant = tile.getTopPlant();
                if (plant != null) {
                    tile.removeSpecificPlant(plant);
                    plant.setMarkedForRemoval(true);
                }
            }
        }
    }

    private void placeFreshPlant(Tile tile, String plantName) {
        Plant plant = PlantFactory.create(plantName);
        placeExistingPlant(tile, plant);
        plant.getPlantType().onPlanted(plant, getGameState());
    }

    private void placeExistingPlant(Tile tile, Plant plant) {
        plant.setPosX(tile.getColumn());
        plant.setPosY(tile.getLane());
        tile.setPlant(plant);
    }

    private String randomBasePlantName() {
        List<String> names = config.basePlantNames();
        return names.get(random.nextInt(names.size()));
    }

    private List<MatchGroup> findMatchGroups() {
        Board board = getGameState().getBoard();
        List<MatchGroup> groups = new ArrayList<>();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            int column = 0;
            while (column < board.getColumnCount()) {
                Tile start = board.getTile(lane, column);
                Plant plant = start.isCrater() ? null : start.getTopPlant();
                if (plant == null) {column++;continue;}
                int id = plant.getId();
                int end = column + 1;
                while (end < board.getColumnCount()) {
                    Tile next = board.getTile(lane, end);
                    Plant nextPlant = next.isCrater() ? null : next.getTopPlant();
                    if (nextPlant == null || nextPlant.getId() != id) {break;}
                    end++;}
                if (end - column >= 3) {
                    List<Tile> tiles = new ArrayList<>();
                    for (int current = column; current < end; current++) {
                        tiles.add(board.getTile(lane, current));}
                    groups.add(new MatchGroup(List.copyOf(tiles)));}
                column = end;
            }
        }
        for (int column = 0; column < board.getColumnCount(); column++) {
            int lane = 0;
            while (lane < board.getLaneCount()) {
                Tile start = board.getTile(lane, column);
                Plant plant = start.isCrater() ? null : start.getTopPlant();
                if (plant == null) {
                    lane++;
                    continue;}
                int id = plant.getId();
                int end = lane + 1;
                while (end < board.getLaneCount()) {
                    Tile next = board.getTile(end, column);
                    Plant nextPlant = next.isCrater() ? null : next.getTopPlant();
                    if (nextPlant == null || nextPlant.getId() != id) {break;}
                    end++;}
                if (end - lane >= 3) {
                    List<Tile> tiles = new ArrayList<>();
                    for (int current = lane; current < end; current++) {
                        tiles.add(board.getTile(current, column));}
                    groups.add(new MatchGroup(List.copyOf(tiles)));}
                lane = end;
            }
        }
        return groups;
    }

    private boolean groupsTouchEitherTile(List<MatchGroup> groups, Tile first, Tile second
    ) {
        return groups.stream().anyMatch(group ->
                group.tiles().contains(first) || group.tiles().contains(second)
        );
    }

    public boolean hasAnyLegalSwap() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) {
            return false;
        }
        Board board = state.getBoard();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile current = board.getTile(lane, column);
                if (current.isCrater() || !current.hasTopPlant()) {
                    continue;
                }
                if (column + 1 < board.getColumnCount()) {
                    Tile right = board.getTile(lane, column + 1);
                    if (isLegalSwap(current, right)) {
                        return true;
                    }
                }
                if (lane + 1 < board.getLaneCount()) {
                    Tile down = board.getTile(lane + 1, column);
                    if (isLegalSwap(current, down)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isLegalSwap(Tile first, Tile second) {
        if (second == null || second.isCrater() || !second.hasTopPlant()) {
            return false;
        }
        swapPlantsInternal(first, second);
        List<MatchGroup> groups = findMatchGroups();
        boolean legal = groupsTouchEitherTile(groups, first, second);
        swapPlantsInternal(first, second);
        return legal;
    }

    private void swapPlantsInternal(Tile first, Tile second) {
        Plant firstPlant = first.getTopPlant();
        Plant secondPlant = second.getTopPlant();
        first.setPlant(secondPlant);
        second.setPlant(firstPlant);
        if (secondPlant != null) {
            secondPlant.setPosX(first.getColumn());
            secondPlant.setPosY(first.getLane());
        }
        if (firstPlant != null) {
            firstPlant.setPosX(second.getColumn());
            firstPlant.setPosY(second.getLane());
        }
    }

    private boolean resetBoardBecauseNoMoveExists() {
        getGameState().logEvent(
                "No legal match-producing swap remained; the board was reset.\n"
        );
        return generatePlayableBoard();
    }

    private void tickZombiesAndCreateCraters() {
        GameState state = getGameState();
        Board board = state.getBoard();
        Map<Tile, Plant> beforeZombieAttacks = new LinkedHashMap<>();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Plant plant = board.getTile(lane, column).getTopPlant();
                if (plant != null) {
                    beforeZombieAttacks.put(board.getTile(lane, column), plant);
                }
            }
        }
        for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
            zombie.onTick(state);
        }
        for (Map.Entry<Tile, Plant> entry : beforeZombieAttacks.entrySet()) {
            Tile tile = entry.getKey();
            Plant previousPlant = entry.getValue();
            if (tile.getTopPlant() == null && !tile.isCrater() && (previousPlant.isDead()
                    || previousPlant.isMarkedForRemoval())) {
                tile.setCrater(true);
                state.logEvent("A crater created at (" + (tile.getColumn() + 1) + ", " + (tile.getLane() + 1) + ").\n");
            }
        }
    }
    private void checkWinCondition() {
        GameState state = getGameState();
        if (state.isFinished() || completedMatches < config.targetMatches()) {
            return;
        }
        for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
            zombie.killInstantlyWithoutLoot(
                state, QuestKillSourceType.OTHER
            );
        }
        state.setFinished(true);
        state.setWon(true);
        state.logEvent(
                "Target reached: " + completedMatches + "/"
                        + config.targetMatches()
                        + " matches. Every zombie was destroyed; YOU WIN!\n"
        );
    }

    private void finishAsLoss() {
        GameState state = getGameState();
        state.setFinished(true);
        state.setWon(false);
    }

    private void ensureRunning() {
        if (getGameState() == null) {
            throw new IllegalStateException("No Beghouled level is loaded.");
        }
        if (getGameState().isFinished()) {
            throw new IllegalStateException("The Beghouled level has already ended.");
        }
    }

    private Tile userTile(int x, int y) {
        Tile tile = getGameState().getBoard().getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null) {
            throw new IllegalArgumentException("Coordinates are outside the map.");
        }
        return tile;
    }

    private PlantData resolvePlant(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            throw new IllegalArgumentException("Plant name cannot be empty.");
        }
        PlantData data = PlantRegistry.getByName(plantName.trim());
        if (data == null) {
            throw new IllegalArgumentException("Unknown plant: " + plantName.trim());
        }
        return data;
    }

    private UpgradeRule findUpgradeRule(PlantData from, PlantData to) {
        return config.upgrades().stream()
                .filter(rule -> rule.fromPlant().equalsIgnoreCase(from.name()))
                .filter(rule -> rule.toPlant().equalsIgnoreCase(to.name()))
                .findFirst()
                .orElse(null);
    }
    public int getTargetMatches() {
        return config.targetMatches();
    }
    public int getLivingZombieCount() {
        if (getGameState() == null) {
            return 0;
        }
        return (int) getGameState().getZombiesInTheGame().stream()
                .filter(zombie -> !zombie.isDead())
                .count();
    }

    public int getCraterCount() {
        if (getGameState() == null) {
            return 0;
        }
        Board board = getGameState().getBoard();
        int count = 0;
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                if (board.getTile(lane, column).isCrater()) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<UpgradeRule> getAvailableUpgrades() {
        return config.upgrades();
    }

    public Map<String, Integer> getPlantCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (getGameState() == null) {
            return counts;
        }
        getGameState().getBoard().getAllPlants().stream()
                .sorted(Comparator.comparing(Plant::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(plant -> counts.merge(plant.getName(), 1, Integer::sum));
        return counts;
    }

    public record SwapOutcome(
            int directMatches, int cascadeMatches, int sunGained, int totalMatches, boolean boardReset) {
    }
    public record UpgradeOutcome(
            String fromPlant, String toPlant, int transformedCount, int cost, int remainingSun) {
    }

    public record UpgradeRule(String fromPlant, String toPlant, int cost) {
        public UpgradeRule {
            if (fromPlant == null || fromPlant.isBlank() || toPlant == null || toPlant.isBlank()) {
                throw new IllegalArgumentException("Upgrade plant names are required.");
            }
            if (cost < 0) {
                throw new IllegalArgumentException("Upgrade cost cannot be negative.");
            }
        }
    }

    private record MatchGroup(List<Tile> tiles) {
        private MatchGroup {
            tiles = List.copyOf(tiles);
    }


    }
    private record StageConfig(int targetMatches, float baseWaveBudget,
            List<String> basePlantNames, List<UpgradeRule> upgrades, List<ZombieType> zombieTypes) {
        private StageConfig {
            basePlantNames = List.copyOf(basePlantNames);
            upgrades = List.copyOf(upgrades);
            zombieTypes = List.copyOf(zombieTypes);
        }

        private static StageConfig forStage(int stageNumber) {
            return switch (stageNumber) {
                case 1 -> new StageConfig(10,
                        300f,
                        List.of("Peashooter", "Wall-nut", "Puff-shroom", "Cabbage-pult", "Bonk Choy"),
                        List.of(
                                new UpgradeRule("Peashooter", "Repeater", 250),
                                new UpgradeRule("Wall-nut", "Tall-nut", 250),
                                new UpgradeRule("Puff-shroom", "Fume-shroom", 150)
                        ),
                        List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1, ZombieType.IMP)
                );
                case 2 -> new StageConfig(15,
                        450f,
                        List.of(
                                "Peashooter", "Repeater", "Wall-nut", "Cabbage-pult", "Snow Pea"
                        ),
                        List.of(
                                new UpgradeRule("Peashooter", "Repeater", 250),
                                new UpgradeRule("Repeater", "Mega Gatling Pea", 750),
                                new UpgradeRule("Wall-nut", "Tall-nut", 250),
                                new UpgradeRule("Cabbage-pult", "Melon-pult", 500)
                        ),
                        List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1, ZombieType.ARMOR_2, ZombieType.NEWSPAPER)
                );
                case 3 -> new StageConfig(20,
                        600f,
                        List.of(
                                "Peashooter", "Repeater", "Wall-nut", "Cabbage-pult", "Melon-pult"
                        ),
                        List.of(
                                new UpgradeRule("Peashooter", "Repeater", 250),
                                new UpgradeRule("Repeater", "Mega Gatling Pea", 750),
                                new UpgradeRule("Wall-nut", "Tall-nut", 250),
                                new UpgradeRule("Cabbage-pult", "Melon-pult", 500),
                                new UpgradeRule("Melon-pult", "Winter Melon", 400)
                        ),
                        List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1, ZombieType.ARMOR_2, ZombieType.ARMOR_4,
                                ZombieType.GARGANTUAR)
                );
                default -> throw new IllegalArgumentException(
                        "Beghouled stage must be 1, 2, or 3."
                );
            };
        }}
    public int getWaveNumber() {
        GameState state = getGameState();
        if (state == null || state.getZombieWaveManager() == null) {
            return 0;
        }
        return state.getZombieWaveManager().getCurrentWaveNumber();
    }
}
