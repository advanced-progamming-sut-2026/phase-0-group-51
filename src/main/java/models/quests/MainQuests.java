package models.quests;

public class MainQuests extends Quest{
    public MainQuests(String name,String condition,QuestPriority priority,String unlockableId,int targetAmount,
                      int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority,targetAmount, rewardAmount, rewardType,QuestType.MAIN);
        this.unlockableId = unlockableId;

    }
    }

