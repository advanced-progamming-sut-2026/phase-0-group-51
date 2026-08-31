package network.protocol.minigame;

import models.minigames.MinigameType;

public class MinigameCompleteRequest {
    private MinigameType type;
    private int stageNumber;

    public MinigameCompleteRequest() {
    }

    public MinigameCompleteRequest(
            MinigameType type,
            int stageNumber
    ) {
        this.type = type;
        this.stageNumber = stageNumber;
    }

    public MinigameType getType() {
        return type;
    }

    public void setType(MinigameType type) {
        this.type = type;
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public void setStageNumber(int stageNumber) {
        this.stageNumber = stageNumber;
    }
}
