package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.games.GameState;

public interface ZombieBehavior {
    default void onTick(Zombie zombie, GameState gs) {};
    default int onHit(Zombie zombie, int rawDamage) {
        return rawDamage;
    }
    default void onDeath(Zombie zombie) {}
}
