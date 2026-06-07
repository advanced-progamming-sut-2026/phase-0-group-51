package models.Quests;

import models.App;

import java.util.List;

public class MainQuests extends Quest{
    public MainQuests(String name,String condition,QuestPriority priority, int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority, rewardAmount, rewardType,QuestType.MAIN);
    }
    public static final List<Quest> MainQuests =List.of(
            new MainQuests("شکارچی chapter","شکست دادن ۵۰ زامبی از فصل chapter",
                    QuestPriority.HIGH,10,QuestRewardType.INVENTORY),
            new MainQuests("گیاه خوار اقتصادی","پیروزی در یک مرحله بدون از دست دادن بیش از n گیاه",
                     QuestPriority.HIGH,n-20,QuestRewardType.INVENTORY),
            new MainQuests("سرعت عمل","کشتن ۱۰ زامبی در کمتر از ۳۰ ثانیه از شروع موج اول حمله زامبی ها",
                                       QuestPriority.AVERAGE,500,QuestRewardType.CURRENCY_COINS));

}
