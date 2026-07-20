package models.games;

import Data.database.NewsRepository;
import Data.loader.PlantData;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Plant.Plant;
import models.User;
import models.Zombie.Zombie;
import models.Board.Board;
import models.Board.Tile;
import models.items.Mower;
import models.quests.QuestKillSourceType;
import models.quests.QuestRunTracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Setter
@Getter
public class GameState {
    private static final int MAX_PLANT_FOOD = 3;
    private final int ticksPerSecond = 10;
    private final Board board;
    private Set<Zombie> zombiesInTheGame = new HashSet<>();
    private final Map<Integer, Integer> cooldownUntilTick = new HashMap<>();
    private int sun;
    private int plantFoodCount;
    private boolean isFinished;
    private boolean isWon;
    private final ChapterTheme chapterTheme;
    private Level currentLevel;
    private int tickCounter = 0;
    private ZombieWaveManager zombieWaveManager;
    private final Mower[] lawnMowers;
    private final QuestRunTracker questTracker = new QuestRunTracker();
    private Consumer<String> eventLogger;
    private int deadlineColumn = -1;
    private boolean deadlineBreached;
    boolean mowerEnabled =true;
    private int plantLossLimit = -1;
    private boolean plantLossLimitReached;
    public void logEvent(String message) {
        if (eventLogger != null) {
            eventLogger.accept(message);
        }
    }
    public GameState(Board board, ChapterTheme chapterTheme) {
        this(board, chapterTheme, true);
    }
    public GameState(Board board, ChapterTheme chapterTheme,boolean mowerEnabled) {
        this.board = board;
        this.board.setZombie(this.zombiesInTheGame);
        this.chapterTheme = chapterTheme;
        this.mowerEnabled = mowerEnabled;
        this.lawnMowers = new Mower[board.getLaneCount()];
        for (int i = 0; i < lawnMowers.length; i++) {
            lawnMowers[i] = new Mower(i,this);
        }
    }
    public boolean checkLoseCondition() {
        if (checkDeadlineLoseCondition() || checkPlantLossLoseCondition()) {
            return true;
        }
        if (!mowerEnabled) {
            return false;
        }

        for (Zombie zombie : zombiesInTheGame) {
            if (!zombie.isDead() && zombie.getX() < 0) {
                int lane = zombie.getLane();
                Mower mower = lawnMowers[lane];
                if (mower.isActivated() || mower.isDestroyed()) {
                    logEvent("The zombie ate your brain; LOSER!!!\n");
                    return true;
                }
            }
        }
        return false;
    }

    public void configureDeadline(int userFacingColumn) {
        if (userFacingColumn < 1 || userFacingColumn > board.getColumnCount()) {
            throw new IllegalArgumentException(
                    "Deadline column must be inside the board."
            );
        }
        deadlineColumn = userFacingColumn;
        deadlineBreached = false;
        logEvent(
                "Deadline is haunting you : zombies must not cross the line before column "
                        + deadlineColumn + ".\n"
        );
    }

    public boolean hasDeadline() {
        return deadlineColumn > 0;
    }

