package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.GameState;

public class SummonBehavior implements ZombieBehavior {
    private final SummonType type;
    private final String unitAlias;
    private final int        count; // تعداد زامبی اسپان شده
    private final int        hpThreshold;   // اون مقدار جونی که اگر بهش برسیم یچیزی اسپان بشه .میتونه صفر باشه
    private boolean          fired = false;   // یه فلگ برای اینکه بفهمیم یبار اسپان انجام شده

    SummonBehavior(SummonType type, String unitAlias, int count, int hpThreshold) {
        this.type        = type;
        this.unitAlias   = unitAlias;
        this.count       = count;
        this.hpThreshold = hpThreshold;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {}

    @Override
    public int onHit(Zombie zombie, int rawDamage) {return 0;}

    @Override
    public void onDeath(Zombie zombie) {}

    private void spawnUnits() {}



    public enum SummonType {
        IMP_THROW,    // Gargantuar at HP threshold
        IMP_ON_DEATH  // Gargantuar on death
    }

}
