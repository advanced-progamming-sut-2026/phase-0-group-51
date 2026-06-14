package models.Zombie;

import lombok.Getter;

import java.util.List;
@Getter
public class ArmorDefinition {
    public final String      alias;
    public final int         baseHealth;
    public final boolean     metallic;        // immune to certain fire/ice effects
    public final boolean     passDamage;      // true = ShoulderArmor (lets damage through)
    public final List<Float> layerThresholds; // [0.666, 0.333] → art changes at these HP fractions

    public ArmorDefinition(String alias, int baseHealth,
                           boolean metallic, boolean passDamage,
                           List<Float> layerThresholds) {
        this.alias           = alias;
        this.baseHealth      = baseHealth;
        this.metallic        = metallic;
        this.passDamage      = passDamage;
        this.layerThresholds = layerThresholds;
    }
}
