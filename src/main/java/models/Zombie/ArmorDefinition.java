package models.Zombie;

import lombok.Getter;

import java.util.List;
@Getter
public class ArmorDefinition {
    public final String alias;
    public final int baseHealth;
    public final boolean metallic;
    public final boolean passDamage;
    public final List<Float> layerThresholds; // [0.666, 0.333] graphic changes

    public ArmorDefinition(String alias, int baseHealth,
                           boolean metallic, boolean passDamage,
                           List<Float> layerThresholds) {
        this.alias = alias;
        this.baseHealth = baseHealth;
        this.metallic = metallic;
        this.passDamage = passDamage;
        this.layerThresholds = layerThresholds;
    }

    public String getAlias() {
        return alias;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public boolean isMetallic() {
        return metallic;
    }

    public boolean isPassDamage() {
        return passDamage;
    }

    public List<Float> getLayerThresholds() {
        return layerThresholds;
    }
}
