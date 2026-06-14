package models.Zombie;

import lombok.Getter;
import lombok.Setter;
import models.Zombie.Behavior.ZombieBehavior;
import models.games.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Getter
@Setter
public class Zombie {
    private final String alias;
    private final int    maxHitpoints;
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

    public Zombie copy() {
        Zombie z = new Zombie(alias, maxHitpoints, baseSpeed, baseEatDPS, wavePointCost, weight);
        for (ZombieBehavior behavior : behaviors) {
            z.addBehavior(behavior);
        }
        return z;
    }

}
