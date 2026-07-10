package models.Zombie.Behavior;

import lombok.Getter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class AuraBehavior implements PersistableBehavior {
    private final AuraType auraType;
    private final float    radius;
    private final int      intervalTicks;
    private int timer;

    public AuraBehavior(AuraType type, float radius, int intervalTicks) {
        this.auraType      = type;
        this.radius        = radius;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {}

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

    public enum AuraType {
        BUFF_SPEED_NEARBY,   // DarkKing
        BUFF_DAMAGE_NEARBY,  // DarkKing
    }

    @Override public String behaviorType() { return "AURA"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }
}
