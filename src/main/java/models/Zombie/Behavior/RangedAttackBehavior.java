package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Map;

@Getter
public class RangedAttackBehavior implements PersistableBehavior {

    private final RangedAttackType type;
    private final int intervalTicks;
    private final int range;
    private final int extraParam;
    private int cooldown;
    private int snowballsRemaining;
    private int snowballDelayTicks;

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
        if (type == RangedAttackType.SNOWBALL && snowballsRemaining > 0) {
            tickSnowballBarrage(zombie, state);
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = intervalTicks;
        Board board = state.getBoard();
        int lane = zombie.getLane();
        int column = (int) zombie.getX();
        switch (type) {
            case SNOWBALL -> startSnowballBarrage(zombie, state);
            case OCTOPUS_NET -> {
                // TODO: Implement the Chapter 3 octopus covering mechanic.
                return;
            }
            case JUGGLE_BALL -> {
                // TODO: Implement the Chapter 4 reflected-projectile mechanic.
                return;
            }
            case HOOK_PULL -> hookPull(board, lane, column);
            case LASER_BEAM -> fireLaser(board, lane, column);
        }
    }

    private void startSnowballBarrage(Zombie zombie, GameState state) {
        snowballsRemaining = Math.max(1, extraParam);
        snowballDelayTicks = 0;
        tickSnowballBarrage(zombie, state);
    }

    private void tickSnowballBarrage(Zombie zombie, GameState state) {
        if (snowballDelayTicks > 0) {
            snowballDelayTicks--;
            return;
        }
        Plant target = state.getBoard().findNearestPlantInRange(
                zombie.getLane(),
                (int) zombie.getX(),
                range
        );
        if (target == null) {
            snowballsRemaining = 0;
            return;
        }
        target.addFrostLevel(state, zombie.getAlias() + " snowball");
        snowballsRemaining--;
        snowballDelayTicks = 2;
    }

    private void fireLaser(Board board, int lane, int column) {
        for (Plant plant : board.getPlantsInLane(lane)) {
            int distance = column - plant.getPosX();
            if (distance >= 0 && distance <= range) {
                plant.takeDamage(extraParam);
            }
        }
    }

    private void hookPull(Board board, int lane, int column) {
        // TODO: Implement the Chapter 3 Fisherman hook-pull mechanic.
        return;
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return type == RangedAttackType.HOOK_PULL;
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
