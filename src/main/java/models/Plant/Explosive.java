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
                    return current; // Arm Time -3s: arm time isn't a
                    // PlantStats field. Simplest fix without widening
                    // PlantStats further: keep a small per-level lookup,
                    // e.g. a static ARM_TICKS_BY_LEVEL = {15, 12, 9, 9}
                    // array in this enum, and read it by plant.getLevel()
                    // inside onPlanted below instead of a hardcoded 15.
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

    @Override
    public void onPlanted(Plant plant, GameState gameState) {
        // Arm delay: the mine can't hurt anything for 15s after planting
        // (or being re-armed by Plant Food). Make sure your GameState's
        // plantPlant(...) actually calls plantType.onPlanted(plant, state)
        // when a plant is placed — Plant.tick() never calls it on its own.
        plant.disableFor(15);
    }

    @Override
    public void onTick(Plant plant, GameState gameState) {
        Zombie zombie = gameState.getBoard().getZombieInPosition(plant.getPosY(), plant.getPosX());
        if (zombie != null) {
            zombie.takeDamage(plant.getDamage(), gameState);
            plant.setMarkedForRemoval(true);
            gameState.getBoard().removePlant(plant.getPosY(), plant.getPosX()); // it dies after the explosion
        }
    }

    @Override
    public void onFeed(Plant plant, GameState gameState) {
        // Re-arm instantly, then throw 2 *new* clone mines onto other tiles.
        // Reusing the same `plant` instance for both tiles (as before) would
        // just teleport one mine back and forth rather than cloning it, and
        // it would never explode on its original tile again.
        plant.disableFor(0);
        for (Tile tile : gameState.getBoard().getTwoRandomTilesWithoutPlants()) {
            Plant clone = createAtLevel(plant.getLevel());
            gameState.plantPlant(clone, tile);
        }
    }
}