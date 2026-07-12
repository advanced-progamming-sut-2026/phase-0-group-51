package models.Zombie;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Plant.PlantType;
import models.Zombie.Behavior.ArmorBehavior;
import models.Zombie.Behavior.DamageReactionBehavior;
import models.Zombie.Behavior.ZombieBehavior;
import models.games.GameState;
import models.projectile.ElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Getter
@Setter
public class Zombie {
    private final String alias;
    private final int    maxHitpoints; //from json
    private int          hitpoints;   //current zombie's health
    private final float  baseSpeed;
    private final float  baseEatDPS;  //Damage per second  (while eating plant)
    private final float    wavePointCost;
    private final int    weight;


    private int   lane;
    private float x;

    private float speedMultiplier  = 1.0f;
    private float damageMultiplier = 1.0f;


    private boolean eating = false;
    private boolean dead   = false;
    private boolean hypnotized = false;

    private float slowTicksRemaining = 0f;
    private float poisonDPS = 0f;
    private float poisonTicksRemaining = 0f;
    private float poisonDamageAccumulator = 0f;

    private final List<ZombieBehavior> behaviors = new ArrayList<>();

    public Zombie(String alias, float hp, float speed, float eatDPS, float wpc, int weight) {
        this.alias         = alias;
        this.maxHitpoints  = hp;
        this.hitpoints     = hp;
        this.baseSpeed     = speed;
        this.baseEatDPS    = eatDPS;
        this.wavePointCost = wpc;
        this.weight        = weight;
    }

    public void setSpeedMultiplier(float speedScale) {
        this.speedMultiplier = speedScale/baseSpeed;
    }

    public void addBehavior(ZombieBehavior b) { behaviors.add(b); }

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
        if (dead) return;
        int damage = rawDamage;
        for (ZombieBehavior behavior : behaviors) {
            damage = behavior.onHit(this, damage, element,plant);
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

    public void takeDamage(int rawDamage, GameState gs) {
        takeDamage(rawDamage, ElementType.NORMAL, gs, null);
    }

    public void applySlow(GameState gs, float multiplier, float durationSeconds) {
        this.speedMultiplier = multiplier;
        this.slowTicksRemaining = durationSeconds * gs.getTicksPerSecond();
    }

    public void removeSlowEffect() {
        this.speedMultiplier = 1.0f;
        this.slowTicksRemaining = 0f;
    }

    public void applyPoison(GameState gs, float damagePerSecond, float durationSeconds) {
        this.poisonDPS = damagePerSecond;
        this.poisonTicksRemaining = durationSeconds * gs.getTicksPerSecond();
    }

    private float eatDamageAccumulator = 0f;
    public void onTick(GameState gs) {
        if (dead) return;
        for (ZombieBehavior behavior : behaviors) {
            behavior.onTick(this, gs);
        }
        if (dead) return;

        tickSlow();
        tickPoison(gs);
        if (dead) return;

        boolean suppressed = behaviors.stream().anyMatch(b -> b.suppressesDefaultEating(this));
        if (suppressed) {
            eating = false;
            return;
        }
        Plant target = gs.getBoard().findNearestPlantInRange(lane, (int) x, 1);
        if (target != null) {
            eating = true;
            eatDamageAccumulator += (baseEatDPS * damageMultiplier) / gs.getTicksPerSecond();
            int wholeDamage = (int) eatDamageAccumulator;
            if (wholeDamage > 0) {
                eatDamageAccumulator = 0f;
                target.takeDamage(wholeDamage);
            }
        } else {
            eating = false;
            boolean movementSuppressed = behaviors.stream().anyMatch(b -> b.suppressesMovement(this));
            if (!movementSuppressed) {
                x -= baseSpeed * speedMultiplier;
            }
        }
    }

    private void tickSlow() {
        if (slowTicksRemaining <= 0) return;
        slowTicksRemaining--;
        if (slowTicksRemaining <= 0) {
            speedMultiplier = 1.0f;
        }
    }

    private void tickPoison(GameState gs) {
        if (poisonTicksRemaining <= 0) return;
        poisonTicksRemaining--;
        poisonDamageAccumulator += poisonDPS / gs.getTicksPerSecond();
        int whole = (int) poisonDamageAccumulator;
        if (whole > 0) {
            poisonDamageAccumulator -= whole;
            hitpoints -= whole;
            if (hitpoints <= 0) {
                hitpoints = 0;
                die(gs);
            }
        }
    }


    private void die(GameState gs) {
        if (dead) return;
        dead = true;
        for (ZombieBehavior behavior : behaviors) {
            behavior.onDeath(this,gs);
        }
        gs.removeZombie(this);
    }

    public Zombie copy() {
        float increaseMultiplier = App.getInstance().getLoggedInUser().getDifficultyLevel() / 3.0f;
        float decreaseMultiplier = 3.0f / App.getInstance().getLoggedInUser().getDifficultyLevel();
        float newMaxHitpoints = this.maxHitpoints * increaseMultiplier;
        float newBaseEatDPS = this.baseEatDPS * increaseMultiplier;
        float newBaseSpeed = this.baseSpeed * increaseMultiplier;
        float newWavePointCost = this.wavePointCost * decreaseMultiplier;
        Zombie z = new Zombie(alias, newMaxHitpoints, newBaseSpeed, newBaseEatDPS, newWavePointCost, weight);
        for (ZombieBehavior behavior : behaviors) {
            z.addBehavior(behavior.copy());
        }
        return z;
    }

}
