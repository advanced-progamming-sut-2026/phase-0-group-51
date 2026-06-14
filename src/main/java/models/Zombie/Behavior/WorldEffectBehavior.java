package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    public void onTick(Zombie zombie, GameState gs) {}

    public enum WorldEffectType {
        SPAWN_TOMB,     // TombRaiser
        FREEZE_COLUMN   // Troglobite ice block

    }

    @Override public String behaviorType() { return "WORLD_EFFECT"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setString(18, type.name()); // world_effect_type
        ps.setInt(19, intervalTicks);  // effect_interval
        ps.setInt(20, count);          // effect_count
    }
}
