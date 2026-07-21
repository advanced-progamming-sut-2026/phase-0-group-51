package models.projectile;

import lombok.Getter;
import lombok.Setter;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantTag;
import models.Zombie.Zombie;
import models.Zombie.Behavior.DamageReactionBehavior;
import models.games.GameState;
import models.games.ancientEgypt.Grave;
import models.projectile.move.MovingStrategy;
import models.projectile.move.BounceMove;
import models.projectile.move.StarMove;
import models.projectile.move.StraightMove;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Projectile {
    @Getter
    private int damage;
    @Getter
    private int splashDamage;
    @Getter
    private ElementType elementType;
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
    private boolean graveTarget;
    private Plant sourcePlant;
    private int remainingTicks = -1;
    private final Set<Plant> passedModifiers = new HashSet<>();
    private boolean torchwoodModified;
    @Getter
    private boolean reflected;

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

    public static Projectile bouncing(
        int damage,
        ElementType elementType,
        List<PlantTag> tags,
        double speed,
        double posX,
        int lane,
        double initialVerticalSign,
        int hitCount,
        double aoeRadius
    ) {
        return bouncing(
            damage,
            elementType,
            tags,
            speed,
            posX,
            lane,
            1.0,
            initialVerticalSign,
            hitCount,
            aoeRadius
        );
    }

    public static Projectile bouncing(
        int damage,
        ElementType elementType,
        List<PlantTag> tags,
        double speed,
        double posX,
        int lane,
        double horizontalSign,
        double initialVerticalSign,
        int hitCount,
        double aoeRadius
    ) {
        return new Projectile(
            damage,
            damage,
            elementType,
            tags,
            speed,
            posX,
            lane,
            horizontalSign,
            initialVerticalSign,
            new BounceMove(horizontalSign, initialVerticalSign),
            Math.max(1, hitCount),
            aoeRadius,
            0,
            null,
            null,
            null
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

    public Projectile withGraveTarget() {
        graveTarget = true;
        return this;
    }

    public Projectile withLifetime(int ticks) {
        remainingTicks = Math.max(1, ticks);
        return this;
    }

    public void tick(GameState state) {
        if (markedForRemoval) {
            return;
        }
        double previousX = posX;
        double previousY = posY;
        movingStrategy.move(this, reflected ? -speed : speed);
        if (remainingTicks > 0 && --remainingTicks == 0) {
            destroy(state);
            return;
        }
        if (reflected) {
            if (isOutOfBounds(state)) {
                destroy(state);
                return;
            }
            hitPlantIfCrossedWhileReflected(state, previousX);
            return;
        }
        applyTorchwoodIfCrossed(state, previousX, previousY);
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
            if (tryReflectByJester(state, contact)) {
                return;
            }
            impact(state, contact);
        }
    }

    private void applyTorchwoodIfCrossed(
        GameState state,
        double previousX,
        double previousY
    ) {
        if (torchwoodModified
            || !tags.contains(PlantTag.PEA)
            || Math.abs(posY - previousY) >= 0.001) {
            return;
        }
        Plant torchwood = state.getBoard().getFirstTorchwoodCrossed(
            (int) Math.round(posY),
            previousX,
            posX,
            passedModifiers
        );
        if (torchwood == null) {
            return;
        }
        passedModifiers.add(torchwood);
        torchwoodModified = true;
        int multiplier = torchwood.isBlueFlame() ? 3 : 2;
        damage *= multiplier;
        splashDamage *= multiplier;
        elementType = ElementType.FIRE;
        state.logEvent("Torchwood changed a passing pea into "
            + (multiplier == 3 ? "blue" : "normal")
            + " fire.\n");
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
        Grave grave = graveTile.getGrave();
        if (grave == null) return;
        grave.takeDamage(damage);
        state.logEvent(
            "Grave at (" + (graveTile.getColumn() + 1)
                + ", " + (graveTile.getLane() + 1)
                + ") took " + damage + " damage.\n"
        );
        if (!grave.isDestroyed()) {
            return;
        }
        if (grave.isHasSun()) {
            state.increaseSunBalance(50);

            state.logEvent(
                "The grave contained 50 sun. "
                    + "You now have " + state.getSun() + " sun.\n"
            );
        }
        if (grave.isHasPlantFood()) {
            boolean collected = state.addPlantFood();
            if (collected) {
                state.logEvent(
                    "The grave contained a Plant Food. "
                        + "You now have " + state.getPlantFoodCount() + " Plant Foods.\n"
                );
            } else {
                state.logEvent(
                    "The grave contained a Plant Food, " + "but your Plant Food storage is full.\n"
                );
            }
        }
        graveTile.removeGrave();
        state.logEvent(
            "Grave at (" + (graveTile.getColumn() + 1) + ", " + (graveTile.getLane() + 1) + ") was destroyed.\n"
        );
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
        if (graveTarget) {
            hitLandingTarget(state);
        } else if (aoeRadius > 0 && primaryTarget != null) {
            hitArea(state, primaryTarget);
            if (movingStrategy instanceof BounceMove bounceMove && pierceRemaining > 0) {
                bounceMove.onHit();
                return;
            }
            destroy(state);
        } else if (aoeRadius > 0) {
            hitArea(state, null);
            destroy(state);
        } else if (primaryTarget != null) {
            hit(primaryTarget, state);
            if (movingStrategy instanceof BounceMove bounceMove
                && pierceRemaining > 0) {
                bounceMove.onHit();
                return;
            }
            if (pierceRemaining <= 0) {
                destroy(state);
            }
        } else if (targetX != null) {
            hitLandingTarget(state);
        } else {
            destroy(state);
        }
    }

    private void hitArea(GameState state, Zombie explicitPrimary) {
        Zombie primary = explicitPrimary != null
            ? explicitPrimary
            : state.getBoard().getZombieNear(
            (int) Math.round(targetY == null ? posY : targetY),
            targetX == null ? posX : targetX,
            0.75
        );
        List<Zombie> targets = state.getBoard().getZombiesInRadius(
            posY,
            posX,
            aoeRadius
        );
        if (targets.isEmpty() && primary != null) {
            hit(primary, state);
            return;
        }
        boolean primaryHit = false;
        for (Zombie zombie : targets) {
            if (zombie == primary) {
                hit(zombie, state, damage);
                primaryHit = true;
            } else {
                damageWithoutConsumingPierce(zombie, state, splashDamage);
            }
        }
        if (!primaryHit && primary != null) {
            hit(primary, state, damage);
        }
    }

    private void damageWithoutConsumingPierce(Zombie zombie, GameState state, int appliedDamage) {
        boolean protectedByIce = zombie.hasIceShell();
        zombie.takeDamage(appliedDamage, elementType, state, sourcePlant);
        if (!protectedByIce) {
            elementType.onHit(zombie, state, effectDurationTicks, sourcePlant);
        }
        alreadyHit.add(zombie);
    }

    private void hitLandingTarget(GameState state) {
        if (graveTarget) {
            Tile tile = state.getBoard().getTile(
                (int) Math.round(targetY),
                (int) Math.round(targetX)
            );
            if (tile != null && tile.hasGrave()) {
                damageGrave(state, tile);
                hitGraveSplash(state);
            }
            destroy(state);
            return;
        }

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

    private void hitGraveSplash(GameState state) {
        if (aoeRadius <= 0) {
            return;
        }
        for (Zombie zombie : state.getBoard().getZombiesInRadius(
            targetY,
            targetX,
            aoeRadius
        )) {
            hit(zombie, state, splashDamage);
        }
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

    public boolean isStraightShot() {
        return movingStrategy instanceof StraightMove;
    }

    private boolean tryReflectByJester(GameState state, Zombie zombie) {
        if (reflected || !(movingStrategy instanceof StraightMove) || graveTarget) {
            return false;
        }
        DamageReactionBehavior reaction = zombie.getBehavior(DamageReactionBehavior.class);
        if (reaction == null
            || reaction.getType() != DamageReactionBehavior.DamageReactionType.REFLECT_PROJECTILE
            || !reaction.isSpinning()) {
            return false;
        }
        reflected = true;
        alreadyHit.add(zombie);
        state.logEvent("Jester " + zombie.getAlias()
            + " reflected a projectile back toward your plants!\n");
        return true;
    }

    private void hitPlantIfCrossedWhileReflected(GameState state, double previousX) {
        int lane = (int) Math.round(posY);
        int fromColumn = (int) Math.floor(previousX);
        int toColumn = (int) Math.floor(posX);
        for (int column = fromColumn; column >= toColumn; column--) {
            if (column < 0 || column >= state.getBoard().getColumnCount()) {
                continue;
            }
            Tile tile = state.getBoard().getTile(lane, column);
            if (tile == null || !tile.hasTopPlant()) {
                continue;
            }
            Plant plant = tile.getTopPlant();
            if (plant.isFrozenByIce()) {
                plant.damageIce(damage, elementType, state);
                state.logEvent("A reflected projectile hit the ice around "
                    + plant.getName() + ". It has "
                    + plant.getIceHealth() + " health left.\n");
            } else if (elementType == ElementType.ICE) {
                plant.addFrostLevel(state, "reflected icy projectile");
            } else {
                plant.takeDamage(damage, state);
                state.logEvent("A reflected projectile hit " + plant.getName()
                    + " at (" + (column + 1) + ", " + (lane + 1) + ") for "
                    + damage + " damage.\n");
            }
            destroy(state);
            return;
        }
    }

    private boolean isOutOfBounds(GameState state) {
        return posX < 0
            || posX > state.getBoard().getColumnCount()
            || posY < 0
            || posY > state.getBoard().getLaneCount() - 1;
    }
}
