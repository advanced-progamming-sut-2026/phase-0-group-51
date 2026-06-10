package models.projectile.move;

import models.projectile.Projectile;

public interface MovingStrategy {
    void move(Projectile projectile);
}
