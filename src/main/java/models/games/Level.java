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
        FrostbiteLevelConfig frostbiteConfig,
        int deadlineColumn,
        int plantLossLimit
) {
    public Level(int levelNumber, LevelType type, int totalWaves, float baseDifficulty) {
        this(
                levelNumber,
                type,
                totalWaves,
                baseDifficulty,
                50,
                List.of(),
                FrostbiteLevelConfig.none(),
                -1,
                -1
        );
    }

    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty,
            FrostbiteLevelConfig frostbiteConfig
    ) {
        this(
                levelNumber,
                type,
                totalWaves,
                baseDifficulty,
                50,
                List.of(),
                frostbiteConfig,
                -1,
                -1
        );
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
                FrostbiteLevelConfig.none(),
                -1,
                -1
        );
    }

    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty,
            FrostbiteLevelConfig frostbiteConfig,
            int deadlineColumn
    ) {
        this(
                levelNumber,
                type,
                totalWaves,
                baseDifficulty,
                50,
                List.of(),
                frostbiteConfig,
                deadlineColumn,
                -1
        );
    }

    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty,
            int plantLossLimit
    ) {
        this(
                levelNumber,
                type,
                totalWaves,
                baseDifficulty,
                50,
                List.of(),
                FrostbiteLevelConfig.none(),
                -1,
                plantLossLimit
        );
    }

    public Level {
        allowedZombies = allowedZombies == null ? List.of() : List.copyOf(allowedZombies);
        frostbiteConfig = frostbiteConfig == null ? FrostbiteLevelConfig.none() : frostbiteConfig;
        if (type == LevelType.DEAD_LINE && deadlineColumn < 1) {
            throw new IllegalArgumentException(
                    "A Dead Line level requires a positive deadline column."
            );
        }

        if (type == LevelType.LOVE_YOUR_PLANTS && plantLossLimit < 1) {
            throw new IllegalArgumentException(
                    "A Plants You Love level requires a positive plant-loss limit."
            );
    }
}

    public List<ZombieType> resolveAllowedZombies(
            List<ZombieType> chapterDefaults
    ) {
        return allowedZombies.isEmpty()
                ? chapterDefaults
                : allowedZombies;
    }
}