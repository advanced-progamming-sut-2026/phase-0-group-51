package models.effects;

public record VisualEffectEvent(
    Type type,
    double posX,
    double posY
) {
    public enum Type {
        PROJECTILE_IMPACT,
        PLANT_EXPLOSION,
        ICY_WIND
    }

    public static VisualEffectEvent projectileImpact(
        double posX,
        double posY
    ) {
        return new VisualEffectEvent(
            Type.PROJECTILE_IMPACT,
            posX,
            posY
        );
    }

    public static VisualEffectEvent plantExplosion(
        double posX,
        double posY
    ) {
        return new VisualEffectEvent(
            Type.PLANT_EXPLOSION,
            posX,
            posY
        );
    }

    public static VisualEffectEvent icyWind() {
        return new VisualEffectEvent(
            Type.ICY_WIND,
            0.0,
            0.0
        );
    }
}
