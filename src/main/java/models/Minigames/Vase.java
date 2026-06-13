package models.Minigames;
import models.Plant.Plant;
import models.Zombie.Zombie;
public class Vase {
private VaseType vaseType;
private int x,y;
public void breakVase(){}
private Plant plantInside;//اگر نوعش plant عه
private boolean isBroken;
private Plant seedPacket; //برای نوع گیاه
private Zombie gargantuar;// برای نوع غول

    public Zombie getGargantuar() {
        return gargantuar;
    }

    public boolean isBroken() {
        return isBroken;
    }

    public Plant getPlantInside() {
        return plantInside;
    }

    public Plant getSeedPacket() {
        return seedPacket;
    }

    public VaseType getVaseType() {
        return vaseType;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
