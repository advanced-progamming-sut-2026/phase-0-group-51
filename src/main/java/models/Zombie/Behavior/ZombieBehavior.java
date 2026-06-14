package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.games.GameState;

public interface ZombieBehavior {
    default void onTick(Zombie zombie, GameState gs) {};

    // Return actual damage after this behavior processes the hit.
    default int onHit(Zombie zombie, int rawDamage) {
        return rawDamage;
    }

    // Called when zombie HP reaches 0.
    default void onDeath(Zombie zombie) {}
}
