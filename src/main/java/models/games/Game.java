package models.games;


import Data.database.NewsRepository;
import Data.database.PlantRepository;
import Data.database.ProgressRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.PlantTag;
import models.User;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ancientEgypt.ConveyorBeltLevel;
import models.games.ancientEgypt.Grave;
import models.games.bigWaveBeach.BigWaveBeachFeature;
import models.games.darkAges.LockedPlantsMode;
import models.games.frostbite.FrostbiteCavesFeature;
import models.quests.QuestService;
import models.sun.SkySunSpawner;

import java.util.*;

@Getter
@Setter
public class Game{
    private final List<ChapterTheme> chapters = List.of(
            ChapterTheme.ANCIENT_EGYPT,
            ChapterTheme.FROSTBITE_CAVES,
           ChapterTheme.BIG_WAVE_BEACH,
           ChapterTheme.DARK_AGES
    );
    private int currentChapterIndex = 0;
    private int currentLevelIndex   = 0;
    private final List<PlantData> selectedPlantsForThisGame = new ArrayList<>();
    private GameState gameState;
    private SkySunSpawner skySunSpawner;
    private ConveyorBeltLevel conveyorBeltLevel;
    private long pendingScaledTicks;
    private boolean zombieWavesManuallyStarted;
    private LockedPlantsMode lockedPlantsMode;
    private final Map<String, Integer> allowedPlantByLockedFamily = new LinkedHashMap<>();
    private final Random random = new Random();

    public void start() {
        if (gameState == null || gameState.getZombieWaveManager() == null) {
            return;
        }
        if (isPlantWhatYouGetLevel()) {
            zombieWavesManuallyStarted = false;
            gameState.logEvent(
                    "plant your plants without cooldown time, "
                            + "then start zombie wave manually .\n"
            );
            return;
    }
        gameState.getZombieWaveManager().start();
    }

    public boolean isPlantWhatYouGetLevel() {
        return gameState != null
                && gameState.getCurrentLevel() != null
                && gameState.getCurrentLevel().type() == LevelType.PLANT_WHAT_YOU_GET;
    }

    public boolean isPreparingPlantWhatYouGet() {
        return isPlantWhatYouGetLevel() && !zombieWavesManuallyStarted;
    }

    public boolean startZombieWaves() {
        if (!isPreparingPlantWhatYouGet()
                || gameState.getZombieWaveManager() == null) {
            return false;
        }
        zombieWavesManuallyStarted = true;
        gameState.clearPlantCooldowns();
        gameState.getZombieWaveManager().start();
        gameState.logEvent("Zombie waves started. Recharge rules are active now.\n");
        return true;
    }

    public Level getSelectedLevel() {
        if (currentChapterIndex < 0 || currentChapterIndex >= chapters.size()) {
            throw new IllegalStateException("The selected chapter is invalid.");
        }

        ChapterTheme chapter = chapters.get(currentChapterIndex);
        if (currentLevelIndex < 0 || currentLevelIndex >= chapter.getLevels().size()) {
            throw new IllegalStateException("The selected level is invalid.");
        }

        return chapter.getLevels().get(currentLevelIndex);
    }

    public boolean isLockedPlantsLevel() {
        return getSelectedLevel().type() == LevelType.LOCKED_PLANTS;
    }

    public boolean hasChosenLockedPlantsMode() {
        return lockedPlantsMode != null;
    }

    public boolean isForcedLoadoutMode() {
        return lockedPlantsMode == LockedPlantsMode.FORCED;
    }

    public void chooseLockedPlantsMode(LockedPlantsMode mode) {
        if (!isLockedPlantsLevel()) {
            throw new IllegalStateException(
                    "The selected level is not a Locked Plants level."
            );
        }
        if (mode == null) {
            throw new IllegalArgumentException(
                    "Locked Plants mode cannot be null."
            );
        }

        selectedPlantsForThisGame.clear();
        allowedPlantByLockedFamily.clear();

        if (mode == LockedPlantsMode.FAMILY) {
            prepareFamilyLockMode(mode);
        } else {
            prepareForcedLoadoutMode(mode);
        }

        lockedPlantsMode = mode;
    }

