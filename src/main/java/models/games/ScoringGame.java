package models.games;

import Data.database.ScoringRepository;
import lombok.Getter;
import models.App;
import models.Board.Board;
import models.User;
import models.Zombie.Zombie;
import models.meowPoint.ScoreBreakdown;
import models.meowPoint.ScoreTracker;
import models.meowPoint.ScoringRules;
import models.meowPoint.ScoringSunSpawner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

@Getter
public class ScoringGame extends Game{
    private final ScoreTracker scoreTracker = new ScoreTracker();
    private final ScoringRepository scoringRepository = new ScoringRepository();
    private LocalDate scoringDate;
    private ScoringSunSpawner scoringSunSpawner;

    @Override
    public void loadLevel() {
        scoringDate = ScoringRules.currentDate();
        Level level = ScoringRules.dailyLevel();
        Board board = new Board();
        GameState state = new GameState(board, ChapterTheme.MINIGAME);
        state.setCurrentLevel(level);
        state.setSun(level.startingSun());
        ZombieWaveManager waveManager = new ZombieWaveManager(
                state, level.allowedZombies(),
                level.totalWaves(), level.baseDifficulty(),
                false,
                new Random(ScoringRules.dailyZombieSeed(scoringDate))
        );
        state.setZombieWaveManager(waveManager);

        setGameState(state);
        scoringSunSpawner = new ScoringSunSpawner(
                ScoringRules.dailySunSeed(scoringDate)
        );
    }

    @Override
    public void onTick() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) {
            return;
        }

        User user = App.getInstance().getLoggedInUser();
        int originalDifficulty = user == null
                ? ScoringRules.FIXED_DIFFICULTY
                : user.getDifficultyLevel();

        if (user != null) {
            user.setDifficultyLevel(ScoringRules.FIXED_DIFFICULTY);
        }

        try {
            state.addTick(1);
            scoringSunSpawner.onTick(state);
            state.getZombieWaveManager().onTick();
            scoreTracker.observeWaveAndSpawns(state);
            state.getBoard().tickFrozenPlants(state);
            state.getBoard().tickPlants(state);
            scoreTracker.observeWaveAndSpawns(state);
            state.getBoard().tickProjectiles(state);
            scoreTracker.observeWaveAndSpawns(state);
            for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
                zombie.onTick(state);
            }
            scoreTracker.observeWaveAndSpawns(state);
            state.tickMowers();
            state.getBoard().tickSuns(state);
            scoreTracker.observeDeathsAndWaveCompletion(state);
            if (state.getZombieWaveManager().isLevelCleared()) {
                state.logEvent(
                        "Dear humanz, zis is not done yet; we will come back "
                                + "to eat your brainz, humanz.\n"
                );
                finishRun(true);
            } else if (state.checkLoseCondition()) {
                finishRun(false);
            }
        } finally {
            if (user != null) {
                user.setDifficultyLevel(originalDifficulty);
            }
        }
    }

    @Override
    public void forward(int requestedTicks) {
        if (requestedTicks <= 0) {
            return;
        }
        for (int i = 0; i < requestedTicks; i++) {
            GameState state = getGameState();
            if (state == null || state.isFinished()) {
                break;
            }
            onTick();
        }
    }

    public String showScoringRules() {
        LocalDate date = scoringDate == null
                ? ScoringRules.currentDate()
                : scoringDate;
        return ScoringRules.describe(date);
    }

    private void finishRun(boolean won) {
        GameState state = getGameState();
        state.setFinished(true);
        state.setWon(won);
        ScoreBreakdown breakdown = scoreTracker.finish(state, won);
        state.logEvent(breakdown.format());
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            state.logEvent("The score was not saved because no user is logged in.\n");
            return;
        }

        try {
            int dailyBest = scoringRepository.saveDailyBest(user,
                    scoringDate, breakdown.total(), won
            );
            state.logEvent("Today's best MeowPoint: " + dailyBest + "\n");
        } catch (IllegalStateException exception) {
            state.logEvent("The MeowPoint result could not be saved: "
                    + exception.getMessage() + "\n");
        }
    }
}
