package models.games;

import Data.database.NewsRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Plant.Plant;
import models.Plant.PlantFactory;
import models.User;
import models.Zombie.Zombie;
import models.Board.Board;
import models.Board.Tile;
import models.games.specialLevelConfig.TimedBattleConfig;
import models.items.Mower;
import models.quests.QuestKillSourceType;
import models.quests.QuestRunTracker;
import models.games.specialLevelConfig.SaveOurSeedsConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
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
    private final Set<Plant> protectedPlants = Collections.newSetFromMap(
            new IdentityHashMap<>()
    );
    private boolean protectedPlantLost;
    private TimedBattleConfig timedBattleConfig = TimedBattleConfig.none();
    private int timedBattleStartTick;
    private int timedBattleZombieKills;
    private int timedBattleSunProduced;
    private boolean timedBattleFailed;
    boolean mowerEnabled =true;
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
        if (checkSaveOurSeedsLoseCondition() || checkDeadlineLoseCondition()) {
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

    public void configureSaveOurSeeds(SaveOurSeedsConfig config) {
        if (config == null || !config.isConfigured()) {
            throw new IllegalArgumentException(
                    "Save Our Seeds requires at least one protected plant."
            );
        }

        protectedPlants.clear();
        protectedPlantLost = false;

        List<SaveOurSeedsConfig.ProtectedPlantPlacement> placements =
                resolveProtectedPlantPlacements(config);

        for (SaveOurSeedsConfig.ProtectedPlantPlacement placement
                : placements) {
            Tile tile = board.getTileAtUserCoordinates(
                    placement.column() - 1,
                    placement.row() - 1
            );
            if (tile == null) {
                throw new IllegalArgumentException(
                        "Protected plant coordinates are outside the board: ("
                                + placement.column() + ", "
                                + placement.row() + ")."
                );
            }
            PlantData data = PlantRegistry.getById(placement.plantId());
            if (data == null) {
                throw new IllegalArgumentException(
                        "Unknown protected plant id: " + placement.plantId()
                );
            }

            Plant plant = PlantFactory.create(data, placement.level());
            validatePlantPlacement(plant, tile);
            plant.setPosX(tile.getColumn());
            plant.setPosY(tile.getLane());
            protectedPlants.add(plant);
            tile.setPlant(plant);
            plant.getPlantType().onPlanted(plant, this);
            activateEntryPlantFood(plant);

            if (plant.isDead() || plant.isMarkedForRemoval()
                    || board.getTileForPlant(plant) == null) {
                protectedPlants.remove(plant);
                board.removePlant(plant);
                throw new IllegalArgumentException(
                        data.name() + " cannot be used as a protected starting plant."
                );
            }
        }
    }

    private List<SaveOurSeedsConfig.ProtectedPlantPlacement>
    resolveProtectedPlantPlacements(SaveOurSeedsConfig config) {
        if (!config.usesRandomPlacement()) {
            return config.protectedPlants();
        }

        SaveOurSeedsConfig.RandomPlacementRule rule =
                config.randomPlacementRule();
        int maximumColumn = board.getColumnCount()
                - rule.excludedRightColumns();
        if (rule.minimumColumn() > maximumColumn) {
            throw new IllegalArgumentException(
                    "No columns remain for random protected-plant placement."
            );
        }
        if (rule.distinctRows() && rule.count() > board.getLaneCount()) {
            throw new IllegalArgumentException(
                    "Not enough rows for distinct protected-plant placement."
            );
        }

        List<Tile> candidates = new ArrayList<>();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = rule.minimumColumn() - 1;
                    column < maximumColumn;
                    column++) {
                Tile tile = board.getTile(lane, column);
                if (tile != null && tile.isOccupiable()) {
                    candidates.add(tile);
                }
            }
        }
        Collections.shuffle(candidates, new Random());

        List<SaveOurSeedsConfig.ProtectedPlantPlacement> placements =
                new ArrayList<>();
        Set<Integer> usedRows = new HashSet<>();
        for (Tile tile : candidates) {
            if (rule.distinctRows() && !usedRows.add(tile.getLane())) {
                continue;
            }
            placements.add(new SaveOurSeedsConfig.ProtectedPlantPlacement(
                    rule.plantId(),
                    tile.getColumn() + 1,
                    tile.getLane() + 1,
                    rule.level()
            ));
            if (placements.size() == rule.count()) {
                break;
            }
        }

        if (placements.size() < rule.count()) {
            throw new IllegalArgumentException(
                    "Not enough eligible tiles for random protected plants."
            );
        }
        return List.copyOf(placements);
    }

    public boolean isSaveOurSeedsActive() {
        return currentLevel != null
                && currentLevel.type() == LevelType.SAVE_OUR_SEEDS;
    }

    public boolean isProtectedPlant(Plant plant) {
        return plant != null && protectedPlants.contains(plant);
    }

    public boolean isProtectedRow(int zeroBasedLane) {
        for (Plant plant : protectedPlants) {
            if (plant.getPosY() == zeroBasedLane) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLostProtectedPlant() {
        return protectedPlantLost;
    }

    public boolean checkSaveOurSeedsLoseCondition() {
        if (!isSaveOurSeedsActive()) {
            return false;
        }
        if (!protectedPlantLost) {
            for (Plant plant : protectedPlants) {
                if (plant.isDead()
                        || plant.isMarkedForRemoval()
                        || board.getTileForPlant(plant) == null) {
                    onPlantDestroyed(plant);
                    break;
                }
            }
        }
        return protectedPlantLost;
    }

    public void onPlantDestroyed(Plant plant) {
        if (!isProtectedPlant(plant) || protectedPlantLost) {
            return;
        }
        protectedPlantLost = true;
        logEvent(
                "SAVE OUR SEEDS FAILED: protected " + plant.getName()
                        + " at (" + (plant.getPosX() + 1) + ", "
                        + (plant.getPosY() + 1)
                        + ") was destroyed. You lose immediately.\n"
        );
    }

    public String getSaveOurSeedsStatus() {
        if (!isSaveOurSeedsActive()) {
            return "";
        }
        List<Plant> ordered = protectedPlants.stream()
                .sorted(
                        Comparator.comparingInt(Plant::getPosY)
                                .thenComparingInt(Plant::getPosX)
                )
                .toList();
        long surviving = ordered.stream()
                .filter(plant -> !plant.isDead()
                        && !plant.isMarkedForRemoval()
                        && board.getTileForPlant(plant) != null)
                .count();
        String locations = ordered.stream()
                .map(plant -> plant.getName() + " ("
                        + (plant.getPosX() + 1) + ", "
                        + (plant.getPosY() + 1) + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        return "Save Our Seeds: " + surviving + "/" + ordered.size()
                + " protected plants alive. Protect: " + locations + ".";
    }

    public void configureTimedBattle(TimedBattleConfig config) {
        if (config == null || !config.isEnabled()) {
            throw new IllegalArgumentException(
                    "Timed Battle requires at least one objective."
            );
        }
        timedBattleConfig = config;
        timedBattleStartTick = tickCounter;
        timedBattleZombieKills = 0;
        timedBattleSunProduced = 0;
        timedBattleFailed = false;
        logEvent("Timed Battle started. " + timedBattleStatusLine());
    }

    public boolean isTimedBattleActive() {
        return currentLevel != null
                && currentLevel.type() == LevelType.TIMED_BATTLE
                && timedBattleConfig.isEnabled();
    }

    public int getTimedBattleRemainingTicks() {
        if (!isTimedBattleActive()) {
            return 0;
        }
        int durationTicks = timedBattleConfig.durationSeconds()
                * ticksPerSecond;
        int elapsedTicks = tickCounter - timedBattleStartTick;
        return Math.max(0, durationTicks - elapsedTicks);
    }

    public double getTimedBattleRemainingSeconds() {
        return getTimedBattleRemainingTicks() / (double) ticksPerSecond;
    }

    public boolean isTimedBattleKillObjectiveComplete() {
        return !timedBattleConfig.requiresZombieKills()
                || timedBattleZombieKills
                >= timedBattleConfig.zombieKillTarget();
    }

    public boolean isTimedBattleSunObjectiveComplete() {
        return !timedBattleConfig.requiresSunProduction()
                || timedBattleSunProduced
                >= timedBattleConfig.sunProductionTarget();
    }

    public boolean isTimedBattleComplete() {
        return isTimedBattleActive()
                && isTimedBattleKillObjectiveComplete()
                && isTimedBattleSunObjectiveComplete();
    }

    public boolean checkTimedBattleLoseCondition() {
        if (!isTimedBattleActive() || isTimedBattleComplete()) {
            return false;
        }
        if (timedBattleFailed) {
            return true;
        }
        if (getTimedBattleRemainingTicks() > 0) {
            return false;
        }
        timedBattleFailed = true;
        logEvent(
                "Timed Battle failed: time is up. "
                        + timedBattleStatusLine()
        );
        return true;
    }

    public void recordTimedBattleZombieKill(
            Zombie zombie,
            QuestKillSourceType sourceType
    ) {
        if (!isTimedBattleActive()
                || !timedBattleConfig.requiresZombieKills()
                || zombie == null
                || zombie.isHypnotized()
                || !zombie.isQuestEligible()
                || sourceType == QuestKillSourceType.CHEAT
                || isTimedBattleKillObjectiveComplete()) {
            return;
        }
        timedBattleZombieKills = Math.min(
                timedBattleConfig.zombieKillTarget(),
                timedBattleZombieKills + 1
        );
        logTimedBattleProgress();
    }

    public void recordTimedBattleSunProduced(int amount) {
        if (!isTimedBattleActive()
                || !timedBattleConfig.requiresSunProduction()
                || amount <= 0
                || isTimedBattleSunObjectiveComplete()) {
            return;
        }
        timedBattleSunProduced = Math.min(
                timedBattleConfig.sunProductionTarget(),
                timedBattleSunProduced + amount
        );
        logTimedBattleProgress();
    }

    public String timedBattleStatusLine() {
        if (!isTimedBattleActive()) {
            return "";
        }
        StringBuilder status = new StringBuilder("Timed Battle: ");
        if (timedBattleConfig.requiresZombieKills()) {
            status.append("Kills ")
                    .append(timedBattleZombieKills)
                    .append('/')
                    .append(timedBattleConfig.zombieKillTarget());
        }
        if (timedBattleConfig.requiresZombieKills()
                && timedBattleConfig.requiresSunProduction()) {
            status.append(" | ");
        }
        if (timedBattleConfig.requiresSunProduction()) {
            status.append("Plant-produced sun ")
                    .append(timedBattleSunProduced)
                    .append('/')
                    .append(timedBattleConfig.sunProductionTarget());
        }
        status.append(" | Time left: ")
                .append(String.format(
                        Locale.US,
                        "%.1f",
                        getTimedBattleRemainingSeconds()
                ))
                .append(" seconds.\n");
        return status.toString();
    }

    private void logTimedBattleProgress() {
        logEvent(timedBattleStatusLine());
        if (isTimedBattleComplete()) {
            logEvent(
                    "Both Timed Battle objectives completed with "
                            + String.format(
                                    Locale.US,
                                    "%.1f",
                                    getTimedBattleRemainingSeconds()
                            )
                            + " seconds remaining.\n"
            );
        }
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
        int cost = plant.getPlantStat().cost();
        if (sun < cost) throw new IllegalStateException("Not enough sun");
        decreaseSunBalance(cost);
        plant.setRefundableSunCost(cost);
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
        existing.setRefundableSunCost(
                existing.getRefundableSunCost() + cost
        );
        questTracker.recordPlantPlaced(addition);
        addition.getPlantType().onStacked(existing, this);
    }

    public void plantPlantWithoutSunCost(Plant plant, Tile tile) {
        validatePlantPlacement(plant, tile);
        plant.setRefundableSunCost(0);
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
        lilyPad.setRefundableSunCost(cost);
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
        plant.setRefundableSunCost(cost);
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
        pumpkin.setRefundableSunCost(cost);
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
        plant.setRefundableSunCost(cost);
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
    public int pluckPlant(Plant plant, Tile tile){
        if (plant == null || tile == null || tile.getPlant() != plant) {
            throw new IllegalArgumentException("Plant is not on this tile");
        }
        if (isProtectedPlant(plant)) {
            throw new IllegalStateException(
                    "Protected plants cannot be plucked in Save Our Seeds"
            );
        }
        int refund = Math.max(0, plant.getRefundableSunCost());
        tile.removeSpecificPlant(plant);
        plant.setMarkedForRemoval(true);
        increaseSunBalance(refund);
        plant.setRefundableSunCost(0);
        return refund;
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

    public Zombie findNearestHypnotizedZombieInRange(
            Zombie self,
            int lane,
            float x,
            int range
    ) {
        return findNearestHypnotizedZombieInRange(
                self,
                lane,
                x,
                (float) range
        );
    }

    public Zombie findNearestHypnotizedZombieInRange(
            Zombie self,
            int lane,
            float x,
            float range
    ) {
        Zombie nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Zombie other : zombiesInTheGame) {
            if (other == self
                    || !other.isHypnotized()
                    || other.isDead()
                    || other.getLane() != lane) {
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
