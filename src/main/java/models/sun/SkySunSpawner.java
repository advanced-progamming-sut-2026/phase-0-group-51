package models.sun;

import models.App;
import models.Board.Board;
import models.Board.Tile;
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
        int chance = random.nextInt(100);
        float spawnX = column * Tile.TILEWIDTH;
        float targetY = lane * Tile.TILEHEIGHT;
        SunType type;
        if (chance < 80) {
            type = SunType.ORDINARY;
        } else if (chance < 95) {
            type = SunType.SPECIAL;
        } else {
            type = SunType.RADIOACTIVE;
        }
        Sun sun = new Sun(spawnX, 0f, lane, type, type.getAmount(), type.getLifeTicks());
        gs.getBoard().addSun(sun);
        System.out.printf("New %s sun is dropping at position (<%.1f>, <%.1f>)\n", type.name(), spawnX, targetY);
        return sun;
    }
}
