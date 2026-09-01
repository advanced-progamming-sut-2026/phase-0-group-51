package models.effects;

public record VisualEffectEvent(
    Type type,
    double posX,
    double posY,
    double intensity
) {
    public enum Type {
        PROJECTILE_IMPACT,
        PLANT_EXPLOSION,
        PLANT_DROWNING,
        ICY_WIND
    }

    public static VisualEffectEvent projectileImpact(
        double posX,
        double posY
    ) {
        return new VisualEffectEvent(
            Type.PROJECTILE_IMPACT,
            posX,
            posY,
            1.0
        );
    }

    public static VisualEffectEvent plantExplosion(
        double posX,
        double posY
    ) {
        return plantExplosion(posX, posY, 1.0);
    }

    public static VisualEffectEvent plantExplosion(
        double posX,
        double posY,
        double intensity
    ) {
        return new VisualEffectEvent(
            Type.PLANT_EXPLOSION,
            posX,
            posY,
            intensity
        );
    }

    public static VisualEffectEvent plantDrowning(
        double posX,
        double posY
    ) {
        return new VisualEffectEvent(
            Type.PLANT_DROWNING,
            posX,
            posY,
            1.0
        );
    }

    public static VisualEffectEvent icyWind() {
        return new VisualEffectEvent(
            Type.ICY_WIND,
            0.0,
            0.0,
            1.0
        );
    }
}
