package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Map;

@Getter
public class RangedAttackBehavior implements PersistableBehavior {
    private static final int DEFAULT_JUGGLE_DAMAGE = 20;

    private static final int SNOWBALL_REST_SECONDS = 3;

    private final RangedAttackType type;
    private final int intervalTicks;
    private final int range;
    private final int extraParam;
    private int cooldown;
    private int snowThrowCount;
    private boolean octopusHasTarget;
    private boolean snowballThrown;

    public RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range) {
        this(type, intervalTicks, range, 0);
    }

    public RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range, int extraParam) {
        this.type = type;
        this.intervalTicks = intervalTicks;
        this.range = range;
        this.extraParam = extraParam;
        this.cooldown = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState state) {
        if (type == RangedAttackType.SNOWBALL) {
            tickSnowball(zombie, state);
            return;
        }
        if (type == RangedAttackType.OCTOPUS_NET) {
            tickOctopus(zombie, state);
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = intervalTicks;
        Board board = state.getBoard();
        int lane = zombie.getLane();
        int col = zombie.getColumn();
        switch (type) {
            case JUGGLE_BALL -> {
                Plant target = board.findNearestPlantInRange(lane, col, range);
                if (target != null) {
                    target.takeDamage(extraParam > 0 ? extraParam : DEFAULT_JUGGLE_DAMAGE, state);
                }
            }
            case HOOK_PULL -> hookPull(board, lane, col, state);
            case LASER_BEAM -> {
                // Crystal skull: hits every plant ahead of it in the lane.
                for (Plant plant : board.getPlantsInLane(lane)) {
                    int dist = col - plant.getPosX();
                    if (dist >= 0 && dist <= range) {
                        plant.takeDamage(extraParam, state);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void tickSnowball(Zombie zombie, GameState state) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        Board board = state.getBoard();
        if (!hasSnowballTarget(zombie, board, state)) {
            return;
        }
        throwSnowball(zombie, board, state);
        snowThrowCount++;
        int throwsPerSet = extraParam > 0 ? extraParam : 3;
        if (snowThrowCount >= throwsPerSet) {
            snowThrowCount = 0;
            cooldown = SNOWBALL_REST_SECONDS * state.getTicksPerSecond();
        } else {
            cooldown = intervalTicks;
        }
    }

    public boolean hasSnowballTarget(Zombie zombie, GameState state) {
        return hasSnowballTarget(zombie, state.getBoard(), state);
    }

    private boolean hasSnowballTarget(Zombie zombie, Board board, GameState state) {
        if (board.findNearestPlantInRange(
            zombie.getLane(), zombie.getColumn(), range) != null) {
            return true;
        }
        for (Zombie other : state.getZombiesInTheGame()) {
            if (other == zombie || other.isDead() || !other.hasIceShell()) {
                continue;
            }
            if (other.getLane() != zombie.getLane()) {
                continue;
            }
            if (Math.abs(zombie.getX() - other.getX()) <= range) {
                return true;
            }
        }
        return false;
    }

    private void tickOctopus(Zombie zombie, GameState state) {
        Plant target = findThrowablePlant(state.getBoard(), zombie);
        octopusHasTarget = target != null;
        if (target == null) {
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = intervalTicks;
        target.attachOctopus();
    }

    private Plant findThrowablePlant(Board board, Zombie zombie) {
        int lane = zombie.getLane();
        int col = zombie.getColumn();
        Plant best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Plant plant : board.getPlantsInLane(lane)) {
            if (plant.hasOctopus()) {
                continue;
            }
            int dist = col - plant.getPosX();
            if (dist < 0 || dist > range) {
                continue;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = plant;
            }
        }
        return best;
    }

    private void throwSnowball(Zombie zombie, Board board, GameState state) {
        Plant target = board.findNearestPlantInRange(
            zombie.getLane(), zombie.getColumn(), range);
        if (target != null) {
            target.addFrostLevel(state, "Hunter snowball");
            snowballThrown = true;
        }
    }

    public boolean consumeSnowballThrow() {
        if (!snowballThrown) {
            return false;
        }
        snowballThrown = false;
        return true;
    }

    private void hookPull(Board board, int lane, int col, GameState gs) {
        // Fisherman
        Plant target = board.findNearestPlantInRange(lane, col, range);
        if (target == null) {
            return;
        }
        if (col - target.getPosX() <= 1) {
            target.takeDamage(target.getCurrentHP(), gs);
        } else if (board.isTileFree(lane, target.getPosX() + 1)) {
            board.movePlant(target, lane, target.getPosX() + 1);
        }
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return type == RangedAttackType.HOOK_PULL
            || (type == RangedAttackType.OCTOPUS_NET && octopusHasTarget);
    }

    public enum RangedAttackType {
        SNOWBALL,
        HOOK_PULL,
        OCTOPUS_NET,
        JUGGLE_BALL,
        LASER_BEAM
    }

    @Override
    public String behaviorType() {
        return "RANGED_ATTACK";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("ranged_type", getType().name());
        cols.put("interval_ticks", getIntervalTicks());
        cols.put("range", getRange());
        cols.put("extra_param", getExtraParam());
    }

    @Override
    public ZombieBehavior copy() {
        return new RangedAttackBehavior(type, intervalTicks, range, extraParam);
    }
}
