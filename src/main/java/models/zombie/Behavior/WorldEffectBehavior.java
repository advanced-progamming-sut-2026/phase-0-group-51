package models.zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Board.Tile;
import models.zombie.Zombie;
import models.games.GameState;
import java.util.Map;

@Getter
public class WorldEffectBehavior implements PersistableBehavior {
    private final WorldEffectType type;
    private final int intervalTicks;
    private final int count;
    private int cooldown;

    public WorldEffectBehavior(WorldEffectType type, int intervalTicks, int count) {
        this.type = type;
        this.intervalTicks = intervalTicks;
        this.count = count;
        this.cooldown = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (--cooldown > 0) {
            return;
        }
        cooldown = intervalTicks;

        Board board = gs.getBoard();
        switch (type) {
            case SPAWN_TOMB -> {
                // Tomb raiser
                for (int i = 0; i < count; i++) {
                    Tile placed = board.placeGraveOnRandomTile();
                    if (placed == null) {
                        break;
                    }
                }
            }
            case RANDOM_LANE_SWAP ->
                // Pianist
                gs.swapRandomZombieLanes(count);
            default -> {
            }
        }
    }


    public enum WorldEffectType {
        SPAWN_TOMB,      // TombRaiser
        RANDOM_LANE_SWAP // Pianist
    }

    @Override
    public String behaviorType() {
        return "WORLD_EFFECT";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("world_effect_type", getType().name());
        cols.put("effect_interval", getIntervalTicks());
        cols.put("effect_count", getCount());
    }

    @Override
    public ZombieBehavior copy() {
        return new WorldEffectBehavior(type, intervalTicks, count);
    }
}
