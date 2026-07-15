package models.projectile;

import lombok.Getter;
import lombok.Setter;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantTag;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.move.MovingStrategy;
import models.projectile.move.StarMove;
import models.projectile.move.StraightMove;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Projectile {
    @Getter
    private final int damage;
    @Getter
    private final int splashDamage;
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
    private int pierceRemaining;
    private final double aoeRadius;
    private final int effectDurationTicks;
    @Getter
    private final Double targetX;
    @Getter
    private final Double targetY;
    @Setter
    @Getter
    private Zombie homingTarget;
    private final Set<Zombie> alreadyHit = new HashSet<>();
    @Getter
    private boolean markedForRemoval;
    private Plant sourcePlant;

    public static Projectile straight(
            int damage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            int lane,
            MovingStrategy movingStrategy,
            int pierceCount
    ) {
        return straight(
                damage,
                elementType,
                tags,
                speed,
                posX,
                lane,
                movingStrategy,
                pierceCount,
                0
        );
    }

    public static Projectile straight(
            int damage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            int lane,
            MovingStrategy movingStrategy,
            int pierceCount,
            int effectDurationTicks
    ) {
        return new Projectile(
                damage,
                damage,
                elementType,
                tags,
                speed,
                posX,
                lane,
                1,
                0,
                movingStrategy,
                pierceCount,
                0,
                effectDurationTicks,
                null,
                null,
                null
        );
    }

    public static Projectile directional(
            int damage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            int lane,
            double dirX,
            double dirY,
            MovingStrategy movingStrategy
    ) {
        return new Projectile(
                damage,
                damage,
                elementType,
                tags,
                speed,
                posX,
                lane,
                dirX,
                dirY,
                movingStrategy,
                1,
                0,
                0,
                null,
                null,
                null
        );
    }

    public static Projectile targeted(
            int damage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            int lane,
            double targetX,
            double targetLane,
            MovingStrategy movingStrategy,
            double aoeRadius
    ) {
        return targeted(
                damage,
                damage,
                elementType,
                tags,
                speed,
                posX,
                lane,
                targetX,
                targetLane,
                movingStrategy,
                aoeRadius
        );
    }

    public static Projectile targeted(
            int damage,
            int splashDamage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            int lane,
            double targetX,
            double targetLane,
            MovingStrategy movingStrategy,
            double aoeRadius
    ) {
        return new Projectile(
                damage,
                splashDamage,
                elementType,
                tags,
                speed,
                posX,
                lane,
                0,
                0,
                movingStrategy,
                1,
                aoeRadius,
                0,
                targetX,
                targetLane,
                null
        );
    }

    public static Projectile homing(
            int damage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            int lane,
            Zombie initialTarget,
            MovingStrategy movingStrategy
    ) {
        return new Projectile(
                damage,
                damage,
                elementType,
                tags,
                speed,
                posX,
                lane,
                1,
                0,
                movingStrategy,
                1,
                0,
                0,
                null,
                null,
                initialTarget
        );
    }

    public Projectile(
            int damage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            int lane,
            MovingStrategy movingStrategy
    ) {
        this(
                damage,
                damage,
                elementType,
                tags,
                speed,
                posX,
                lane,
                1,
                0,
                movingStrategy,
                1,
                0,
                0,
                null,
                null,
                null
        );
    }

    private Projectile(
            int damage,
            int splashDamage,
            ElementType elementType,
            List<PlantTag> tags,
            double speed,
            double posX,
            double posY,
            double dirX,
            double dirY,
            MovingStrategy movingStrategy,
            int pierceCount,
            double aoeRadius,
            int effectDurationTicks,
            Double targetX,
            Double targetY,
            Zombie homingTarget
    ) {
        this.damage = damage;
        this.splashDamage = splashDamage;
        this.elementType = elementType;
        this.tags = tags;
        this.speed = speed;
        this.posX = posX;
        this.posY = posY;
        this.dirX = dirX;
        this.dirY = dirY;
        this.movingStrategy = movingStrategy;
        this.pierceRemaining = pierceCount;
        this.aoeRadius = aoeRadius;
        this.effectDurationTicks = effectDurationTicks;
        this.targetX = targetX;
        this.targetY = targetY;
        this.homingTarget = homingTarget;
    }

    public Projectile withSource(Plant plant) {
        this.sourcePlant = plant;
        return this;
    }

    public void tick(GameState state) {
        if (markedForRemoval) {
            return;
        }
        double previousX = posX;
        double previousY = posY;
        movingStrategy.move(this, speed);
        if (isOutOfBounds(state)) {
            destroy(state);
            return;
        }
        if (hitFrozenPlantIfCrossed(state, previousX, previousY)) {
            return;
        }
        if (hitGraveIfCrossed(state, previousX, previousY)) {
            return;
        }
        if (movingStrategy.isTargeted()) {
            handleTargetedMovement(state);
            return;
        }
        Zombie contact = findContact(state, previousX, previousY);
        if (contact != null && !alreadyHit.contains(contact)) {
            impact(state, contact);
        }
    }

    private boolean hitFrozenPlantIfCrossed(
            GameState state,
            double previousX,
            double previousY
    ) {
        if (!(movingStrategy instanceof StraightMove)
                || Math.abs(posY - previousY) >= 0.001) {
            return false;
        }
        Plant frozenPlant = state.getBoard().getFirstFrozenPlantCrossed(
                (int) Math.round(posY),
                previousX,
                posX
        );
        if (frozenPlant == null) {
            return false;
        }
        frozenPlant.damageIce(damage, elementType, state);
        state.logEvent("Ice around " + frozenPlant.getName() + " has "
                + frozenPlant.getIceHealth() + " health left.\n");
        destroy(state);
        return true;
    }

    private boolean hitGraveIfCrossed(
            GameState state,
            double previousX,
            double previousY
    ) {
        Tile graveTile;
        if (movingStrategy instanceof StraightMove
                && Math.abs(posY - previousY) < 0.001) {
            graveTile = state.getBoard().getFirstGraveCrossed(
                    (int) Math.round(posY),
                    previousX,
                    posX
            );
        } else if (movingStrategy instanceof StarMove) {
            graveTile = state.getBoard().getFirstGraveCrossed(
                    previousX,
                    previousY,
                    posX,
                    posY
            );
        } else {
            return false;
        }
        if (graveTile == null) {
            return false;
        }
        damageGrave(state, graveTile);
        destroy(state);
        return true;
    }

    private void damageGrave(GameState state, Tile graveTile) {
        graveTile.getGrave().takeDamage(damage);
        state.logEvent("Grave at (" + (graveTile.getColumn() + 1) + ", "
                + (graveTile.getLane() + 1) + ") took " + damage + " damage.\n");
        if (graveTile.getGrave().isDestroyed()) {
            graveTile.removeGrave();
            state.logEvent("Grave at (" + (graveTile.getColumn() + 1) + ", "
                    + (graveTile.getLane() + 1) + ") was destroyed.\n");
        }
    }

    private void handleTargetedMovement(GameState state) {
        if (movingStrategy.hasReachedTarget(this)) {
            impact(state, null);
        }
    }

    private Zombie findContact(
            GameState state,
            double previousX,
            double previousY
    ) {
        boolean movedStraight = movingStrategy instanceof StraightMove
                && Math.abs(posY - previousY) < 0.001;
        if (movedStraight) {
            return state.getBoard().getFirstZombieCrossed(
                    (int) Math.round(posY),
                    previousX,
                    posX,
                    alreadyHit
            );
        }
        return state.getBoard().getZombieNear(
                (int) Math.round(posY),
                posX,
                0.35
        );
    }

    private void impact(GameState state, Zombie primaryTarget) {
        if (aoeRadius > 0) {
            hitArea(state);
        } else if (primaryTarget != null) {
            hit(primaryTarget, state);
            if (pierceRemaining <= 0) {
                destroy(state);
            }
        } else if (targetX != null) {
            hitLandingTarget(state);
        } else {
            destroy(state);
        }
    }

    private void hitArea(GameState state) {
        Zombie primary = state.getBoard().getZombieNear(
                (int) Math.round(targetY == null ? posY : targetY),
                targetX == null ? posX : targetX,
                0.75
        );
        for (Zombie zombie : state.getBoard().getZombiesInRadius(
                posY,
                posX,
                aoeRadius
        )) {
            int appliedDamage = zombie == primary ? damage : splashDamage;
            hit(zombie, state, appliedDamage);
        }
        destroy(state);
    }

    private void hitLandingTarget(GameState state) {
        Zombie landed = state.getBoard().getZombieNear(
                (int) Math.round(targetY),
                targetX,
                0.75
        );
        if (landed != null) {
            hit(landed, state);
        }
        destroy(state);
    }

    private void hit(Zombie zombie, GameState state) {
        hit(zombie, state, damage);
    }

    private void hit(Zombie zombie, GameState state, int appliedDamage) {
        boolean protectedByIce = zombie.hasIceShell();
        zombie.takeDamage(appliedDamage, elementType, state, sourcePlant);
        if (!protectedByIce) {
            elementType.onHit(zombie, state, effectDurationTicks, sourcePlant);
        }
        alreadyHit.add(zombie);
        pierceRemaining--;
    }

    private void destroy(GameState state) {
        markedForRemoval = true;
        state.getBoard().removeProjectile(this);
    }

    private boolean isOutOfBounds(GameState state) {
        return posX < 0
                || posX > state.getBoard().getColumnCount()
                || posY < 0
                || posY > state.getBoard().getLaneCount() - 1;
    }
}
