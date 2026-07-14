package models.zombie.Behavior;

import data.loader.ZombieRegistry;
import lombok.Getter;
import models.zombie.Zombie;
import models.games.GameState;
import java.util.Map;


@Getter
public class DeathEffectBehavior implements PersistableBehavior {

    private final DeathEffectType type;
    private final String spawnAlias;
    private final int spawnCount;

    public DeathEffectBehavior(DeathEffectType type, String spawnAlias, int spawnCount) {
        this.type = type;
        this.spawnAlias = spawnAlias;
        this.spawnCount = spawnCount;
    }

    @Override
    public void onDeath(Zombie zombie, GameState gs) {
        if (type != DeathEffectType.SPAWN_ZOMBIE) {
            return;
        }
        for (int i = 0; i < spawnCount; i++) {
            Zombie template = ZombieRegistry.getTemplate(spawnAlias);
            if (template == null) {
                return;
            }
            Zombie spawned = template.copy();
            spawned.setLane(zombie.getLane());
            spawned.setX(zombie.getX());
            gs.addZombie(spawned);
        }
    }

    public DeathEffectType getType() {
        return type;
    }

    public String getSpawnAlias() {
        return spawnAlias;
    }

    public int getSpawnCount() {
        return spawnCount;
    }

    public enum DeathEffectType {
        SPAWN_ZOMBIE
    }

    @Override
    public String behaviorType() {
        return "DEATH_EFFECT";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("death_effect_type", getType().name());
        cols.put("death_spawn_alias", getSpawnAlias());
        cols.put("death_spawn_count", getSpawnCount());
    }

    @Override
    public ZombieBehavior copy() {
        return new DeathEffectBehavior(type, spawnAlias, spawnCount);
    }
}
