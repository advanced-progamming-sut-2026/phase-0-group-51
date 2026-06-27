package models.Zombie.Behavior;

import lombok.Getter;
import models.Plant.Lobber;
import models.Plant.Plant;
import models.Plant.PlantType;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

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
    public int onHit(Zombie zombie, int rawDamage, ElementType elementType, Plant plant) {
        switch (type) {
            case REFLECT_PROJECTILE:
                if (plant.getPlantType().equals(Lobber.class)) {
                    return 0;
                }
        }
        return rawDamage;
    }


    public enum DamageReactionType {
        NEWSPAPER_RAGE,      // speed + damage boost when newspaper breaks
        REFLECT_PROJECTILE
    }

    @Override public String behaviorType() { return "DAMAGE_REACTION"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }
    @Override
    public ZombieBehavior copy() {
        return new DamageReactionBehavior(type, param1, param2);
    }
}
