package models.quests;

public  class EpicQuests extends Quest{

    public EpicQuests(String name,String condition,QuestPriority priority,String unlockableId,int targetAmount,
                      int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority,targetAmount, rewardAmount, rewardType, QuestType.EPIC);
        this.unlockableId = unlockableId;
    }

}

