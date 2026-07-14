package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Board.Tile;
import models.games.GameState;
import models.sun.Sun;
import models.sun.SunType;

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
        private static final int DOUBLE_SUN_CHANCE_PERCENT = 25;

        @Override
        public void onTick(Plant plant, GameState state) {
            if (state.getBoard().getTile(plant.getPosY(), plant.getPosX()) == null) return;

            float spawnX = plant.getPosX() * Tile.TILEWIDTH;
            float spawnY = plant.getPosY() * Tile.TILEHEIGHT;

            boolean hasUncollectedSun = state.getBoard().getActiveSuns().stream()
                    .anyMatch(s -> s.getX() == spawnX && s.getLane() == plant.getPosY() && s.isGrounded());
            if (hasUncollectedSun) return;

            int amount = BASE_SUN;
            if (plant.getPlantStat().doubleSunChance()
                    && state.getBoard().getRandom().nextInt(100) < 25) {
                amount *= 2;
            }

            Sun plantSun = new Sun(spawnX, spawnY, plant.getPosY(), SunType.ORDINARY, amount, Integer.MAX_VALUE);
            plantSun.setGrounded(true);
            state.getBoard().spawnSun(plantSun);
        }

        @Override
        public void onFeed(Plant plant, GameState state) {
            state.increaseSunBalance(150);
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
                data.projectileSpeed()
        );
        return new Plant(
                data.id(), data.name(), this,
                baseStats,
                upgrades,
                data.tags()
        );
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        Tile tile = state.getBoard().getTileForPlant(plant);
        if (tile == null) return;
        boolean hasUncollectedSun = state.getBoard().getActiveSuns().stream()
                .anyMatch(s -> s.getX() == tile.getX() && s.getLane() == tile.getLane() && s.isGrounded());
        if (hasUncollectedSun) {
            return;
        }
        Sun plantSun = new Sun(tile.getX(), tile.getY(), tile.getLane(), SunType.ORDINARY, 25, Integer.MAX_VALUE);
        plantSun.setGrounded(true);
        state.getBoard().spawnSun(plantSun);
        state.logEvent("plant "+plant.getName()+" produced a sun at ("+ plantSun.getX()+", "+ plantSun.getY()+")\n");
    }

}
