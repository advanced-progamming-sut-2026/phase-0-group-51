package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;

import java.util.List;

public final class PlantFactory {
    private PlantFactory() {}

    public static Plant create(String name) {
        PlantData data = PlantRegistry.getByName(name);
        if (data == null) throw new IllegalArgumentException("Unknown plant: " + name);
        return create(data);
    }

    public static Plant create(int id) {
        PlantData data = PlantRegistry.getById(id);
        if (data == null) throw new IllegalArgumentException("Unknown plant id: " + id);
        return create(data);
    }

    public static Plant create(PlantData data) {
        PlantStats stats = new PlantStats(data.baseHp(), data.damage(), data.cost(),
                data.actionInterval(), data.recharge(), data.projectileSpeed());
        List<PlantUpgrade> upgrades = data.upgrades().stream()
                .map(DataDrivenPlantUpgrade::new).map(x -> (PlantUpgrade) x).toList();
        return new Plant(data.id(), data.name(), new DataDrivenPlantType(data), stats, upgrades, data.tags());
    }
}
