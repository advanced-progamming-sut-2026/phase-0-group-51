package models.games.ancientEgypt;

public class Grave {
    private int health;
    private final int lane;
    private final int column;
//این دوتا مخصوص فصل dark ages ان
    private boolean hasSun;
    private boolean hasPlantFood;
    public Grave(int lane, int column) {
        this.lane = lane;
        this.column = column;
        this.health = 700;
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
