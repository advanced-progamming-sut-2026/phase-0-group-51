package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class RangedAttackBehavior implements PersistableBehavior {
    private final RangedAttackType type;
    private final int intervalTicks;
    private final int range;
    private final int extraParam;
    private int cooldown;

    public RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range) {
        this(type, intervalTicks, range, 0);
    }

    public RangedAttackBehavior(RangedAttackType type, int intervalTicks, int range, int extraParam) {
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

    @Override public String behaviorType() { return "RANGED_ATTACK"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setString(5, type.name());
        ps.setInt(6, intervalTicks);
        ps.setInt(7, range);
        ps.setInt(8, extraParam);
    }

}
