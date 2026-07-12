package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum SunProducer implements PlantType {

    SUNFLOWER(1,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats s) {
                    return s.withInterval(s.actionInterval() - 2);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats s) {
                    return s.withMaxHp(s.maxHp() + 150);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats s) {
                    // Double Sun Chance: just flips a flag on PlantStats,
                    // checked in onTick below. No new class, no change to
                    // any other plant.
                    return s.withDoubleSunChance(true);
                }
            }) {
        private static final int BASE_SUN = 50;
        private static final int DOUBLE_SUN_CHANCE_PERCENT = 25; // tweak to taste

        @Override
        public void onTick(Plant plant, GameState state) {
            int sun = BASE_SUN;
            if (plant.getPlantStat().doubleSunChance()
                    && state.getBoard().getRandom().nextInt(100) < DOUBLE_SUN_CHANCE_PERCENT) {
                sun *= 2;
            }
            state.addSun(sun);
        }

        @Override
        public void onFeed(Plant plant, GameState state) {
            state.addSun(150);
        }

        @Override
        public void onFoodTick(Plant plant, GameState state) {}
    },
    ;

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
                0
        );
        return new Plant(
                data.id(), data.name(), this,
                baseStats,
                upgrades,
                data.tags()
        );
    }
}