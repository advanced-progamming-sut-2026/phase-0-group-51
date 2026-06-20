package models.Board;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
@Getter
@Setter
public class Tile {
    private final int lane;
    private final int column;
    private final float x;

    private Plant plant;
    private boolean iceBlocked = false;
    private boolean frosted = false;

    static final float tileWidth = 80f; // for example , we'll change it later in phase 2
    static final float tileHeight = 70f;

    public Tile(int lane,int column){
        this.lane = lane;
        this.column = column;
        this.x = column * tileWidth;
    }

    public boolean hasPlant() {
        if (plant == null) {
            return false;
        }
        return true;
    }
    public void removePlant() {
        this.plant = null;
    }
    public boolean isOccupiable() {
        return !iceBlocked;
    }
    public static int toTiles(double worldUnits) {
        return (int) Math.ceil(worldUnits / tileWidth);
    }


}
