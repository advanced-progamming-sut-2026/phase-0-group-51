package models.games;

import models.Zombie.ZombieType;
import models.games.frostbite.FrostbiteLevelConfig;
import models.games.specialLevelConfig.SaveOurSeedsConfig;
import models.games.specialLevelConfig.TimedBattleConfig;

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
        int plantLossLimit,
        SaveOurSeedsConfig saveOurSeedsConfig,
        TimedBattleConfig timedBattleConfig
) {
    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty
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
                -1,
                SaveOurSeedsConfig.none(),
                TimedBattleConfig.none()
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
                -1,
                SaveOurSeedsConfig.none(),
                TimedBattleConfig.none()
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
                -1,
                SaveOurSeedsConfig.none(),
                TimedBattleConfig.none()
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
                -1,
                SaveOurSeedsConfig.none(),
                TimedBattleConfig.none()
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
                plantLossLimit,
                SaveOurSeedsConfig.none(),
                TimedBattleConfig.none()
        );
    }

    public Level(
            int levelNumber,
            LevelType type,
            int totalWaves,
            float baseDifficulty,
            FrostbiteLevelConfig frostbiteConfig,
            TimedBattleConfig timedBattleConfig
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
                -1,
                SaveOurSeedsConfig.none(),
                timedBattleConfig
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
                -1,
                saveOurSeedsConfig,
                TimedBattleConfig.none()
        );
    }

    public Level {
        allowedZombies = allowedZombies == null
                ? List.of()
                : List.copyOf(allowedZombies);

        frostbiteConfig = frostbiteConfig == null
                ? FrostbiteLevelConfig.none()
                : frostbiteConfig;

        saveOurSeedsConfig = saveOurSeedsConfig == null
                ? SaveOurSeedsConfig.none()
                : saveOurSeedsConfig;

        timedBattleConfig = timedBattleConfig == null
                ? TimedBattleConfig.none()
                : timedBattleConfig;

        if (type == LevelType.SAVE_OUR_SEEDS
                && !saveOurSeedsConfig.isConfigured()) {
            throw new IllegalArgumentException(
                    "A Save Our Seeds level requires protected plants."
            );
        }

        if (type == LevelType.DEAD_LINE
                && deadlineColumn < 1) {
            throw new IllegalArgumentException(
                    "A Dead Line level requires "
                            + "a positive deadline column."
            );
        }

        if (type == LevelType.LOVE_YOUR_PLANTS
                && plantLossLimit < 1) {
            throw new IllegalArgumentException(
                    "A Plants You Love level requires "
                            + "a positive plant-loss limit."
            );
        }

        if (type == LevelType.TIMED_BATTLE
                && !timedBattleConfig.isEnabled()) {
            throw new IllegalArgumentException(
                    "A Timed Battle level requires "
                            + "at least one objective."
            );
        }

        if (type != LevelType.TIMED_BATTLE
                && timedBattleConfig.isEnabled()) {
            throw new IllegalArgumentException(
                    "Timed Battle configuration belongs only "
                            + "to Timed Battle levels."
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