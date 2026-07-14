package models.Board;

import lombok.Getter;
import lombok.Setter;
import models.plant.Plant;
import models.zombie.Zombie;
import models.games.GameState;
import models.games.ancientEgypt.Grave;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Tile {
    private final int lane;
    private final int column;
    private final float x;
    private final float y;
    private Plant plant;
    private boolean iceBlocked = false;
    private boolean frosted = false;
    private Grave grave;

    public static final float TILEWIDTH = 80f; // for example , we'll change it later in phase 2
    public static final float TILEHEIGHT = 70f;

    public Tile(int lane,int column){
        this.lane = lane;
        this.column = column;
        this.x = column * TILEWIDTH;
        this.y = lane * TILEHEIGHT;
    }

    public boolean hasPlant() {
        if (plant == null) {
            return false;
        }
        return true;
    }

    public boolean hasGrave() {
        return this.grave != null;
    }

    public void removePlant() {
        this.plant = null;
    }
    public static int toTiles(double worldUnits) {
        return (int) Math.ceil(worldUnits / TILEWIDTH);
    }
    public boolean isOccupiable() {
        return !iceBlocked && !hasGrave() && !hasPlant();
    }

    public void removeGrave() {
        this.grave = null;
    }
    public List<Zombie> getZombies(GameState state) {
        List<Zombie> zombies = new ArrayList<>();
        if (state == null || state.getZombiesInTheGame() == null) {
            return zombies;
        }
        float tileStartX = column;
        float tileEndX = tileStartX + 1f;
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie.isDead()) {
                continue;
            }
            if (zombie.getLane() != lane) {
                continue;
            }
            float zombieX = zombie.getX();

            if (zombieX >= tileStartX && zombieX < tileEndX) {
                zombies.add(zombie);
            }
        }
        return zombies;
    }
    public boolean hasZombie(GameState state) {
        return !getZombies(state).isEmpty();
    }
}
