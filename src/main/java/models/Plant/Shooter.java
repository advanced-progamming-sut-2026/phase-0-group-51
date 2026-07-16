package models.Plant;

import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.StarMove;
import models.projectile.move.StraightMove;

import java.util.List;

import static models.Plant.PlantEnumSupport.projectileSpeed;

public enum Shooter implements PlantType {
    PEASHOOTER(6, ShotPattern.FORWARD, 1, ElementType.NORMAL, 9),
    REPEATER(7, ShotPattern.FORWARD, 2, ElementType.NORMAL, 9),
    THREEPEATER(8, ShotPattern.THREE_LANES, 1, ElementType.NORMAL, 9),
    SNOW_PEA(9, ShotPattern.FORWARD, 1, ElementType.ICE, 9),
    ROTOBAGA(10, ShotPattern.ROTOBAGA, 1, ElementType.NORMAL, 9),
    PEA_POD(11, ShotPattern.PEA_POD, 1, ElementType.NORMAL, 9),
    SPLIT_PEA(12, ShotPattern.SPLIT_PEA, 1, ElementType.NORMAL, 9),
    CITRON(13, ShotPattern.CITRON, 1, ElementType.NORMAL, 9),
    FIRE_PEASHOOTER(18, ShotPattern.FORWARD, 1, ElementType.FIRE, 9),
    STARFRUIT(19, ShotPattern.STARFRUIT, 1, ElementType.NORMAL, 9),
    GOO_PEASHOOTER(20, ShotPattern.FORWARD, 1, ElementType.POISON, 9),
    MEGA_GATLING_PEA(21, ShotPattern.FORWARD, 4, ElementType.NORMAL, 9),
    SEA_SHROOM(22, ShotPattern.SHORT_RANGE, 1, ElementType.NORMAL, 3),
    PUFF_SHROOM(23, ShotPattern.SHORT_RANGE, 1, ElementType.NORMAL, 3);

    private static final double RAY_TOLERANCE = 0.45;
    private static final int MAX_PEA_POD_HEADS = 5;
    private static final double SHROOM_LIFESPAN_SECONDS = 60;

    private final int id;
    private final ShotPattern pattern;
    private final int shotCount;
    private final ElementType element;
    private final double baseRange;

