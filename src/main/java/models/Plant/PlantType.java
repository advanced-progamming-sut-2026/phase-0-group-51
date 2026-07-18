package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;

public interface PlantType {
    void onTick(Plant plant, GameState gameState);

    default void onEveryTick(Plant plant, GameState gameState) {
        return;
    }

    default void onFeed(Plant plant, GameState gameState) {
        return;
    }

    default void onFoodTick(Plant plant, GameState gameState) {
        return;
    }

    default int plantFoodDurationTicks(Plant plant, GameState gameState) {
        return 10;
    }

    default void onDeath(Plant plant, GameState gameState) {
        return;
    }

    default void onArmorBroken(Plant plant, GameState gameState) {
        return;
    }

    default void onZombieKilled(Plant plant, Zombie zombie, GameState gameState) {
        return;
    }

    default void onPlanted(Plant plant, GameState gameState) {
        return;
    }

    default void onEatenBy(
            Plant plant,
            Zombie zombie,
            int damage,
            GameState gameState
    ) {
        return;
    }

    default boolean canStackOn(Plant existing) {
        return false;
    }

    default void onStacked(Plant existing, GameState gameState) {
        return;
    }

    default void mintActing(Plant plant, GameState gameState) {
        return;
    }

    default boolean isLobber() {
        return false;
    }

    default boolean blocksJumping() {
        return false;
    }

    default double actionIntervalSeconds(Plant plant) {
        return plant.getPlantStat().actionInterval();
    }
}
