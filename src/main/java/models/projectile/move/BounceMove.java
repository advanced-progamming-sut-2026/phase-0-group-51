package models.projectile.move;

import models.projectile.Projectile;

/**
 * Forward rolling movement used by Bowling Bulb and Grapeshot grapes.
 * The projectile reflects from the top/bottom board edges and changes its
 * vertical direction whenever it strikes a zombie.
 */
public class BounceMove implements MovingStrategy {
    private static final double MIN_LANE = 0.0;
    private static final double MAX_LANE = 4.0;
    private final double horizontalSign;
    private double verticalSign;

    public BounceMove(double initialVerticalSign) {
        this(1.0, initialVerticalSign);
    }

    public BounceMove(double horizontalSign, double initialVerticalSign) {
        this.horizontalSign = horizontalSign >= 0 ? 1.0 : -1.0;
        this.verticalSign = initialVerticalSign >= 0 ? 1.0 : -1.0;
    }

    @Override
    public void move(Projectile projectile, double speed) {
        projectile.setPosX(projectile.getPosX() + horizontalSign * speed);
        projectile.setPosY(projectile.getPosY() + verticalSign * speed * 0.7);
        if (projectile.getPosY() <= MIN_LANE) {
            projectile.setPosY(MIN_LANE);
            verticalSign = 1.0;
        } else if (projectile.getPosY() >= MAX_LANE) {
            projectile.setPosY(MAX_LANE);
            verticalSign = -1.0;
        }
    }

    public void onHit() {
        verticalSign *= -1.0;
    }
}
