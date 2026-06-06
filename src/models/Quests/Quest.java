package models.Quests;

public abstract class Quest {
    protected boolean isCompleted;
    protected QuestRewardType rewardType;
    protected int rewardAmount;
    protected String UnlockableId;



    protected void giveReward(){}
    public void checkComplete(){}
}
