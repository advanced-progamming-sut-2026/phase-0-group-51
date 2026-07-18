package models.Board;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.games.ancientEgypt.Grave;
import models.games.frostbite.IceFloorDirection;

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
    private Plant lilyPadPlant;
    private Plant pumpkinPlant;

    private boolean iceBlocked = false;
    private boolean water = false;
    private boolean lowShore = false;
    private boolean crater = false;
    private boolean frosted = false;
    private IceFloorDirection iceFloorDirection;
    private Grave grave;

    public static final float TILEWIDTH = 80f;
    public static final float TILEHEIGHT = 70f;

    public Tile(int lane,int column){
        this.lane = lane;
        this.column = column;
        this.x = column * TILEWIDTH;
        this.y = lane * TILEHEIGHT;
    }

    public Plant getPlant() {
        if (pumpkinPlant != null) {
            return pumpkinPlant;
        }
        return plant != null ? plant : lilyPadPlant;
    }

    public Plant getTopPlant() {
        return plant;
    }

    public boolean hasTopPlant() {
        return plant != null;
    }

    public boolean hasLilyPad() {
        return lilyPadPlant != null;
    }

    public boolean hasPumpkin() {
        return pumpkinPlant != null;
    }

    public boolean hasPlant() {
        return plant != null || lilyPadPlant != null || pumpkinPlant != null;
    }

    public boolean hasGrave() {
        return this.grave != null;
    }

    public void removePlant() {
        if (pumpkinPlant != null) {
            pumpkinPlant = null;
        } else if (plant != null) {
            plant = null;
        } else {
            lilyPadPlant = null;
        }
    }

    public void removeSpecificPlant(Plant target) {
        if (target == null) {
            return;
        }
        if (plant == target) {
            plant = null;
        } else if (lilyPadPlant == target) {
            lilyPadPlant = null;
        } else if (pumpkinPlant == target) {
            pumpkinPlant = null;
        }
    }

    public List<Plant> getPlants() {
        List<Plant> plants = new ArrayList<>();
        if (pumpkinPlant != null) {
            plants.add(pumpkinPlant);
        }
        if (plant != null) {
            plants.add(plant);
        }
        if (lilyPadPlant != null) {
            plants.add(lilyPadPlant);
        }
        return plants;
    }

    public static int toTiles(double worldUnits) {
        return (int) Math.ceil(worldUnits / TILEWIDTH);
    }
    public boolean isOccupiable() {
        return !iceBlocked && !crater && (iceFloorDirection == null) && !hasGrave() && !hasPlant();
    }

    public void removeGrave() {
        this.grave = null;
    }
    public List<Zombie> getZombies(GameState state) {
        List<Zombie> zombies = new ArrayList<>();
        if (state == null || state.getZombiesInTheGame() == null) {
            return zombies;
        }
        // TEST
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
