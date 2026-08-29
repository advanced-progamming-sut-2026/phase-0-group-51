package network.protocol.shop;

import java.util.ArrayList;
import java.util.List;

public class ShopResponse {
    private boolean success;
    private String message;
    private List<ShopItemDto> catalogue = new ArrayList<>();
    private ShopDailyOfferDto dailyOffer;
    private List<ShopPlantStateDto> plants = new ArrayList<>();
    private int coins;
    private int gems;
    private int plantFood;
    private int claimedPlantFood;

    public ShopResponse() {
    }

    public ShopResponse(
            boolean success,
            String message,
            List<ShopItemDto> catalogue,
            ShopDailyOfferDto dailyOffer,
            List<ShopPlantStateDto> plants,
            int coins,
            int gems,
            int plantFood
    ) {
        this(
                success,
                message,
                catalogue,
                dailyOffer,
                plants,
                coins,
                gems,
                plantFood,
                0
        );
    }

    public ShopResponse(
            boolean success,
            String message,
            List<ShopItemDto> catalogue,
            ShopDailyOfferDto dailyOffer,
            List<ShopPlantStateDto> plants,
            int coins,
            int gems,
            int plantFood,
            int claimedPlantFood
    ) {
        this.success = success;
        this.message = message;
        this.catalogue = catalogue == null ? new ArrayList<>() : new ArrayList<>(catalogue);
        this.dailyOffer = dailyOffer;
        this.plants = plants == null ? new ArrayList<>() : new ArrayList<>(plants);
        this.coins = coins;
        this.gems = gems;
        this.plantFood = plantFood;
        this.claimedPlantFood = claimedPlantFood;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<ShopItemDto> getCatalogue() { return catalogue; }
    public void setCatalogue(List<ShopItemDto> catalogue) { this.catalogue = catalogue == null ? new ArrayList<>() : new ArrayList<>(catalogue); }
    public ShopDailyOfferDto getDailyOffer() { return dailyOffer; }
    public void setDailyOffer(ShopDailyOfferDto dailyOffer) { this.dailyOffer = dailyOffer; }
    public List<ShopPlantStateDto> getPlants() { return plants; }
    public void setPlants(List<ShopPlantStateDto> plants) { this.plants = plants == null ? new ArrayList<>() : new ArrayList<>(plants); }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }
    public int getPlantFood() { return plantFood; }
    public void setPlantFood(int plantFood) { this.plantFood = plantFood; }
    public int getClaimedPlantFood() { return claimedPlantFood; }
    public void setClaimedPlantFood(int claimedPlantFood) { this.claimedPlantFood = claimedPlantFood; }
}
