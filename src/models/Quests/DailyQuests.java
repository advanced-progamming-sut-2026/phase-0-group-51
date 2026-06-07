package models.Quests;

import java.util.List;

public class DailyQuests extends Quest{

    public DailyQuests(String name,String condition,QuestPriority priority,String unlockableId, int rewardAmount, QuestRewardType rewardType) {
        super(name,condition,priority, rewardAmount, rewardType,QuestType.DAILY);
        this.UnlockableId = unlockableId;
    }
    public final List<Quest> DailyQuests =List.of(
            new DailyQuests("آفتاب گیر روزانه","جمع آوری sun_amount واحد خورشید در طول یک روز",
                    QuestPriority.AVERAGE,null,app.loggedInUser.getSunAmount()/100,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("plant باز حرفه‌ای","ده تا زامبی را فقط با plant بکش",
             QuestPriority.HIGH,UnlockableId,0,QuestRewardType.UNLOCKABLE),
            new DailyQuests("only cactus","ده تا زامبی را فقط با کاکتوس بکش",
                    QuestPriority.HIGH,null,20,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("تخریب گر حرفه ای","استفاده از ۳ گیاه انفجاری در یک مرحله",
                    QuestPriority.LOW,null,100,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("تقارن","باغچه بازی در نهایت باید متقارن باشد",
                    QuestPriority.HIGH,null,500,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("کشتار خانوادگی","تنها از گیاهان family_type برای کشتن زامبی ها استفاده شود",
                    QuestPriority.AVERAGE,null,1000,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("شکوفایی در محدودیت ها","برای برد در مرحله از هیچ گیاهی از خانواده family_type استفاده نشود",
                    QuestPriority.HIGH,null,100,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("برد پشت برد","۵ مرحله را پشت سر هم با بیشترین سختی ببر",
                    QuestPriority.AVERAGE,null,5000,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("تقریبا پیروز","۱۰ زامبی را در ستون اول ردیفی بکش که چمن زن ندارد",
                    QuestPriority.AVERAGE,null,300,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("OCD نَمَنَ","در حالتی مرحله را ببر که هیچ تقارنی در باغچه وجود ندارد (به جز ردیف وسط)",
                    QuestPriority.AVERAGE,null,800,QuestRewardType.CURRENCY_COINS),
            new DailyQuests("روز ابری","یک مرحله را تنها با ۳ تا گیاه تولیدکننده خورشید بزن",
                    QuestPriority.HIGH,null,10,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("یه ستون کمتر","یک مرحله را در صورتی ببرد که در ستون nام گیاهی نکاشته باشد",
                    QuestPriority.HIGH,null,10,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("سطر بی دفاع","یک مرحله را در صورتی ببرد که سطر nام را هیچ گیاهی نکاشته باشد",
                    QuestPriority.HIGH,null,20,QuestRewardType.CURRENCY_GEMS),
            new DailyQuests("صلیب بی دفاع"," یک مرحله را در صورتی ببرد که ستون و ردیف nام خالی باشد",
                    QuestPriority.HIGH,null,25,QuestRewardType.CURRENCY_GEMS));

}
