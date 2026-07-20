package controllers.validation;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import lombok.Getter;
import models.App;
import models.User;
import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;
import models.shop.ShopItem;
@Getter
public class ShopMenuValidation {
    private int count;
    private int id;
    private int totalGemsNeeded;
    private int totalCoinsNeeded;
    private PlantData data;
    public boolean isCountValid(String countString) {
        try {
            count = Integer.parseInt(countString);
            return count >= 1;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public boolean isIdValid(String idString) {
        try {
            id = Integer.parseInt(idString);
            return id >= 1 && id <= 5;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public boolean isPlantTypeValid(String type) {
        data = PlantRegistry.getByName(type);
        return data != null;
    }

    public boolean isCoinEnough(User user, ShopItem item) {
        totalCoinsNeeded = item.getBasePrice() * count;
        return user != null && user.getCoins() >= totalCoinsNeeded;
    }

    public boolean isGemEnough(User user, ShopItem item) {
        totalGemsNeeded = item.getBasePrice() * count;
        return user != null && user.getGems() >= totalGemsNeeded;
    }
}
