package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum Explosive implements PlantType{
    POTATO_MINE(30,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current;//arm time changes
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current;//cooldown time changes
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withDamage(current.damage() + 600);
                }
            }
    );
    private final int id;
    private final List<PlantUpgrade> upgrades;



    Explosive(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
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
                0
        );
        return new Plant(
                data.id(), data.name(), this,
                baseStats,
                upgrades,
                data.tags()
        );
    }

    public Plant createAtLevel(int savedLevel) {
        Plant plant = create();
        for (int i = 1; i < savedLevel; i++) plant.levelUp();
        return plant;
    }


    @Override
    public void onTick(Plant plant, GameState gameState) {

    }

    @Override
    public void onPlantFood(Plant plant, GameState gameState) {

    }

}
