package models.games.ancientEgypt;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Grave {
    private int health;
    private final int lane;
    private final int column;

    private boolean hasSun;
    private boolean hasPlantFood;
    public Grave(int lane, int column) {
        this.lane = lane;
        this.column = column;
        this.health = 700;
    }
    public void makeSunGrave() {
        hasSun = true;
        hasPlantFood = false;
    }
    public void makePlantFoodGrave() {
        hasPlantFood = true;
        hasSun = false;
    }
    public String getDisplayType() {
        if (hasPlantFood) {
            return "Plant Food grave";
        }
        if (hasSun) {
            return "Sun grave";
        }
        return "normal grave";
    }

    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health < 0) {
            this.health = 0;
        }
    }

    public boolean isDestroyed() {
        return this.health <= 0;
    }
}
