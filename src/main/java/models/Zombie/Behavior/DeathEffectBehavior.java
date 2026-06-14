package models.Zombie.Behavior;

import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class DeathEffectBehavior implements PersistableBehavior {
    private final DeathEffectType type;
    private final String          spawnAlias; // null if not a spawn effect
    private final int             spawnCount;

    public DeathEffectBehavior(DeathEffectType type) { this(type, null, 0); }

    public DeathEffectBehavior(DeathEffectType type, String spawnAlias, int count) {
        this.type       = type;
        this.spawnAlias = spawnAlias;
        this.spawnCount = count;
    }

    public enum DeathEffectType {
        SPAWN_IMP,          // Gargantuar
        TOMBSTONE_CRUMBLE   // TombRaiser
    }

    @Override public String behaviorType() { return "DEATH_EFFECT"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setString(24, type.name()); // death_effect_type
        ps.setString(25, spawnAlias);  // death_spawn_alias
        ps.setInt(26, spawnCount);     // death_spawn_count
    }
}
