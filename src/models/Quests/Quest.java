package models.Quests;

public abstract class Quest {
    protected String name;
    protected String condition;
    protected boolean isCompleted;
    protected QuestRewardType rewardType;
    protected int rewardAmount;
    protected String UnlockableId;
     protected QuestPriority priority;
     protected QuestType type;
     protected int progressAmount;
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
        this.progressAmount =0;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
