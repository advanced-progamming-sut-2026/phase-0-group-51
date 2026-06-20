package models.Quests;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public abstract class Quest {
    protected int id;
    protected String name;
    protected String condition;
    protected QuestRewardType rewardType;
    protected int rewardAmount;
    protected int targetAmount;
    protected  String UnlockableId;
     protected QuestPriority priority;
     protected QuestType type;
    protected void giveReward(){}
    public void checkComplete(){}
    public Quest(String name,String condition,QuestPriority priority,int targetAmount, int rewardAmount, QuestRewardType rewardType,QuestType type) {
        this.name=name;
        this.condition=condition;
        this.priority = priority;
        this.targetAmount = targetAmount;
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
        this.type=type;
    }

}
