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
    private static final float PLANT_FOOD_DURATION_TICKS = 10;
    private final List<PlantUpgrade> upgrades;
    private final List<PlantTag> plantTags;
    private PlantStats plantStat;
    private int level = 1;
    private int sunPer;
    private float tickFromLastAct;
    private float ticksOfPlantFood;
    // "can't act right now" timer.
    // covers arm delay (Potato Mine)
    // (Chomper), charge-up (Citron, Bowling Bulb), etc.
    private float disabledTicksRemaining;
    // plants with limited life span:
    private float lifespanRemaining;
    //for stack plants:
    private int stackCount = 1;
    // for wramp-up plants
    private int growthStage = 0;
    // removing themselves after acting
    private boolean markedForRemoval = false;
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
        if (disabledTicksRemaining > 0) {
            disabledTicksRemaining--;
            return;
        }
        if (lifespanRemaining > 0) {
            lifespanRemaining--;
            if (lifespanRemaining <= 0) {
                markedForRemoval = true;
                return;
            }
        }
        tickFromLastAct++;

        if (isOnPlantFood()) {
            plantType.onFoodTick(this, gameState);
            ticksOfPlantFood--;
            if (markedForRemoval) return;
        } else if (canAct()) {
            plantType.onTick(this, gameState);
        }
    }

    public boolean canAct() {
        if (tickFromLastAct >= plantStat.actionInterval()) {
            tickFromLastAct = 0;
            return true;
        }
        return false;
    }

    public boolean isOnPlantFood() {
        return ticksOfPlantFood > 0;
    }

    public void feed(GameState gameState) {
        plantType.onFeed(this, gameState);
        ticksOfPlantFood = PLANT_FOOD_DURATION_TICKS;
    }
    public void disableFor(float ticks) {
        this.disabledTicksRemaining = ticks;
    }

    public boolean isDisabled() {
        return disabledTicksRemaining > 0;
    }
    public void activatePlantFood(){}
    public void levelUp() {
        if (level >= upgrades.size() + 1) return;
        int oldMaxHp = plantStat.maxHp();
        plantStat = upgrades.get(level - 1).apply(plantStat);
        int hpGain = plantStat.maxHp() - oldMaxHp;
        if (hpGain > 0) currentHP += hpGain;
        level++;
    }
    public void takeDamage(int damage){
        this.currentHP -= damage;
    }
    public boolean isDead(){
        return currentHP <= 0;
    }
    public void addArmor(int hp){
        this.currentHP += hp;
    }
    private void die(GameState gameState) {
        if (markedForRemoval) return;
        markedForRemoval = true;
        plantType.onDeath(this, gameState);
        gameState.getBoard().removePlant(posY, posX);
    }
    public int getDamage() {
        return plantStat.damage();
    }
    public void updateTick(int tick){}
    public void shoot(){}
    public void produceSun(){}
    public void usePlantFood(){}
}
