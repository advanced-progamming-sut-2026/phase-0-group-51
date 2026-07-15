package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.StraightMove;

import java.util.Arrays;
import java.util.List;

public enum Shooter implements PlantType {

    PEASHOOTER(6,
            damageUpgrade(10),
            healthUpgrade(150),
            costUpgrade(-25)) {
        @Override
        public void onTick(Plant plant, GameState state) {
            shootStraight(plant, state, 1, ElementType.NORMAL);
        }

        @Override
        public void onFoodTick(Plant plant, GameState state) {
            shootStraight(plant, state, 1, ElementType.NORMAL);
        }
    },

    REPEATER(7,
            damageUpgrade(10),
            healthUpgrade(200),
            costUpgrade(-25)) {
        @Override
        public void onTick(Plant plant, GameState state) {
            shootStraight(plant, state, 2, ElementType.NORMAL);
        }

        @Override
        public void onFoodTick(Plant plant, GameState state) {
            shootStraight(plant, state, 2, ElementType.NORMAL);
        }
    },

    SNOW_PEA(9,
            damageUpgrade(10),
            chillUpgrade(2),
            costUpgrade(-25)) {
        @Override
        public void onTick(Plant plant, GameState state) {
            shootStraight(plant, state, 1, ElementType.ICE);
        }

        @Override
        public void onFoodTick(Plant plant, GameState state) {
            shootStraight(plant, state, 1, ElementType.ICE);
        }
    };

    private final int id;
    private final List<PlantUpgrade> upgrades;

    Shooter(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);
    }

    public Plant create() {
        PlantData data = PlantRegistry.get(id);
        PlantStats baseStats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                data.recharge(),
                data.projectileSpeed()
        );
        return new Plant(
                data.id(),
                data.name(),
                this,
                baseStats,
                upgrades,
                data.tags()
        );
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
        }
    }

    static boolean zombieInLane(Plant plant, GameState state) {
        List<Zombie> zombies = state.getBoard().getZombiesInLane(plant.getPosY());
        for (Zombie zombie : zombies) {
            if (zombie.getX() >= plant.getPosX()) {
                return true;
            }
        }
        return false;
    }


    private static int effectDurationTicks(
            Plant plant,
            GameState state,
            ElementType element
    ) {
        if (element != ElementType.ICE) {
            return 0;
        }
        return (int) Math.round(
                plant.getPlantStat().chillDuration() * state.getTicksPerSecond()
        );
    }

    private static double projectileSpeed(Plant plant) {
        double configuredSpeed = plant.getPlantStat().projectileSpeed();
        return configuredSpeed > 0 ? configuredSpeed : 0.5;
    }

    private static PlantUpgrade damageUpgrade(int amount) {
        return current -> current.withDamage(current.damage() + amount);
    }

    private static PlantUpgrade healthUpgrade(int amount) {
        return current -> current.withMaxHp(current.maxHp() + amount);
    }

    private static PlantUpgrade costUpgrade(int amount) {
        return current -> current.withCost(current.cost() + amount);
    }

    private static PlantUpgrade chillUpgrade(double seconds) {
        return current -> current.apply(PlantStatKey.CHILL_DURATION, "ADD", seconds);
    }
}
