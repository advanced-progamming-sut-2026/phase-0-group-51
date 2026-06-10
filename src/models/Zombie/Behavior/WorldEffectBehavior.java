package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class WorldEffectBehavior implements ZombieBehavior {
    private final WorldEffectType effectType;
    private final int             intervalTicks; //هر چند تیک یبار انحام بشه
    private int cooldown; //یه تایمر برای انجام دادن رفتار

    public WorldEffectBehavior(WorldEffectType type, int intervalTicks) {
        this.effectType    = type;
        this.intervalTicks = intervalTicks;
        this.cooldown      = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie) {}




    public enum WorldEffectType {

    }
}
