package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.games.GameState;

import java.util.Arrays;
import java.util.List;

public enum SunProducer implements PlantType{
    SUNFLOWER(
            1,
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
                    return s;//what is double sun chance? =)
                }
            }
    );
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
                0
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
        state.addSun(50);
    }

    @Override
    public void onPlantFood(Plant plant, GameState state) {
        state.addSun(150);
    }
}
