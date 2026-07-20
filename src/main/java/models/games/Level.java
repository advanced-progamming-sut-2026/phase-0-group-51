package models.games;

import models.Zombie.ZombieType;
import models.games.frostbite.FrostbiteLevelConfig;
import models.games.saveourseeds.SaveOurSeedsConfig;

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
        SaveOurSeedsConfig saveOurSeedsConfig
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
                SaveOurSeedsConfig.none()
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
                SaveOurSeedsConfig.none()
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
                SaveOurSeedsConfig.none()
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
                SaveOurSeedsConfig.none()
        );
    }

    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty,
            int startingSun,
            SaveOurSeedsConfig saveOurSeedsConfig
    ) {
        this(
                levelNumber,
                type,
                totalWaves,
                baseDifficulty,
                startingSun,
                List.of(),
                FrostbiteLevelConfig.none(),
                -1,
                saveOurSeedsConfig
        );
    }

    public Level {
        allowedZombies = allowedZombies == null ? List.of() : List.copyOf(allowedZombies);
        frostbiteConfig = frostbiteConfig == null ? FrostbiteLevelConfig.none() : frostbiteConfig;
        saveOurSeedsConfig = saveOurSeedsConfig == null
                ? SaveOurSeedsConfig.none()
                : saveOurSeedsConfig;
        if (type == LevelType.SAVE_OUR_SEEDS
                && !saveOurSeedsConfig.isConfigured()) {
            throw new IllegalArgumentException(
                    "A Save Our Seeds level requires protected plants."
            );
        }
        if (type == LevelType.DEAD_LINE && deadlineColumn < 1) {
            throw new IllegalArgumentException(
                    "A Dead Line level requires a positive deadline column."
            );
        }
    }

    public List<ZombieType> resolveAllowedZombies(List<ZombieType> chapterDefaults) {
        return allowedZombies.isEmpty() ? chapterDefaults : allowedZombies;
    }
}
