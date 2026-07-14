package models.plant;

import data.loader.PlantData;
import data.loader.PlantRegistry;

import java.util.List;

public final class PlantFactory {
    private PlantFactory() {
    }

    public static Plant create(String name) {
        PlantData data = PlantRegistry.getByName(name);
        if (data == null) {
            throw new IllegalArgumentException("Unknown plant: " + name);
        }
        return create(data);
    }

    public static Plant create(int id) {
        PlantData data = PlantRegistry.getById(id);
        if (data == null) {
            throw new IllegalArgumentException("Unknown plant id: " + id);
        }
        return create(data);
    }

    public static Plant create(PlantData data) {
        return create(data, 1);
    }

    public static Plant create(PlantData data, int requestedLevel) {
        PlantStats stats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                data.recharge(),
                data.projectileSpeed()
        );
        List<PlantUpgrade> upgrades = data.upgrades().stream()
                .map(DataDrivenPlantUpgrade::new)
                .map(upgrade -> (PlantUpgrade) upgrade)
                .toList();
        Plant plant = new Plant(
                data.id(),
                data.name(),
                new DataDrivenPlantType(data),
                stats,
                upgrades,
                data.tags()
        );
        int safeLevel = Math.max(1, Math.min(requestedLevel, upgrades.size() + 1));
        while (plant.getLevel() < safeLevel) {
            plant.levelUp();
        }
        return plant;
    }
}
