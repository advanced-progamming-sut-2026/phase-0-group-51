package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.HomingMove;

import java.util.Arrays;
import java.util.List;

public enum Homing implements PlantType {

    CAT_TAIL(55,
            current -> current.withDamage(current.damage() + 10),
            current -> current.withMaxHp(current.maxHp() + 200),
            current -> current.withCost(current.cost() - 25)) {
        @Override
        public void onTick(Plant plant, GameState state) {
            shootAtClosestZombie(plant, state);
        }

        @Override
        public void onFoodTick(Plant plant, GameState state) {
            shootAtClosestZombie(plant, state);
        }
    };

    private final int id;
    private final List<PlantUpgrade> upgrades;

    Homing(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
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

    private static void shootAtClosestZombie(Plant plant, GameState state) {
        Zombie target = state.getBoard().getClosestZombieAnywhere(
                plant.getPosY(),
                plant.getPosX()
        );
        if (target == null) {
            return;
        }
        state.getBoard().addProjectile(Projectile.homing(
                plant.getDamage(),
                ElementType.NORMAL,
                plant.getPlantTags(),
                projectileSpeed(plant),
                plant.getPosX(),
                plant.getPosY(),
                target,
                new HomingMove()
        ));
    }

    private static double projectileSpeed(Plant plant) {
        double configuredSpeed = plant.getPlantStat().projectileSpeed();
        return configuredSpeed > 0 ? configuredSpeed : 0.45;
    }
}
