package models.games;

import models.Plant.Plant;
import models.Zombie.Zombie;

import java.util.List;

public enum ChapterTheme {
    ANCIENT_EGYPT(),
    FROSTBITE_CAVES(),
    BIG_WAVE_BEACH(),
    DARK_AGES();

    public final String name;
    public final List<Zombie> allowedZombies;
    public final List<ChapterFeature> chapterFeatures;
    public final TimeOfTheDay timeOfTheDay;

    ChapterTheme(String name, List<Zombie> allowedZombies, List<ChapterFeature> chapterFeatures, TimeOfTheDay timeOfTheDay) {
        this.name = name;
        this.allowedZombies = allowedZombies;
        this.chapterFeatures = chapterFeatures;
        this.timeOfTheDay = timeOfTheDay;
    }
    public String getName(){ return this.name; }
}
