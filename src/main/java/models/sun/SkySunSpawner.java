package models.sun;

import models.App;
import models.Board.Board;
import models.games.ChapterTheme;
import models.games.GameState;

import java.util.Random;

public class SkySunSpawner {
    private static final int AMOUNT = 50;
    private final int ticksPerSecond = 10;
    private float tickCounter = 0;
    private final Random random = new Random();

    public Sun onTick(GameState gs) {
        if (gs.getChapterTheme() == ChapterTheme.DARK_AGES) {
            return null;
        }
        float t = gs.getTickCounter() / (float) ticksPerSecond;
        float baseSecondsPerDrop = Math.max(6.0f + 0.05f * t, 12.0f);
        int difficultyLevel = App.getInstance().getLoggedInUser().getDifficultyLevel();
        float delayMultiplier = difficultyLevel / 3.0f;
        float secondsPerDrop = baseSecondsPerDrop * delayMultiplier;
        tickCounter++;
        float ticksNeeded = secondsPerDrop * ticksPerSecond;

        if (tickCounter < ticksNeeded) return null;
        tickCounter = 0;

        Board board = gs.getBoard();
        int lane = random.nextInt(board.getLaneCount());
        int column = random.nextInt(board.getColumnCount());

        Sun sun = new Sun(column, 0f, lane, SunType.ORDINARY, AMOUNT);
        gs.getBoard().addSun(sun);
        return sun;
    }
}
