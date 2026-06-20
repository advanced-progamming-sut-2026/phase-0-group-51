package models.Plant;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class Plant {
    private final int id;
    private final String name;
    private final PlantType plantType;
    private final List<PlantUpgrade> upgrades;
    private final List<PlantTag> plantTags;
    private PlantStats plantStat;
    private int level = 1;
    private int sunPer;
    private float tickFromLastSun;
    private float tickFromLastShoot;
    private int posX;
    private int posY;

    private int damage;

    public Plant(int id, String name, PlantType plantType, List<PlantUpgrade> upgrades, List<PlantTag> plantTags) {
        this.id = id;
        this.name = name;
        this.plantType = plantType;
        this.upgrades = upgrades;
        this.plantTags = plantTags;
    }




    public void activatePlantFood(){}
    public void levelUp(){}
    public void takeDamage(int damage){}
    public void updateTick(int tick){}
    public void shoot(){}
    public void produceSun(){}
    public void usePlantFood(){}
}
