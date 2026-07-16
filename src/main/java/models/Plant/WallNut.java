package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;

import java.util.ArrayList;

public enum WallNut implements PlantType {
    WALL_NUT(44, DefenseMode.PLAIN),
    TALL_NUT(45, DefenseMode.PLAIN),
    ENDURIAN(46, DefenseMode.REFLECT),
    GARLIC(47, DefenseMode.REDIRECT),
    SWEET_POTATO(48, DefenseMode.ATTRACT),
    EXPLODE_O_NUT(49, DefenseMode.EXPLODE) {
        @Override
        public void onDeath(Plant plant, GameState state) {
            int damage = plant.getDamage();
            if (plant.getLevel() >= 3) {
                damage += 200;
            }
            for (Zombie zombie : state.getBoard().getZombiesInSquare(
                    plant.getPosY(),
                    plant.getPosX(),
                    1,
                    1
            )) {
                zombie.takeDamage(damage, state, plant);
            }
        }
    },
    SUN_BEAN(51, DefenseMode.SUN_ON_BITE);

    private static final double SWEET_POTATO_ATTRACT_RANGE = 1.5;
    private static final double SWEET_POTATO_FOOD_RANGE = 3.0;

    private final int id;
    private final DefenseMode mode;

    WallNut(int id, DefenseMode mode) {
        this.id = id;
        this.mode = mode;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        return;
    }

    @Override
    public void onEveryTick(Plant plant, GameState state) {
        if (mode == DefenseMode.ATTRACT) {
            attractNearbyZombies(
                    plant,
                    state,
                    SWEET_POTATO_ATTRACT_RANGE
            );
        }
    }

    @Override
    public void onEatenBy(
            Plant plant,
            Zombie zombie,
            int damage,
            GameState state
    ) {
        switch (mode) {
            case REFLECT -> reflectDamage(plant, zombie, state);
            case REDIRECT -> state.getBoard()
                    .moveZombieToAdjacentLane(zombie, state);
            case SUN_ON_BITE -> produceBiteSun(plant, state);
            default -> {
                return;
            }
        }
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (mode == DefenseMode.REDIRECT) {
            for (Zombie zombie : new ArrayList<>(
                    state.getBoard().getZombiesInLane(plant.getPosY())
            )) {
                state.getBoard().moveZombieToAdjacentLane(zombie, state);
            }
            return;
        }
        if (mode == DefenseMode.ATTRACT) {
            attractNearbyZombies(
                    plant,
                    state,
                    SWEET_POTATO_FOOD_RANGE
            );
            plant.healToFull();
            return;
        }
        plant.addArmor(plant.getPlantStat().maxHp());
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        return 0;
    }

    private static void reflectDamage(
            Plant plant,
            Zombie zombie,
            GameState state
    ) {
        int reflected = plant.getDamage();
        if (plant.getLevel() >= 2) {
            reflected += 5;
        }
        if (plant.hasTemporaryArmor()) {
            reflected *= 2;
        }
        zombie.takeDamage(reflected, state, plant);
    }

    private static void produceBiteSun(Plant plant, GameState state) {
        int amount = plant.getLevel() >= 2 ? 10 : 5;
        state.increaseSunBalance(amount);
        state.logEvent(plant.getName() + " produced " + amount
                + " suns after being bitten.\n");
    }

    private static void attractNearbyZombies(
            Plant plant,
            GameState state,
            double horizontalRange
    ) {
        for (Zombie zombie : new ArrayList<>(
                state.getZombiesInTheGame()
        )) {
            if (zombie.isDead()
                    || Math.abs(zombie.getLane() - plant.getPosY()) != 1
                    || Math.abs(zombie.getX() - plant.getPosX())
                    > horizontalRange) {
                continue;
            }
            int oldLane = zombie.getLane();
            zombie.setLane(plant.getPosY());
            state.logEvent(plant.getName() + " attracted "
                    + zombie.getAlias() + " from row "
                    + (oldLane + 1) + " to row "
                    + (plant.getPosY() + 1) + ".\n");
        }
    }

    private enum DefenseMode {
        PLAIN,
        REFLECT,
        REDIRECT,
        ATTRACT,
        EXPLODE,
        SUN_ON_BITE
    }
}
