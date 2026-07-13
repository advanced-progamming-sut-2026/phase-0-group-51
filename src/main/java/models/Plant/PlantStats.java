package models.Plant;

public record PlantStats(
        int maxHp,
        int damage,
        int cost,
        double actionInterval,
        double recharge,
        double projectileSpeed,
        boolean doubleSunChance
) {
    // for ones who don't use double sun chance.
    public PlantStats(int maxHp, int damage, int cost, double actionInterval,
                      double recharge, double projectileSpeed) {
        this(maxHp, damage, cost, actionInterval, recharge, projectileSpeed, false);
    }

    public PlantStats withMaxHp(int maxHp)
    { return new PlantStats(maxHp, damage, cost, actionInterval, recharge, projectileSpeed, doubleSunChance); }
    public PlantStats withDamage(int damage)
    { return new PlantStats(maxHp, damage, cost, actionInterval, recharge, projectileSpeed, doubleSunChance); }
    public PlantStats withCost(int cost)
    { return new PlantStats(maxHp, damage, cost, actionInterval, recharge, projectileSpeed, doubleSunChance); }
    public PlantStats withInterval(double i)
    { return new PlantStats(maxHp, damage, cost, i, recharge, projectileSpeed, doubleSunChance); }
    public PlantStats withRecharge(double r)
    { return new PlantStats(maxHp, damage, cost, actionInterval, r, projectileSpeed, doubleSunChance); }
    public PlantStats withProjectileSpeed(double s)
    { return new PlantStats(maxHp, damage, cost, actionInterval, recharge, s, doubleSunChance); }
    public PlantStats withDoubleSunChance(boolean d)
    { return new PlantStats(maxHp, damage, cost, actionInterval, recharge, projectileSpeed, d); }
}