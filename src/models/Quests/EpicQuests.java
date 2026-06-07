package models.Quests;

import java.util.List;

public  class EpicQuests extends Quest{

    public EpicQuests(String name,String condition,QuestPriority priority, int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority, rewardAmount, rewardType, QuestType.EPIC);

    }
    public static final List<Quest> EpicQuests =List.of(
            new EpicQuests("استاد دفاع","اتمام یک مرحله دقیقا با صفر خورشید",
                    QuestPriority.CRITICAL,200,QuestRewardType.CURRENCY_GEMS),
            new EpicQuests("شب یا صبح","به پایان رساندن بازی روز با گیاهان شب (mushroom ها)",
                    QuestPriority.HIGH,20,QuestRewardType.CURRENCY_GEMS),
            new EpicQuests("وقت چمن زنی","حداقل n زامبی را با چمن زن بکش",
                    QuestPriority.AVERAGE,n,QuestRewardType.CURRENCY_GEMS));

}

