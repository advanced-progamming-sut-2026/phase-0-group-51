package models.Board;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.games.ancientEgypt.Grave;
import models.projectile.Projectile;
import models.sun.Sun;
import models.sun.SunType;

import java.util.*;

@Setter
@Getter
public class Board {
    private final int laneCount = 5;
    private final int columnCount = 9;
    private final Tile[][] tiles = new Tile[laneCount][columnCount];
    private final List<Sun> suns = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private Set<Zombie> zombies;
    private Random random = new Random();

    public Board() {
        initializeTiles();

    }
    public void setZombie(Set<Zombie> zombies){
        this.zombies = zombies;
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
        int column = (int) (x/Tile.TILEWIDTH);
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
    public boolean collectSun(Sun sun, GameState game) {
        if (!sun.isActive())
            return false;
        if (sun.getSunType() == SunType.RADIOACTIVE && !sun.isGrounded()) {
            explodeRadioactiveSun(sun,game);
        } else {
            game.increaseSunBalance(sun.getAmount());
        }
        sun.setCollected(true);
        suns.remove(sun);
        return true;
    }

    private void explodeRadioactiveSun(Sun sun,GameState gs) {
        int sunLane = sun.getLane();
        int sunColumn = (int) (sun.getX() / Tile.TILEWIDTH);

        for (Zombie zombie : gs.getZombiesInTheGame()) {
            int zombieLane = zombie.getLane();
            int zombieColumn = (int) (zombie.getX() / Tile.TILEWIDTH);
            if (Math.abs(zombieLane - sunLane) <= 2 && Math.abs(zombieColumn - sunColumn) <= 2) {
                zombie.takeDamage(150, gs, null);
            }
        }

        int startLane = Math.max(0, sunLane - 1);
        int endLane = Math.min(laneCount - 1, sunLane + 1);
        int startCol = Math.max(0, sunColumn - 1);
        int endCol = Math.min(columnCount - 1,sunColumn + 1);

        for (int r = startLane; r <= endLane; r++) {
            for (int c = startCol; c <= endCol; c++) {
                Tile tile = tiles[r][c];
                if (tile.hasPlant()) {
                    Plant p = tile.getPlant();
                    p.takeDamage(80);

                    if (p.isDead()) {
                        tile.removePlant();
                    }
                }
            }
        }
    }
    public Tile getTileAtUserCoordinates(int x, int y) {
        if (y < 0 || y >= laneCount) {
            return null;
        }
        if (x < 0 || x >= columnCount) {
            return null;
        }
        return tiles[y][x];
    }
    public void tickSuns(GameState gs){
        Iterator<Sun> iterator = suns.iterator();
        while(iterator.hasNext()){
            Sun sun = iterator.next();
            models.Result result = sun.tick();
            if (result.isSuccess() && result.getMessage() != null && !result.getMessage().isEmpty()) {
                gs.logEvent(result.getMessage());
            }
            if (sun.isExpired() || sun.isCollected()) {
                iterator.remove();
            }
        }
    }
    public void spawnSun(Sun sun) {
        suns.add(sun);
    }
    public Tile placeGraveOnRandomTile() {
        List<Tile> eligible = new ArrayList<>();

        for (int lane = 0; lane < laneCount; lane++) {
            for (int col = 4; col < columnCount; col++) {
                Tile tile = tiles[lane][col];
                if (tile.isOccupiable()) {
                    eligible.add(tile);
                }
            }
        }
        if (eligible.isEmpty()) return null;
        Tile chosen = eligible.get(random.nextInt(eligible.size()));
        Grave grave = new Grave(chosen.getLane(), chosen.getColumn());
        chosen.setGrave(grave);
        return chosen;
    }
    public Zombie getFirstZombieInLane(int lane) {
        Zombie first = null;
        for (Zombie zombie :zombies) {
            if (zombie.getLane() == lane && !zombie.isDead()) {
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
            if (zombie.getLane() == lane && !zombie.isDead()) {
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

    public List<Zombie> getZombiesInRadius(double lane, double column, double radius) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : zombies) {
            double dLane = zombie.getLane() - lane;
            double dCol = zombie.getX() - column;
            if (Math.sqrt(dLane * dLane + dCol * dCol) <= radius) {
                result.add(zombie);
            }
        }
        return result;
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
                if (!tile.hasPlant() && !tile.hasGrave() && tile.isOccupiable()) {
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
    public Tile getTileForPlant(Plant plant) {
        if (plant == null) return null;
        for (int i = 0; i < laneCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                if (tiles[i][j].hasPlant() && tiles[i][j].getPlant().equals(plant)) {
                    return tiles[i][j];
                }
            }
        }
        return null;
    }
    public void removeProjectile(Projectile projectile) {
        projectiles.remove(projectile);
    }

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }
}
