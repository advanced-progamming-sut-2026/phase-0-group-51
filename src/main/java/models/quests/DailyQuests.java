package models.quests;

public class DailyQuests extends Quest{
    public DailyQuests(
            String name, String condition, QuestPriority priority,
            QuestEventType eventType, int targetAmount, int rewardAmount,
            QuestRewardType rewardType, String unlockableId
    ) {
        super(name, condition, priority, eventType, targetAmount,
                rewardAmount, rewardType, QuestType.DAILY, unlockableId);
    }
}
