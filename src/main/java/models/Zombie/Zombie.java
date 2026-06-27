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
    private final int    wavePointCost;
    private final int    weight;


    private int   lane;
    private float x;

    private float speedMultiplier  = 1.0f;
    private float damageMultiplier = 1.0f;


    private boolean eating = false;
    private boolean dead   = false;
    private boolean hypnotized = false;

    private final List<ZombieBehavior> behaviors = new ArrayList<>();

    public Zombie(String alias, int hp, float speed, float eatDPS, int wpc, int weight) {
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
    private float eatDamageAccumulator = 0f;
    public void onTick(GameState gs) {
        if (dead) return;
        for (ZombieBehavior behavior : behaviors) {
            behavior.onTick(this, gs);
        }
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


    private void die(GameState gs) {
        if (dead) return;
        dead = true;
        for (ZombieBehavior behavior : behaviors) {
            behavior.onDeath(this,gs);
        }
        gs.removeZombie(this);
    }

    public Zombie copy() {
        Zombie z = new Zombie(alias, maxHitpoints, baseSpeed, baseEatDPS, wavePointCost, weight);
        for (ZombieBehavior behavior : behaviors) {
            z.addBehavior(behavior.copy());
        }
        return z;
    }

}
