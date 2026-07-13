package models.projectile.move;

import models.projectile.Projectile;

public interface MovingStrategy {
    void move(Projectile projectile, double speed);


    //True for strategies that fly toward a fixed target point and impact
    default boolean isTargeted() { return false; }

    // Only meaningful when isTargeted() is true.
    default boolean hasReachedTarget(Projectile projectile) { return false; }
}