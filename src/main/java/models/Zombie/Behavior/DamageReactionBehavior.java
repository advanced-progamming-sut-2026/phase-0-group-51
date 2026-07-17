package models.Zombie.Behavior;

import lombok.Getter;
import models.Plant.Lobber;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import java.util.Map;

@Getter
public class DamageReactionBehavior implements PersistableBehavior {

    private final DamageReactionType type;
    private final float param1;  // rage speed scale / juggler spin speed scale
    private final float param2;  // rage damage scale
    private boolean raged = false;

    private boolean spinning = false;

    public DamageReactionBehavior(DamageReactionType type) {
        this(type, 1.0f, 1.0f);
    }

    public DamageReactionBehavior(DamageReactionType type, float param1) {
        this(type, param1, 1.0f);
    }

    public DamageReactionBehavior(DamageReactionType type, float param1, float param2) {
        this.type = type;
        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        // Newspaper zombie
        if (type == DamageReactionType.NEWSPAPER_RAGE && !raged) {
            ArmorBehavior armor = zombie.getBehavior(ArmorBehavior.class);
            if (armor != null && armor.isGone()) {
                raged = true;
                zombie.applySpeedScale(param1);
                zombie.applyDamageScale(param2);
            }
        }

    }

    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        switch (type) {
            case REFLECT_PROJECTILE -> {
                //TODO
            }
            case SUBMERGE_DODGE -> {
                // Snorkel
                MovementBehavior movement = zombie.getBehavior(MovementBehavior.class);
                if (movement != null && movement.isSubmerged() && !(plant.getPlantType() instanceof Lobber)) {
                    return 0;
                }
            }
            case DEFLECT_LOBBER -> {
                // Parasol
                if (plant.getPlantType() instanceof Lobber) {
                    return 0;
                }
            }
            case FIRE_IMMUNE -> {
                // Imp dragon
                if (element == ElementType.FIRE) {
                    return 0;
                }
            }
            default -> {
            }
        }
        return rawDamage;
    }

    private void startSpin(Zombie zombie) {
        if (!spinning) {
            spinning = true;
            zombie.applySpeedScale(param1);
        }
    }

    private void stopSpin(Zombie zombie) {
        if (spinning) {
            spinning = false;
            zombie.applySpeedScale(1.0f / param1);
        }
    }


    public enum DamageReactionType {
        NEWSPAPER_RAGE,
        REFLECT_PROJECTILE,  // juggler
        SUBMERGE_DODGE,      // snorkel
        DEFLECT_LOBBER,      // parasol
        FIRE_IMMUNE          // imp dragon
    }

    @Override
    public String behaviorType() {
        return "DAMAGE_REACTION";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("reaction_type", getType().name());
        cols.put("param1", getParam1());
        cols.put("param2", getParam2());
    }

    @Override
    public ZombieBehavior copy() {
        return new DamageReactionBehavior(type, param1, param2);
    }
}
