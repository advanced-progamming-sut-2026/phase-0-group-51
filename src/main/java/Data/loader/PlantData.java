package Data.loader;

import models.Plant.PlantTag;
import java.util.List;

public record PlantData(
        int id,
        String name,
        String category,
        List<PlantTag> tags,
        int cost,
        int baseHp,
        int damage,
        double actionInterval,
        int recharge,
        String lvl2,
        String lvl3,
        String lvl4
) {}