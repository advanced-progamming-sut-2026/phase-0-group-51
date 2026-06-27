package models.Plant;

import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum WallNut implements PlantType{
    WALL_NUT(44,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withMaxHp(current.maxHp() + 1000);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current;//cooldown
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withMaxHp(current.maxHp() + 1500);
                }
            }
    );
    private final int id;
    private final List<PlantUpgrade> upgrades;


    WallNut(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);

    }

    @Override
    public void onTick(Plant plant, GameState gameState) {
        //just block the way
    }

    @Override
    public void onPlantFood(Plant plant, GameState gameState) {
        plant.addArmor(4000);
    }
}
