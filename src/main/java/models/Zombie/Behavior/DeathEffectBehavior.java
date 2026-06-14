package models.Zombie.Behavior;

import lombok.Getter;

@Getter
public class DeathEffectBehavior implements ZombieBehavior {
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
}
