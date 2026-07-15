package models.projectile.move;

import models.projectile.Projectile;

public class StarMove implements MovingStrategy {
    private static final double DIAGONAL_X = 2.0 / Math.sqrt(5.0);
    private static final double DIAGONAL_Y = 1.0 / Math.sqrt(5.0);
    private static final double FULL_DIAGONAL = 1.0 / Math.sqrt(2.0);

    public static final double[][] STARFRUIT_DIRECTIONS = {
            {-1, 0},
            {0, -1},
            {0, 1},
            {DIAGONAL_X, -DIAGONAL_Y},
            {DIAGONAL_X, DIAGONAL_Y}
    };

    public static final double[][] ROTOBAGA_DIRECTIONS = {
            {FULL_DIAGONAL, FULL_DIAGONAL},
            {FULL_DIAGONAL, -FULL_DIAGONAL},
            {-FULL_DIAGONAL, FULL_DIAGONAL},
            {-FULL_DIAGONAL, -FULL_DIAGONAL}
    };

    @Override
    public void move(Projectile projectile, double speed) {
        projectile.setPosX(projectile.getPosX() + projectile.getDirX() * speed);
        projectile.setPosY(projectile.getPosY() + projectile.getDirY() * speed);
    }
}
