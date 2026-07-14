package models.Plant;

import Data.loader.StatModifierData;
import Data.loader.UpgradeData;

public final class DataDrivenPlantUpgrade implements PlantUpgrade {
    private final UpgradeData data;
    public DataDrivenPlantUpgrade(UpgradeData data) { this.data = data; }
    public String description() { return data.description(); }
    @Override public PlantStats apply(PlantStats current) {
        PlantStats result = current;
        for (StatModifierData modifier : data.modifiers()) {
            PlantStatKey key = PlantStatKey.valueOf(modifier.stat());
            result = result.apply(key, modifier.operation(), modifier.value());
        }
        return result;
    }
}
