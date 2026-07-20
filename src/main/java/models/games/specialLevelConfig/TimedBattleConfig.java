package models.games.specialLevelConfig;

public record TimedBattleConfig(
        int zombieKillTarget,
        int sunProductionTarget,
        int durationSeconds
) {
    public TimedBattleConfig {
        if (zombieKillTarget < 0 || sunProductionTarget < 0) {
            throw new IllegalArgumentException(
                    "Timed Battle targets cannot be negative."
            );
        }
        if (zombieKillTarget == 0 && sunProductionTarget == 0) {
            durationSeconds = 0;
        } else if (durationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "An active Timed Battle requires a positive duration."
            );
        }
    }

    public static TimedBattleConfig none() {
        return new TimedBattleConfig(0, 0, 0);
    }

    public static TimedBattleConfig killZombies(int target, int seconds) {
        return new TimedBattleConfig(target, 0, seconds);
    }

    public static TimedBattleConfig produceSun(int target, int seconds) {
        return new TimedBattleConfig(0, target, seconds);
    }

    public static TimedBattleConfig combined(
            int zombieTarget,
            int sunTarget,
            int seconds
    ) {
        return new TimedBattleConfig(zombieTarget, sunTarget, seconds);
    }

    public boolean isEnabled() {
        return zombieKillTarget > 0 || sunProductionTarget > 0;
    }

    public boolean requiresZombieKills() {
        return zombieKillTarget > 0;
    }

    public boolean requiresSunProduction() {
        return sunProductionTarget > 0;
    }
}
