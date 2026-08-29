package network.protocol.shop;

public class ShopPurchaseRequest {
    private int itemId;
    private int count;
    private Integer selectedPlantId;

    public ShopPurchaseRequest() {
    }

    public ShopPurchaseRequest(
            int itemId,
            int count,
            Integer selectedPlantId
    ) {
        this.itemId = itemId;
        this.count = count;
        this.selectedPlantId = selectedPlantId;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public Integer getSelectedPlantId() { return selectedPlantId; }
    public void setSelectedPlantId(Integer selectedPlantId) { this.selectedPlantId = selectedPlantId; }
}
