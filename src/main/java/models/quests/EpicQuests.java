package models.quests;

public class EpicQuests extends Quest {
    public EpicQuests(
            String name, String condition, QuestPriority priority,
            QuestEventType eventType, int targetAmount, int rewardAmount,
            QuestRewardType rewardType, String unlockableId,
            String parameterOptions
    ) {
        super(name, condition, priority, eventType, targetAmount, rewardAmount,
                rewardType, QuestType.EPIC, unlockableId, parameterOptions);
    }
}
