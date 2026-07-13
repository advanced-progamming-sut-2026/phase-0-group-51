package models.Shop;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopItem {
    private final ShopItemType type;
    private final String     name;
    private final int        basePrice;
    private final Currency   currency;
    private final int        amountPerPurchase;
    private final Integer    maxStack;
    private final boolean    requiresPlantType;

    public ShopItem(ShopItemType type, String name, int basePrice, Currency currency,
                    int amountPerPurchase, Integer maxStack, boolean requiresPlantType) {
        this.type               = type;
        this.name              = name;
        this.basePrice         = basePrice;
        this.currency          = currency;
        this.amountPerPurchase = amountPerPurchase;
        this.maxStack          = maxStack;
        this.requiresPlantType = requiresPlantType;
    }
}
