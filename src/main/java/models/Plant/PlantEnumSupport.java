package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.projectile.ElementType;

import java.util.Comparator;
import java.util.List;

final class PlantEnumSupport {
    private PlantEnumSupport() {
    }

    static Plant create(int id, PlantType type) {
        PlantData data = PlantRegistry.get(id);
        if (data == null) {
            throw new IllegalStateException("Plant data was not loaded for id " + id);
        }
        PlantStats stats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                data.recharge(),
                data.projectileSpeed()
        );
        List<PlantUpgrade> upgrades = data.upgrades().stream()
                .sorted(Comparator.comparingInt(upgrade -> upgrade.level()))
                .map(DataDrivenPlantUpgrade::new)
                .map(upgrade -> (PlantUpgrade) upgrade)
                .toList();
        return new Plant(
                data.id(),
                data.name(),
                type,
                stats,
                upgrades,
                data.tags()
        );
    }

    static double projectileSpeed(Plant plant, double fallback) {
        double configured = plant.getPlantStat().projectileSpeed();
        return configured > 0 ? configured : fallback;
    }

    static ElementType elementFromTags(Plant plant) {
        if (plant.hasTag(PlantTag.FIRE)) {
            return ElementType.FIRE;
        }
        if (plant.hasTag(PlantTag.ICE)) {
            return ElementType.ICE;
        }
        if (plant.hasTag(PlantTag.POISON)) {
            return ElementType.POISON;
        }
        return ElementType.NORMAL;
    }
}
