package models.greenHouse;

import Data.loader.PlantData;

import java.util.Set;

public class GreenHousePlantHelper {
    private static final Set<String> NO_PLANT_FOOD = Set.of(
            "Gold Bloom", "Cherry Bomb", "Grapeshot",
            "Jalapeno", "Doom-shroom", "Ice-shroom",
            "Hot Potato", "Grave Buster", "Imitater",
            "Enlighten-mint", "Appease-mint", "Arma-mint",
            "Bombard-mint", "Enforce-mint", "Reinforce-mint",
            "Enchant-mint", "Pierce-mint", "catTail-mint"
    );
    public static boolean canAppearInGreenHouse(PlantData plant) {
        return !NO_PLANT_FOOD.contains(plant.name());
    }
}
