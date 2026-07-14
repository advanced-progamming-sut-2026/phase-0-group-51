package models.quests;

import data.database.UserRepository;
import lombok.Getter;
import lombok.Setter;
import models.User;

@Setter
@Getter
public abstract class Quest {
    protected int id;
    protected String name;
    protected String condition;
    protected QuestRewardType rewardType;
    protected int rewardAmount;
    protected int targetAmount;
    protected  String unlockableId;
     protected QuestPriority priority;
     protected QuestType type;
    protected void giveReward(){}
    public void checkComplete(){}
    public Quest(String name,String condition
            ,QuestPriority priority,int targetAmount
            , int rewardAmount, QuestRewardType rewardType
            ,QuestType type) {
        this.name=name;
        this.condition=condition;
        this.priority = priority;
        this.targetAmount = targetAmount;
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
        this.type=type;
    }
    public void giveReward(User user, UserRepository userRepository) {
        if (this.rewardType == null) return;

        switch (this.rewardType.toString()) {
            case "CURRENCY_COINS":
                user.setCoins(user.getCoins() + this.rewardAmount);
                break;
            case "CURRENCY_GEMS":
                user.setGems(user.getGems() + this.rewardAmount);
                break;
            case "INVENTORY":
             //  user.setSeedPacket(user.getSeedPacket() + this.rewardAmount);
                break;
            case "UNLOCKABLE":
                //  باید منطق باز شدن گیاه جدید توی جدول user_unlocked_plants پیاده بشه
                break;
        }
        userRepository.updateStats(user);
    }

}
