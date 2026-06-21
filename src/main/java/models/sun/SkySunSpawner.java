package models.sun;

import models.Board.Board;
import models.games.GameState;

import java.util.Random;

public class SkySunSpawner {
    private static final int AMOUNT = 50;
    private  float secondsPerDrop ; //\max(6 + 0.05t, 12)
    private final int ticksPerSecond = 10;
    private float tickCounter = 0;
    private final Random random = new Random();

    public Sun onTick(GameState gs) {
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
