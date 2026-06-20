package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

public interface ZombieBehavior {
    default void onTick(Zombie zombie, GameState gs) {};
    default int onHit(Zombie zombie, int rawDamage, ElementType element) {
        return rawDamage;
    }
    default void onDeath(Zombie zombie, GameState gs) {};
    ZombieBehavior copy();
}
