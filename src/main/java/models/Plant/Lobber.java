package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;

import java.util.Arrays;
import java.util.List;

public enum Lobber implements PlantType {

    CABBAGE_PULT(25,
            current -> current.withDamage(current.damage() + 10),
            current -> current.withInterval(current.actionInterval() * 0.85),
            current -> current.withMaxHp(current.maxHp() + 150)) {
        @Override
        public void onTick(Plant plant, GameState state) {
            Zombie target = state.getBoard().getFirstZombieAheadInLane(
                    plant.getPosY(),
                    plant.getPosX()
            );
            if (target == null) {
                return;
            }
            launchAt(plant, state, target);
        }

        @Override
        public void onFoodTick(Plant plant, GameState state) {
            for (Zombie zombie : state.getBoard().getRandomZombies(3)) {
                launchAt(plant, state, zombie);
            }
        }
    };

    private final int id;
    private final List<PlantUpgrade> upgrades;

    Lobber(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
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

    @Override
    public boolean isLobber() {
        return true;
    }

    private static void launchAt(Plant plant, GameState state, Zombie target) {
        state.getBoard().addProjectile(Projectile.targeted(
                plant.getDamage(),
                ElementType.NORMAL,
                plant.getPlantTags(),
                projectileSpeed(plant),
                plant.getPosX(),
                plant.getPosY(),
                target.getX(),
                target.getLane(),
                new ArcMove(),
                0
        ).withSource(plant));
    }

    private static double projectileSpeed(Plant plant) {
        double configuredSpeed = plant.getPlantStat().projectileSpeed();
        return configuredSpeed > 0 ? configuredSpeed : 0.35;
    }
}
