package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.ArmorDefinition;
@Getter
public class ArmorBehavior implements ZombieBehavior {
    private final ArmorDefinition def;
    private int currentHP;
    private boolean destroyed = false;

    public ArmorBehavior(ArmorDefinition def) {
        this.def = def;
    }
}
