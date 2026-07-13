package models.projectile.move;

import models.projectile.Projectile;

public class StraightMove implements MovingStrategy{
    @Override
    public void move(Projectile projectile, double speed) {
        projectile.setPosX(projectile.getPosX() + projectile.getDirX() * speed);
    }
}
