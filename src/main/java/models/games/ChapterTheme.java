package models.games;

import lombok.Getter;
import models.Zombie.ZombieType;
import models.games.frostbite.FrostbiteLevelConfig;
import models.games.frostbite.IceFloorDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public enum ChapterTheme {
    ANCIENT_EGYPT(
            "Ancient Egypt",
            withDefaults(ZombieType.RA, ZombieType.EXPLORER, ZombieType.TOMB_RAISER),
            List.of(ChapterFeature.GRAVE, ChapterFeature.TORNADO),
            List.of(
                    new Level(
                            1,
                            LevelType.NORMAL,
                            2,
                            1000f,
                            50,
                            List.of(
                                    ZombieType.DEFAULT,
                                    ZombieType.ARMOR_1,
                                    ZombieType.ARMOR_2,
                                    ZombieType.RA,
                                    ZombieType.EXPLORER,
                                    ZombieType.TOMB_RAISER
                            )
                    ),
                    new Level(2, LevelType.NORMAL, 3, 1500f),
                    new Level(3, LevelType.CONVEYOR_BELT, 4, 1500f),
                    new Level(4, LevelType.BOSS, 5, 2000f)
            ),
            TimeOfTheDay.DAY
    ),
    FROSTBITE_CAVES(
            "Frostbite Caves",
            withDefaults(
                    ZombieType.ICE_AGE_TROGLOBITE,
                    ZombieType.ICE_AGE_DODO,
                    ZombieType.ICE_AGE_HUNTER
            ),
            List.of(ChapterFeature.ICE_WIND, ChapterFeature.ICE_FLOOR),
            List.of(
                    new Level(1, LevelType.NORMAL, 2, 1000f, frostbiteLevelOne()),
                    new Level(2, LevelType.NORMAL, 3, 1500f, frostbiteLevelTwo()),
                    new Level(3, LevelType.SAVE_OUR_SEEDS, 4, 2000f, frostbiteLevelThree()),
                    new Level(4, LevelType.BOSS, 5, 2000f, frostbiteLevelFour())
            ),
            TimeOfTheDay.DAY
    ),
    BIG_WAVE_BEACH(
            "Big Wave Beach",
            withDefaults(
                    ZombieType.BEACH_FISHERMAN,
                    ZombieType.BEACH_OCTOPUS,
                    ZombieType.BEACH_SNORKEL
            ),
            List.of(ChapterFeature.WATER_LEVEL, ChapterFeature.BACKWATER),
            List.of(
                    new Level(1, LevelType.NORMAL, 2, 1000f),
                    new Level(2, LevelType.NORMAL, 3, 1500f),
                    new Level(3, LevelType.PLANT_WHAT_YOU_GET, 4, 2000f),
                    new Level(4, LevelType.BOSS, 5, 2000f)
            ),
            TimeOfTheDay.DAY
    ),
    DARK_AGES(
            "Dark Ages",
            withDefaults(ZombieType.DARK_KING, ZombieType.DARK_JUGGLER, ZombieType.WIZARD),
            List.of(
                    ChapterFeature.NIGHT,
                    ChapterFeature.GRAVE,
                    ChapterFeature.GRAVE_SPAWN,
                    ChapterFeature.NECROMANCY
            ),
            List.of(
                    new Level(1, LevelType.NORMAL, 2, 1000f),
                    new Level(2, LevelType.NORMAL, 3, 1500f),
                    new Level(3, LevelType.NIGHT_OPS, 4, 2000f),
                    new Level(4, LevelType.BOSS, 5, 2000f)
            ),
            TimeOfTheDay.NIGHT
    );

    public final String name;
    public final List<ZombieType> allowedZombies;
    public final List<ChapterFeature> chapterFeatures;
    public final List<Level> levels;
    public final TimeOfTheDay timeOfTheDay;

    ChapterTheme(
            String name,
            List<ZombieType> allowedZombies,
            List<ChapterFeature> chapterFeatures,
            List<Level> levels,
            TimeOfTheDay timeOfTheDay
    ) {
        this.name = name;
        this.allowedZombies = allowedZombies;
        this.chapterFeatures = chapterFeatures;
        this.levels = levels;
        this.timeOfTheDay = timeOfTheDay;
    }

    private static List<ZombieType> withDefaults(ZombieType... specifics) {
        List<ZombieType> combined = new ArrayList<>();
        combined.add(ZombieType.DEFAULT);
        combined.add(ZombieType.ARMOR_1);
        combined.add(ZombieType.ARMOR_2);
        combined.add(ZombieType.ARMOR_4);
        combined.add(ZombieType.IMP);
        combined.addAll(Arrays.asList(specifics));
        return Collections.unmodifiableList(combined);
    }

    private static FrostbiteLevelConfig frostbiteLevelOne() {
        return new FrostbiteLevelConfig(0.50, 1, 2, List.of(), List.of());
    }

    private static FrostbiteLevelConfig frostbiteLevelTwo() {
        return new FrostbiteLevelConfig(
                0.65,
                1,
                2,
                List.of(
                        iceFloor(0, 5, IceFloorDirection.DOWN),
                        iceFloor(2, 4, IceFloorDirection.UP),
                        iceFloor(3, 6, IceFloorDirection.DOWN)
                ),
                List.of(frozenZombie(ZombieType.DEFAULT, 1, 6))
        );
    }

    private static FrostbiteLevelConfig frostbiteLevelThree() {
        return new FrostbiteLevelConfig(
                0.75,
                1,
                3,
                List.of(
                        iceFloor(1, 5, IceFloorDirection.UP),
                        iceFloor(2, 6, IceFloorDirection.DOWN),
                        iceFloor(4, 4, IceFloorDirection.UP)
                ),
                List.of(
                        frozenZombie(ZombieType.ARMOR_1, 0, 7),
                        frozenZombie(ZombieType.DEFAULT, 3, 6)
                )
        );
    }

    private static FrostbiteLevelConfig frostbiteLevelFour() {
        return new FrostbiteLevelConfig(
                0.85,
                2,
                3,
                List.of(
                        iceFloor(1, 6, IceFloorDirection.DOWN),
                        iceFloor(3, 5, IceFloorDirection.UP)
                ),
                List.of(frozenZombie(ZombieType.ARMOR_2, 2, 7))
        );
    }

    private static FrostbiteLevelConfig.IceFloorPlacement iceFloor(
            int lane,
            int column,
            IceFloorDirection direction
    ) {
        return new FrostbiteLevelConfig.IceFloorPlacement(lane, column, direction);
    }

    private static FrostbiteLevelConfig.FrozenZombiePlacement frozenZombie(
            ZombieType zombieType,
            int lane,
            int column
    ) {
        return new FrostbiteLevelConfig.FrozenZombiePlacement(zombieType, lane, column);
    }
}
