package models.Quests;

import lombok.Setter;

public abstract class Quest {
    protected int id;
    protected String name;
    protected String condition;
    @Setter
    protected boolean isCompleted;
    protected QuestRewardType rewardType;
    protected int rewardAmount;
    protected static String UnlockableId;
     protected QuestPriority priority;
     protected QuestType type;
    protected void giveReward(){}
    public void checkComplete(){}
    abstract public void setProgress(String QuestName,int plusAmount); //چک میشه اگر لازم بود progressAmount اضافه میشه
    public Quest(String name,String condition,QuestPriority priority, int rewardAmount, QuestRewardType rewardType,QuestType type) {
        this.name=name;
        this.condition=condition;
        this.isCompleted = false;
        this.priority = priority;
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
        this.type=type;
    }

}
