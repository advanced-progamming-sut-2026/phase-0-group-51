package models.Shop;

import java.util.Collections;
import java.util.List;

public class Shop {

    //permanent
    private static final List<ShopItem> CATALOGUE = List.of(
            new ShopItem(ShopItemType.POT,
                    "Pot", 2000, Currency.COIN, 1, 20, false),

            new ShopItem(ShopItemType.PLANT_FOOD,
                    "Plant Food", 3, Currency.DIAMOND, 1, 3, false),

            new ShopItem(ShopItemType.SEED_PACKET_RANDOM,
                    "Seed Packet Random", 1000, Currency.COIN, 5, null, false),

            new ShopItem(ShopItemType.SEED_PACKET_SELECTED,
                    "Seed Packet Selected", 5, Currency.DIAMOND, 10, null, true),

            new ShopItem(ShopItemType.COIN_CONVERSION,
                    "Coin Conversion", 5, Currency.DIAMOND, 500, null, false)
    );
    private DailyOffer currentDailyOffer;
    public Shop() {
        refreshDailyOfferIfNeeded(Collections.emptyList());
    }
    public void refreshDailyOfferIfNeeded(List<String> unlockedPlants) {};


    public List<ShopItem> getPermanentItems() {
        return Collections.unmodifiableList(CATALOGUE);
    }
    public DailyOffer getCurrentDailyOffer() {
        return currentDailyOffer;
    }

}
