package controllers.validation;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.App;
import models.GreenHouse.FlowerPot;
import models.GreenHouse.GreenHouse;
import models.Plant.PlantType;
import models.Shop.Shop;
import models.Shop.ShopItem;

import static models.Shop.ShopItemType.PLANT_FOOD;
import static models.Shop.ShopItemType.POT;

public class ShopMenuValidation {
    public int count, id, totalGemsNeeded, totalCoinsNeeded;
    public PlantData data;

    public boolean isCountValid(String countString) {
        try {
            count = Integer.parseInt(countString);
            return count >= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isIdValid(String IdString) {
        try {
            id = Integer.parseInt(IdString);
            return id >= 1 && id <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isPlantTypeValid(String type) {
        data = PlantRegistry.getByName(type);
        return data != null;
    }

    public boolean isCoinEnough(ShopItem item) {
        totalCoinsNeeded = item.getBasePrice() * count;
        return App.getInstance().getLoggedInUser().getCoins() >= totalCoinsNeeded;
    }

    public boolean isGemEnough(ShopItem item) {
        totalGemsNeeded = item.getBasePrice() * count;
        return App.getInstance().getLoggedInUser().getGems() >= totalGemsNeeded;
    }

    public boolean doesExceedsMaxStackPot() {
        GreenHouse greenHouse = App.getInstance().getLoggedInUser().getGreenHouse();
        int unlocked = 0;
        for (FlowerPot[] row : greenHouse.getPots()) {
            for (FlowerPot pot : row) {
                if (pot.isUnlocked()) {
                    unlocked++;
                }
            }
        }
        return unlocked + count > 20;
    }

    public boolean doesExceedsMaxStackFood() {
        return App.getInstance().getLoggedInUser().getPlantFoodNum() + count > 3;
    }
}

