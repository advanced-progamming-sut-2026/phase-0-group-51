package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;
import models.projectile.move.StraightMove;

import java.util.Arrays;
import java.util.List;


public enum Shooter implements PlantType {

    PEASHOOTER(6,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withDamage(current.damage() + 10);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withMaxHp(current.maxHp() + 150);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withCost(current.cost() - 25);
                }
            }) {
        @Override
        public void onTick(Plant plant, GameState state) {
            shootStraight(plant, state, 1, ElementType.NORMAL);
        }

        @Override
        public void onFeed(Plant plant, GameState gameState) {

        }

        @Override
        public void onFoodTick(Plant plant, GameState state) {
            // rapid fire: just fire straight shots every tick of the boost
            shootStraight(plant, state, 1, ElementType.NORMAL);
        }
    },

    // REPEATER(7, ...) {
    //     @Override
    //     public void onTick(Plant plant, GameState state) {
    //         shootStraight(plant, state, 2, ElementType.NORMAL);
    //     }
    // },
    ;

    private final int id;
    private final List<PlantUpgrade> upgrades;

    Shooter(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);
    }

    public Plant create() {
        PlantData data = PlantRegistry.get(id);
        PlantStats baseStats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                data.recharge(),
                0
        );
        return new Plant(
                data.id(), data.name(), this,
                baseStats,
                upgrades,
                data.tags()
        );
    }

    // helpers for shooters

    static void shootStraight(Plant plant, GameState state, int shotCount, ElementType element) {
        if (!zombieInLane(plant, state)) return;
        for (int i = 0; i < shotCount; i++) {
            state.getBoard().addProjectile(new Projectile(
                    plant.getDamage(), element, plant.getPlantTags(),
                    plant.getPlantStat().projectileSpeed(),
                    plant.getPosX(), plant.getPosY(), new StraightMove()));
        }
    }

    static boolean zombieInLane(Plant plant, GameState state) {
        List<Zombie> zombies = state.getBoard().getZombiesInLane(plant.getPosY());
        for (Zombie zombie : zombies) {
            if (zombie.getX() >= plant.getPosX()) return true;
        }
        return false;
    }
}