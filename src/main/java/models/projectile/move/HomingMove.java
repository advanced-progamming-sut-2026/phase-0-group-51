package models.projectile.move;

import models.zombie.Zombie;
import models.projectile.Projectile;

 //Used by Cat-tail, Magnet-shroom, Electric Blueberry, Caulipower.

public class HomingMove implements MovingStrategy {
    @Override
    public void move(Projectile projectile, double speed) {
        Zombie target = projectile.getHomingTarget();

        if (target == null || target.isDead()) {
            // keep flying straight rather than freezing in place
            projectile.setPosX(projectile.getPosX() + speed);
            return;
        }

        double dx = target.getX() - projectile.getPosX();
        double dy = target.getLane() - projectile.getPosY();
        double dist = Math.hypot(dx, dy);
        if (dist < 0.01) return;

        projectile.setPosX(projectile.getPosX() + dx / dist * speed);
        projectile.setPosY(projectile.getPosY() + dy / dist * speed);
    }
}