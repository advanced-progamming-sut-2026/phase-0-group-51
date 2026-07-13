package models.Board;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.games.ancientEgypt.Grave;

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

}
