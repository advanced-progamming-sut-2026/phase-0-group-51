package network.protocol.minigame;

import models.minigames.MinigameType;

public class MinigameProgressDto {
    private MinigameType type;
    private int highestUnlockedStage;
    private int highestCompletedStage;

    public MinigameProgressDto() {
    }

    public MinigameProgressDto(
            MinigameType type,
            int highestUnlockedStage,
            int highestCompletedStage
    ) {
        this.type = type;
        this.highestUnlockedStage = highestUnlockedStage;
        this.highestCompletedStage = highestCompletedStage;
    }

    public MinigameType getType() {
        return type;
    }

    public void setType(MinigameType type) {
        this.type = type;
    }

    public int getHighestUnlockedStage() {
        return highestUnlockedStage;
    }

    public void setHighestUnlockedStage(int highestUnlockedStage) {
        this.highestUnlockedStage = highestUnlockedStage;
    }

    public int getHighestCompletedStage() {
        return highestCompletedStage;
    }

    public void setHighestCompletedStage(int highestCompletedStage) {
        this.highestCompletedStage = highestCompletedStage;
    }
}
