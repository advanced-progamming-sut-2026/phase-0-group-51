package models.Zombie.Behavior;

import models.Plant.Plant;
import models.Plant.PlantType;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

public interface ZombieBehavior {
    final float TILE_WIDTH = 80f;
    default void onTick(Zombie zombie, GameState gs) {};
    default int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        return rawDamage;
    }
    default boolean suppressesDefaultEating(Zombie zombie) {
        return false;
    }
    default boolean suppressesMovement(Zombie zombie) {
        return false;
    }
    default void onDeath(Zombie zombie, GameState gs) {};
    ZombieBehavior copy();
}
