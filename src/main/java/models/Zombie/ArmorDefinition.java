package models.Zombie;

import lombok.Getter;

import java.util.List;

@Getter
public class ArmorDefinition {
    public final String alias;
    public final int baseHealth;
    public final boolean metallic;
    public final boolean passDamage;
    public final List<String> armorLayers;
    public final List<Float> layerThresholds; // [0.666, 0.333] graphic changes

    public ArmorDefinition(
        String alias,
        int baseHealth,
        boolean metallic,
        boolean passDamage,
        List<Float> layerThresholds
    ) {
        this(
            alias,
            baseHealth,
            metallic,
            passDamage,
            List.of(),
            layerThresholds
        );
    }

    public ArmorDefinition(
        String alias,
        int baseHealth,
        boolean metallic,
        boolean passDamage,
        List<String> armorLayers,
        List<Float> layerThresholds
    ) {
        this.alias = alias;
        this.baseHealth = baseHealth;
        this.metallic = metallic;
        this.passDamage = passDamage;
        this.armorLayers = armorLayers == null
            ? List.of()
            : List.copyOf(armorLayers);
        this.layerThresholds = layerThresholds == null
            ? List.of()
            : List.copyOf(layerThresholds);
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

    public List<String> getArmorLayers() {
        return armorLayers;
    }

    public List<Float> getLayerThresholds() {
        return layerThresholds;
    }
}
