package models.games;

import lombok.Getter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.Board.Board;
import models.Board.Tile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
@Getter
public class GameState {
    private final Board board;
    private Set<Zombie> zombiesInTheGame = new HashSet<>();
    private int sun;
    private boolean isFinished;
    private final ChapterTheme chapterTheme;
    private Level currentLevel;
    private int tickCounter = 0;


    public GameState(Board board, ChapterTheme chapterTheme) {
        this.board = board;
        this.chapterTheme = chapterTheme;
    }

    public void addTick(int tick){
        this.tickCounter += tick;
    }
    public void spawnWave(){}
    public void spawnBoss(){}
    public void addSun(){}
    public void plantPlant(Plant plant, Tile tile){}
    public void pluckPlant(Plant plant, Tile tile){}
    public void applyChapterFeature(ChapterFeature feature){}


    public void addZombie(Zombie zombie) {
        zombiesInTheGame.add(zombie);
    }
    public void removeZombie(Zombie zombie) {
        zombiesInTheGame.remove(zombie);
    }
    //for zombie's actions
    public void stealSun(int amount){
        this.sun = Math.max(0, this.sun - amount);
    }






}
