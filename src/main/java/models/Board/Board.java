package models.Board;

import lombok.Getter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.projectile.Projectile;
import models.sun.Sun;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
public class Board {
    private final int laneCount = 5;
    private final int columnCount = 9;
    private final Tile[][] tiles = new Tile[laneCount][columnCount];
    private final List<Sun> suns = new ArrayList<>();
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private Random random = new Random();

    public Board() {
        initializeTiles();
    }

    private void initializeTiles() {
        for (int i = 0; i < laneCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                tiles[i][j] = new Tile(i, j);
            }
        }
    }
    public Tile getTile(int lane, int column) {
        return tiles[lane][column];
    }
    public Plant getPlant(int lane, float x) {
        int column = (int) (x/Tile.tileWidth);
        Tile tile = getTile(lane,column);
        return tile.getPlant();
    }
    public void removePlant(int lane, int column) {
        Tile tile = getTile(lane, column);
        if (tile != null) tile.removePlant();
    }
    public Plant findNearestPlantInRange(int lane, int fromColumn, int range) {
        int minCol = Math.max(0, fromColumn - range);
        for (int col = fromColumn; col >= minCol; col--) {
            Tile tile = getTile(lane, col);
            if (tile != null && tile.hasPlant()) {
                return tile.getPlant();
            }
        }
        return null;
    }
    public List<Plant> getPlantsInLane(int lane) {
        List<Plant> result = new ArrayList<>();
        for (int col = 0; col < columnCount; col++) {
            Tile tile = getTile(lane, col);
            if (tile != null && tile.hasPlant()) {
                result.add(tile.getPlant());
            }
        }
        return result;
    }
    public boolean pushPlantBack(int lane, int column) {
        Tile current = getTile(lane, column);
        if (current == null || !current.hasPlant()) return false;

        Tile behind = getTile(lane, column - 1);
        if (behind == null || behind.hasPlant() || !behind.isOccupiable()) return false;

        Plant plant = current.getPlant();
        plant.setPosX(behind.getColumn());
        behind.setPlant(plant);
        current.setPlant(null);
        return true;
    }
    public void setIceBlock(int lane, int column, boolean blocked) {
        Tile tile = getTile(lane, column);
        if (tile != null) tile.setIceBlocked(blocked);
    }
    public List<Tile> dropIceBlocks(int lane, int column, int count) {
        List<Tile> blocked = new ArrayList<>();
        int placed = 0;
        for (int offset = 0; offset < laneCount && placed < count; offset++) {
            int l = lane + offset;
            if (l >= laneCount) break;
            Tile tile = getTile(l, column);
            if (tile != null && !tile.hasPlant()) {
                tile.setIceBlocked(true);
                blocked.add(tile);
                placed++;
            }
        }
        return blocked;
    }
    public void addSun(Sun sun) { suns.add(sun); }
    public void removeSun(Sun sun) {
        sun.setCollected(true);
        suns.remove(sun);
    }
    public List<Sun> getActiveSuns() {
        List<Sun> result = new ArrayList<>();
        for (Sun sun : suns) {
            if (sun.isActive()) result.add(sun);
        }
        return result;
    }
    public Tile placeGraveOnRandomTile() {
        List<Tile> eligible = new ArrayList<>();
        for (int lane = 0; lane < laneCount; lane++) {
            for (int col = 0; col < columnCount; col++) {
                Tile tile = tiles[lane][col];
                if (!tile.hasPlant() && !tile.isGrave() && tile.isOccupiable()) {
                    eligible.add(tile);
                }
            }
        }
        if (eligible.isEmpty()) return null;
        Tile chosen = eligible.get(random.nextInt(eligible.size()));
        chosen.setGrave(true);
        return chosen;
    }
    public Zombie getFirstZombieInLane(int lane) {
        Zombie first = null;
        for (Zombie zombie : zombies) {
            if (zombie.getLane() == lane) {
                if (first == null || zombie.getX() < first.getX()) {
                    first = zombie;
                }
            }
        }
        return first;
    }
    public List<Zombie> getZombiesInLane(int lane) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (zombie.getLane() == lane) {
                result.add(zombie);
            }
        }return result;
    }

    public Zombie getZombieInPosition(int lane, int column) {
        for (Zombie zombie : getZombiesInLane(lane)) {
            if(zombie.getX() == column) {
                return zombie;
            }
        }return null;
    }

    public Zombie getClosestZombieAnywhere(int fromLane, int fromColumn) {
        Zombie closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Zombie zombie : zombies) {
            double dLane = zombie.getLane() - fromLane;
            double dCol = zombie.getX() - fromColumn;
            double distSq = dLane * dLane + dCol * dCol;
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = zombie;
            }
        }
        return closest;
    }

    public List<Zombie> getRandomZombies(int count) {
        List<Zombie> pool = new ArrayList<>(zombies);
        List<Zombie> chosen = new ArrayList<>();
        int n = Math.min(count, pool.size());
        for (int i = 0; i < n; i++) {
            int index = random.nextInt(pool.size());
            chosen.add(pool.remove(index));
        }
        return chosen;
    }

    public List<Tile> getTwoRandomTilesWithoutPlants(){
        List<Tile> eligible = new ArrayList<>();
        for (int lane = 0; lane < laneCount; lane++) {
            for (int col = 0; col < columnCount; col++) {
                Tile tile = tiles[lane][col];
                if (!tile.hasPlant() && !tile.isGrave() && tile.isOccupiable()) {
                    eligible.add(tile);
                }
            }
        }
        if (eligible.isEmpty()) return null;
        List<Tile> chosen = new ArrayList<>();
        List<Tile> pool = new ArrayList<>(eligible);
        for (int i = 0; i < 2; i++) {
            int index = random.nextInt(pool.size());
            chosen.add(pool.remove(index));
        }
        return chosen;
    }

    public void removeProjectile(Projectile projectile) {
        projectiles.remove(projectile);
    }

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }
}
