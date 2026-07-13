package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.List;
import java.util.Map;
@Getter
public class TurquoiseLaserBehavior implements PersistableBehavior {
    private static final int LASER_RANGE_TILES = 4;
    private static final int TICKS_PER_SECOND = 10;

    private final int detectRangeTiles;
    private final int stealDurationTicks;
    private final int stealDurationSeconds;
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
        this.stealDurationSeconds = stealDurationSeconds;
        this.stealDurationTicks = stealDurationSeconds * TICKS_PER_SECOND;
        this.stealPerSecond = stealPerSecond;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        switch (phase) {
            case IDLE -> tryStartSequence(zombie, gs);
            case STEALING -> stealTick(gs);
        }
    }

    private void tryStartSequence(Zombie zombie, GameState gs) {
        Board board = gs.getBoard();
        Plant nearby = board.findNearestPlantInRange(
            zombie.getLane(), (int) zombie.getX(), detectRangeTiles);
        if (nearby == null) {
            return;
        }
        phase = Phase.STEALING;
        ticksInPhase = 0;
        lockedLane = zombie.getLane();
        lockedColumn = (int) zombie.getX();
        stealAccumulator = 0f;
    }

    private void stealTick(GameState gs) {
        ticksInPhase++;
        stealAccumulator += (float) stealPerSecond / TICKS_PER_SECOND;
        int whole = (int) stealAccumulator;
        if (whole > 0) {
            stealAccumulator -= whole;
            int actuallyStolen = Math.min(whole, gs.getSun());
            if (actuallyStolen > 0) {
                gs.stealSun(actuallyStolen);
                totalStolen += actuallyStolen;
            }
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

    @Override
    public void onDeath(Zombie zombie, GameState gs) {
        if (totalStolen > 0) {
            int dropped = totalStolen / 2;
            if (dropped > 0) {
                gs.addSun(dropped);
            }
        }
    }

    @Override
    public String behaviorType() {
        return "TURQUOISE_LASER";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("detect_range", getDetectRangeTiles());
        cols.put("steal_duration", getStealDurationSeconds());
        cols.put("steal_per_second", getStealPerSecond());
    }

    @Override
    public ZombieBehavior copy() {
        return new TurquoiseLaserBehavior(
            detectRangeTiles, stealDurationTicks / TICKS_PER_SECOND, stealPerSecond);
    }
}
