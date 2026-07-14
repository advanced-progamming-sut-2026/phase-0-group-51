package models.zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.plant.Plant;
import models.zombie.Zombie;
import models.games.GameState;
import java.util.Map;

@Getter
public class RangedAttackBehavior implements PersistableBehavior {

    private final RangedAttackType type;
    private final int intervalTicks;
    private final int range;
    private final int extraParam;
    private int cooldown;

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
    public void onTick(Zombie zombie, GameState gs) {
        if (--cooldown > 0) {
            return;
        }
        cooldown = intervalTicks;

        Board board = gs.getBoard();
        int lane = zombie.getLane();
        int col = (int) zombie.getX();
        switch (type) {
            case SNOWBALL -> {
                // IceAge hunter

            }
            case OCTOPUS_NET -> {
                // Octopus
            }
            case JUGGLE_BALL -> {
            }
            case HOOK_PULL -> hookPull(board, lane, col);
            case LASER_BEAM -> {
                // Crystal skull
                for (Plant plant : board.getPlantsInLane(lane)) {
                    int dist = col - plant.getPosX();
                    if (dist >= 0 && dist <= range) {
                        plant.takeDamage(extraParam);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void hookPull(Board board, int lane, int col) {


    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return type == RangedAttackType.HOOK_PULL;
    }


    public enum RangedAttackType {
        SNOWBALL,     // IceAge Hunter
        HOOK_PULL,    // Fisherman
        OCTOPUS_NET,  // Octopus
        JUGGLE_BALL,  // Juggler
        LASER_BEAM    // Crystal Skull
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
