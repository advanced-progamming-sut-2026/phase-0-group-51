package models.Plant;

import models.games.GameState;

public interface PlantType {
    void onTick(Plant plant, GameState gameState);

    default void onFeed(Plant plant, GameState gameState) {
        return;
    }

    default void onFoodTick(Plant plant, GameState gameState) {
        return;
    }

    default void onDeath(Plant plant, GameState gameState) {
        return;
    }

    default void onPlanted(Plant plant, GameState gameState) {
        return;
    }

    default void mintActing(Plant plant, GameState gameState) {
        return;
    }

    default boolean isLobber() {
        return false;
    }
}
