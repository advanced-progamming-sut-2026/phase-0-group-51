package models.Plant;

import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum Lobber implements PlantType{
    CABBAGE_PULT(25,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withDamage(current.damage() + 10);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withInterval(current.actionInterval() * 0.85);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withMaxHp(current.maxHp() + 150);
                }
            }
    );
    private final int id;
    private final List<PlantUpgrade> upgrades;

    Lobber(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);
    }

    @Override
    public void onTick(Plant plant, GameState gameState) {

    }

    @Override
    public void onPlantFood(Plant plant, GameState gameState) {

    }
}
