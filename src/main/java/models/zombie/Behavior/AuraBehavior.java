package models.zombie.Behavior;

import lombok.Getter;
import models.plant.Plant;
import models.zombie.ArmorDefinition;
import models.zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
@Getter
public class AuraBehavior implements PersistableBehavior {
    private static final String KNIGHT_TARGET_ALIAS = "ZombieDefault";

    private final AuraType auraType;
    private final float radius;
    private final int intervalTicks;
    private final List<ArmorDefinition> knightArmors;
    private int timer;

    public AuraBehavior(AuraType type, float radius, int intervalTicks) {
        this(type, radius, intervalTicks, Collections.emptyList());
    }

    public AuraBehavior(AuraType type, float radius, int intervalTicks,
                        List<ArmorDefinition> knightArmors) {
        this.auraType = type;
        this.radius = radius;
        this.intervalTicks = intervalTicks;
        this.knightArmors = knightArmors;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (++timer < intervalTicks) {
            return;
        }
        timer = 0;
        if (auraType == AuraType.KNIGHT_NEARBY) {
            knightNearby(zombie, gs);
        }
    }

    private void knightNearby(Zombie king, GameState gs) {
        for (Zombie other : gs.getZombiesInTheGame()) {
            if (other == king || other.isDead() || other.isHypnotized()) {
                continue;
            }
            if (!KNIGHT_TARGET_ALIAS.equals(other.getAlias())) {
                continue;
            }
            if (other.getBehavior(ArmorBehavior.class) != null) {
                continue;
            }
            if (Math.abs(other.getX() - king.getX()) > radius
                || Math.abs(other.getLane() - king.getLane()) > 3) {
                continue;
            }
            for (ArmorDefinition def : knightArmors) {
                other.addBehavior(new ArmorBehavior(def));
            }
            return;
        }
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return auraType == AuraType.KNIGHT_NEARBY;
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
    public void onDeath(Zombie zombie, GameState gs) {
        PersistableBehavior.super.onDeath(zombie, gs);
    }

    public enum AuraType {
        KNIGHT_NEARBY       // DarkKing
    }

    @Override
    public String behaviorType() {
        return "AURA";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("aura_type", getAuraType().name());
        cols.put("aura_radius", getRadius());
        cols.put("aura_interval", getIntervalTicks());
        if (!getKnightArmors().isEmpty()) {
            StringBuilder aliases = new StringBuilder();
            for (ArmorDefinition def : getKnightArmors()) {
                if (aliases.length() > 0) aliases.append(",");
                aliases.append(def.getAlias());
            }
            cols.put("aura_armor_aliases", aliases.toString());
        }
    }

    @Override
    public ZombieBehavior copy() {
        return new AuraBehavior(auraType, radius, intervalTicks, knightArmors);
    }
}
