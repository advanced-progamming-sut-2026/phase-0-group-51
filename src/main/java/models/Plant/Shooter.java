package models.Plant;

import models.games.GameState;

public enum Shooter implements PlantType{
    ;
    private final int id;
    private final PlantUpgrade upgrade2;
    private final PlantUpgrade upgrade3;
    private final PlantUpgrade upgrade4;

    Shooter(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrade2 = upgrade2;
        this.upgrade3 = upgrade3;
        this.upgrade4 = upgrade4;
    }

    @Override
    public void onTick(Plant plant, GameState gameState) {

    }

    @Override
    public void onPlantFood(Plant plant, GameState gameState) {

    }
}
