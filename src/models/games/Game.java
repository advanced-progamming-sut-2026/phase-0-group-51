package models.games;


import java.util.List;

public class Game{
    private final List<ChapterTheme> chapters = List.of(
            ChapterTheme.ANCIENT_EGYPT,
            ChapterTheme.FROSTBITE_CAVES,
            ChapterTheme.BIG_WAVE_BEACH,
            ChapterTheme.DARK_AGES
    );
    private int currentChapterIndex = 0;
    private int currentLevelIndex   = 0;
    private GameState gameState;

    public void start(){}
    public void loadLevel(){}
    public void onTick(){}
    public void forward(){}
}