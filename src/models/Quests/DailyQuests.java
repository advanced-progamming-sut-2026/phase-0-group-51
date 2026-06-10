package models.Quests;

import models.App;
import views.AppView;

import java.util.List;

public class DailyQuests extends Quest{

    public DailyQuests(String name,String condition,QuestPriority priority,String unlockableId, int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority, rewardAmount, rewardType,QuestType.DAILY);
        this.UnlockableId = unlockableId;
    }
    public final List<Quest> DailyQuests =List.of(
            new DailyQuests("Daily Sun Gather","Collect sun_amount units of sun during one day",
                    QuestPriority.AVERAGE,null, App.loggedInUser.getSunAmount()/100,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("Pro Plant Killer","Kill ten zombies using only plants",
             QuestPriority.HIGH,UnlockableId,0,QuestRewardType.UNLOCKABLE),
            new DailyQuests("Only Cactus","Kill ten zombies using only cactus",
                    QuestPriority.HIGH,null,20,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("Professional Demolisher","Use 3 explosive plants in one level",
                    QuestPriority.LOW,null,100,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("Symmetry","The garden must end up symmetrical",
                    QuestPriority.HIGH,null,500,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("Family Slaughter","Use only plants of family_type to kill zombies",
                    QuestPriority.AVERAGE,null,1000,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("Blooming within Limits","Win the level without using any plant from family_type",
                    QuestPriority.HIGH,null,100,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("Back-to-Back Victory","Win 5 consecutive levels on the highest difficulty",
                    QuestPriority.AVERAGE,null,5000,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("Almost Win","Kill 10 zombies in the first column of a row that has no lawn mower",
                    QuestPriority.AVERAGE,null,300,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("No OCD","Win the level with no symmetry in the garden (except the middle row)",
                    QuestPriority.AVERAGE,null,800,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("Cloudy Day","Play a level using only 3 sun-producing plants",
                    QuestPriority.HIGH,null,10,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("One Less Column","Win a level without planting anything in the nth column",
                    QuestPriority.HIGH,null,10,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("Defenseless Row","Win a level without planting anything in the nth row",
                    QuestPriority.HIGH,null,20,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("Defenseless Cross","Win a level leaving the nth column and nth row empty",
                    QuestPriority.HIGH,null,25,QuestRewardType.CURRENCY_GEMS));


    public Quest getDailyQuest(String QuestName){
        for(Quest q : DailyQuests){
            if(QuestName.equals(q.name)) return q;
        }
        return null;
    }
    @Override
    public void setProgress(String DailyQuestName,int plus) {
        getDailyQuest(DailyQuestName).progressAmount += plus;
    }
}
