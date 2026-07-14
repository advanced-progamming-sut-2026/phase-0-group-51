package models.games;

import lombok.Getter;
import lombok.Setter;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ancientEgypt.Grave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
@Getter

public enum ChapterTheme {
    ANCIENT_EGYPT(
            "Ancient Egypt",
            withDefaults(ZombieType.RA,ZombieType.EXPLORER,ZombieType.TOMB_RAISER),
            List.of(ChapterFeature.GRAVE, ChapterFeature.TORNADO),
            List.of(
                    new Level(
                            1,
                            LevelType.NORMAL,
                            2,
                            1000f,
                            50,
                            List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1, ZombieType.ARMOR_2,
                                    ZombieType.RA, ZombieType.EXPLORER, ZombieType.TOMB_RAISER)
                    ),
                    new Level(2, LevelType.NORMAL, 3,1500f),
                    new Level(3, LevelType.CONVEYOR_BELT,4,1500f),
                    new Level(4, LevelType.BOSS, 5,2000f)
            ),
            TimeOfTheDay.DAY
    ),
    FROSTBITE_CAVES("Frostbite Caves",
            withDefaults(ZombieType.ICE_AGE_TROGLOBITE,ZombieType.ICE_AGE_DODO,ZombieType.ICE_AGE_HUNTER),
            List.of(ChapterFeature.ICE_WIND, ChapterFeature.ICE_FLOOR),
            List.of(
                    new Level(1, LevelType.NORMAL, 2,1000f),
                    new Level(2, LevelType.NORMAL, 3,1500f),
                    new Level(3, LevelType.SAVE_OUR_SEEDS, 4,2000f),
                    new Level(4, LevelType.BOSS, 5,2000f)
            ),
            TimeOfTheDay.DAY),
    BIG_WAVE_BEACH("Big Wave Beach",
           withDefaults(ZombieType.BEACH_FISHERMAN,ZombieType.BEACH_OCTOPUS,ZombieType.BEACH_SNORKEL),
            List.of(ChapterFeature.WATER_LEVEL, ChapterFeature.BACKWATER),
            List.of(
                    new Level(1, LevelType.NORMAL, 2,1000f),
                    new Level(2, LevelType.NORMAL, 3,1500f),
                    new Level(3, LevelType.PLANT_WHAT_YOU_GET, 4,2000f),
                    new Level(4, LevelType.BOSS, 5,2000f)
            ),
            TimeOfTheDay.DAY),
    DARK_AGES("Dark Ages",
           withDefaults(ZombieType.DARK_KING,ZombieType.DARK_JUGGLER,ZombieType.WIZARD),
            List.of(ChapterFeature.NIGHT, ChapterFeature.GRAVE, ChapterFeature.GRAVE_SPAWN, ChapterFeature.NECROMANCY),
            List.of(
                    new Level(1, LevelType.NORMAL, 2,1000f),
                    new Level(2, LevelType.NORMAL, 3,1500f),
                    new Level(3, LevelType.NIGHT_OPS, 4,2000f),
                    new Level(4, LevelType.BOSS, 5,2000f)
            ),
            TimeOfTheDay.NIGHT);


   public final String name;
    public final List<ZombieType> allowedZombies;
    public final List<ChapterFeature> chapterFeatures;
    public final List<Level> levels;
public final TimeOfTheDay timeOfTheDay;

    ChapterTheme(String name, List<ZombieType> allowedZombies, List<ChapterFeature> chapterFeatures, List<Level> levels,
                 TimeOfTheDay timeOfTheDay) {
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
}
