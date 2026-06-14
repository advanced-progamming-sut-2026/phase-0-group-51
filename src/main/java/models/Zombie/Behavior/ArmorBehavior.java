package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.ArmorDefinition;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class ArmorBehavior implements PersistableBehavior {
    private final ArmorDefinition def;
    private int currentHP;
    private boolean destroyed = false;

    public ArmorBehavior(ArmorDefinition def) {
        this.def = def;
    }

    @Override public String behaviorType() { return "ARMOR"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setString(4, def.alias); // armor_alias
    }
}
