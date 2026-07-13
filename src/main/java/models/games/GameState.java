package models.games;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.Board.Board;
import models.Board.Tile;
import models.items.Mower;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Setter
@Getter
public class GameState {
    private final int ticksPerSecond = 10;
    private final Board board;
    private Set<Zombie> zombiesInTheGame = new HashSet<>();
    private int sun;
    private boolean isFinished;
    private boolean isWon;
    private final ChapterTheme chapterTheme;
    private Level currentLevel;
    private int tickCounter = 0;
    private ZombieWaveManager zombieWaveManager;
    private final Mower[] lawnMowers;
    private Consumer<String> eventLogger;
    public void logEvent(String message) {
        if (eventLogger != null) {
            eventLogger.accept(message);
        }
    }
    public GameState(Board board, ChapterTheme chapterTheme) {
        this.board = board;
        this.board.setZombie(this.zombiesInTheGame);
        this.chapterTheme = chapterTheme;
        this.lawnMowers = new Mower[board.getLaneCount()];
        for (int i = 0; i < lawnMowers.length; i++) {
            lawnMowers[i] = new Mower(i,this);
        }
    }
    public boolean checkLoseCondition() {
        for (Zombie zombie : zombiesInTheGame) {
            if (!zombie.isDead() && zombie.getX() < 0) {
                int lane = zombie.getLane();
                Mower mower = lawnMowers[lane];
                if (mower.isActivated() || mower.isDestroyed()) {
                    logEvent("The zombie ate your brain; LOSER!!!\n");
                    return true;
                }
            }
        }
        return false;
    }
    private void killAllZombiesInLane(int lane) {
        for (Zombie z : zombiesInTheGame) {
            if (z.getLane() == lane) {
                z.killInstantly(this);
            }
        }
    }
    public void addTick(int tick){
        this.tickCounter += tick;
    }
    public void spawnWave(){}
    public void spawnBoss(){}
    public void increaseSunBalance(int amount) {
        this.sun += amount;
    }

    public void decreaseSunBalance(int amount) {
        this.sun = Math.max(0, this.sun - amount);
    }
    public void plantPlant(Plant plant, Tile tile){}
    public void pluckPlant(Plant plant, Tile tile){}


    public void addZombie(Zombie zombie) {
        zombiesInTheGame.add(zombie);
    }
    public void removeZombie(Zombie zombie) {
        zombiesInTheGame.remove(zombie);
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
    public void tickMowers() {
        for (Mower mower : lawnMowers) {
            mower.update(this.board);
        }
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
