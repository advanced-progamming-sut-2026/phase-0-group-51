package models.games;


import Data.loader.PlantData;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Board.Board;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.sun.SkySunSpawner;

import java.util.ArrayList;
import java.util.List;
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
    private int sunAmount=50;
    private SkySunSpawner skySunSpawner;
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
        this.skySunSpawner = new SkySunSpawner();
        List<ZombieType> allowedZombies = theme.getAllowedZombies();
        int totalWaves = level.totalWaves();
        float baseDifficulty = level.baseDifficulty();
        ZombieWaveManager waveManager = new ZombieWaveManager(
                this.gameState, allowedZombies, totalWaves, baseDifficulty
        );
        this.gameState.setZombieWaveManager(waveManager);
        applyChapterFeatures(theme, board, waveManager);
        level.type().initialize(this.gameState);
    }
    private void applyChapterFeatures(ChapterTheme theme, Board board, ZombieWaveManager waveManager) {
        if (theme.getChapterFeatures().contains(ChapterFeature.GRAVE)) {
            for (int i = 0; i < 5; i++) {
                board.placeGraveOnRandomTile();
            }
        }
        if (theme.getChapterFeatures().contains(ChapterFeature.TORNADO)) {

        }
    }
    public void onTick(){
        if (gameState == null || gameState.isFinished()) return;
        gameState.addTick(1);
        if (skySunSpawner != null) {
            skySunSpawner.onTick(gameState);
        }
        gameState.getZombieWaveManager().onTick();
        List<Zombie> zombies = new ArrayList<>(gameState.getZombiesInTheGame());
        for (Zombie zombie : zombies) {
            zombie.onTick(gameState);
        }
        gameState.tickMowers();
        gameState.getBoard().tickSuns();
        if (gameState.getZombieWaveManager().isLevelCleared() || gameState.checkLoseCondition()) {
            gameState.setFinished(true);
        }

    }
    public void forward(int requestedTicks){
        int dl = App.getInstance().getLoggedInUser().getDifficultyLevel();
        float speedMultiplier = dl / 3.0f;
        int actualTicks = Math.round(requestedTicks * speedMultiplier);
        for (int i = 0; i < actualTicks; i++) {
            onTick();
        }
    }
}