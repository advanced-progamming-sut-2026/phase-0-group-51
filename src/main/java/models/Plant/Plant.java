package models.Plant;

import lombok.Getter;
import lombok.Setter;
import models.games.GameState;
import models.projectile.ElementType;

import java.util.List;
@Getter
@Setter
public class Plant {
    private final int id;
    private final String name;
    private final PlantType plantType;
    private int currentHP;
    public static final int MAX_FROST_LEVEL = 3;
    public static final int ICE_MAX_HEALTH = 600;
    private final List<PlantUpgrade> upgrades;
    private final List<PlantTag> plantTags;
    private PlantStats plantStat;
    private int level = 1;
    private int sunPer;
    private boolean pendingSun;
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
    private int ageTicks = 0;
    // removing themselves after acting
    private boolean markedForRemoval = false;
    private int posX;
    private int posY;
    private int frostLevel;
    private int iceHealth;

    // Octopus Zombie binding
    private static final int OCTOPUS_HP = 400;
    private int octopusHP = 0;

    // Wizard sheep transform
    private boolean transformed = false;

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
        if (isFrozenByIce() || hasOctopus() || isTransformed()) {
            return;
        }
        ageTicks++;
        plantType.onEveryTick(this, gameState);
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
        } else if (canAct(gameState)) {
            plantType.onTick(this, gameState);
        }
    }

    public boolean canAct(GameState gameState) {
        double requiredTicks = plantStat.actionInterval() * gameState.getTicksPerSecond();
        if (tickFromLastAct >= requiredTicks) {
            tickFromLastAct = 0;
            return true;
        }
        return false;
    }

    public boolean isOnPlantFood() {
        return ticksOfPlantFood > 0;
    }

    public void feed(GameState gameState) {
        if (markedForRemoval || isDead() || isFrozenByIce() || hasOctopus() || isOnPlantFood()) {
            return;
        }
        plantType.onFeed(this, gameState);
        ticksOfPlantFood = Math.max(0, plantType.plantFoodDurationTicks(this, gameState));
    }
    public void setLifespanSeconds(double seconds, GameState state) {
        lifespanRemaining = Math.max(0, (float) (seconds * state.getTicksPerSecond()));
    }

    public void resetLifespanSeconds(double seconds, GameState state) {
        setLifespanSeconds(seconds, state);
    }

    public void forceReady(GameState state) {
        tickFromLastAct = (float) (plantStat.actionInterval() * state.getTicksPerSecond());
    }

    public void healToFull() {
        currentHP = plantStat.maxHp();
    }

    public void incrementStackCount(int maximum) {
        stackCount = Math.min(maximum, stackCount + 1);
    }

    public boolean hasTemporaryArmor() {
        return currentHP > plantStat.maxHp();
    }

    public void disableFor(float ticks) {
        this.disabledTicksRemaining = ticks;
    }

    public boolean isDisabled() {
        return disabledTicksRemaining > 0;
    }
    public void activatePlantFood(){}

    public boolean hasTag(PlantTag tag) {
        return plantTags.contains(tag);
    }

    public boolean isFrozenByIce() {
        return frostLevel >= MAX_FROST_LEVEL && iceHealth > 0;
    }

    public void addFrostLevel(GameState state, String source) {
        if (markedForRemoval || hasTag(PlantTag.FIRE) || frostLevel >= MAX_FROST_LEVEL) {
            return;
        }
        frostLevel++;
        state.logEvent(name + " frost level increased to " + frostLevel + " from " + source + ".\n");
        if (frostLevel == MAX_FROST_LEVEL) {
            iceHealth = ICE_MAX_HEALTH;
            state.logEvent(name + " at (" + (posX + 1) + ", " + (posY + 1) + ") is completely frozen now.\n");
        }
    }

    public boolean damageIce(int damage, ElementType element, GameState state) {
        if (!isFrozenByIce()) {
            return false;
        }
        if (element == ElementType.FIRE) {
            iceHealth = 0;
        } else {
            iceHealth = Math.max(0, iceHealth - Math.max(0, damage));
        }
        if (iceHealth == 0) {
            melting(state);
        }
        return true;
    }

    public void meltIce(int damage, GameState state) {
        if (!isFrozenByIce()) {
            return;
        }
        iceHealth = Math.max(0, iceHealth - Math.max(0, damage));
        if (iceHealth == 0) {
            melting(state);
        }
    }

    private void melting(GameState state) {
        frostLevel = 0;
        state.logEvent(name + " at (" + (posX + 1) + ", " + (posY + 1) + ") has melted.\n");
    }
    public void levelUp() {
        if (level >= upgrades.size() + 1) return;
        int oldMaxHp = plantStat.maxHp();
        plantStat = upgrades.get(level - 1).apply(plantStat);
        int hpGain = plantStat.maxHp() - oldMaxHp;
        if (hpGain > 0) currentHP += hpGain;
        level++;
    }
    public void takeDamage(int damage){
        this.currentHP = Math.max(0, this.currentHP - Math.max(0, damage));
    }
    public void takeDamage(int damage, GameState gameState){
        if (markedForRemoval || damage <= 0) return;
        if (damageIce(damage, ElementType.NORMAL, gameState)) {
            return;
        }
        if (hasOctopus()) {
            octopusHP = Math.max(0, octopusHP - damage);
            return;
        }
        this.currentHP = Math.max(0, this.currentHP - damage);
        if (this.currentHP <= 0) die(gameState);
    }
    public boolean isDead(){
        return currentHP <= 0;
    }
    public void addArmor(int hp){
        this.currentHP += hp;
    }
    public void die(GameState gameState) {
        if (markedForRemoval) return;
        markedForRemoval = true;
        gameState.getQuestTracker().recordPlantLost(this);
        plantType.onDeath(this, gameState);
        gameState.logEvent("Plant " + name + " at (" + (posX + 1) + ", " + (posY + 1) + ") is destroyed.\n");
        gameState.getBoard().removePlant(this);
    }
    public int getDamage() {
        return plantStat.damage();
    }
    public void updateTick(int tick){}
    public void shoot(){}
    public void produceSun(){}
    public void usePlantFood(){}


    public boolean hasOctopus() {
        return octopusHP > 0;
    }

    public void attachOctopus() {
        this.octopusHP = OCTOPUS_HP;
    }
}
