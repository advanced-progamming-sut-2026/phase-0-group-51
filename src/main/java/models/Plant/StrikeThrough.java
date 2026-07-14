package models.Plant;

import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum StrikeThrough implements PlantType{
    ;
    private final int id;
    private final List<PlantUpgrade> upgrades;


    StrikeThrough(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);

    }

    @Override
    public void onTick(Plant plant, GameState gameState) {

    }

    @Override
    public void onFeed(Plant plant, GameState gameState) {

    }
}
