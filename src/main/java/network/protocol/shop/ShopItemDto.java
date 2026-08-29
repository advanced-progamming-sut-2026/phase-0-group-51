package network.protocol.shop;

import models.shop.Currency;
import models.shop.ShopItemType;

public class ShopItemDto {
    private int id;
    private ShopItemType type;
    private String name;
    private int basePrice;
    private Currency currency;
    private int amountPerPurchase;
    private Integer maxStack;
    private boolean requiresPlantType;

    public ShopItemDto() {
    }

    public ShopItemDto(
            int id,
            ShopItemType type,
            String name,
            int basePrice,
            Currency currency,
            int amountPerPurchase,
            Integer maxStack,
            boolean requiresPlantType
    ) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.basePrice = basePrice;
        this.currency = currency;
        this.amountPerPurchase = amountPerPurchase;
        this.maxStack = maxStack;
        this.requiresPlantType = requiresPlantType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public ShopItemType getType() { return type; }
    public void setType(ShopItemType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getBasePrice() { return basePrice; }
    public void setBasePrice(int basePrice) { this.basePrice = basePrice; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public int getAmountPerPurchase() { return amountPerPurchase; }
    public void setAmountPerPurchase(int amountPerPurchase) { this.amountPerPurchase = amountPerPurchase; }
    public Integer getMaxStack() { return maxStack; }
    public void setMaxStack(Integer maxStack) { this.maxStack = maxStack; }
    public boolean isRequiresPlantType() { return requiresPlantType; }
    public void setRequiresPlantType(boolean requiresPlantType) { this.requiresPlantType = requiresPlantType; }
}
