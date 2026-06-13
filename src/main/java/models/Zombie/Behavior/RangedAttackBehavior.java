package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.games.GameState;

public class RangedAttackBehavior implements ZombieBehavior {
    private final RangedAttackType type;
    private final int intervalTicks;
    private final int range;
    private final int extraParam;
    private int cooldown;

    RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range) {
        this(type, intervalTicks, range, 0);
    }

    RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range, int extraParam) {
        this.type          = type;
        this.intervalTicks = intervalTicks;
        this.range         = range;
        this.extraParam    = extraParam;
        this.cooldown      = intervalTicks;
    }
    @Override
    public void onTick(Zombie zombie, GameState gs) {}


    public enum RangedAttackType {
        SUN_STEAL,    // Ra
        TORCH_FIRE,   // Explorer
        BONE_THROW,   // TombRaiser
        SNOWBALL,     // IceAge Hunter
        HOOK_PULL,    // Fisherman
        OCTOPUS_NET,  // Octopus
        JUGGLE_BALL,  // Juggler
        SPELL_SHEEP,  // Wizard
        LASER_BEAM    // Crystal Skull
    }

}
