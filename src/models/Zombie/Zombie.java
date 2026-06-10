package models.Zombie;

import models.Zombie.Behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.List;

public class Zombie {
    private final String id;
    private int health;
    private int lane;
    private float x;
    private boolean isGlowy;
    private final List<ZombieBehavior> behaviors = new ArrayList<>();

    public Zombie(String id, int health, int lane, float x) {
        this.id = id;
        this.health = health;
        this.lane = lane;
        this.x = x;
    }

    public void addBehavior(ZombieBehavior behavior) {
        behaviors.add(behavior);
    }


}
