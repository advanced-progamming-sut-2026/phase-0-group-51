package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.Zombie.ZombieType;

public class SummonBehavior implements ZombieBehavior {
    private final SummonType summonType;   //مشخص میکنه کی اتفاق بیفته
    private final ZombieType unitToSummon; // نوع زامبی که اسپان میشه
    private final int        count; // تعداد زامبی اسپان شده
    private final int        hpThreshold;   // اون مقدار جونی که اگر بهش برسیم یچیزی اسپان بشه .میتونه صفر باشه
    private boolean          thresholdFired = false;   // یه فلگ برای اینکه بفهمیم یبار اسپان انجام شده

    public SummonBehavior(SummonType type, ZombieType unit, int count, int hpThreshold) {
        this.summonType   = type;
        this.unitToSummon = unit;
        this.count        = count;
        this.hpThreshold  = hpThreshold;
    }

    @Override
    public void onTick(Zombie zombie) {}

    @Override
    public int onHit(Zombie zombie, int rawDamage) {return 0;}

    @Override
    public void onDeath(Zombie zombie) {}

    private void spawnUnits() {}







    public enum SummonType {
        IMP_THROW,       // Gargantuar → throws imp at 50% HP
        IMP_ON_DEATH,    // Gargantuar → throws imp on death
        WEASELS_ON_HIT,  // WeaselHoarder → releases weasels when damaged
        DODO_RIDER       // Dodo rider summons turkey flock
    }

}
