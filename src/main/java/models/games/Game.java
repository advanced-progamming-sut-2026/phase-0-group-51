package models.games;


import Data.loader.PlantData;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
@Getter
public class Game{
    private final List<ChapterTheme> chapters = List.of(
            ChapterTheme.ANCIENT_EGYPT,
            ChapterTheme.FROSTBITE_CAVES,
           ChapterTheme.BIG_WAVE_BEACH,
           ChapterTheme.DARK_AGES
    );
    private int currentChapterIndex = 0;
    private int currentLevelIndex   = 0;
    private final List<PlantData> selectedPlantsForThisGame = new ArrayList<>();
    private GameState gameState;
    private int sunAmount;
    public void start(){}
    public void loadLevel(){}
    public void onTick(){}
    public void forward(){}
}