    Shooter(
            int id,
            ShotPattern pattern,
            int shotCount,
            ElementType element,
            double baseRange
    ) {
        this.id = id;
        this.pattern = pattern;
        this.shotCount = shotCount;
        this.element = element;
        this.baseRange = baseRange;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onPlanted(Plant plant, GameState state) {
        if (this == SEA_SHROOM || this == PUFF_SHROOM) {
            plant.setLifespanSeconds(
                    PlantEnumSupport.upgradedLifespan(
                            plant,
                            SHROOM_LIFESPAN_SECONDS
                    ),
                    state
            );
        }
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        fire(plant, state);
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (this == PEA_POD) {
            fireGiantPeas(plant, state);
        } else if (this == CITRON) {
            clearLane(plant, state);
        } else if (this == SEA_SHROOM || this == PUFF_SHROOM) {
            resetFamilyLifespans(plant, state);
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        if (this != PEA_POD && this != CITRON) {
            fire(plant, state);
        }
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        if (this == PEA_POD || this == CITRON) {
            return 0;
        }
        return 10;
    }

    @Override
    public boolean canStackOn(Plant existing) {
        return this == PEA_POD
                && existing != null
                && existing.getPlantType() == PEA_POD
                && existing.getStackCount() < MAX_PEA_POD_HEADS;
    }

    @Override
    public void onStacked(Plant existing, GameState state) {
        existing.incrementStackCount(MAX_PEA_POD_HEADS);
        state.logEvent("Pea Pod at (" + (existing.getPosX() + 1)
                + ", " + (existing.getPosY() + 1) + ") now has "
                + existing.getStackCount() + " heads.\n");
    }

    private void fire(Plant plant, GameState state) {
        if (!hasTarget(plant, state)) {
            if (this == CITRON) {
                plant.forceReady(state);
            }
            return;
        }
        switch (pattern) {
            case FORWARD, CITRON, SHORT_RANGE -> shootForward(plant, state);
            case THREE_LANES -> shootThreeLanes(plant, state);
            case ROTOBAGA -> shootRotobagaDirections(plant, state);
            case STARFRUIT -> shootDirections(
                    plant,
                    state,
                    StarMove.STARFRUIT_DIRECTIONS
            );
            case PEA_POD -> shootPeaPod(plant, state);
            case SPLIT_PEA -> shootSplitPea(plant, state);
        }
    }

    private boolean hasTarget(Plant plant, GameState state) {
        return switch (pattern) {
            case FORWARD, PEA_POD, CITRON -> zombieAheadInLane(
                    plant,
                    state,
                    plant.getPosY(),
                    baseRange
            );
            case SHORT_RANGE -> zombieAheadInLane(
                    plant,
                    state,
                    plant.getPosY(),
                    PlantEnumSupport.upgradedRange(plant, baseRange)
            );
            case THREE_LANES -> zombieInThreepeaterRange(plant, state);
            case ROTOBAGA -> hasZombieInAnyDirection(
                    plant,
                    state,
                    StarMove.ROTOBAGA_DIRECTIONS
            );
            case STARFRUIT -> hasStarfruitTarget(plant, state);
            case SPLIT_PEA -> zombieAheadInLane(
                    plant,
                    state,
                    plant.getPosY(),
                    baseRange
            ) || zombieBehindInLane(plant, state);
        };
    }

    private void shootForward(Plant plant, GameState state) {
        for (int i = 0; i < shotCount; i++) {
            addStraightProjectile(
                    plant,
                    state,
                    plant.getPosY(),
                    plant.getDamage()
            );
        }
    }

    private void shootPeaPod(Plant plant, GameState state) {
        for (int i = 0; i < plant.getStackCount(); i++) {
            addStraightProjectile(
                    plant,
                    state,
                    plant.getPosY(),
                    plant.getDamage()
            );
        }
    }

    private void shootSplitPea(Plant plant, GameState state) {
        if (zombieAheadInLane(plant, state, plant.getPosY(), baseRange)) {
            addStraightProjectile(
                    plant,
                    state,
                    plant.getPosY(),
                    plant.getDamage()
            );
        }
        if (zombieBehindInLane(plant, state)) {
            addDirectionalProjectile(
                    plant,
                    state,
                    new double[]{-1, 0},
                    plant.getDamage()
            );
            addDirectionalProjectile(
                    plant,
                    state,
                    new double[]{-1, 0},
                    plant.getDamage()
            );
        }
    }

    private void shootThreeLanes(Plant plant, GameState state) {
        int firstLane = Math.max(0, plant.getPosY() - 1);
        int lastLane = Math.min(
                state.getBoard().getLaneCount() - 1,
                plant.getPosY() + 1
        );
        for (int lane = firstLane; lane <= lastLane; lane++) {
            addStraightProjectile(plant, state, lane, plant.getDamage());
        }
    }

    private void shootRotobagaDirections(Plant plant, GameState state) {
        for (double[] direction : StarMove.ROTOBAGA_DIRECTIONS) {
            if (hasZombieInDirection(plant, state, direction)) {
                addDirectionalProjectile(
                        plant,
                        state,
                        direction,
                        plant.getDamage()
                );
            }
        }
    }

    private void shootDirections(
            Plant plant,
            GameState state,
            double[][] directions
    ) {
        for (double[] direction : directions) {
            addDirectionalProjectile(
                    plant,
                    state,
                    direction,
                    plant.getDamage()
            );
        }
    }

    private void fireGiantPeas(Plant plant, GameState state) {
        for (int i = 0; i < plant.getStackCount(); i++) {
            addStraightProjectile(
                    plant,
                    state,
                    plant.getPosY(),
                    plant.getDamage() * 20
            );
        }
    }

    private void clearLane(Plant plant, GameState state) {
        for (Zombie zombie : state.getBoard().getZombiesInLane(
                plant.getPosY()
        )) {
            zombie.takeDamage(
                    Integer.MAX_VALUE,
                    ElementType.NORMAL,
                    state,
                    plant
            );
        }
        state.logEvent("Citron plant food cleared row "
                + (plant.getPosY() + 1) + ".\n");
    }

    private void resetFamilyLifespans(Plant plant, GameState state) {
        double seconds = PlantEnumSupport.upgradedLifespan(
                plant,
                SHROOM_LIFESPAN_SECONDS
        );
        for (Plant other : state.getBoard().getAllPlants()) {
            if (other.getId() == plant.getId()) {
                other.resetLifespanSeconds(seconds, state);
            }
        }
    }

    private void addDirectionalProjectile(
            Plant plant,
            GameState state,
            double[] direction,
            int damage
    ) {
        state.getBoard().addProjectile(Projectile.directional(
                damage,
                element,
                plant.getPlantTags(),
                projectileSpeed(plant, 0.5),
                plant.getPosX(),
                plant.getPosY(),
                direction[0],
                direction[1],
                new StarMove()
        ).withSource(plant));
    }

    private boolean hasStarfruitTarget(Plant plant, GameState state) {
        for (double[] direction : StarMove.STARFRUIT_DIRECTIONS) {
            if (hasZombieInDirection(plant, state, direction)
                    || hasGraveInDirection(plant, state, direction)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasZombieInAnyDirection(
            Plant plant,
            GameState state,
            double[][] directions
    ) {
        for (double[] direction : directions) {
            if (hasZombieInDirection(plant, state, direction)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasZombieInDirection(
            Plant plant,
            GameState state,
            double[] direction
    ) {
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (!zombie.isDead() && isOnRay(
                    plant.getPosX(),
                    plant.getPosY(),
                    zombie.getX(),
                    zombie.getLane(),
                    direction
            )) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGraveInDirection(
            Plant plant,
            GameState state,
            double[] direction
    ) {
        for (int lane = 0;
             lane < state.getBoard().getLaneCount();
             lane++) {
            for (int column = 0;
                 column < state.getBoard().getColumnCount();
                 column++) {
                Tile tile = state.getBoard().getTile(lane, column);
                if (tile != null && tile.hasGrave() && isOnRay(
                        plant.getPosX(),
                        plant.getPosY(),
                        column,
                        lane,
                        direction
                )) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isOnRay(
            double originX,
            double originY,
            double targetX,
            double targetY,
            double[] direction
    ) {
        double length = Math.hypot(direction[0], direction[1]);
        double unitX = direction[0] / length;
        double unitY = direction[1] / length;
        double relativeX = targetX - originX;
        double relativeY = targetY - originY;
        double forwardDistance = relativeX * unitX + relativeY * unitY;
        if (forwardDistance <= 0) {
            return false;
        }
        double perpendicularDistance = Math.abs(
                relativeX * unitY - relativeY * unitX
        );
        return perpendicularDistance <= RAY_TOLERANCE;
    }

    private void addStraightProjectile(
            Plant plant,
            GameState state,
            int lane,
            int damage
    ) {
        state.getBoard().addProjectile(Projectile.straight(
                damage,
                element,
                plant.getPlantTags(),
                projectileSpeed(plant, 0.5),
                plant.getPosX(),
                lane,
                new StraightMove(),
                1,
                effectDurationTicks(plant, state)
        ).withSource(plant));
    }

    private int effectDurationTicks(Plant plant, GameState state) {
        if (element != ElementType.ICE) {
            return 0;
        }
        return (int) Math.round(
                plant.getPlantStat().chillDuration()
                        * state.getTicksPerSecond()
        );
    }

    private static boolean zombieAheadInLane(
            Plant plant,
            GameState state,
            int lane,
            double range
    ) {
        List<Zombie> zombies = state.getBoard().getZombiesInLane(lane);
        for (Zombie zombie : zombies) {
            double distance = zombie.getX() - plant.getPosX();
            if (!zombie.isDead() && distance >= 0 && distance <= range) {
                return true;
            }
        }
        return false;
    }

    private static boolean zombieBehindInLane(
            Plant plant,
            GameState state
    ) {
        for (Zombie zombie : state.getBoard().getZombiesInLane(
                plant.getPosY()
        )) {
            if (!zombie.isDead() && zombie.getX() < plant.getPosX()) {
                return true;
            }
        }
        return false;
    }

    private static boolean zombieInThreepeaterRange(
            Plant plant,
            GameState state
    ) {
        int firstLane = Math.max(0, plant.getPosY() - 1);
        int lastLane = Math.min(
                state.getBoard().getLaneCount() - 1,
                plant.getPosY() + 1
        );
        for (int lane = firstLane; lane <= lastLane; lane++) {
            if (zombieAheadInLane(plant, state, lane, 9)) {
                return true;
            }
        }
        return false;
    }

    private enum ShotPattern {
        FORWARD,
        THREE_LANES,
        ROTOBAGA,
        STARFRUIT,
        PEA_POD,
        SPLIT_PEA,
        CITRON,
        SHORT_RANGE
    }
}
