package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;

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
        Plant plant = createBasePlant(data);
        int maximumLevel = plant.getUpgrades().size() + 1;
        int safeLevel = Math.max(1, Math.min(requestedLevel, maximumLevel));
        while (plant.getLevel() < safeLevel) {
            plant.levelUp();
        }
        return plant;
    }

    private static Plant createBasePlant(PlantData data) {
        return switch (data.id()) {
            case 1 -> SunProducer.SUNFLOWER.create();
            case 6 -> Shooter.PEASHOOTER.create();
            case 7 -> Shooter.REPEATER.create();
            case 9 -> Shooter.SNOW_PEA.create();
            case 25 -> Lobber.CABBAGE_PULT.create();
            case 30 -> Explosive.POTATO_MINE.create();
            case 44 -> WallNut.WALL_NUT.create();
            case 55 -> Homing.CAT_TAIL.create();
            default -> createDataDrivenPlant(data);
        };
    }

    private static Plant createDataDrivenPlant(PlantData data) {
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
        return new Plant(
                data.id(),
                data.name(),
                new DataDrivenPlantType(data),
                stats,
                upgrades,
                data.tags()
        );
    }
}
