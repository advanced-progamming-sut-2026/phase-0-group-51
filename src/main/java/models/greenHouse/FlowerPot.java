package models.greenHouse;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
@Getter
@Setter
public class FlowerPot {
    private final int row;
    private final int column;
    private boolean unlocked;
    private Integer plantId;
    private LocalDateTime plantedAt;
    public static final int MARIGOLD_ID = -1;
    public FlowerPot(int row, int column) {
        this.row = row;
        this.column = column;
        if(row==1) this.unlocked = true;
    }
    public boolean isEmpty() {
        return plantId == null;
    }
    public boolean isReady() {
        if (isEmpty()) return false;
        return !LocalDateTime.now().isBefore(getReadyTime());
    }
    public Duration getRemainingTime() {
        if (isEmpty()) return Duration.ZERO;
        if (isReady()) return Duration.ZERO;
        return Duration.between(LocalDateTime.now(), getReadyTime());
    }
    public LocalDateTime getReadyTime() {
        if (isEmpty()) return null;
        int growthHours = (plantId == MARIGOLD_ID) ? 2 : 8;
        return plantedAt.plusHours(growthHours);
    }
    public long getCeilRemainingHours() {
        long seconds = getRemainingTime().getSeconds();
        return (long) Math.ceil(seconds / 3600.0);
    }

    public void clear() {
        plantId = null;
        plantedAt = null;
    }
    public String getRemainingTimeString() {
        Duration duration = getRemainingTime();
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return hours + "h " + minutes + "m";
    }
    public String getPlantName() {
        if (isEmpty()) {return "";}
        if (plantId == MARIGOLD_ID) {
            return "Marigold";}
        PlantData plant = PlantRegistry.getById(plantId);
        return plant != null ? plant.name() : "";
    }
    public void finishGrowing() {
        int growthHours = (plantId == MARIGOLD_ID) ? 2 : 8;
        plantedAt = LocalDateTime.now().minusHours(growthHours);
    }
}
