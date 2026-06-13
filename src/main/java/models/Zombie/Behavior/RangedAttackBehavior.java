package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class RangedAttackBehavior implements ZombieBehavior {
    private final RangedAttackType attackType;
    private final int intervalTicks;
    private final int range;
    private int cooldown;

    public RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range) {
        this.attackType    = type;
        this.intervalTicks = intervalTicks;
        this.range         = range;
        this.cooldown      = intervalTicks;
    }
    @Override
    public void onTick(Zombie zombie) {}



    public enum RangedAttackType {

    }

}
