package models.Zombie.Behavior;

import lombok.Getter;
import models.Plant.Plant;
import models.Zombie.ArmorDefinition;
import models.Zombie.Zombie;
import models.projectile.ElementType;
import java.util.Map;

@Getter
public class ArmorBehavior implements PersistableBehavior {
    private final ArmorDefinition def;
    private int currentHP;
    private boolean destroyed = false;
    private boolean removed = false;

    public ArmorBehavior(ArmorDefinition def) {
        this.def = def;
        this.currentHP = def.getBaseHealth();
    }

    public boolean isGone() {
        return destroyed || removed;
    }

    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        if (element == ElementType.POISON) {
            return rawDamage;
        }
        if (isGone()) {
            return rawDamage;
        }
        int leftOver = 0;
        if (def.isPassDamage()) {
            int absorbed = rawDamage / 2;
            leftOver = rawDamage - absorbed;
            currentHP -= absorbed;
        } else {
            currentHP -= rawDamage;
        }
        if (currentHP <= 0) {
            int overflow = -currentHP;
            currentHP = 0;
            leftOver += overflow;
            destroyed = true;
        }
        return leftOver;
    }

    public boolean tryMagnetPull() {
        if (!def.isMetallic() || isGone()) return false;
        removed = true;
        return true;
    }

    public ArmorDefinition getDefinition() {
        return def;
    }


    @Override
    public String behaviorType() {
        return "ARMOR";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("armor_alias", getDefinition().getAlias());
    }

    @Override
    public ZombieBehavior copy() {
        return new ArmorBehavior(def);
    }
}
