package models.Shop;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class Shop {

    private static final List<ShopItem> CATALOGUE = List.of(
            new ShopItem(ShopItemType.POT,
                    "Pot", 2000, Currency.COIN, 1, 20, false),

            new ShopItem(ShopItemType.PLANT_FOOD,
                    "Plant Food", 3, Currency.GEM, 1, 3, false),

            new ShopItem(ShopItemType.SEED_PACKET_RANDOM,
                    "Seed Packet Random", 1000, Currency.COIN, 5, null, false),

            new ShopItem(ShopItemType.SEED_PACKET_SELECTED,
                    "Seed Packet Selected", 5, Currency.GEM, 10, null, true),

            new ShopItem(ShopItemType.COIN_CONVERSION,
                    "Coin Conversion", 5, Currency.GEM, 500, null, false)
    );

    public ShopItem getItemById(int id){
        if(id < 1 || id > CATALOGUE.size())  return null;
        return CATALOGUE.get(id-1);
    }
    public List<ShopItem> getCatalogue(){return CATALOGUE;}

}
