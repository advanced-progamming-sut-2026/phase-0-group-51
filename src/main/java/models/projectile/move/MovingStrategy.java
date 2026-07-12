package models.projectile.move;

import models.projectile.Projectile;

public interface MovingStrategy {

    // give the next position of the projectile
    void move(Projectile projectile, double speed);

    /**
     * True for strategies that fly toward a fixed target point and impact
     * there regardless of what's directly under them when they arrive
     * (lobbed shots - ArcMove). False (default) for strategies where impact
     * is decided by "is a zombie exactly where I am right now" (Straight
     * and Star shots).
     */
    default boolean isTargeted() { return false; }

    /** Only meaningful when isTargeted() is true. */
    default boolean hasReachedTarget(Projectile projectile) { return false; }
}