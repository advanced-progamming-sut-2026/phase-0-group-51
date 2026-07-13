package models.Zombie;

import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Plant.Plant;
import models.Zombie.Behavior.ArmorBehavior;
import models.Zombie.Behavior.ZombieBehavior;
import models.games.GameState;
import models.projectile.ElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Getter
@Setter
public class Zombie {
    public static final String EFFECT_CHILLED = "chilled";
    public static final String EFFECT_FROZEN = "frozen";
    private static final float CHILL_SPEED_FACTOR = 0.5f;

    private final String alias;
    private final int maxHitpoints;
    private int hitpoints;
    private final float baseSpeed;
    private final float baseEatDps;
    private final float wavePointCost;
    private final int weight;

    private int lane;
    private float x;
    private int direction = 1; // 1 = normal, -1 = reversed

    private float speedMultiplier = 1.0f;
    private float damageMultiplier = 1.0f;

    private boolean eating = false;
    private boolean dead = false;
    private boolean hypnotized = false;

    // effect name -> remaining ticks
    private final Map<String, Integer> effects = new LinkedHashMap<>();
    private final List<ZombieBehavior> behaviors = new ArrayList<>();
    private float eatDamageAccumulator = 0f;

    public Zombie(String alias, float hp, float speed, float eatDps, float wpc, int weight) {
        this.alias = alias;
        this.maxHitpoints = Math.round(hp);
        this.hitpoints = this.maxHitpoints;
        this.baseSpeed = speed;
        this.baseEatDps = eatDps;
        this.wavePointCost = wpc;
        this.weight = weight;
    }

    public void setSpeedMultiplier(float speedScale) {
        if (baseSpeed != 0) {
            this.speedMultiplier = speedScale / baseSpeed;
        }
    }

    public void applySpeedScale(float scale) {
        this.speedMultiplier *= scale;
    }

    public void applyDamageScale(float scale) {
        this.damageMultiplier *= scale;
    }

    public void applyChill(int ticks) {
        effects.merge(EFFECT_CHILLED, ticks, Math::max);
    }

    public void applyFreeze(int ticks) {
        effects.merge(EFFECT_FROZEN, ticks, Math::max);
    }

    public void clearColdEffects() {
        effects.remove(EFFECT_CHILLED);
        effects.remove(EFFECT_FROZEN);
    }

    public boolean isFrozen() {
        return effects.getOrDefault(EFFECT_FROZEN, 0) > 0;
    }

    public boolean isChilled() {
        return effects.getOrDefault(EFFECT_CHILLED, 0) > 0;
    }

    public Map<String, Integer> getEffects() {
        return Collections.unmodifiableMap(effects);
    }

    public void addBehavior(ZombieBehavior behavior) {
        behaviors.add(behavior);
    }

    public List<ZombieBehavior> getBehaviors() {
        return Collections.unmodifiableList(behaviors);
    }

    @SuppressWarnings("unchecked")
    public <T extends ZombieBehavior> T getBehavior(Class<T> cls) {
        return (T) behaviors.stream()
            .filter(cls::isInstance)
            .findFirst().orElse(null);
    }

    public boolean pullMetallicArmor() {
        for (ZombieBehavior behavior : behaviors) {
            if (behavior instanceof ArmorBehavior armor && armor.tryMagnetPull()) {
                return true;
            }
        }
        return false;
    }

    public void takeDamage(int rawDamage, ElementType element, GameState gs, Plant plant) {
        if (dead) {
            return;
        }
        int damage = rawDamage;
        for (ZombieBehavior behavior : behaviors) {
            damage = behavior.onHit(this, damage, element, plant);
        }
        hitpoints -= damage;
        if (hitpoints <= 0) {
            hitpoints = 0;
            die(gs);
        }
    }

    public void takeDamage(int rawDamage, GameState gs, Plant plant) {
        takeDamage(rawDamage, ElementType.NORMAL, gs, plant);
    }

    public void killInstantly(GameState gs) {
        if (dead) {
            return;
        }
        hitpoints = 0;
        die(gs);
    }

    public void onTick(GameState gs) {
        if (dead) {
            return;
        }
        tickEffects();
        if (isFrozen()) {
            return;
        }
        for (ZombieBehavior behavior : new ArrayList<>(behaviors)) {
            behavior.onTick(this, gs);
        }
        if (dead) {
            return;
        }
        boolean eatSuppressed = behaviors.stream().anyMatch(b -> b.suppressesDefaultEating(this));
        Plant target = eatSuppressed ? null
            : gs.getBoard().findNearestPlantInRange(lane, (int) x, 1);
        if (target != null) {
            eating = true;
            eatDamageAccumulator += (baseEatDps * damageMultiplier) / gs.getTicksPerSecond();
            int wholeDamage = (int) eatDamageAccumulator;
            if (wholeDamage > 0) {
                eatDamageAccumulator -= wholeDamage;
                target.takeDamage(wholeDamage);
            }
        } else {
            eating = false;
            boolean movementSuppressed = behaviors.stream().anyMatch(b -> b.suppressesMovement(this));
            if (!movementSuppressed) {
                float chillFactor = isChilled() ? CHILL_SPEED_FACTOR : 1.0f;
                x -= direction * (baseSpeed * speedMultiplier * chillFactor) / gs.getTicksPerSecond();
            }
        }
    }

    private void tickEffects() {
        effects.replaceAll((name, ticks) -> ticks - 1);
        effects.values().removeIf(ticks -> ticks <= 0);
    }

    private void die(GameState gs) {
        if (dead) {
            return;
        }
        dead = true;
        for (ZombieBehavior behavior : behaviors) {
            behavior.onDeath(this, gs);
        }
        gs.removeZombie(this);
    }

    public Zombie copy() {
        int difficultyLevel = App.getInstance().getLoggedInUser().getDifficultyLevel();
        float increaseMultiplier = difficultyLevel / 3.0f;
        float decreaseMultiplier = 3.0f / difficultyLevel;
        Zombie z = new Zombie(alias,
            maxHitpoints * increaseMultiplier,
            baseSpeed,
            baseEatDps * increaseMultiplier,
            wavePointCost * decreaseMultiplier,
            weight);
        for (ZombieBehavior behavior : behaviors) {
            z.addBehavior(behavior.copy());
        }
        return z;
    }

    public void reverseDirection() {
        direction = -1;
    }
}
