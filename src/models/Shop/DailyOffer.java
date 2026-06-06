package models.Shop;

public class DailyOffer {
    private static final int BASE_PRICE      = 2000;
    private static final double DISCOUNT     = 0.20;
    private static final int SEED_PACKETS    = 10;

    private final String    plantType;
    private final int       finalPrice;
    private final String    offerDate;
    private       boolean   purchased;

    public DailyOffer(String plantType, String offerDate) {
        this.plantType  = plantType;
        this.offerDate  = offerDate;
        this.finalPrice = 1600;
        this.purchased  = false;
    }



    public String    getPlantType()   { return plantType; }
    public int       getFinalPrice()  { return finalPrice; }
    public int       getSeedPackets() { return SEED_PACKETS; }
    public String    getOfferDate()   { return offerDate; }
    public boolean   isPurchased()    { return purchased; }


    public void markPurchased() { this.purchased = true; }
}
