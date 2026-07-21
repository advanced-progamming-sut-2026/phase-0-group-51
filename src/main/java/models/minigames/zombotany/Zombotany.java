package models.minigames.zombotany;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import lombok.Getter;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantFactory;
import models.Zombie.Behavior.Zombotany.JalapenoZombieBehavior;
import models.Zombie.Behavior.Zombotany.PeashooterZombieBehavior;
import models.Zombie.Behavior.Zombotany.SquashZombieBehavior;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
@Getter
public class Zombotany extends Game {

    public static final int START_SUN = 150;

    private static final int TICKS_PER_SECOND = 10;
    private static final int FIRST_WAVE_DELAY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int WAVE_INTERVAL_TICKS = 15 * TICKS_PER_SECOND;
    private static final int SKY_SUN_INTERVAL_TICKS = 10 * TICKS_PER_SECOND;
    private static final int SKY_SUN_AMOUNT = 25;

    public static final int JALAPENO_FUSE_TICKS = 10 * TICKS_PER_SECOND;

    private final int stageNumber;
    private final Random random;
    private final List<Zombie> templates = new ArrayList<>();

    private int totalWaves;
    private float currentWaveBudget;
    private int wavesSent;
    private int nextWaveTick;
    private int nextSkySunTick;
    private boolean zombieReachedHouse;

    public Zombotany(int stageNumber) {
        this(stageNumber, new Random());
    }

