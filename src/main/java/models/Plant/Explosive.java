package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum Explosive implements PlantType {
    POTATO_MINE(30,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current;
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withRecharge(current.recharge() - 5);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withDamage(current.damage() + 600);
                }
            }
    );
    private static final float[] ARM_TICKS_BY_LEVEL = {15f, 12f, 9f, 9f};
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

    public Plant createAtLevel(int savedLevel) {
        Plant plant = create();
        for (int i = 1; i < savedLevel; i++) plant.levelUp();
        return plant;
    }

    private static float armTimeFor(int level) {
        int index = Math.max(0, Math.min(level - 1, ARM_TICKS_BY_LEVEL.length - 1));
        return ARM_TICKS_BY_LEVEL[index];
    }

    @Override
    public void onPlanted(Plant plant, GameState gameState) {
        plant.disableFor(armTimeFor(plant.getLevel()));
    }

    @Override
    public void onTick(Plant plant, GameState gameState) {
        explodeIfZombiePresent(plant, gameState);
    }

    @Override
    public void onFeed(Plant plant, GameState gameState) {
        plant.disableFor(0);
        for (Tile tile : gameState.getBoard().getTwoRandomTilesWithoutPlants()) {
            Plant clone = createAtLevel(plant.getLevel());
            gameState.plantPlant(clone, tile);
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState gameState) {
        explodeIfZombiePresent(plant, gameState);
    }

    private static void explodeIfZombiePresent(Plant plant, GameState gameState) {
        Zombie zombie = gameState.getBoard().getZombieInPosition(plant.getPosY(), plant.getPosX());
        if (zombie != null) {
            zombie.takeDamage(plant.getDamage(), gameState,plant);
            plant.setMarkedForRemoval(true);
            gameState.getBoard().removePlant(plant.getPosY(), plant.getPosX());
        }
    }


}
