package models.Zombie.Behavior;

import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class TurquoiseLaserBehavior implements PersistableBehavior {

    private static final int LASER_RANGE_TILES = 4;

    private final int detectRangeTiles;
    private final int stealDurationTicks;
    private final int stealPerSecond;

    private enum Phase { IDLE, STEALING }
    private Phase phase = Phase.IDLE;

    private int ticksInPhase = 0;
    private int lockedLane;
    private int lockedColumn;
    private int totalStolen = 0;
    private float stealAccumulator = 0f;

    public TurquoiseLaserBehavior(int detectRangeTiles, int stealDurationSeconds, int stealPerSecond) {
        this.detectRangeTiles = detectRangeTiles;
        this.stealDurationTicks = stealDurationSeconds * 10;
        this.stealPerSecond = stealPerSecond;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        switch (phase) {
            case IDLE -> tryStartSequence(zombie, gs);
            case STEALING -> stealTick(zombie, gs);
        }
    }

    private void tryStartSequence(Zombie zombie, GameState gs) {
        Board board = gs.getBoard();
        Plant nearby = board.findNearestPlantInRange(zombie.getLane(), (int) zombie.getX(), detectRangeTiles);
        if (nearby == null) return;

        phase = Phase.STEALING;
        ticksInPhase = 0;
        lockedLane = zombie.getLane();
        lockedColumn = (int) zombie.getX();
        stealAccumulator = 0f;
    }

    private void stealTick(Zombie zombie, GameState gs) {
        ticksInPhase++;
        stealAccumulator = (float) stealPerSecond / 10;
        float actuallyStolen = (gs.getSun()>stealAccumulator) ? stealAccumulator : gs.getSun();
        if (actuallyStolen > 0) {
          //  gs.stealSun(actuallyStolen);
            totalStolen += actuallyStolen;
        }
        if (ticksInPhase >= stealDurationTicks) {
            fireLaser(gs);
            phase = Phase.IDLE;
        }
    }

    private void fireLaser(GameState gs) {
        Board board = gs.getBoard();
        List<Plant> targets = board.getPlantsInLane(lockedLane).stream()
            .filter(p -> {
                int dist = lockedColumn - p.getPosX();
                return dist >= 0 && dist < LASER_RANGE_TILES;
            })
            .toList();

        for (Plant target : targets) {
            target.takeDamage(target.getCurrentHP());
        }
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return phase == Phase.STEALING;
    }

    public void onDeath(Zombie zombie, GameState gs) {
        if (totalStolen > 0) {
            int dropped = totalStolen / 2;
            if (dropped > 0) {
                gs.addSun(dropped);
            }
        }
    }

    @Override public String behaviorType() { return "TURQUOISE_LASER"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }

    @Override
    public ZombieBehavior copy() {
        return new TurquoiseLaserBehavior(
            detectRangeTiles, stealDurationTicks / 10, stealPerSecond);
    }
}
