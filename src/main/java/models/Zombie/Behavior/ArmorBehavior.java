package models.Zombie.Behavior;

import lombok.Getter;
import models.Plant.Plant;
import models.Plant.PlantType;
import models.Zombie.ArmorDefinition;
import models.Zombie.Zombie;
import models.projectile.ElementType;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class ArmorBehavior implements PersistableBehavior {
    private final ArmorDefinition def;
    private int currentHP;
    private boolean destroyed = false;
    private boolean removed = false;
    public boolean isGone() {
        return destroyed || removed;
    }

    public ArmorBehavior(ArmorDefinition def) {
        this.def = def;
        this.currentHP = def.getBaseHealth();
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
            int absorbed = rawDamage/2;
            leftOver = rawDamage - absorbed;
            currentHP -= absorbed;
        }else {
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

    @Override public String behaviorType() { return "ARMOR"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }

    @Override
    public ZombieBehavior copy() {
        return new ArmorBehavior(def);
    }
}
