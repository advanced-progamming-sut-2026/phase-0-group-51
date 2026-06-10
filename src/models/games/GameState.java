package models.games;

import models.Plant.Plant;
import models.Zombie.Zombie;
import models.Board.Board;
import models.Board.Tile;

import java.util.Set;

public class GameState {
    private final Board board;
    private Set<Zombie> zombiesInTheGame;
    private int sun;
    private boolean isFinished;
    private final ChapterTheme chapterTheme;
    private Level currentLevel;
    private int tickConuter = 0;



    public GameState(Board board, ChapterTheme chapterTheme) {
        this.board = board;
        this.chapterTheme = chapterTheme;
    }

    public void addTick(int tick){}
    public void spawnWave(){}
    public void spawnBoss(){}
    public void addSun(){}
    public void plantPlant(Plant plant, Tile tile){}
    public void pluckPlant(Plant plant, Tile tile){}
    public void applyChapterFeature(ChapterFeature feature){}


}
