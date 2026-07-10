package models.games;

import lombok.Getter;
import lombok.Setter;
import models.Zombie.Zombie;

import java.util.List;
@Getter

public enum ChapterTheme {
    ANCIENT_EGYPT(),
    FROSTBITE_CAVES(),
    BIG_WAVE_BEACH(),
    DARK_AGES();


   public final String name;
    public final List<Zombie> allowedZombies;
    public final List<ChapterFeature> chapterFeatures;
    public final List<Level> levels;
public final TimeOfTheDay timeOfTheDay;

    ChapterTheme(String name, List<Zombie> allowedZombies, List<ChapterFeature> chapterFeatures, List<Level> levels,
                 TimeOfTheDay timeOfTheDay) {
       this.name = name;
       this.allowedZombies = allowedZombies;
       this.chapterFeatures = chapterFeatures;
       this.levels = levels;
       this.timeOfTheDay = timeOfTheDay;
    }
}
