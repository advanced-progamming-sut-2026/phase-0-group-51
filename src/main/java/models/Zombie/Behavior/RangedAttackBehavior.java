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
       // if (type == RangedAttackType.SNOWBALL && snowballsRemaining > 0) {
           // tickSnowballBarrage(zombie, state);
           // return;
        //}
        if (--cooldown > 0) {
            return;
        }
        cooldown = intervalTicks;
        Board board = state.getBoard();
        int lane = zombie.getLane();
        int col = zombie.getColumn();
        switch (type) {
            case SNOWBALL -> {
                // IceAge hunter
                Plant target = board.findNearestPlantInRange(lane, col, range);
                if (target != null) {
                    target.addFrostLevel(state, "Hunter snowball");
                }
            }
            case OCTOPUS_NET -> {
                // Octopus
                Plant target = board.findNearestPlantInRange(lane, col, range);
                if (target != null) {
                    target.attachOctopus();
                }
            }
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
