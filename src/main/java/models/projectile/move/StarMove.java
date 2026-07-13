package models.projectile.move;

import models.projectile.Projectile;


public class StarMove implements MovingStrategy {
    @Override
    public void move(Projectile projectile, double speed) {
        projectile.setPosX(projectile.getPosX() + projectile.getDirX() * speed);
        projectile.setPosY(projectile.getPosY() + projectile.getDirY() * speed);
    }
    public static final double[][] STARFRUIT_DIRECTIONS = {
            {1, 0}, {0.7071, 0.7071}, {0.7071, -0.7071}, {0, 1}, {-1, 0}
    };
    public static final double[][] ROTOBAGA_DIRECTIONS = {
            {0.7071, 0.7071}, {0.7071, -0.7071}, {-0.7071, 0.7071}, {-0.7071, -0.7071}
    };
}