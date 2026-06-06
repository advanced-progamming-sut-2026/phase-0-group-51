package models.Zombie.Behavior;

import models.Zombie.Zombie;

public interface ZombieBehavior {
    default void onTick(Zombie zombie) {};

    // Return actual damage after this behavior processes the hit.
    default int onHit(Zombie zombie, int rawDamage) {
        return rawDamage;
    }

    // Called when zombie HP reaches 0.
    default void onDeath(Zombie zombie) {}
}
