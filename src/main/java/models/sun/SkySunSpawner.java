package models.sun;

import models.App;
import models.Board.Board;
import models.User;
import models.games.ChapterTheme;
import models.games.GameState;

import java.util.Random;

public class SkySunSpawner {
    private float tickCounter;
    private final Random random = new Random();

    public Sun onTick(GameState gameState) {
        if (gameState.getChapterTheme() == ChapterTheme.DARK_AGES) {
            return null;
        }

        float elapsedSeconds = gameState.getTickCounter() / (float) gameState.getTicksPerSecond();
        float baseSecondsPerDrop = Math.max(6.0f + 0.05f * elapsedSeconds, 12.0f);
        User user = App.getInstance().getLoggedInUser();
        int difficultyLevel = user == null ? 3 : user.getDifficultyLevel();
        float secondsPerDrop = baseSecondsPerDrop * (difficultyLevel / 3.0f);

        tickCounter++;
        if (tickCounter < secondsPerDrop * gameState.getTicksPerSecond()) {
            return null;
        }
        tickCounter = 0;

        Board board = gameState.getBoard();
        int lane = random.nextInt(board.getLaneCount());
        int column = random.nextInt(board.getColumnCount());
        int chance = random.nextInt(100);

        SunType type;
        if (chance < 80) {
            type = SunType.ORDINARY;
        } else if (chance < 95) {
            type = SunType.SPECIAL;
        } else {
            type = SunType.RADIOACTIVE;
        }

        Sun sun = new Sun(
                column,
                lane,
                lane,
                type,
                type.getAmount(),
                type.getLifeTicks()
        );
        board.spawnSun(sun);
        gameState.logEvent("New " + type.name().toLowerCase()
                + " sun is dropping at position (" + (column + 1) + ", " + (lane + 1) + ")\n");
        return sun;
    }
}
