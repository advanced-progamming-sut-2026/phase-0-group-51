package models.Zombie;

import models.Zombie.Behavior.ZombieBehavior;
import models.games.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Zombie {
    private final String alias;
    private final int    maxHitpoints;
    private int          hitpoints;   //current zombie's health
    private final float  baseSpeed;
    private final float  baseEatDPS;  //Damage per second  (while eating plant)
    private final int    wavePointCost;

    private int   lane;
    private float x;

    private float speedMultiplier  = 1.0f;
    private float damageMultiplier = 1.0f;

    private boolean eating = false;
    private boolean dead   = false;

    private final List<ZombieBehavior> behaviors = new ArrayList<>();

    public Zombie(String alias, int hp, float speed, float eatDPS, int wpc) {
        this.alias         = alias;
        this.maxHitpoints  = hp;
        this.hitpoints     = hp;
        this.baseSpeed     = speed;
        this.baseEatDPS    = eatDPS;
        this.wavePointCost = wpc;
    }

    public void addBehavior(ZombieBehavior b) { behaviors.add(b); }

    public List<ZombieBehavior> getBehaviors() {
        return Collections.unmodifiableList(behaviors);
    }

    public String  getAlias()         { return alias; }
    public int     getHitpoints()     { return hitpoints; }
    public int     getMaxHitpoints()  { return maxHitpoints; }
    public float   getSpeed()         { return baseSpeed * speedMultiplier; }
    public float   getEatDPS()        { return baseEatDPS * damageMultiplier; }
    public int     getWavePointCost() { return wavePointCost; }
    public int     getLane()          { return lane; }
    public float   getX()             { return x; }
    public boolean isEating()         { return eating; }
    public boolean isDead()           { return dead; }


    public void setLane(int lane)             { this.lane = lane; }
    public void setX(float x)                { this.x = x; }
    public void setEating(boolean eating)    { this.eating = eating; }
    public void setSpeedMultiplier(float m)  { this.speedMultiplier = m; }
    public void setDamageMultiplier(float m) { this.damageMultiplier = m; }
    public void overrideHitpoints(int hp)    { this.hitpoints = hp; }







}
