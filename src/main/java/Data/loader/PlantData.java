package Data.loader;

import models.Plant.PlantTag;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record PlantData(
        int id,
        String name,
        String category,
        List<PlantTag> tags,
        int cost,
        int baseHp,
        int damage,
        String damageExpression,
        String baseAbility,
        String plantFoodEffect,
        double actionInterval,
        double recharge,
        double projectileSpeed,
        String lvl2,
        String lvl3,
        String lvl4,
        List<UpgradeData> upgrades,
        String cardAssetId,
        String idlePamPath,
        String idleClip,
        String onPlantFoodDescription,
        String overallDescription,
        String funDescription,
        Map<String, PlantAnimationData> animations,
        Map<String, ProjectileVisualData> projectiles
) {
    public PlantAnimationData animation(String key) {
        return animations.get(key);
    }

    public boolean hasAnimation(String key) {
        return animations.containsKey(key);
    }

    public ProjectileVisualData projectile(String key) {
        return projectiles == null ? null : projectiles.get(key);
    }

    /**
     * Resolves a projectile visual without requiring every plant JSON file to
     * use exactly the same spelling for its projectile key.
     */
    public ProjectileVisualData resolveProjectile(String key) {
        if (projectiles == null || projectiles.isEmpty()) {
            return null;
        }

        if (key != null && !key.isBlank()) {
            ProjectileVisualData exact = projectiles.get(key);
            if (exact != null) {
                return exact;
            }

            String normalizedKey = normalizeProjectileKey(key);
            for (Map.Entry<String, ProjectileVisualData> entry
                    : projectiles.entrySet()) {
                if (normalizeProjectileKey(entry.getKey())
                        .equals(normalizedKey)) {
                    return entry.getValue();
                }
            }
        }

        ProjectileVisualData defaultVisual = projectiles.get("default");
        if (defaultVisual != null) {
            return defaultVisual;
        }

        // Some plant data names its only projectile after the projectile
        // instead of "default".
        return projectiles.size() == 1
                ? projectiles.values().iterator().next()
                : null;
    }

    private static String normalizeProjectileKey(String value) {
        return value == null
                ? ""
                : value.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("-", "")
                        .replace("_", "")
                        .replace(" ", "");
    }

    public boolean hasProjectiles() {
        return projectiles != null && !projectiles.isEmpty();
    }
}