    public boolean checkDeadlineLoseCondition() {
        if (!hasDeadline()) {
            return false;
        }
        if (deadlineBreached) {
            return true;
        }

        double deadlineX = deadlineColumn - 1.0;
        for (Zombie zombie : zombiesInTheGame) {
            if (zombie.isDead() || zombie.isHypnotized()) {
                continue;
            }
            if (zombie.getX() < deadlineX) {
                deadlineBreached = true;
                logEvent(
                        zombie.getAlias() + " crossed the Dead Line before column "
                                + deadlineColumn + " in row "
                                + (zombie.getLane() + 1) + "; YOU ARE PASSED YOUR DEADLINE LOSER!!!\n"
                );
                return true;
            }
        }
        return false;
    }
    private void killAllZombiesInLane(int lane) {
        for (Zombie z : zombiesInTheGame) {
            if (z.getLane() == lane) {
                z.killInstantly(this, QuestKillSourceType.MOWER);
            }
        }
    }
    public void configurePlantLossLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Plant-loss limit must be positive.");
        }
        plantLossLimit = limit;
        plantLossLimitReached = false;
        logEvent("Plants You Love started. You lose when "
                        + plantLossLimit + " plants are destroyed.\n");
    }

    public boolean hasPlantLossLimit() {
        return plantLossLimit > 0;
    }

    public boolean checkPlantLossLoseCondition() {
        if (!hasPlantLossLimit()) {
            return false;
        }

        if (plantLossLimitReached) {
            return true;
        }
        int plantsLost = questTracker.getPlantsLost();
        if (plantsLost < plantLossLimit) {
            return false;
        }
        plantLossLimitReached = true;
        logEvent(
                "You lost " + plantsLost + " plants out of the allowed " + plantLossLimit + "; LOSER!!!\n"
        );

        return true;
    }
    public void addTick(int tick){
        this.tickCounter += tick;
    }
    public void spawnWave(){}
    public void spawnBoss(){}
    public void increaseSunBalance(int amount) {
        this.sun += amount;
    }

    public void decreaseSunBalance(int amount) {
        this.sun = Math.max(0, this.sun - amount);
    }

    public boolean addPlantFood() {
        if (plantFoodCount >= MAX_PLANT_FOOD) {
            return false;
        }
        plantFoodCount++;
        return true;
    }

    public boolean consumePlantFood() {
        if (plantFoodCount <= 0) {
            return false;
        }
        plantFoodCount--;
        return true;
    }

    public void clearPlantCooldowns() {
        cooldownUntilTick.clear();
    }

    public void resetPlantCooldown(int plantId) {
        cooldownUntilTick.remove(plantId);
    }

    public void plantPlant(Plant plant, Tile tile){
        if (plant == null || tile == null) throw new IllegalArgumentException("Plant and tile are required");
        if (!tile.isOccupiable()) throw new IllegalStateException("Tile is not occupiable");
        if (sun < plant.getPlantStat().cost()) throw new IllegalStateException("Not enough sun");
        decreaseSunBalance(plant.getPlantStat().cost());
        plant.setPosX(tile.getColumn());
        plant.setPosY(tile.getLane());
        tile.setPlant(plant);
        questTracker.recordPlantPlaced(plant);
        plant.getPlantType().onPlanted(plant, this);
        activateEntryPlantFood(plant);
    }
    public void stackPlant(Plant addition, Plant existing) {
        if (addition == null || existing == null) {
            throw new IllegalArgumentException("Both plants are required");
        }
        if (!addition.getPlantType().canStackOn(existing)) {
            throw new IllegalStateException("This plant cannot be stacked here");
        }
        int cost = addition.getPlantStat().cost();
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun");
        }
        decreaseSunBalance(cost);
        questTracker.recordPlantPlaced(addition);
        addition.getPlantType().onStacked(existing, this);
    }

    public void plantPlantWithoutSunCost(Plant plant, Tile tile) {
        validatePlantPlacement(plant, tile);
        placePlantOnTile(plant, tile);
    }

    public void plantLilyPad(Plant lilyPad, Tile tile) {
        if (lilyPad == null || tile == null) {
            throw new IllegalArgumentException("Plant and tile are required");
        }
        if (!tile.isWater()) {
            throw new IllegalStateException("Lily Pad can only be planted on water");
        }
        if (tile.hasPlant() || tile.hasGrave() || tile.isIceBlocked()
                || tile.isCrater() || tile.getIceFloorDirection() != null) {
            throw new IllegalStateException("Tile is not available for a Lily Pad");
        }
        int cost = lilyPad.getPlantStat().cost();
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun");
        }
        decreaseSunBalance(cost);
        lilyPad.setPosX(tile.getColumn());
        lilyPad.setPosY(tile.getLane());
        tile.setLilyPadPlant(lilyPad);
        questTracker.recordPlantPlaced(lilyPad);
        lilyPad.getPlantType().onPlanted(lilyPad, this);
        activateEntryPlantFood(lilyPad);
    }

    public void plantOnLilyPad(Plant plant, Tile tile) {
        if (plant == null || tile == null) {
            throw new IllegalArgumentException("Plant and tile are required");
        }
        if (!tile.isWater() || !tile.hasLilyPad()) {
            throw new IllegalStateException("This plant requires a Lily Pad on water");
        }
        if (tile.hasTopPlant()) {
            throw new IllegalStateException("The Lily Pad already has a plant");
        }
        int cost = plant.getPlantStat().cost();
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun");
        }
        decreaseSunBalance(cost);
        plant.setPosX(tile.getColumn());
        plant.setPosY(tile.getLane());
        tile.setPlant(plant);
        questTracker.recordPlantPlaced(plant);
        plant.getPlantType().onPlanted(plant, this);
        activateEntryPlantFood(plant);
    }

    public void plantPumpkin(Plant pumpkin, Tile tile) {
        if (pumpkin == null || tile == null) {
            throw new IllegalArgumentException("Plant and tile are required");
        }
        if (!tile.hasPlant() || tile.hasPumpkin()) {
            throw new IllegalStateException(
                    "Pumpkin requires a plant without another Pumpkin"
            );
        }
        int cost = pumpkin.getPlantStat().cost();
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun");
        }
        decreaseSunBalance(cost);
        pumpkin.setPosX(tile.getColumn());
        pumpkin.setPosY(tile.getLane());
        tile.setPumpkinPlant(pumpkin);
        questTracker.recordPlantPlaced(pumpkin);
        pumpkin.getPlantType().onPlanted(pumpkin, this);
        activateEntryPlantFood(pumpkin);
    }

    public void useInstantPlantOnTile(Plant plant, Tile tile) {
        if (plant == null || tile == null) {
            throw new IllegalArgumentException("Plant and tile are required");
        }
        int cost = plant.getPlantStat().cost();
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun");
        }
        decreaseSunBalance(cost);
        plant.setPosX(tile.getColumn());
        plant.setPosY(tile.getLane());
        questTracker.recordPlantPlaced(plant);
        plant.getPlantType().onPlanted(plant, this);
        activateEntryPlantFood(plant);
    }

    public void plantOnGrave(Plant plant, Tile tile) {
        if (plant == null || tile == null || !tile.hasGrave()) {
            throw new IllegalStateException(
                    "Grave Buster must be planted on a grave"
            );
        }
        if (tile.hasPlant()) {
            throw new IllegalStateException("The grave tile already has a plant");
        }
        int cost = plant.getPlantStat().cost();
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun");
        }
        decreaseSunBalance(cost);
        plant.setPosX(tile.getColumn());
        plant.setPosY(tile.getLane());
        tile.setPlant(plant);
        questTracker.recordPlantPlaced(plant);
        plant.getPlantType().onPlanted(plant, this);
        activateEntryPlantFood(plant);
    }
    private void validatePlantPlacement(Plant plant, Tile tile) {

       if (plant == null || tile == null) {
                throw new IllegalArgumentException("Plant and tile are required");
       }

       if (!tile.isOccupiable()) {
                throw new IllegalStateException("Tile is not occupiable");
       }

    }

    private void placePlantOnTile(Plant plant, Tile tile) {
        plant.setPosX(tile.getColumn());
        plant.setPosY(tile.getLane());
        tile.setPlant(plant);
        questTracker.recordPlantPlaced(plant);
        plant.getPlantType().onPlanted(plant, this);
        activateEntryPlantFood(plant);
    }

    private void activateEntryPlantFood(Plant plant) {
        if (plant.isAutoPlantFoodOnEntry()
                && !plant.isMarkedForRemoval()
                && !plant.isDead()) {
            plant.feed(this);
        }
    }
    public void pluckPlant(Plant plant, Tile tile){
        if (plant == null || tile == null || tile.getPlant() != plant) {
            throw new IllegalArgumentException("Plant is not on this tile");
        }
        tile.removePlant();
        plant.setMarkedForRemoval(true);
    }


    public void addZombie(Zombie zombie) {
        if (zombie == null || !zombiesInTheGame.add(zombie)) {
            return;
        }
        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            new NewsRepository().discoverZombie(user.getId(), zombie.getAlias());
        }
    }
    public void removeZombie(Zombie zombie) {
        zombiesInTheGame.remove(zombie);
    }
    public Zombie findNearestHostileZombieInRange(Zombie self, int lane, float x, float range) {
        Zombie nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Zombie other : zombiesInTheGame) {
            if (other == self || other.isHypnotized() || other.isDead()) {
                continue;
            }
            if (other.getLane() != lane) {
                continue;
            }
            float distance = Math.abs(other.getX() - x);
            if (distance <= range && distance < nearestDistance) {
                nearest = other;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public Zombie findNearestHypnotizedZombieInRange(Zombie self, int lane, float x, int range) {
        Zombie nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Zombie other : zombiesInTheGame) {
            if (other == self || !other.isHypnotized() || other.isDead()) continue;
            if (other.getLane() != lane) continue;
            float distance = Math.abs(other.getX() - x);
            if (distance <= range && distance < nearestDistance) {
                nearest = other;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
    public void tickMowers() {
        for (Mower mower : lawnMowers) {
            mower.update(this.board);
        }
    }
    public void swapRandomZombieLanes(int swapCount) {
        for (Zombie z : getZombiesInTheGame()) {
            if (z.isDead()) continue;
            int current = z.getLane();
            int target = getRandomNeighborLane(current);
            if (target != current) {
                z.setLane(target);
            }
        }
    }

    private int getRandomNeighborLane(int lane) {
        if (lane == 0) return 1;
        if (lane == 4) return 3;
        return Math.random() < 0.5 ? lane - 1 : lane + 1;
    }
    public int getPlantCooldownEnd(int plantId) {
        return cooldownUntilTick.getOrDefault(plantId, 0);
    }
    public void startPlantCooldown(PlantData plant) {
        int rechargeTicks = (int) Math.ceil(plant.recharge() * ticksPerSecond);
        cooldownUntilTick.put(plant.id(), tickCounter + rechargeTicks);
    }

    public void startPlantCooldown(Plant plant) {
        int rechargeTicks = (int) Math.ceil(plant.getPlantStat().recharge() * ticksPerSecond);
        cooldownUntilTick.put(plant.getId(), tickCounter + rechargeTicks);
    }
}
