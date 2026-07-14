package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.games.GameState;
import models.sun.Sun;
import models.sun.SunType;

import java.util.Arrays;
import java.util.List;

public enum SunProducer implements PlantType {

    SUNFLOWER(1,
            current -> current.withInterval(current.actionInterval() - 2),
            current -> current.withMaxHp(current.maxHp() + 150),
            current -> current.withDoubleSunChance(true)) {
        @Override
        public void onTick(Plant plant, GameState state) {
            produceCollectableSun(plant, state);
        }

        @Override
        public void onFeed(Plant plant, GameState state) {
            state.increaseSunBalance(150);
            state.logEvent("Sunflower plant food produced 150 suns.\n");
        }
    };

    private static final int BASE_SUN = 50;
    private static final int DOUBLE_SUN_CHANCE_PERCENT = 25;
    private final int id;
    private final List<PlantUpgrade> upgrades;

    SunProducer(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
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

    private static void produceCollectableSun(Plant plant, GameState state) {
        if (plant.isPendingSun()) {
            return;
        }
        int amount = BASE_SUN;
        if (shouldProduceDoubleSun(plant, state)) {
            amount *= 2;
        }
        plant.setPendingSun(true);
        state.getBoard().spawnSun(new Sun(
                plant.getPosX(),
                plant.getPosY(),
                plant.getPosY(),
                SunType.ORDINARY,
                amount,
                Integer.MAX_VALUE,
                plant
        ));
        state.logEvent("plant " + plant.getName() + " produced a sun at ("
                + (plant.getPosX() + 1) + ", " + (plant.getPosY() + 1) + ")\n");
    }

    private static boolean shouldProduceDoubleSun(Plant plant, GameState state) {
        return plant.getPlantStat().doubleSunChance()
                && state.getBoard().getRandom().nextInt(100) < DOUBLE_SUN_CHANCE_PERCENT;
    }
}
