package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class AuraBehavior implements ZombieBehavior {
    private final AuraType auraType;
    private final float    radius; //شعاعی که تخت تاثیر قرار میگیره
    private final int      tickInterval; //هرچند تیک یبار انجام بشه
    private int timer;

    public AuraBehavior(AuraType type, float radius, int tickInterval) {
        this.auraType     = type;
        this.radius       = radius;
        this.tickInterval = tickInterval;
    }

    @Override
    public void onTick(Zombie zombie) {}





    public enum AuraType {
        BUFF_SPEED_NEARBY,   // DarkKing  → nearby zombies move faster
        BUFF_DAMAGE_NEARBY,  // DarkKing  → nearby zombies do more damage
        TURQUOISE_DEBUFF,    // Turquoise → debuffs plants in radius
        STEAL_SUN_PASSIVE    // Ra        → drains sun over time
    }
}
