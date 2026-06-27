package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

@Getter
public class WorldEffectBehavior implements PersistableBehavior {
    private final WorldEffectType type;
    private final int             intervalTicks;
    private final int             count;
    private int cooldown;

    public WorldEffectBehavior(WorldEffectType type, int intervalTicks, int count) {
        this.type          = type;
        this.intervalTicks = intervalTicks;
        this.count         = count;
        this.cooldown      = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (--cooldown > 0) return;
        cooldown = intervalTicks;

        Board board = gs.getBoard();
        int lane = zombie.getLane();
        int col  = (int) zombie.getX();
        switch (type) {
            case SPAWN_TOMB -> {
                for (int i = 0; i < count; i++) {
                    Tile placed = board.placeGraveOnRandomTile();
                    if (placed == null) break;
                }
            }
            case RANDOM_LANE_SWAP -> {
                gs.swapRandomZombieLanes(count);
                break;
            }

        }
    }

    public enum WorldEffectType {
        SPAWN_TOMB,     // TombRaiser
        FREEZE_COLUMN ,  // Troglobite ice block
        RANDOM_LANE_SWAP

    }

    @Override public String behaviorType() { return "WORLD_EFFECT"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }
}
