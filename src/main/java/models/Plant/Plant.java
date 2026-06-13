package models.Plant;

import java.util.ArrayList;
import java.util.List;

public class Plant {
    private PlantStats plantStat;
    private PlantType plantType;
    private int sunPer;
    private float tickFromLastSun;
    private float tickFromLastShoot;
    private int posX;
    private int posY;
    private List<PlantTag> plantTags = new ArrayList<>();
    private int damage;


    public void activatePlantFood(){}
    public void levelUp(){}
    public void takeDamage(int damage){}
    public void updateTick(int tick){}
    public void shoot(){}
    public void produceSun(){}
    public void usePlantFood(){}
}