    private void prepareFamilyLockMode(LockedPlantsMode mode) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            throw new IllegalStateException(
                    "You must log in before selecting plants."
            );
        }

        Set<Integer> unlockedIds = PlantRepository.loadUnlockedPlants(user.getId());
        List<PlantData> unlockedPlants = unlockedIds.stream()
                .map(PlantRegistry::getById)
                .filter(Objects::nonNull)
                .filter(plant -> !plant.tags().contains(PlantTag.WATER))
                .filter(plant -> plant.id() != 58 && plant.id() != 59)
                .sorted(Comparator.comparingInt(PlantData::id))
                .toList();

        for (String family : mode.getRestrictedFamilies()) {
            String normalizedFamily = LockedPlantsMode.normalizeFamily(family);
            List<PlantData> candidates = unlockedPlants.stream()
                    .filter(plant -> normalizedFamily.equals(
                            LockedPlantsMode.normalizeFamily(plant.category())
                    ))
                    .toList();

            if (candidates.isEmpty()) {
                continue;
            }

            PlantData allowedPlant = candidates.get(random.nextInt(candidates.size()));
            allowedPlantByLockedFamily.put(normalizedFamily, allowedPlant.id());
        }
    }

    private void prepareForcedLoadoutMode(LockedPlantsMode mode) {
        if (mode.getForcedPlantIds().size() != 8) {
            throw new IllegalStateException(
                    "Forced Plants mode must contain exactly eight plants."
            );
        }

        for (int plantId : mode.getForcedPlantIds()) {
            PlantData plant = PlantRegistry.getById(plantId);
            if (plant == null) {
                throw new IllegalStateException(
                        "Forced plant with id " + plantId + " does not exist."
                );
            }
            selectedPlantsForThisGame.add(plant);
        }
    }

    public boolean isPlantLockedByFamilyMode(PlantData plant) {
        if (plant == null || lockedPlantsMode != LockedPlantsMode.FAMILY) {
            return false;
        }

        String family = LockedPlantsMode.normalizeFamily(plant.category());
        Integer allowedPlantId = allowedPlantByLockedFamily.get(family);
        return allowedPlantId != null && allowedPlantId != plant.id();
    }

    public boolean isFamilyChoicePlant(PlantData plant) {
        if (plant == null || lockedPlantsMode != LockedPlantsMode.FAMILY) {
            return false;
        }

        String family = LockedPlantsMode.normalizeFamily(plant.category());
        Integer allowedPlantId = allowedPlantByLockedFamily.get(family);
        return allowedPlantId != null && allowedPlantId == plant.id();
    }

    public PlantData getAllowedPlantForFamily(PlantData plant) {
        if (plant == null) {
            return null;
        }

        String family = LockedPlantsMode.normalizeFamily(plant.category());
        Integer allowedPlantId = allowedPlantByLockedFamily.get(family);
        return allowedPlantId == null ? null : PlantRegistry.getById(allowedPlantId);
    }

    public boolean isForcedLockedPlant(PlantData plant) {
        return plant != null
                && lockedPlantsMode == LockedPlantsMode.FORCED
                && lockedPlantsMode.getForcedPlantIds().contains(plant.id());
    }

    private void validateLockedPlantsSelection(Level level) {
        if (level.type() != LevelType.LOCKED_PLANTS) {
            return;
        }
        if (lockedPlantsMode == null) {
            throw new IllegalStateException(
                    "Choose a Locked Plants mode before starting the level."
            );
        }
        if (lockedPlantsMode == LockedPlantsMode.FORCED
                && selectedPlantsForThisGame.size() != 8) {
            throw new IllegalStateException(
                    "Forced Plants mode requires exactly eight locked plants."
            );
        }
        if (selectedPlantsForThisGame.stream().anyMatch(this::isPlantLockedByFamilyMode)) {
            throw new IllegalStateException(
                    "A plant locked by Family Lock mode is still selected."
            );
        }
    }
    public void loadLevel(){
        ChapterTheme theme = chapters.get(currentChapterIndex);
        Level level = theme.getLevels().get(currentLevelIndex);
        Board board = new Board();
        this.zombieWavesManuallyStarted = false;
        this.gameState = new GameState(board, theme);
        this.gameState.setCurrentLevel(level);
        this.gameState.setSun(level.startingSun());
        boolean forceSkySun =
                level.type() == LevelType.LOCKED_PLANTS || level.type() == LevelType.LOVE_YOUR_PLANTS;
        boolean skySunDisabled =
                !forceSkySun && (theme.getTimeOfTheDay() == TimeOfTheDay.NIGHT || level.type() == LevelType.NIGHT_OPS
                                || level.type() == LevelType.PLANT_WHAT_YOU_GET);
        this.skySunSpawner = skySunDisabled ? null : new SkySunSpawner();
        this.conveyorBeltLevel = null;
        List<ZombieType> allowedZombies = level.resolveAllowedZombies(theme.getAllowedZombies());
        int totalWaves = level.totalWaves();
        float baseDifficulty = level.baseDifficulty();
        boolean autoStartWaves = level.type() != LevelType.PLANT_WHAT_YOU_GET;
        ZombieWaveManager waveManager = new ZombieWaveManager(
                this.gameState,  allowedZombies,totalWaves, baseDifficulty,autoStartWaves,new Random()
        );
        this.gameState.setZombieWaveManager(waveManager);
        applyChapterFeatures(theme, level, board, waveManager);
        level.type().initialize(this.gameState);
        initializeConveyorBeltLevel(level);
        loadPurchasedPlantFood();
    }

    private void loadPurchasedPlantFood() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null || gameState == null) {
            return;
    }
        int storedPlantFood = new UserRepository().claimStoredPlantFood(user.getId());
        if (storedPlantFood < 0) {
            gameState.logEvent("Stored Plant Food could not be loaded.\n");
            return;
        }
        gameState.setPlantFoodCount(storedPlantFood);
        user.setPlantFoodNum(0);
    }

    private void initializeConveyorBeltLevel(Level level) {
        if (level.type() != LevelType.CONVEYOR_BELT) {
            return;
        }
        gameState.setSun(0);
        skySunSpawner = null;
        conveyorBeltLevel = new ConveyorBeltLevel(loadUnlockedConveyorPlants());
        conveyorBeltLevel.initialize(gameState);
    }
    private List<PlantData> loadUnlockedConveyorPlants() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            throw new IllegalStateException(
                    "You must log in before starting a Conveyor Belt level."
            );
        }

        Set<Integer> unlockedIds = PlantRepository.loadUnlockedPlants(user.getId());
        List<PlantData> plants = unlockedIds.stream()
                .map(PlantRegistry::getById)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(PlantData::id))
                .toList();
        if (plants.isEmpty()) {
            throw new IllegalStateException(
                    "No unlocked plants are available for the Conveyor Belt."
            );
        }
        return plants;
    }
    public boolean isConveyorBeltLevel() {
        return conveyorBeltLevel != null;
    }
    public List<PlantData> getConveyorBeltPlants() {
        return conveyorBeltLevel == null ? List.of() : conveyorBeltLevel.getPlants();
    }
    public boolean hasConveyorPlant(PlantData plantData) {
        return conveyorBeltLevel != null && conveyorBeltLevel.contains(plantData);
    }
    public boolean consumeConveyorPlant(PlantData plantData) {
        return conveyorBeltLevel != null && conveyorBeltLevel.consume(plantData);
    }
    public int getTicksUntilNextConveyorDelivery() {
        if (conveyorBeltLevel == null || gameState == null) {
            return 0;
        }
        return conveyorBeltLevel.getTicksUntilNextDelivery(gameState);
    }
    private void applyChapterFeatures(
            ChapterTheme theme,
            Level level,
            Board board,
            ZombieWaveManager waveManager
    ) {
        applyAncientEgyptFeatures(theme, board, waveManager);
        applyFrostbiteFeatures(theme, level, waveManager);
        applyBigWaveBeachFeatures(theme, level, board, waveManager);
        applyDarkAgesFeatures(theme, board, waveManager);
    }

    private void applyAncientEgyptFeatures(ChapterTheme theme, Board board, ZombieWaveManager waveManager) {
        if (theme == ChapterTheme.ANCIENT_EGYPT && theme.getChapterFeatures().contains(ChapterFeature.GRAVE)) {
            for (int i = 0; i < 5; i++) {
                board.placeGraveOnRandomTile();
            }
        }
        if (theme == ChapterTheme.ANCIENT_EGYPT && theme.getChapterFeatures().contains(ChapterFeature.TORNADO)) {
            waveManager.setTornadoFinalWave(true);
        }
    }
    private void applyBigWaveBeachFeatures(ChapterTheme theme,Level level,Board board,ZombieWaveManager waveManager) {
        if (theme != ChapterTheme.BIG_WAVE_BEACH) {
            return;
        }
        if (level.type() == LevelType.PLANT_WHAT_YOU_GET) {
        board.clearWater();
        return;
        }
        BigWaveBeachFeature feature = new BigWaveBeachFeature( gameState,waveManager);
        feature.initialize();
    }

    private void applyDarkAgesFeatures(ChapterTheme theme, Board board, ZombieWaveManager waveManager) {
        if (theme != ChapterTheme.DARK_AGES) {
            return;
        }
        skySunSpawner = null;

        if (theme.getChapterFeatures().contains(ChapterFeature.GRAVE)) {
            for (int i = 0; i < 4; i++) {
                createDarkAgesGrave(board, 0);
            }
        }
        boolean graveSpawnEnabled = theme.getChapterFeatures().contains(ChapterFeature.GRAVE_SPAWN);
        boolean necromancyEnabled = theme.getChapterFeatures().contains(ChapterFeature.NECROMANCY);
        waveManager.setOnWaveStart(waveNumber -> {
            if (graveSpawnEnabled && random.nextInt(100) < 80) {
                int graveCount = 1 + random.nextInt(4);
                for (int i = 0; i < graveCount; i++) {
                    Tile created = createDarkAgesGrave(board, waveNumber);
                    if (created == null) {
                        break;
                    }
                }
            }

            if (necromancyEnabled && random.nextInt(100) < 80) {
                performNecromancy(board, waveManager, waveNumber);
            }
        });
    }
    private Tile createDarkAgesGrave(Board board, int waveNumber) {
        Tile tile = board.placeGraveOnRandomTile();
        if (tile == null) {
            gameState.logEvent("No valid tile was available for a new grave.\n");
            return null;
        }
        Grave grave = tile.getGrave();
        int rewardRoll = random.nextInt(100);
        if (rewardRoll < 30) {
            grave.makePlantFoodGrave();
        } else if (rewardRoll < 60) {
            grave.makeSunGrave();
        }
        String moment = waveNumber == 0
                ? "at the start of the level"
                : "at the start of wave " + waveNumber;
        gameState.logEvent(
                "A " + grave.getDisplayType()
                        + "  appeared at ("
                        + (tile.getColumn() + 1)
                        + ", "
                        + (tile.getLane() + 1)
                        + ") "
                        + moment
                        + ".\n"
        );

        return tile;
    }
    private void performNecromancy(Board board, ZombieWaveManager waveManager, int waveNumber) {
        List<Tile> graveTiles = new ArrayList<>();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                if (tile == null || !tile.hasGrave()) {
                    continue;
                }
                if (board.getZombieInPosition(lane, column) != null) {
                    continue;
                }
                graveTiles.add(tile);
            }
        }
        if (graveTiles.isEmpty()) {
            return;
        }
        Collections.shuffle(graveTiles, random);
        int zombieCount = Math.min(1 + random.nextInt(3), graveTiles.size());
        for (int i = 0; i < zombieCount; i++) {
            waveManager.spawnZombieFromGrave(graveTiles.get(i), waveNumber);
        }
    }
    private void applyFrostbiteFeatures(
            ChapterTheme theme,
            Level level,
            ZombieWaveManager waveManager
    ) {
        if (theme != ChapterTheme.FROSTBITE_CAVES) {
            return;
        }
        FrostbiteCavesFeature feature = new FrostbiteCavesFeature(
                gameState,
                level.frostbiteConfig()
        );
        feature.initialize();
        waveManager.setOnWaveStart(feature::onWaveStart);
    }
    public void onTick(){
        if (gameState == null || gameState.isFinished()) return;
        gameState.addTick(1);
        if (skySunSpawner != null) {
            skySunSpawner.onTick(gameState);
        }
        if (conveyorBeltLevel != null) {
            conveyorBeltLevel.onTick(gameState);
        }
        gameState.getZombieWaveManager().onTick();
        gameState.getBoard().tickFrozenPlants(gameState);
        gameState.getBoard().tickPlants(gameState);
        gameState.getBoard().tickProjectiles(gameState);
        List<Zombie> zombies = new ArrayList<>(gameState.getZombiesInTheGame());
        for (Zombie zombie : zombies) {
            zombie.onTick(gameState);
        }
        if (gameState.checkSaveOurSeedsLoseCondition()
                || gameState.checkDeadlineLoseCondition()) {
            finishAsLoss();
            return;
        }
        if (gameState.checkDeadlineLoseCondition()
                || gameState.checkPlantLossLoseCondition()) {
            finishAsLoss();
            return;
        }
        gameState.tickMowers();
        gameState.getBoard().tickSuns(gameState);
        if (gameState.checkTimedBattleLoseCondition()) {
            finishAsLoss();
            return;
        }
        if (gameState.getCurrentLevel().type().isFinished(gameState)) {
            gameState.setFinished(true);
            gameState.setWon(true);
            gameState.logEvent(
                    "Dear humanz, zis is not done yet; "
                            + "we will come back to eat your brainz, humanz.\n"
            );
            evaluateQuestRun(true);
            saveProgressInDatabase();
        }

    }

    private void finishAsLoss() {
        gameState.setFinished(true);
        gameState.setWon(false);
        evaluateQuestRun(false);
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return;
        }
        int gamesPlayed = new UserRepository().recordAdventureLoss(user.getId());
        if (gamesPlayed < 0) {
            gameState.logEvent("The played-game counter could not be saved.\n");
            return;}
        user.setGamesPlayed(gamesPlayed);
    }

    private void evaluateQuestRun(boolean won) {
        User user = App.getInstance().getLoggedInUser();
        int difficulty = user == null ? 3 : user.getDifficultyLevel();
        QuestService.getInstance().evaluateAdventureRun(
                user, gameState, gameState.getChapterTheme(), difficulty, won);
    }

    public void forward(int requestedTicks){
        User user = App.getInstance().getLoggedInUser();
        int difficultyLevel = user == null ? 3 : user.getDifficultyLevel();
        pendingScaledTicks += (long) requestedTicks * difficultyLevel;
        long ticksToRun = pendingScaledTicks / 3;
        pendingScaledTicks %= 3;
        for (
                long i = 0;
                i < ticksToRun && !gameState.isFinished();
                i++
        ) {
            onTick();
        }
    }
    private void saveProgressInDatabase() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) return;
        int completedChapter = currentChapterIndex + 1;
        int completedLevel = currentLevelIndex + 1;
        Integer candidateChapter = null;
        Integer candidateLevel = null;
        ChapterTheme currentTheme = chapters.get(currentChapterIndex);
        if (currentLevelIndex + 1 < currentTheme.getLevels().size()) {
            candidateChapter = completedChapter;
            candidateLevel = completedLevel + 1;
        } else if (currentChapterIndex + 1 < chapters.size()) {
            candidateChapter = completedChapter + 1;
            candidateLevel = 1;
        }
        ProgressRepository progressRepository = new ProgressRepository();
        ProgressRepository.AdventureWinResult result =
                progressRepository.recordAdventureWin(
                        user.getId(), completedChapter, completedLevel, candidateChapter, candidateLevel);
        if (!result.saved()) {
            gameState.logEvent("Adventure progress could not be saved.\n");
            return;
        }
        user.setGamesPlayed(result.gamesPlayed());
        user.setLastWonGame("Chapter " + completedChapter + " Level " + completedLevel);
        if (!result.progressAdvanced()) return;
        NewsRepository newsRepository = new NewsRepository();
        ChapterTheme unlockedTheme = chapters.get(result.newChapter() - 1);
        if (result.newChapter() > result.oldChapter()) {
            newsRepository.createNewsForUser(
                    user.getId(),
                    "New chapter unlocked: "
                            + unlockedTheme.getName()
                            + ". Level 1 is now available."
            );
        } else {
            newsRepository.createNewsForUser(
                    user.getId(),
                    "New level unlocked: "
                            + unlockedTheme.getName()
                            + " Level "
                            + result.newLevel()
                            + "."
            );
        }
    }
}