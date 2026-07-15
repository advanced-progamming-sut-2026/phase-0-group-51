package models.Plant;

import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.StarMove;
import models.projectile.move.StraightMove;

import java.util.Arrays;
import java.util.List;

public enum Shooter implements PlantType {
    PEASHOOTER(6, ShotPattern.FORWARD, 1, ElementType.NORMAL),
    REPEATER(7, ShotPattern.FORWARD, 2, ElementType.NORMAL),
    THREEPEATER(8, ShotPattern.THREE_LANES, 1, ElementType.NORMAL),
    SNOW_PEA(9, ShotPattern.FORWARD, 1, ElementType.ICE),
    ROTOBAGA(10, ShotPattern.ROTOBAGA, 1, ElementType.NORMAL),
    FIRE_PEASHOOTER(18, ShotPattern.FORWARD, 1, ElementType.FIRE),
    STARFRUIT(19, ShotPattern.STARFRUIT, 1, ElementType.NORMAL),
    MEGA_GATLING_PEA(21, ShotPattern.FORWARD, 4, ElementType.NORMAL);

    private static final double RAY_TOLERANCE = 0.45;
    private final int id;
    private final ShotPattern pattern;
    private final int shotCount;
    private final ElementType element;

    Shooter(int id, ShotPattern pattern, int shotCount, ElementType element) {
        this.id = id;
        this.pattern = pattern;
        this.shotCount = shotCount;
        this.element = element;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        fire(plant, state);
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        fire(plant, state);
    }

    private void fire(Plant plant, GameState state) {
        if (!hasTarget(plant, state)) {
            return;
        }
        switch (pattern) {
            case FORWARD -> shootForward(plant, state);
            case THREE_LANES -> shootThreeLanes(plant, state);
            case ROTOBAGA -> shootRotobagaDirections(plant, state);
            case STARFRUIT -> shootDirections(
                    plant,
                    state,
                    StarMove.STARFRUIT_DIRECTIONS
            );
        }
    }

    private boolean hasTarget(Plant plant, GameState state) {
        return switch (pattern) {
            case FORWARD -> zombieAheadInLane(plant, state, plant.getPosY());
            case THREE_LANES -> zombieInThreepeaterRange(plant, state);
            case ROTOBAGA -> hasZombieInAnyDirection(
                    plant,
                    state,
                    StarMove.ROTOBAGA_DIRECTIONS
            );
            case STARFRUIT -> hasStarfruitTarget(plant, state);
        };
    }

    private void shootForward(Plant plant, GameState state) {
        for (int i = 0; i < shotCount; i++) {
            addStraightProjectile(plant, state, plant.getPosY());
        }
    }

    private void shootThreeLanes(Plant plant, GameState state) {
        int firstLane = Math.max(0, plant.getPosY() - 1);
        int lastLane = Math.min(
                state.getBoard().getLaneCount() - 1,
                plant.getPosY() + 1
        );
        for (int lane = firstLane; lane <= lastLane; lane++) {
            addStraightProjectile(plant, state, lane);
        }
    }

    private void shootRotobagaDirections(Plant plant, GameState state) {
        for (double[] direction : StarMove.ROTOBAGA_DIRECTIONS) {
            if (hasZombieInDirection(plant, state, direction)) {
                addDirectionalProjectile(plant, state, direction);
            }
        }
    }

    private void shootDirections(
            Plant plant,
            GameState state,
            double[][] directions
    ) {
        for (double[] direction : directions) {
            addDirectionalProjectile(plant, state, direction);
        }
    }

    private void addDirectionalProjectile(
            Plant plant,
            GameState state,
            double[] direction
    ) {
        state.getBoard().addProjectile(Projectile.directional(
                plant.getDamage(),
                element,
                plant.getPlantTags(),
                PlantEnumSupport.projectileSpeed(plant, 0.5),
                plant.getPosX(),
                plant.getPosY(),
                direction[0],
                direction[1],
                new StarMove()
        ));
    }

    static void shootStraight(Plant plant, GameState state, int shotCount, ElementType element) {
        if (!zombieInLane(plant, state)) {
            return;
        }
        for (int i = 0; i < shotCount; i++) {
            state.getBoard().addProjectile(Projectile.straight(
                    plant.getDamage(),
                    element,
                    plant.getPlantTags(),
                    projectileSpeed(plant),
                    plant.getPosX(),
                    plant.getPosY(),
                    new StraightMove(),
                    1,
                    effectDurationTicks(plant, state, element)
            ).withSource(plant));
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
        for (int lane = 0; lane < state.getBoard().getLaneCount(); lane++) {
            for (int column = 0; column < state.getBoard().getColumnCount(); column++) {
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

    private void addStraightProjectile(Plant plant, GameState state, int lane) {
        state.getBoard().addProjectile(Projectile.straight(
                plant.getDamage(),
                element,
                plant.getPlantTags(),
                PlantEnumSupport.projectileSpeed(plant, 0.5),
                plant.getPosX(),
                lane,
                new StraightMove(),
                1,
                effectDurationTicks(plant, state)
        ));
    }

    private int effectDurationTicks(Plant plant, GameState state) {
        if (element != ElementType.ICE) {
            return 0;
        }
        return (int) Math.round(
                plant.getPlantStat().chillDuration() * state.getTicksPerSecond()
        );
    }

    private static boolean zombieAheadInLane(
            Plant plant,
            GameState state,
            int lane
    ) {
        List<Zombie> zombies = state.getBoard().getZombiesInLane(lane);
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && zombie.getX() >= plant.getPosX()) {
                return true;
            }
        }
        return false;
    }

    private static boolean zombieInThreepeaterRange(Plant plant, GameState state) {
        int firstLane = Math.max(0, plant.getPosY() - 1);
        int lastLane = Math.min(
                state.getBoard().getLaneCount() - 1,
                plant.getPosY() + 1
        );
        for (int lane = firstLane; lane <= lastLane; lane++) {
            if (zombieAheadInLane(plant, state, lane)) {
                return true;
            }
        }
        return false;
    }

    private enum ShotPattern {
        FORWARD,
        THREE_LANES,
        ROTOBAGA,
        STARFRUIT
    }
}
