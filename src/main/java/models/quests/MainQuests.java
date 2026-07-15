package models.quests;

public class MainQuests extends Quest{
    public MainQuests(
            String name, String condition, QuestPriority priority,
            QuestEventType eventType, int targetAmount, int rewardAmount,
            QuestRewardType rewardType, String unlockableId
    ) {
        super(name, condition, priority, eventType, targetAmount,
                rewardAmount, rewardType, QuestType.MAIN, unlockableId);
    }
    }

