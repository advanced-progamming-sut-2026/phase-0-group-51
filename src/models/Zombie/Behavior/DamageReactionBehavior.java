package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class DamageReactionBehavior implements ZombieBehavior {
    private final DamageReactionType reactionType;
    private boolean reacted = false;   // یه فلگ برا اینکه بدونیم اون ریکشن یبار اتفاق افتاده یا نه

    public DamageReactionBehavior(DamageReactionType type) {
        this.reactionType = type;
    }


    @Override
    public void onTick(Zombie zombie) {}

    @Override
    public int onHit(Zombie zombie, int rawDamage) {return 0;}



    public enum DamageReactionType {

    }

}
