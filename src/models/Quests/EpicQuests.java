package models.Quests;

import java.util.List;

public  class EpicQuests extends Quest{

    public EpicQuests(String name,String condition,QuestPriority priority, int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority, rewardAmount, rewardType, QuestType.EPIC);

    }
    public static final List<Quest> EpicQuests =List.of(
            new EpicQuests("Defense Master","Finish a level with exactly zero sun",
                    QuestPriority.CRITICAL,200,QuestRewardType.CURRENCY_GEMS),
            new EpicQuests("Night or Morning","Finish a day level using only night plants (mushrooms)",
                    QuestPriority.HIGH,20,QuestRewardType.CURRENCY_GEMS),
            new EpicQuests("Mowing Time","Kill at least n zombies with lawn mowers",
                    QuestPriority.AVERAGE,n,QuestRewardType.CURRENCY_GEMS));

}

