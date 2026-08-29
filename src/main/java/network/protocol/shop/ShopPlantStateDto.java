package network.protocol.shop;

public class ShopPlantStateDto {
    private int plantId;
    private int level;
    private int seedPackets;
    private boolean boosted;

    public ShopPlantStateDto() {
    }

    public ShopPlantStateDto(
            int plantId,
            int level,
            int seedPackets,
            boolean boosted
    ) {
        this.plantId = plantId;
        this.level = level;
        this.seedPackets = seedPackets;
        this.boosted = boosted;
    }

    public int getPlantId() { return plantId; }
    public void setPlantId(int plantId) { this.plantId = plantId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getSeedPackets() { return seedPackets; }
    public void setSeedPackets(int seedPackets) { this.seedPackets = seedPackets; }
    public boolean isBoosted() { return boosted; }
    public void setBoosted(boolean boosted) { this.boosted = boosted; }
}
