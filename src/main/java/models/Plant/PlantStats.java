package models.Plant;

public record PlantStats(
        int hp,
        int maxHp,
        int damage,
        double actionInterval,
        double projectileSpeed
) {
    public PlantStats withHp(int hp)             { return new PlantStats(hp, maxHp, damage, actionInterval, projectileSpeed); }
    public PlantStats withMaxHp(int maxHp)       { return new PlantStats(hp, maxHp, damage, actionInterval, projectileSpeed); }
    public PlantStats withDamage(int damage)     { return new PlantStats(hp, maxHp, damage, actionInterval, projectileSpeed); }
    public PlantStats withInterval(double i)     { return new PlantStats(hp, maxHp, damage, i, projectileSpeed); }
    public PlantStats withProjectileSpeed(double s) { return new PlantStats(hp, maxHp, damage, actionInterval, s); }
}