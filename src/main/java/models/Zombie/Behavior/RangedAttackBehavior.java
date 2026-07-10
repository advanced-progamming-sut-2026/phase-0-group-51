package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

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
    public void onTick(Zombie zombie, GameState gs) {
        if (--cooldown > 0) return;
        cooldown = intervalTicks;

        Board board = gs.getBoard();
        int lane = zombie.getLane();
        int col  = (int) zombie.getX();
        switch(type) {

        }
    }

    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        return PersistableBehavior.super.onHit(zombie, rawDamage, element, plant);
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return PersistableBehavior.super.suppressesDefaultEating(zombie);
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return PersistableBehavior.super.suppressesMovement(zombie);
    }

    @Override
    public void onDeath(Zombie zombie, GameState gs) {
        PersistableBehavior.super.onDeath(zombie, gs);
    }

    @Override
    public ZombieBehavior copy() {
        return null;
    }


    public enum RangedAttackType {
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

    }

}