    public Zombotany(int stageNumber, Random random) {
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException("Zombotany stage must be 1, 2, or 3.");
        }
        this.stageNumber = stageNumber;
        this.random = Objects.requireNonNull(random, "Random cannot be null.");
    }

    @Override
    public void loadLevel() {
        if (PlantRegistry.getAll().isEmpty()) {
            throw new IllegalStateException("PlantRegistry is empty.");
        }
        Board board = new Board();
        GameState state = new GameState(board, ChapterTheme.MINIGAME, true); // mowers enabled, like a normal level
        state.setSun(START_SUN);
        setGameState(state);
        setSkySunSpawner(null);
        templates.clear();
        buildZombotanyTemplates();
        totalWaves = 2 + stageNumber;
        currentWaveBudget = 0f;
        wavesSent = 0;
        zombieReachedHouse = false;
        nextWaveTick = FIRST_WAVE_DELAY_TICKS;
        nextSkySunTick = SKY_SUN_INTERVAL_TICKS;
    }

    private void buildZombotanyTemplates() {
        Zombie peashooter = new Zombie("ZombotanyPeashooter", 190f, 0.185f, 100f, 100f, 1000);
        peashooter.addBehavior(new PeashooterZombieBehavior(
            15,
            9,
            20));
        templates.add(peashooter);
        Zombie wallnut = new Zombie("ZombotanyWallnut", 4000f, 0.185f, 100f, 150f, 600);
        templates.add(wallnut);
        Zombie jalapeno = new Zombie("ZombotanyJalapeno", 300f, 0.185f, 100f, 150f, 500);
        jalapeno.addBehavior(new JalapenoZombieBehavior(JALAPENO_FUSE_TICKS));
        templates.add(jalapeno);

        Zombie squash = new Zombie("ZombotanySquash", 240f, 0.6f, 100f, 125f, 500);
        squash.addBehavior(new SquashZombieBehavior());
        templates.add(squash);
    }

    @Override
    public void start() {
    }

    @Override
    public void onTick() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) {
            return;
        }
        state.addTick(1);
        state.getBoard().tickPlants(state);
        state.getBoard().tickProjectiles(state);
        for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
            zombie.onTick(state);
        }
        state.getBoard().tickLoots(state);
        dropSkySun();
        maybeStartWave();
        state.tickMowers();
        checkZombiesReachedHouse();
        endState();
    }

    @Override
    public void forward(int requestedTicks) {
        if (requestedTicks < 0) {
            throw new IllegalArgumentException("Tick count cannot be negative.");
        }
        for (int i = 0; i < requestedTicks && !getGameState().isFinished(); i++) {
            onTick();
        }
    }
    public Plant placePlant(String plantName, int x, int y) {
        ensureRunning();
        PlantData data = resolvePlant(plantName);
        if (data == null) {
            throw new IllegalArgumentException(
                "Plant " + plantName + " is not available in stage " + stageNumber
                    + ". Use 'show available plants' to see this stage's roster.");
        }
        GameState state = getGameState();
        Board board = state.getBoard();
        if (y < 1 || y > board.getLaneCount() || x < 1 || x > board.getColumnCount()) {
            throw new IllegalArgumentException("Coordinates are outside the map.");
        }
        if (data.cost() > state.getSun()) {
            throw new IllegalStateException(
                "Not enough sun to plant " + data.name() + " (costs " + data.cost()
                    + ", you have " + state.getSun() + ").");
        }
        Tile tile = board.getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null || !tile.isOccupiable() || tile.hasPlant()) {
            throw new IllegalArgumentException("Tile (" + x + ", " + y + ") is not free.");
        }
        Plant plant = PlantFactory.create(data.name());
        plant.setPosX(x - 1);
        plant.setPosY(y - 1);
        tile.setPlant(plant);
        plant.getPlantType().onPlanted(plant, state);
        state.setSun(state.getSun() - data.cost());
        state.logEvent("Plant " + data.name() + " placed at (" + x + ", " + y
            + ") for " + data.cost() + " sun.\n");
        endState();
        return plant;
    }

    public List<PlantData> getAvailablePlants() {
        return PlantRegistry.getAll().stream()
            .filter(data -> allowedPlantIds().contains(data.id()))
            .filter(data -> IMPLEMENTED_PLANT_IDS.contains(data.id()))
            .sorted(Comparator.comparingInt(PlantData::cost)
                .thenComparing(PlantData::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public boolean hasWon() {
        return !zombieReachedHouse
            && wavesSent == totalWaves
            && getLivingZombieCount() == 0;
    }

    public boolean hasLost() {
        return zombieReachedHouse;
    }

    public int getLivingZombieCount() {
        if (getGameState() == null) {
            return 0;
        }
        return (int) getGameState().getZombiesInTheGame().stream()
            .filter(zombie -> !zombie.isDead()).count();
    }

    public List<Zombie> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    private void dropSkySun() {
        GameState state = getGameState();
        if (state.getTickCounter() < nextSkySunTick) {
            return;
        }
        nextSkySunTick += SKY_SUN_INTERVAL_TICKS;
        state.setSun(state.getSun() + SKY_SUN_AMOUNT);
        state.logEvent("The sky dropped " + SKY_SUN_AMOUNT
            + " sun. Total sun: " + state.getSun() + ".\n");
    }

    private void maybeStartWave() {
        GameState state = getGameState();
        if (wavesSent >= totalWaves || state.getTickCounter() < nextWaveTick) {
            return;
        }
        wavesSent++;
        nextWaveTick += WAVE_INTERVAL_TICKS;
        boolean finalWave = wavesSent == totalWaves;
        if (wavesSent == 1) {
            currentWaveBudget = 150f * stageNumber;
        } else if (finalWave) {
            currentWaveBudget *= 2f;
        } else {
            currentWaveBudget *= 1.25f;
        }
        state.logEvent(finalWave
            ? "The final wave has come.\n"
            : "Wave " + wavesSent + " started.\n");
        spawnWave(currentWaveBudget);
    }

    private void spawnWave(float budget) {
        GameState state = getGameState();
        Board board = state.getBoard();
        int spawnColumn = board.getColumnCount() - 1;
        float remaining = budget;
        while (true) {
            Zombie zombie = pickAffordableZombie(remaining);
            if (zombie == null) {
                break;
            }
            int lane = random.nextInt(board.getLaneCount());
            zombie.setLane(lane);
            zombie.setX(spawnColumn);
            state.addZombie(zombie);
            remaining -= zombie.getWavePointCost();
            state.logEvent("Zombie " + zombie.getAlias()
                + " spawned at wave " + wavesSent
                + " in lane " + (lane + 1)
                + " which cost " + (int) zombie.getWavePointCost() + ".\n");
        }
    }

    private Zombie pickAffordableZombie(float remainingBudget) {
        List<Zombie> affordable = new ArrayList<>();
        long weightSum = 0;
        for (Zombie template : templates) {
            if (template.getWavePointCost() <= remainingBudget) {
                affordable.add(template);
                weightSum += Math.max(1, template.getWeight());
            }
        }
        if (affordable.isEmpty()) {
            return null;
        }
        long pick = (long) (random.nextDouble() * weightSum);
        for (Zombie template : affordable) {
            pick -= Math.max(1, template.getWeight());
            if (pick < 0) {
                return template.copy();
            }
        }
        return affordable.getLast().copy();
    }

    private void checkZombiesReachedHouse() {
        if (getGameState().checkLoseCondition()) {
            zombieReachedHouse = true;
        }
    }

    private void endState() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) {
            return;
        }
        if (hasLost()) {
            state.setFinished(true);
            state.setWon(false);
            state.logEvent("The Zombotany zombies ate your brains! You lost stage "
                + stageNumber + ".\n");
        } else if (hasWon()) {
            state.setFinished(true);
            state.setWon(true);
            state.logEvent("You survived every Zombotany wave and won stage "
                + stageNumber + "!\n");
        }
    }

    private void ensureRunning() {
        if (getGameState() == null) {
            throw new IllegalStateException("Zombotany has not been loaded.");
        }
        if (getGameState().isFinished()) {
            throw new IllegalArgumentException("This Zombotany stage is already finished.");
        }
    }

    private static final Map<Integer, Set<Integer>> STAGE_PLANT_IDS = Map.of(
        // Stage 1 - the classics: Sunflower, Peashooter, Repeater, Snow Pea,
        // Potato Mine, Cherry Bomb, Chomper, Wall-nut
        1, Set.of(1, 6, 7, 9, 30, 32, 41, 44),
        // Stage 2 - mushrooms and lobbers: Sun-shroom, Puff-shroom, Fume-shroom,
        // Cabbage-pult, Kernel-pult, Squash, Jalapeno, Tall-nut
        2, Set.of(3, 23, 24, 25, 26, 33, 35, 45),
        // Stage 3 - the heavy hitters: Twin Sunflower, Threepeater,
        // Fire Peashooter, Starfruit, Melon-pult, Doom-shroom, Garlic, Explode-o-nut
        3, Set.of(2, 8, 18, 19, 27, 36, 47, 49)
    );

    private Set<Integer> allowedPlantIds() {
        return STAGE_PLANT_IDS.getOrDefault(stageNumber, Set.of());
    }

    private static final Set<Integer> IMPLEMENTED_PLANT_IDS = Set.of(
        1, 2, 3, 4, 5,
        6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        17, 18, 19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29,
        30, 31, 32, 33, 35, 36, 38,
        39, 40, 41, 42, 43,
        44, 45, 46, 47, 48, 49, 51,
        53, 55
    );

    private PlantData resolvePlant(String requestedName) {
        if (requestedName == null || requestedName.isBlank()) {
            return null;
        }
        String trimmed = requestedName.trim();
        return PlantRegistry.getAll().stream()
            .filter(data -> allowedPlantIds().contains(data.id()))
            .filter(data -> IMPLEMENTED_PLANT_IDS.contains(data.id()))
            .filter(data -> data.name().equalsIgnoreCase(trimmed))
            .findFirst()
            .orElse(null);
    }
}
