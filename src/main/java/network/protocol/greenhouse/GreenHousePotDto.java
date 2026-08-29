package network.protocol.greenhouse;

public class GreenHousePotDto {
    private int row;
    private int column;
    private boolean unlocked;
    private Integer plantId;
    private String plantedAt;

    public GreenHousePotDto() {
    }

    public GreenHousePotDto(
            int row,
            int column,
            boolean unlocked,
            Integer plantId,
            String plantedAt
    ) {
        this.row = row;
        this.column = column;
        this.unlocked = unlocked;
        this.plantId = plantId;
        this.plantedAt = plantedAt;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public Integer getPlantId() {
        return plantId;
    }

    public void setPlantId(Integer plantId) {
        this.plantId = plantId;
    }

    public String getPlantedAt() {
        return plantedAt;
    }

    public void setPlantedAt(String plantedAt) {
        this.plantedAt = plantedAt;
    }
}
