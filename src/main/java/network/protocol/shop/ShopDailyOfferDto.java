package network.protocol.shop;

public class ShopDailyOfferDto {
    private int plantId;
    private String date;
    private boolean purchased;
    private int basePrice;
    private int finalPrice;

    public ShopDailyOfferDto() {
    }

    public ShopDailyOfferDto(
            int plantId,
            String date,
            boolean purchased,
            int basePrice,
            int finalPrice
    ) {
        this.plantId = plantId;
        this.date = date;
        this.purchased = purchased;
        this.basePrice = basePrice;
        this.finalPrice = finalPrice;
    }

    public int getPlantId() { return plantId; }
    public void setPlantId(int plantId) { this.plantId = plantId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }
    public int getBasePrice() { return basePrice; }
    public void setBasePrice(int basePrice) { this.basePrice = basePrice; }
    public int getFinalPrice() { return finalPrice; }
    public void setFinalPrice(int finalPrice) { this.finalPrice = finalPrice; }
}
