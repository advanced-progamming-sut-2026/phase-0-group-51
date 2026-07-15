package models.minigames.wallnutBowling;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Getter
@Setter
public class RollingWallnut {
    private static final double COS_45 = Math.sqrt(0.5);
    private static final double MAX_COLLISION_DISTANCE = 0.48;

    private final WallnutType wallnutType;
    private final int damage;
    private final int explosionDamage;
    private final double speedTilesPerSecond;
    private final Set<Zombie> hitZombies = Collections.newSetFromMap(new IdentityHashMap<>());
    private double x;
    private double y;
    private double previousX;
    private double previousY;
    private double directionX = 1.0;
    private double directionY = 0.0;
    private int zombieHitCount;
    private int firstHitDirection;
    private boolean removed;

    public RollingWallnut(WallnutType wallnutType, double x, double y, int damage, int explosionDamage,
                          double speedTilesPerSecond
    ) {
        if (wallnutType == null) {
            throw new IllegalArgumentException("Wallnut type is required.");
        }
        if (speedTilesPerSecond <= 0) {
            throw new IllegalArgumentException("Wallnut speed must be positive.");
        }
        this.wallnutType = wallnutType;
        this.x = x;
        this.y = y;
        this.previousX = x;
        this.previousY = y;
        this.damage = Math.max(0, damage);
        this.explosionDamage = Math.max(0, explosionDamage);
        this.speedTilesPerSecond = speedTilesPerSecond;
        this.firstHitDirection = ((int) Math.round(y)) % 2 == 0 ? 1 : -1;
    }

    public void move(int ticksPerSecond) {
        if (removed) return;
        if (ticksPerSecond <= 0) {
            throw new IllegalArgumentException("Ticks per second must be positive.");
        }
        previousX = x;
        previousY = y;
        double distance = speedTilesPerSecond / ticksPerSecond;
        x += directionX * distance;
        y += directionY * distance;
    }

    public void reflectFromEdge(double minimumY, double maximumY) {
        if (removed) return;
        if (y < minimumY) {
            y = minimumY + (minimumY - y);
            directionY = Math.abs(directionY);
        } else if (y > maximumY) {
            y = maximumY - (y - maximumY);
            directionY = -Math.abs(directionY);
        }
    }

    public boolean hasCollision(Zombie zombie) {
        if (removed || zombie == null || zombie.isDead() || hitZombies.contains(zombie)) {
            return false;
        }
        double distance = minDistanceBetweenZombieAndWallnut(zombie.getX(), zombie.getLane());
        return distance <= MAX_COLLISION_DISTANCE;
    }

    public boolean onHit(Zombie targetZombie, GameState state) {
        if (removed || targetZombie == null || targetZombie.isDead()
                || !hitZombies.add(targetZombie)) {
            return false;
        }
        switch (wallnutType) {
            case BOWLING -> {
                targetZombie.takeDamage(damage, state, null);
                deflectAfterZombieHit();
                state.logEvent("Bowling Walnut hit " + targetZombie.getAlias()
                        + " for " + damage + " damage.\n");
            }
            case EXPLODE -> {
                explodeAt(targetZombie, state);
                removed = true;
            }
            case BIG_WALLNUT -> {
                String alias = targetZombie.getAlias();
                targetZombie.killInstantly(state);
                state.logEvent("Giant Walnut crushed " + alias + ".\n");
            }
        }
        return true;
    }

    private void explodeAt(Zombie firstTarget, GameState state) {
        int centerLane = firstTarget.getLane();
        int centerColumn = boardColumnOf(firstTarget.getX(), state.getBoard().getColumnCount());
        int hitCount = 0;
        for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
            if (zombie.isDead()) {
                continue;
            }
            int zombieColumn = boardColumnOf(zombie.getX(), state.getBoard().getColumnCount());
            boolean insideThreeRows = Math.abs(zombie.getLane() - centerLane) <= 1;
            boolean insideThreeColumns = Math.abs(zombieColumn - centerColumn) <= 1;
            if (insideThreeRows && insideThreeColumns) {
                zombie.takeDamage(
                        explosionDamage, state, null);
                hitCount++;
            }
        }

        state.logEvent(
                "Explode-O-Nut exploded at ("
                        + (centerColumn + 1)
                        + ", "
                        + (centerLane + 1)
                        + ") and hit "
                        + hitCount
                        + " zombie(s) for "
                        + explosionDamage
                        + " damage.\n"
        );
    }

    private static int boardColumnOf(double x, int columnCount) {int column = (int) Math.floor(x);
        return Math.max(0, Math.min(columnCount - 1, column));
    }
    private void deflectAfterZombieHit() {
        if (zombieHitCount == 0) {
            directionX = COS_45;
            directionY = firstHitDirection * COS_45;
        } else {
            directionY = -directionY;
        }
        zombieHitCount++;
    }

    private double minDistanceBetweenZombieAndWallnut(double pointX, double pointY) {
        double segmentX = x - previousX;
        double segmentY = y - previousY;
        double length = segmentX*segmentX + segmentY*segmentY;
        if (length == 0) {
            return Math.hypot(pointX - x, pointY - y);
        }
        double projection = ((pointX - previousX) * segmentX
                + (pointY - previousY) * segmentY) / length;
        projection = Math.max(0.0, Math.min(1.0, projection));
        double closestX = previousX + projection * segmentX;
        double closestY = previousY + projection * segmentY;
        return Math.hypot(pointX - closestX, pointY - closestY);
    }

    private static String formatCoordinate(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    public void markRemoved() {
        removed = true;
    }
}
