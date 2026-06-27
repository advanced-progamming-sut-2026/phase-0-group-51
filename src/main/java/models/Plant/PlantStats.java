package models.Plant;

public record PlantStats(
        int maxHp,
        int damage,
        int cost,
        double actionInterval,
        double projectileSpeed
) {
    public PlantStats withMaxHp(int maxHp)
    { return new PlantStats( maxHp, damage, cost, actionInterval, projectileSpeed); }
    public PlantStats withDamage(int damage)
    { return new PlantStats( maxHp, damage, cost, actionInterval, projectileSpeed); }
    public PlantStats withCost(int cost)
    { return new PlantStats( maxHp, damage, cost, actionInterval, projectileSpeed); }
    public PlantStats withInterval(double i)
    { return new PlantStats( maxHp, damage, cost, i, projectileSpeed); }
    public PlantStats withProjectileSpeed(double s)
    { return new PlantStats( maxHp, damage, cost, actionInterval, s); }
}