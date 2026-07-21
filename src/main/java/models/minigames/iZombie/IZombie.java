package models.minigames.iZombie;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import models.Board.Board;
import models.Board.Tile;
import models.minigames.MinigameStage;
import models.minigames.MinigameType;
import models.minigames.vaseBreaker.Brain;
import models.Plant.Plant;
import models.Plant.PlantFactory;
import models.Plant.PlantTag;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;

import java.util.*;

public class IZombie extends Game {
    private final MinigameStage stage;
    private final Random random;
    private final Map<String, Integer> roster = new LinkedHashMap<>();
    private final List<Brain> brains = new ArrayList<>();
    private final List<SunProducer> sunProducers = new ArrayList<>();

    public static final int START_SUN = 150;
    public static final int PLANT_COLUMNS = 6;
    public static final int RED_LINE_COLUMN = PLANT_COLUMNS;
    private final float plantTileChance;

    public static final float SUN_PRODUCER_HP = 1290f;
    public static final String SUN_PRODUCER_ALIAS = "IZombieSunProducer";
    private static final int SUN_PER_PRODUCTION = 25;

    private static final int START_INTERVAL_TICKS = 100;
    private static final int MIN_INTERVAL_TICKS = 20;
    private static final int INTERVAL_STEP_TICKS = 5;

