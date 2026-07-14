package models.projectile;

import lombok.Getter;
import lombok.Setter;
import models.Board.Tile;
import models.Plant.PlantTag;
import models.projectile.move.MovingStrategy;
import models.projectile.move.StraightMove;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Projectile {
    @Getter
    private final int damage;
    @Getter
    private final ElementType elementType;
    @Getter
    private final List<PlantTag> tags;
    @Getter
    private final double speed;
    @Getter
    @Setter
    private double posX;
    @Setter
    @Getter
    private double posY;

    @Getter
    private final double dirX;
    @Getter
    private final double dirY;

    private final MovingStrategy movingStrategy;

    // How many more zombies this can still hit before being destroyed.
    private int pierceRemaining;

    // 0 = single-target hit. >0 = splash radius in tiles around impact.
    private final double aoeRadius;

    // Only set for targeted (ArcMove) shots — the fixed landing point.
    @Getter
    private Double targetX;
    @Getter
    private Double targetY;

    // Only used by HomingMove.
    @Setter
    @Getter
    private Zombie homingTarget;

    // Prevents a slow-moving piercing shot from hitting the same zombie
    // on every tick it happens to still overlap it.
    private final Set<Zombie> alreadyHit = new HashSet<>();

    @Getter
    private boolean markedForRemoval = false;

    // Peashooter family, Cactus, Goo Peashooter, etc.
    public static Projectile straight(int damage, ElementType elementType, List<PlantTag> tags,
                                      double speed, double posX, int lane,
                                      MovingStrategy movingStrategy, int pierceCount) {
        return new Projectile(damage, elementType, tags, speed, posX, lane, 1, 0,
                movingStrategy, pierceCount, 0, null, null, null);
    }



    // Starfruit, Rotobaga. dirX/dirY should be a (roughly) unit vector.
    public static Projectile directional(int damage, ElementType elementType, List<PlantTag> tags,
                                         double speed, double posX, int lane,
                                         double dirX, double dirY, MovingStrategy movingStrategy) {
        return new Projectile(damage, elementType, tags, speed, posX, lane, dirX, dirY,
                movingStrategy, 1, 0, null, null, null);
    }

    // Lobbed shot toward a fixed point. Cabbage-pult, Kernel-pult. aoeRadius=0 for single target.
    public static Projectile targeted(int damage, ElementType elementType, List<PlantTag> tags,
                                      double speed, double posX, int lane,
                                      double targetX, double targetLane,
                                      MovingStrategy movingStrategy, double aoeRadius) {
        return new Projectile(damage, elementType, tags, speed, posX, lane, 0, 0,
                movingStrategy, 1, aoeRadius, targetX, targetLane, null);
    }

    // Homing shot. Cat-tail, Magnet-shroom, Electric Blueberry, Caulipower.
    public static Projectile homing(int damage, ElementType elementType, List<PlantTag> tags,
                                    double speed, double posX, int lane,
                                    Zombie initialTarget, MovingStrategy movingStrategy) {
        return new Projectile(damage, elementType, tags, speed, posX, lane, 1, 0,
                movingStrategy, 1, 0, null, null, initialTarget);
    }

    public Projectile(int damage, ElementType elementType, List<PlantTag> tags, double speed,
                      double posX, int lane, MovingStrategy movingStrategy) {
        this(damage, elementType, tags, speed, posX, lane, 1, 0, movingStrategy, 1, 0, null, null, null);
    }

    private Projectile(int damage, ElementType elementType, List<PlantTag> tags, double speed,
                       double posX, double posY, double dirX, double dirY,
                       MovingStrategy movingStrategy, int pierceCount, double aoeRadius,
                       Double targetX, Double targetY, Zombie homingTarget) {
        this.damage         = damage;
        this.elementType    = elementType;
        this.tags           = tags;
        this.speed          = speed;
        this.posX           = posX;
        this.posY           = posY;
        this.dirX           = dirX;
        this.dirY           = dirY;
        this.movingStrategy = movingStrategy;
        this.pierceRemaining = pierceCount;
        this.aoeRadius      = aoeRadius;
        this.targetX        = targetX;
        this.targetY        = targetY;
        this.homingTarget    = homingTarget;
    }

    public void tick(GameState state) {
        if (markedForRemoval) return;

        double previousX = posX;
        double previousY = posY;
        movingStrategy.move(this, speed);

        if (isOutOfBounds(state)) {
            destroy(state);
            return;
        }

        if (movingStrategy instanceof StraightMove && Math.abs(posY - previousY) < 0.001) {
            Tile graveTile = state.getBoard().getFirstGraveCrossed(
                    (int) Math.round(posY), previousX, posX
            );
            if (graveTile != null) {
                graveTile.getGrave().takeDamage(damage);
                state.logEvent("Grave at (" + (graveTile.getColumn() + 1) + ", "
                        + (graveTile.getLane() + 1) + ") took " + damage + " damage.\n");
                if (graveTile.getGrave().isDestroyed()) {
                    graveTile.removeGrave();
                    state.logEvent("Grave at (" + (graveTile.getColumn() + 1) + ", "
                            + (graveTile.getLane() + 1) + ") was destroyed.\n");
                }
                destroy(state);
                return;
            }
        }

        if (movingStrategy.isTargeted()) {
            if (movingStrategy.hasReachedTarget(this)) {
                impact(state, null);
            }
            return;
        }

        Zombie contact;
        if (movingStrategy instanceof StraightMove && Math.abs(posY - previousY) < 0.001) {
            contact = state.getBoard().getFirstZombieCrossed(
                    (int) Math.round(posY), previousX, posX, alreadyHit
            );
        } else {
            contact = state.getBoard().getZombieNear(
                    (int) Math.round(posY), posX, 0.35
            );
        }
        if (contact != null && !alreadyHit.contains(contact)) {
            impact(state, contact);
        }
    }

    private void impact(GameState state, Zombie primaryTarget) {
        if (aoeRadius > 0) {
            // Needed for Melon-pult, Winter Melon, Pepper-pult, Cherry Bomb-style splash, etc.
            for (Zombie zombie : state.getBoard().getZombiesInRadius(posY, posX, aoeRadius)) {
                hit(zombie, state);
            }
            destroy(state);
        } else if (primaryTarget != null) {
            hit(primaryTarget, state);
            if (pierceRemaining <= 0) destroy(state);
        } else if (targetX != null) {
            // Targeted, single-target, no splash: whatever's at the
            // landing tile (if anything — a lobbed shot can also land on
            // empty ground and just disappear, matching the original game).
            Zombie landed = state.getBoard().getZombieNear(
                    (int) Math.round(targetY), targetX, 0.75);
            if (landed != null) hit(landed, state);
            destroy(state);
        } else {
            destroy(state);
        }
    }

    private void hit(Zombie zombie, GameState state) {
        zombie.takeDamage(damage, elementType, state, null);
        elementType.onHit(zombie, state);
        alreadyHit.add(zombie);
        pierceRemaining--;
    }

    private void destroy(GameState state) {
        markedForRemoval = true;
        state.getBoard().removeProjectile(this);
    }

    private boolean isOutOfBounds(GameState state) {
        return posX < 0 || posX > state.getBoard().getColumnCount()
                || posY < 0 || posY > state.getBoard().getLaneCount() - 1;
    }

}