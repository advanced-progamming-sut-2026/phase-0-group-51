package Data.loader;

import models.Plant.PlantTag;
import java.util.List;
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

    public boolean hasProjectiles() {
        return projectiles != null && !projectiles.isEmpty();
    }
}
