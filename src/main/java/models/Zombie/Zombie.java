package models.Zombie;

import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Zombie.Behavior.ArmorBehavior;
import models.Zombie.Behavior.DamageReactionBehavior;
import models.Zombie.Behavior.DeathEffectBehavior;
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
    private final float    maxHitpoints;
    private float          hitpoints;   //current zombie's health
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

    public void takeDamage(int rawDamage, ElementType element, GameState gs) {
        if (dead) return;
        int damage = rawDamage;
        for (ZombieBehavior behavior : behaviors) {
            damage = behavior.onHit(this, damage, element);

            if (behavior instanceof ArmorBehavior armor && armor.isDestroyed()) {
                DamageReactionBehavior reaction = getBehavior(DamageReactionBehavior.class);
                if (reaction != null
                    && reaction.getType() == DamageReactionBehavior.DamageReactionType.NEWSPAPER_RAGE) {
                    reaction.triggerRage(this);
                }
            }
        }
        hitpoints -= damage;
        if (hitpoints <= 0) {
            hitpoints = 0;
            die(gs);
        }
    }
    public void takeDamage(int rawDamage, GameState gs) {
        takeDamage(rawDamage, ElementType.NORMAL, gs);
    }

    public void onTick(GameState gs) {
        if (dead) return;
        for (ZombieBehavior behavior : behaviors) {
            behavior.onTick(this, gs);
        }
    }

    private void die(GameState gs) {
        if (dead) return;
        dead = true;
        for (ZombieBehavior behavior : behaviors) {
            behavior.onDeath(this,gs);
            if (behavior instanceof DeathEffectBehavior deathEffect) {
                deathEffect.onDeath(this, gs);
            }
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
