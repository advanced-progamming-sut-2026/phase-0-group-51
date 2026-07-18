package models.Plant;

public record PlantStats(
        int maxHp, int damage, int cost, double actionInterval, double recharge,
        double projectileSpeed, boolean doubleSunChance,
        double range, int pierceCount, double aoeDamage, double chillDuration,
        double freezeDuration, int targetCount, double lifespan,
        double warmthRadius, double poisonDamage, int sunAmount
) {
    public PlantStats(int maxHp, int damage, int cost, double actionInterval, double recharge, double projectileSpeed) {
        this(maxHp, damage, cost, actionInterval, recharge, projectileSpeed, false,
                9, 1, 0, 6, 0, 1, 0, 0, 5, 50);
    }
    public PlantStats(int maxHp, int damage, int cost, double actionInterval, double recharge, double projectileSpeed, boolean doubleSunChance) {
        this(maxHp, damage, cost, actionInterval, recharge, projectileSpeed, doubleSunChance,
                9, 1, 0, 6, 0, 1, 0, 0, 5, 50);
    }
    public PlantStats apply(PlantStatKey key, String operation, double value) {
        double old = switch (key) {
            case MAX_HP -> maxHp; case DAMAGE -> damage; case COST -> cost;
            case ACTION_INTERVAL -> actionInterval; case RECHARGE -> recharge;
            case PROJECTILE_SPEED -> projectileSpeed; case RANGE -> range;
            case PIERCE_COUNT -> pierceCount; case AOE_DAMAGE -> aoeDamage;
            case CHILL_DURATION -> chillDuration; case FREEZE_DURATION -> freezeDuration;
            case TARGET_COUNT -> targetCount; case LIFESPAN -> lifespan;
            case WARMTH_RADIUS -> warmthRadius; case POISON_DAMAGE -> poisonDamage;
            case SUN_AMOUNT -> sunAmount; case DOUBLE_SUN_CHANCE -> doubleSunChance ? 1 : 0;
        };
        double next = switch (operation) { case "MULTIPLY" -> old * value; case "SET" -> value; default -> old + value; };
        return switch (key) {
            case MAX_HP -> withMaxHp((int) next); case DAMAGE -> withDamage((int) next); case COST -> withCost(Math.max(0, (int) next));
            case ACTION_INTERVAL -> withInterval(Math.max(0, next)); case RECHARGE -> withRecharge(Math.max(0, next));
            case PROJECTILE_SPEED -> new PlantStats(maxHp,damage,cost,actionInterval,recharge,next,doubleSunChance,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);
            case DOUBLE_SUN_CHANCE -> withDoubleSunChance(next != 0);
            default -> new PlantStats(maxHp,damage,cost,actionInterval,recharge,projectileSpeed,doubleSunChance,
                    key==PlantStatKey.RANGE?next:range,key==PlantStatKey.PIERCE_COUNT?(int)next:pierceCount,
                    key==PlantStatKey.AOE_DAMAGE?next:aoeDamage,key==PlantStatKey.CHILL_DURATION?next:chillDuration,
                    key==PlantStatKey.FREEZE_DURATION?next:freezeDuration,key==PlantStatKey.TARGET_COUNT?(int)next:targetCount,
                    key==PlantStatKey.LIFESPAN?next:lifespan,key==PlantStatKey.WARMTH_RADIUS?next:warmthRadius,
                    key==PlantStatKey.POISON_DAMAGE?next:poisonDamage,key==PlantStatKey.SUN_AMOUNT?(int)next:sunAmount);
        };
    }
    public PlantStats withMaxHp(int v){return new PlantStats(v,damage,cost,actionInterval,recharge,projectileSpeed,doubleSunChance,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);}
    public PlantStats withDamage(int v){return new PlantStats(maxHp,v,cost,actionInterval,recharge,projectileSpeed,doubleSunChance,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);}
    public PlantStats withCost(int v){return new PlantStats(maxHp,damage,Math.max(0,v),actionInterval,recharge,projectileSpeed,doubleSunChance,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);}
    public PlantStats withInterval(double v){return new PlantStats(maxHp,damage,cost,v,recharge,projectileSpeed,doubleSunChance,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);}
    public PlantStats withRecharge(double v){return new PlantStats(maxHp,damage,cost,actionInterval,v,projectileSpeed,doubleSunChance,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);}
    public PlantStats withProjectileSpeed(double v){return new PlantStats(maxHp,damage,cost,actionInterval,recharge,v,doubleSunChance,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);}
    public PlantStats withDoubleSunChance(boolean v){return new PlantStats(maxHp,damage,cost,actionInterval,recharge,projectileSpeed,v,range,pierceCount,aoeDamage,chillDuration,freezeDuration,targetCount,lifespan,warmthRadius,poisonDamage,sunAmount);}
}
