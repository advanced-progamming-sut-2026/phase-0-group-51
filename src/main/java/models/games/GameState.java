package models.games;

import lombok.Getter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.Board.Board;
import models.Board.Tile;

import java.util.HashSet;
import java.util.Set;

@Getter
public class GameState {
    private final int ticksPerSecond = 10;
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
    public void addSun(int amount){
        this.sun += amount;
    }
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
    public void stealSun(float amount){
        this.sun = Math.max(0, this.sun - amount);
    }
    public Zombie findNearestHypnotizedZombieInRange(Zombie self, int lane, float x, int range) {
        Zombie nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Zombie other : zombiesInTheGame) {
            if (other == self || !other.isHypnotized() || other.isDead()) continue;
            if (other.getLane() != lane) continue;
            float distance = Math.abs(other.getX() - x);
            if (distance <= range && distance < nearestDistance) {
                nearest = other;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
    public void swapRandomZombieLanes(int swapCount) {
        for (Zombie z : getZombiesInTheGame()) {
            if (z.isDead()) continue;
            int current = z.getLane();
            int target = getRandomNeighborLane(current);
            if (target != current) {
                z.setLane(target);
            }
        }
    }

    private int getRandomNeighborLane(int lane) {
        if (lane == 0) return 1;
        if (lane == 4) return 3;
        return Math.random() < 0.5 ? lane - 1 : lane + 1;
    }


}
