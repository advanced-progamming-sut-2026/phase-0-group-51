package network.protocol.shop;

public class PlantAccountRequest {
    private int plantId;

    public PlantAccountRequest() {
    }

    public PlantAccountRequest(int plantId) {
        this.plantId = plantId;
    }

    public int getPlantId() {
        return plantId;
    }

    public void setPlantId(int plantId) {
        this.plantId = plantId;
    }
}
