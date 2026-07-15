package models.quests;

import Data.database.PlantRepository;
import Data.database.UserRepository;
import lombok.Getter;
import lombok.Setter;
import models.User;

@Setter
@Getter
public abstract class Quest {
    private int id;
    private final String name;
    private final String condition;
    private final QuestPriority priority;
    private final QuestEventType eventType;
    private final int targetAmount;
    private final int rewardAmount;
    private final QuestRewardType rewardType;
    private final QuestType type;
    private final String unlockableId;

    protected Quest(
            String name,
            String condition,
            QuestPriority priority,
            QuestEventType eventType,
            int targetAmount,
            int rewardAmount,
            QuestRewardType rewardType,
            QuestType type,
            String unlockableId
    ) {
        this.name = name;
        this.condition = condition;
        this.priority = priority;
        this.eventType = eventType;
        this.targetAmount = targetAmount;
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
        this.type = type;
        this.unlockableId = unlockableId;
    }

    public boolean isComplete(int progress) {
        return progress >= targetAmount;
    }

}
