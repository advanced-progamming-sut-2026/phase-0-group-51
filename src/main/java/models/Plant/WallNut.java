package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum WallNut implements PlantType {

    WALL_NUT(44,
            current -> current.withMaxHp(current.maxHp() + 1000),
            current -> current.withRecharge(current.recharge() - 5),
            current -> current.withMaxHp(current.maxHp() + 1500)) {
        @Override
        public void onTick(Plant plant, GameState state) {
            return;
        }

        @Override
        public void onFeed(Plant plant, GameState state) {
            plant.addArmor(plant.getPlantStat().maxHp());
        }
    };

    private final int id;
    private final List<PlantUpgrade> upgrades;

    WallNut(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
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
}
