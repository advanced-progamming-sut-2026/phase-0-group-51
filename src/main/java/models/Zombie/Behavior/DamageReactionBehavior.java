package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class DamageReactionBehavior implements PersistableBehavior {
    private final DamageReactionType type;
    private final float param1;  // new speed after rage or triggerChance for reflect
    private final float param2;  // new damage after rage
    private boolean reacted = false;

    public DamageReactionBehavior(DamageReactionType type) { this(type, 1.0f, 1.0f); }

    public DamageReactionBehavior(DamageReactionType type, float param1) { this(type, param1, 1.0f); }

    public DamageReactionBehavior(DamageReactionType type, float param1, float param2) {
        this.type   = type;
        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {}

    @Override
    public int onHit(Zombie zombie, int rawDamage) {return 0;}


    public enum DamageReactionType {
        NEWSPAPER_RAGE,      // speed + damage boost when newspaper breaks
        SUBMERGE_DODGE,      // Snorkel dodges while underwater
        REFLECT_PROJECTILE   // Juggler / LostCityJane reflects back
    }

    @Override public String behaviorType() { return "DAMAGE_REACTION"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setString(13, type.name());
        ps.setDouble(14, param1);
        ps.setDouble(15, param2);
    }
}
