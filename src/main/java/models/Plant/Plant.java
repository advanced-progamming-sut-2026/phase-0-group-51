package models.Plant;

import lombok.Getter;
import lombok.Setter;
import models.games.GameState;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class Plant {
    private final int id;
    private final String name;
    private final PlantType plantType;
    private int currentHP;
    private final List<PlantUpgrade> upgrades;
    private final List<PlantTag> plantTags;
    private PlantStats plantStat;
    private int level = 1;
    private int sunPer;
    private float tickFromLastSun;
    private float tickFromLastShoot;
    private float tickFromLastAct;
    private float ticksOfPlantFood;
    private float armTicker = 0;
    private float armedTicks; // i should somehow initialize it
    private int posX;
    private int posY;

    private int damage;

    public Plant(int id, String name, PlantType plantType, PlantStats plantStat,
                 List<PlantUpgrade> upgrades, List<PlantTag> plantTags) {
        this.id        = id;
        this.name      = name;
        this.plantType = plantType;
        this.plantStat = plantStat;
        this.currentHP = plantStat.maxHp();
        this.upgrades  = upgrades;
        this.plantTags = plantTags;

    }


    public void tick(GameState gameState) {
        armTicker++;
        tickFromLastAct++;
        if(plantTags.contains(PlantTag.CHARGE) && armTicker < armedTicks){
            return;
        }
        if(isOnPlantFood()){
            plantType.onPlantFood(this, gameState);
            ticksOfPlantFood--;
        }else if(canAct()){
            plantType.onTick(this, gameState);
        }
    }

    public boolean canAct(){
        if(tickFromLastAct >= plantStat.actionInterval()){
            tickFromLastAct = 0;
            return true;
        } return  false;
    }
    public boolean isOnPlantFood(){
        if(ticksOfPlantFood > 0){
            return true;
        } return  false;
    }
    public void activatePlantFood(){}
    public void levelUp(){}
    public void takeDamage(int damage){
        this.currentHP -= damage;
    }
    public boolean isDead(){
        return currentHP <= 0;
    }
    public void addArmor(int hp){
        this.currentHP += hp;
    }
    public void updateTick(int tick){}
    public void shoot(){}
    public void produceSun(){}
    public void usePlantFood(){}
}