    private static MinigameStage findIZombieStage(int stageNumber) {
        return MinigameStage.getStages(MinigameType.IZOMBIE).stream()
            .filter(candidate -> candidate.getStageNumber() == stageNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "I, Zombie stage must be 1, 2, or 3."));
    }

    public IZombie(int stageNumber) {
        this(findIZombieStage(stageNumber), new Random());
    }

    public IZombie(int stageNumber, Random random) {
        this(findIZombieStage(stageNumber), random);
    }

    public IZombie(MinigameStage stage) {
        this(stage, new Random());
    }

    public IZombie(MinigameStage stage, Random random) {
        this.stage = validateIZombieStage(stage);
        this.random = Objects.requireNonNull(random, "Random cannot be null.");
        this.plantTileChance = Math.min(0.9f, 0.4f + 0.1f * this.stage.getDifficulty());
    }

    @Override
    public void loadLevel() {
        if (PlantRegistry.getAll().isEmpty()) {
            throw new IllegalStateException(
                "PlantRegistry is empty."
            );
        }
        if (ZombieRegistry.getTemplates().isEmpty()) {
            throw new IllegalStateException(
                "ZombieRegistry is empty."
            );
        }
        Board board = new Board();
        GameState state = new GameState(board, ChapterTheme.MINIGAME, false);
        state.setSun(START_SUN);
        setGameState(state);
        setSkySunSpawner(null);
        brains.clear();
        sunProducers.clear();
        roster.clear();
        roster.putAll(LevelConfig.forStage(stage.getStageNumber()).roster());
        roster.keySet().removeIf(alias -> ZombieRegistry.getTemplate(alias) == null);
        if (roster.isEmpty()) {
            throw new IllegalStateException(
                "No roster zombie for this stage is available in the ZombieRegistry.");
        }
        initializeBrains(board.getLaneCount());
        generateLevel();
    }

    private void initializeBrains(int laneCount) {
        for (int row = 1; row <= laneCount; row++) {
            brains.add(new Brain(row));
        }
    }

    @Override
    public void start() {
        // should be empty.
    }

    @Override
    public void onTick() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) return;
        state.addTick(1);
        state.getBoard().tickPlants(state);
        state.getBoard().tickProjectiles(state);
        for (Zombie zombie :
            new ArrayList<>(state.getZombiesInTheGame())) {
            zombie.onTick(state);
        }
        state.getBoard().tickLoots(state);
        produceSun();
        updateBrains();
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

    public Zombie placeZombie(String zombieName, int x, int y) {
        ensureRunning();
        String alias = resolveZombieAlias(zombieName);
        if (alias == null) {
            throw new IllegalArgumentException(
                "Zombie " + zombieName + " is not available in this stage.");
        }
        GameState state = getGameState();
        int cost = roster.get(alias);
        if (cost > state.getSun()) {
            throw new IllegalStateException(
                "Not enough sun to place " + alias + " (costs " + cost
                    + ", you have " + state.getSun() + ").");
        }
        Board board = state.getBoard();
        if (y < 1 || y > board.getLaneCount() || x < 1 || x > board.getColumnCount()) {
            throw new IllegalArgumentException("Coordinates are outside the map.");
        }
        if (x <= RED_LINE_COLUMN) {
            throw new IllegalArgumentException(
                "Zombies can only be placed to the right of the red line.");
        }
        Zombie zombie = ZombieRegistry.spawn(alias);
        zombie.setLane(y - 1);
        zombie.setColumn(x - 1);
        state.addZombie(zombie);
        state.setSun(state.getSun() - cost);
        state.logEvent("Zombie " + alias + " placed at (" + x + ", " + y
            + ") for " + cost + " sun.\n");
        endState();
        return zombie;
    }

    public boolean hasWon() {
        if (getGameState() == null) {
            return false;
        }
        return brains.stream().allMatch(Brain::isEaten);
    }

    public boolean hasLost() {
        if (getGameState() == null || hasWon()) {
            return false;
        }
        int cheapest = roster.values().stream()
            .min(Integer::compareTo)
            .orElse(Integer.MAX_VALUE);
        return getGameState().getSun() < cheapest && getLivingZombieCount() == 0;
    }

    public int getLivingZombieCount() {
        if (getGameState() == null) {
            return 0;
        }
        return (int) getGameState().getZombiesInTheGame().stream()
            .filter(z -> !z.isDead()).count();
    }

    public int getRemainingBrainCount() {
        return (int) brains.stream().filter(brain -> !brain.isEaten()).count();
    }

    public List<Brain> getBrains() {
        return Collections.unmodifiableList(brains);
    }

    public Map<String, Integer> getRoster() {
        return Collections.unmodifiableMap(roster);
    }

    public MinigameStage getStage() {
        return stage;
    }

    public int getLivingSunProducerCount() {
        return (int) sunProducers.stream()
            .filter(producer -> !producer.zombie.isDead()).count();
    }

    private void generateLevel() {
        Board board = getGameState().getBoard();
        for (int y = 1; y <= board.getLaneCount(); y++) {
            boolean planted = false;
            for (int x = 1; x <= PLANT_COLUMNS; x++) {
                if (random.nextFloat() < plantTileChance) {
                    planted |= plantRandomPlant(x, y);
                }
            }
            if (!planted) {
                plantRandomPlant(1 + random.nextInt(PLANT_COLUMNS), y);
            }
            spawnSunProducer(y - 1, board.getColumnCount() - 1);
        }
    }

    private boolean plantRandomPlant(int x, int y) {
        Tile tile = getGameState().getBoard().getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null || !tile.isOccupiable()) {
            return false;
        }
        Plant plant = PlantFactory.create(randomPlantName());
        plant.setPosX(x - 1);
        plant.setPosY(y - 1);
        tile.setPlant(plant);
        plant.getPlantType().onPlanted(plant, getGameState());
        return true;
    }

    private void spawnSunProducer(int lane, int column) {
        Zombie producer = new Zombie(SUN_PRODUCER_ALIAS, SUN_PRODUCER_HP, 0f, 0f, 0f, 0);
        producer.setLane(lane);
        producer.setColumn(column);
        getGameState().addZombie(producer);
        sunProducers.add(new SunProducer(producer));
    }

    private String randomPlantName() {
        List<PlantData> combatPlants = PlantRegistry.getAll().stream()
            .filter(data -> !data.category().equalsIgnoreCase("sunproducer"))
            .filter(data -> !data.tags().contains(PlantTag.SUN))
            .filter(data -> !data.name().toLowerCase().endsWith("-mint"))
            .toList();

        List<PlantData> pool = combatPlants.isEmpty() ? PlantRegistry.getAll() : combatPlants;
        return pool.get(random.nextInt(pool.size())).name();
    }

    private void produceSun() {
        GameState state = getGameState();
        for (SunProducer producer : sunProducers) {
            if (producer.zombie.isDead()) {
                continue;
            }
            producer.ticksUntilProduction--;
            if (producer.ticksUntilProduction <= 0) {
                state.setSun(state.getSun() + SUN_PER_PRODUCTION);
                producer.intervalTicks = Math.max(
                    MIN_INTERVAL_TICKS, producer.intervalTicks - INTERVAL_STEP_TICKS);
                producer.ticksUntilProduction = producer.intervalTicks;
                state.logEvent("A sun producer zombie made " + SUN_PER_PRODUCTION
                    + " sun. Total sun: " + state.getSun() + ".\n");
            }
        }
    }

    private void updateBrains() {
        GameState state = getGameState();
        for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
            if (zombie.isDead()) {
                continue;
            }
            if (zombie.getX() >= 0) {
                continue;
            }
            int lane = zombie.getLane();
            if (lane < 0 || lane >= brains.size()) {
                throw new IllegalStateException("Zombie has an invalid lane: " + lane);
            }
            Brain brain = brains.get(lane);
            if (!brain.isEaten()) {
                brain.eat();
                state.logEvent(
                    "The zombies ate the brain in row " + brain.getRow() + "!\n"
                );
            }
        }
    }

    private void endState() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) {
            return;
        }
        if (hasWon()) {
            state.setFinished(true);
            state.setWon(true);
            state.logEvent("All brains are eaten. You won I, Zombie level "
                + stage.getStageNumber() + "!\n");
        } else if (hasLost()) {
            state.setFinished(true);
            state.setWon(false);
            state.logEvent("You are out of sun and zombies; the plants survived. You lost.\n");
        }
    }

    private String resolveZombieAlias(String requestedName) {
        return roster.keySet().stream()
            .filter(alias -> alias.equalsIgnoreCase(requestedName))
            .findFirst()
            .orElse(null);
    }

    private void ensureRunning() {
        if (getGameState() == null) {
            throw new IllegalStateException("I, Zombie has not been loaded.");
        }
        if (getGameState().isFinished()) {
            throw new IllegalArgumentException("This I, Zombie stage is already finished.");
        }
    }

    private static MinigameStage validateIZombieStage(MinigameStage stage) {
        Objects.requireNonNull(stage, "Minigame stage cannot be null.");

        if (stage.getMinigameType() != MinigameType.IZOMBIE) {
            throw new IllegalArgumentException(
                "IZombie requires an IZOMBIE stage, not "
                    + stage.getMinigameType() + ".");
        }
        return stage;
    }

    private static final class SunProducer {
        private final Zombie zombie;
        private int intervalTicks = START_INTERVAL_TICKS;
        private int ticksUntilProduction = START_INTERVAL_TICKS;

        private SunProducer(Zombie zombie) {
            this.zombie = zombie;
        }
    }

    private record LevelConfig(Map<String, Integer> roster) {
        private static LevelConfig forStage(int stageNumber) {
            LinkedHashMap<String, Integer> roster = new LinkedHashMap<>();
            switch (stageNumber) {
                case 1 -> {
                    roster.put("ZombieImp", 25);
                    roster.put("ZombieDefault", 50);
                    roster.put("ZombieNewspaper", 75);
                    roster.put("ZombieIceAgeDodo", 100);
                    roster.put("ZombieDarkJuggler", 125);
                }
                case 2 -> {
                    roster.put("ZombieExplorer", 75);
                    roster.put("ZombieBeachSnorkel", 75);
                    roster.put("ZombieIceAgeHunter", 100);
                    roster.put("ZombieProspector", 125);
                    roster.put("ZombieModernAllStar", 150);
                }
                case 3 -> {
                    roster.put("ZombieDefault", 50);
                    roster.put("ZombieBeachOctopus", 125);
                    roster.put("ZombieWizard", 150);
                    roster.put("ZombiePiano", 150);
                    roster.put("ZombieGargantuar", 300);
                }
                default -> throw new IllegalArgumentException(
                    "I, Zombie stage must be 1, 2, or 3.");
            }
            return new LevelConfig(roster);
        }
    }
}
