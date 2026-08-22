package models.projectile.move;

import models.projectile.Projectile;

// Used by every Lobber: Cabbage-pult, Kernel-pult, Melon-pult, Winter Melon, Pepper-pult.

public class ArcMove implements MovingStrategy {
    private static final double ARRIVAL_EPSILON = 0.05;
    private static final double ARC_HEIGHT = 1.25;

    private boolean initialized;
    private double totalDistance;

    @Override
    public void move(Projectile projectile, double speed) {
        Double targetX = projectile.getTargetX();
        Double targetY = projectile.getTargetY();

        if (targetX == null || targetY == null) {
            return;
        }

        if (!initialized) {
            double startDx = targetX - projectile.getPosX();
            double startDy = targetY - projectile.getPosY();

            totalDistance = Math.hypot(startDx, startDy);
            initialized = true;
        }

        double dx = targetX - projectile.getPosX();
        double dy = targetY - projectile.getPosY();
        double remainingDistance = Math.hypot(dx, dy);

        if (remainingDistance <= speed || remainingDistance == 0) {
            projectile.setPosX(targetX);
            projectile.setPosY(targetY);
            projectile.setVisualArcOffset(0);
            return;
        }

        projectile.setPosX(
            projectile.getPosX()
                + dx / remainingDistance * speed
        );

        projectile.setPosY(
            projectile.getPosY()
                + dy / remainingDistance * speed
        );

        double newRemainingDistance = Math.hypot(
            targetX - projectile.getPosX(),
            targetY - projectile.getPosY()
        );

        double progress =
            totalDistance <= 0
                ? 1.0
                : 1.0 - newRemainingDistance / totalDistance;

        progress = Math.max(
            0.0,
            Math.min(1.0, progress)
        );

        double arc =
            4.0
                * ARC_HEIGHT
                * progress
                * (1.0 - progress);

        projectile.setVisualArcOffset(arc);
    }

    public void resetForRetarget() {
        initialized = false;
        totalDistance = 0.0;
    }

    @Override
    public boolean isTargeted() {
        return true;
    }

    @Override
    public boolean hasReachedTarget(Projectile projectile) {
        Double targetX = projectile.getTargetX();
        Double targetY = projectile.getTargetY();

        return targetX != null
            && targetY != null
            && Math.abs(projectile.getPosX() - targetX) < ARRIVAL_EPSILON
            && Math.abs(projectile.getPosY() - targetY) < ARRIVAL_EPSILON;
    }
}
