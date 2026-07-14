package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum Explosive implements PlantType {
    POTATO_MINE(30,
            current -> current,
            current -> current.withRecharge(current.recharge() - 5),
            current -> current.withDamage(current.damage() + 600));

    private static final float[] ARM_SECONDS_BY_LEVEL = {15f, 12f, 9f, 9f};
    private final int id;
    private final List<PlantUpgrade> upgrades;

    Explosive(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);
    }

    public Plant create() {
        PlantData data = PlantRegistry.get(id);
        PlantStats baseStats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                data.recharge(),
                data.projectileSpeed()
        );
        return new Plant(
                data.id(),
                data.name(),
                this,
                baseStats,
                upgrades,
                data.tags()
        );
    }

    @Override
    public void onPlanted(Plant plant, GameState state) {
        float armTicks = armSecondsFor(plant.getLevel()) * state.getTicksPerSecond();
        plant.disableFor(armTicks);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        explodeIfZombiePresent(plant, state);
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        plant.disableFor(0);
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        explodeIfZombiePresent(plant, state);
    }

    private static float armSecondsFor(int level) {
        int index = Math.max(0, Math.min(level - 1, ARM_SECONDS_BY_LEVEL.length - 1));
        return ARM_SECONDS_BY_LEVEL[index];
    }

    private static void explodeIfZombiePresent(Plant plant, GameState state) {
        Zombie zombie = state.getBoard().getZombieInPosition(
                plant.getPosY(),
                plant.getPosX()
        );
        if (zombie == null) {
            return;
        }
        zombie.takeDamage(plant.getDamage(), state, plant);
        plant.setMarkedForRemoval(true);
        state.getBoard().removePlant(plant.getPosY(), plant.getPosX());
    }
}
