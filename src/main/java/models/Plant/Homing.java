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

public enum Homing implements PlantType {

    CAT_TAIL(55,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withDamage(current.damage() + 10);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withMaxHp(current.maxHp() + 200);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withCost(current.cost() - 25);
                }
            }) {
        @Override
        public void onTick(Plant plant, GameState gameState) {
            Zombie target = gameState.getBoard().getClosestZombieAnywhere(plant.getPosX(), plant.getPosY());
            if (target != null) {
                gameState.getBoard().addProjectile(new Projectile(
                        plant.getDamage(), ElementType.NORMAL, plant.getPlantTags(),
                        plant.getPlantStat().projectileSpeed(),
                        plant.getPosX(), plant.getPosY(), new ArcMove()));
            }
        }

        @Override
        public void onFeed(Plant plant, GameState gameState) {

        }

        @Override
        public void onFoodTick(Plant plant, GameState gameState) {
            onTick(plant, gameState); // rapid-fire homing shots during boost
        }
    },
    ;

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
                data.id(), data.name(), this,
                baseStats,
                upgrades,
                data.tags()
        );
    }
}