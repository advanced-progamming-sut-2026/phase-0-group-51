package models.meowPoint;

import models.Board.Board;
import models.games.GameState;
import models.sun.Sun;
import models.sun.SunType;

import java.util.Random;

public class ScoringSunSpawner {
    private final Random random;
    private float ticksSinceLastDrop;
    public ScoringSunSpawner(long seed) {
        this.random = new Random(seed);
    }
    public void onTick(GameState state) {
        float elapsedSeconds = state.getTickCounter()
                / (float) state.getTicksPerSecond();
        float secondsPerDrop = Math.max(6.0f + 0.05f * elapsedSeconds, 12.0f);

        ticksSinceLastDrop++;
        if (ticksSinceLastDrop < secondsPerDrop * state.getTicksPerSecond()) {
            return;
        }
        ticksSinceLastDrop = 0;
        Board board = state.getBoard();
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

        Sun sun = new Sun(column, lane, lane, type, type.getAmount(), type.getLifeTicks()
        );
        board.spawnSun(sun);
        state.logEvent("New " + type.name().toLowerCase()
                + " sun is dropping at position ("
                + (column + 1) + ", " + (lane + 1) + ")\n");
    }
}

