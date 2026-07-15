package models.quests;

public class DailyQuests extends Quest{

    public DailyQuests(String name,String condition,QuestPriority priority,String unlockableId, int targetAmount,
                       int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority,targetAmount, rewardAmount, rewardType,QuestType.DAILY);
        this.unlockableId = unlockableId;
    }

}
