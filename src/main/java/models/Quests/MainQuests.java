package models.Quests;

import models.App;

import java.util.List;

public class MainQuests extends Quest{
    public MainQuests(String name,String condition,QuestPriority priority, int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority, rewardAmount, rewardType,QuestType.MAIN);
    }

    public static final List<Quest> MainQuests =List.of(
            new MainQuests("Chapter Hunter","Defeat 50 zombies from chapter",
                    QuestPriority.HIGH,10,QuestRewardType.INVENTORY),
            new MainQuests("Economic Vegetarian","Win a level without losing more than n plants",
                     QuestPriority.HIGH,20,QuestRewardType.INVENTORY),
            new MainQuests("Speedrun","Kill 10 zombies in less than 30 seconds from the start of the first zombie wave",
                                       QuestPriority.AVERAGE,500,QuestRewardType.CURRENCY_COINS));
     public Quest getMainQuest(String QuestName){
         for(Quest q : MainQuests){
             if(QuestName.equals(q.name)) return q;
         }
         return null;
     }
    @Override
    public void setProgress(String MainQuestName,int plus) {
    //getMainQuest(MainQuestName).progressAmount += plus;
    }
}
