package models.projectile.move;

import models.projectile.Projectile;

 // Used by every Lobber: Cabbage-pult, Kernel-pult, Melon-pult, Winter Melon, Pepper-pult.

public class ArcMove implements MovingStrategy {
    private static final double ARRIVAL_EPSILON = 0.05;

    @Override
    public void move(Projectile projectile, double speed) {
        Double targetX = projectile.getTargetX();
        Double targetY = projectile.getTargetY();
        if (targetX == null || targetY == null) return;

        double dx = targetX - projectile.getPosX();
        double dy = targetY - projectile.getPosY();
        double dist = Math.hypot(dx, dy);

        if (dist <= speed || dist == 0) {
            projectile.setPosX(targetX);
            projectile.setPosY(targetY);
        } else {
            projectile.setPosX(projectile.getPosX() + dx / dist * speed);
            projectile.setPosY(projectile.getPosY() + dy / dist * speed);
        }
    }

    @Override
    public boolean isTargeted() { return true; }

    @Override
    public boolean hasReachedTarget(Projectile projectile) {
        Double targetX = projectile.getTargetX();
        Double targetY = projectile.getTargetY();
        return targetX != null && targetY != null
                && Math.abs(projectile.getPosX() - targetX) < ARRIVAL_EPSILON
                && Math.abs(projectile.getPosY() - targetY) < ARRIVAL_EPSILON;
    }
}
