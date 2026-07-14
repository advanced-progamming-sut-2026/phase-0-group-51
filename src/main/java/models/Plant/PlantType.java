package models.Plant;

import models.games.GameState;

public interface PlantType {
    void onTick(Plant plant, GameState gameState);
    void onFeed(Plant plant, GameState gameState);
    default  void onFoodTick(Plant plant, GameState gameState) {}
    default void onDeath(Plant plant, GameState gameState) {}
    default void onPlanted(Plant plant, GameState gameState) {}
    default void mintActing(Plant plant, GameState gameState){}
}
