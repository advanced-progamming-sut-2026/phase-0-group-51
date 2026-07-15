package models.games;

import models.Zombie.ZombieType;
import models.games.frostbite.FrostbiteLevelConfig;

import java.util.List;

public record Level(
        int levelNumber,
        LevelType type,
        int totalWaves,
        float baseDifficulty,
        int startingSun,
        List<ZombieType> allowedZombies,
        FrostbiteLevelConfig frostbiteConfig
) {
    public Level(int levelNumber, LevelType type, int totalWaves, float baseDifficulty) {
        this(levelNumber, type, totalWaves, baseDifficulty, 50, List.of(), FrostbiteLevelConfig.none());
    }

    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty,
            FrostbiteLevelConfig frostbiteConfig
    ) {
        this(levelNumber, type, totalWaves, baseDifficulty, 50, List.of(), frostbiteConfig);
    }

    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty,
            int startingSun,
            List<ZombieType> allowedZombies
    ) {
        this(
                levelNumber,
                type,
                totalWaves,
                baseDifficulty,
                startingSun,
                allowedZombies,
                FrostbiteLevelConfig.none()
        );
    }

    public Level {
        allowedZombies = allowedZombies == null ? List.of() : List.copyOf(allowedZombies);
        frostbiteConfig = frostbiteConfig == null ? FrostbiteLevelConfig.none() : frostbiteConfig;
    }

    public List<ZombieType> resolveAllowedZombies(List<ZombieType> chapterDefaults) {
        return allowedZombies.isEmpty() ? chapterDefaults : allowedZombies;
    }
}
