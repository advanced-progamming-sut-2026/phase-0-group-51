package models.games;


import Data.database.NewsRepository;
import Data.database.PlantRepository;
import Data.database.ProgressRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Board.Board;
import models.User;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ancientEgypt.ConveyorBeltLevel;
import models.games.frostbite.FrostbiteCavesFeature;
import models.quests.QuestService;
import models.sun.SkySunSpawner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
    public void start(){if (this.gameState != null && this.gameState.getZombieWaveManager() != null) {
        this.gameState.getZombieWaveManager().start();
    }
    }
    public void loadLevel(){
        ChapterTheme theme = chapters.get(currentChapterIndex);
        Level level = theme.getLevels().get(currentLevelIndex);
        Board board = new Board();
        this.gameState = new GameState(board, theme);
        this.gameState.setCurrentLevel(level);
        this.gameState.setSun(level.startingSun());
        this.skySunSpawner = new SkySunSpawner();
        this.conveyorBeltLevel = null;
        List<ZombieType> allowedZombies = level.resolveAllowedZombies(theme.getAllowedZombies());
        int totalWaves = level.totalWaves();
        float baseDifficulty = level.baseDifficulty();
        ZombieWaveManager waveManager = new ZombieWaveManager(
                this.gameState, allowedZombies, totalWaves, baseDifficulty
        );
        this.gameState.setZombieWaveManager(waveManager);
        applyChapterFeatures(theme, level, board, waveManager);
        level.type().initialize(this.gameState);
        initializeConveyorBeltLevel(level);
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
        gameState.logEvent("user is null.\n");
        }

        Set<Integer> unlockedIds = PlantRepository.loadUnlockedPlants(user.getId());
        List<PlantData> plants = unlockedIds.stream()
                .map(PlantRegistry::getById)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(PlantData::id))
                .toList();
        if (plants.isEmpty()) {
            gameState.logEvent("You have no unlocked plants available for the Conveyor Belt level.");
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
    }

    private void applyAncientEgyptFeatures(
            ChapterTheme theme,
            Board board,
            ZombieWaveManager waveManager
    ) {
        if (theme.getChapterFeatures().contains(ChapterFeature.GRAVE)) {
            for (int i = 0; i < 5; i++) {
                board.placeGraveOnRandomTile();
            }
        }
        if (theme.getChapterFeatures().contains(ChapterFeature.TORNADO)) {
            waveManager.setTornadoFinalWave(true);
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
        if (gameState.checkDeadlineLoseCondition()) {
            finishAsLoss();
            return;
        }
        gameState.tickMowers();
        gameState.getBoard().tickSuns(gameState);
        if (gameState.getCurrentLevel().type().isFinished(gameState)) {
            gameState.setFinished(true);
            gameState.setWon(true);
            gameState.logEvent(
                    "Dear humanz, zis is not done yet; "
                            + "we will come back to eat your brainz, humanz.\n"
            );
            evaluateQuestRun(true);
            saveProgressInDatabase();
        } else if (gameState.checkLoseCondition()) {
            finishAsLoss();
        }

    }

    private void finishAsLoss() {
        gameState.setFinished(true);
        gameState.setWon(false);
        evaluateQuestRun(false);
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
        ChapterTheme currentTheme = chapters.get(currentChapterIndex);
        int nextLevelIndex = currentLevelIndex;
        int nextChapterIndex = currentChapterIndex;
        if (currentLevelIndex + 1 < currentTheme.getLevels().size()) {
            nextLevelIndex++;
        } else if (currentChapterIndex + 1
                < chapters.size()) {
            nextChapterIndex++;
            nextLevelIndex = 0;
        }
        int newChapter = nextChapterIndex + 1;
        int newLevel = nextLevelIndex + 1;
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return;
        }
        ProgressRepository progressRepository = new ProgressRepository();
        int[] oldProgress = progressRepository.getCurrentProgress(user.getId());
        boolean chapterAdvanced = newChapter > oldProgress[0];
        boolean levelAdvanced = newChapter == oldProgress[0] && newLevel > oldProgress[1];
        if (!chapterAdvanced && !levelAdvanced) {
            return;
        }
        boolean saved = progressRepository.saveProgress(user.getId(), newChapter, newLevel);
        if (!saved) {
            gameState.logEvent(
                    "Progress could not be saved.\n"
            );
            return;
        }
        NewsRepository newsRepository = new NewsRepository();
        ChapterTheme unlockedTheme = chapters.get(nextChapterIndex);
        if (chapterAdvanced) {
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
                            + newLevel
                            + "."
            );
        }
    }
}