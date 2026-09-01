package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ProjectileReleaseData;
import Data.loader.ProjectileVisualData;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.games.GameState;

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


    static Projectile configureProjectileVisual(
            Projectile projectile,
            Plant plant,
            GameState state,
            int requestedReleaseId
    ) {
        projectile.withSource(plant);

        PlantData data = PlantRegistry.getById(plant.getId());
        if (data == null || !data.hasProjectiles()) {
            return projectile;
        }

        ProjectileVisualData visual = data.resolveProjectile("default");
        if (visual == null || visual.releases() == null || visual.releases().isEmpty()) {
            return projectile;
        }

        ProjectileReleaseData release = visual.releases().stream()
                .filter(candidate -> candidate.id() == requestedReleaseId)
                .findFirst()
                .orElse(visual.releases().getFirst());

        int delayTicks = Math.max(
                0,
                Math.round(release.time() * state.getTicksPerSecond())
        );

        return projectile.withVisualRelease(
                "default",
                release.id(),
                delayTicks
        );
    }

    static double projectileSpeed(Plant plant, double fallback) {
        double configured = plant.getPlantStat().projectileSpeed();
        return configured > 0 ? configured : fallback;
    }

    static double upgradedRange(Plant plant, double baseRange) {
        return baseRange + Math.max(0, plant.getPlantStat().range() - 9);
    }

    static double upgradedLifespan(Plant plant, double baseSeconds) {
        return baseSeconds + Math.max(0, plant.getPlantStat().lifespan());
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
