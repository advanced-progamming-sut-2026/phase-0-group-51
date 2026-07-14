package data.loader;

import models.plant.PlantTag;
import java.util.List;

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
        List<UpgradeData> upgrades
) {}
