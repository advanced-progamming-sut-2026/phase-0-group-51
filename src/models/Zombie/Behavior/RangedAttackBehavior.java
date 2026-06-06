package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class RangedAttackBehavior implements ZombieBehavior {
    private final RangedAttackType attackType;
    private final int intervalTicks;
    private final int range;
    private int cooldown;

    public RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range) {
        this.attackType    = type;
        this.intervalTicks = intervalTicks;
        this.range         = range;
        this.cooldown      = intervalTicks;
    }
    @Override
    public void onTick(Zombie zombie) {}



    public enum RangedAttackType {
        SNOWBALL,       // IceAge Hunter  → chill
        TOMB_SPAWN,     // TombRaiser     → spawns tomb
        OCTOPUS_NET,    // Octopus        → disables plant
        HOOK_PULL,      // Fisherman      → pulls plant
        SPELL_SHEEP,    // Wizard         → transforms plant to sheep
        TORCH_FIRE,     // Explorer       → burns plant
        SUN_STEAL,      // Ra             → steals sun
        JUGGLE_BALL     // Juggler        → bounces projectile (not reflect — ranged throw)
    }

}
