package models.minigames.iZombie.multiplayer;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantFactory;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;
import models.minigames.MinigameStage;
import models.minigames.MinigameType;
import models.minigames.vaseBreaker.Brain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MultiplayerIZombieGame extends Game {

    public static final int BRAIN_COLUMN = 1;
    public static final int PLANT_START_COLUMN = 2;
    public static final int PLANT_END_COLUMN = 6;
    public static final int RED_LINE_COLUMN = 6;

    public static final int START_SUN = 150;
    private static final int SUN_INCOME = 25;
    private static final int PLANT_SUN_INTERVAL_TICKS = 100;
    private static final int ZOMBIE_SUN_INTERVAL_TICKS = 80;

    public static final int DEFAULT_MATCH_DURATION_TICKS = 120 * 10;

    private final MinigameStage stage;
    private final Random random;
    private final int matchDurationTicks;

    private final Map<String, Integer> roster = new LinkedHashMap<>();
    private final List<Brain> brains = new ArrayList<>();
    private final Map<String, Integer> zombieReadyAtTick = new HashMap<>();

    private int plantSun;
    private int zombieSun;

    private Outcome outcome = Outcome.IN_PROGRESS;

    public MultiplayerIZombieGame(int stageNumber, long seed) {
        this(stageNumber, seed, DEFAULT_MATCH_DURATION_TICKS);
    }

    public MultiplayerIZombieGame(int stageNumber, long seed, int matchDurationTicks) {
        this(findIZombieStage(stageNumber), new Random(seed), matchDurationTicks);
    }

    public MultiplayerIZombieGame(MinigameStage stage, Random random, int matchDurationTicks) {
        this.stage = validateIZombieStage(stage);
        this.random = Objects.requireNonNull(random, "Random cannot be null.");
        if (matchDurationTicks <= 0) {
            throw new IllegalArgumentException("Match duration must be positive.");
        }
        this.matchDurationTicks = matchDurationTicks;
    }

    @Override
    public void loadLevel() {
        if (PlantRegistry.getAll().isEmpty()) {
            throw new IllegalStateException("PlantRegistry is empty.");
        }
        if (ZombieRegistry.getTemplates().isEmpty()) {
            throw new IllegalStateException("ZombieRegistry is empty.");
        }

        Board board = new Board();
        GameState state = new GameState(board, ChapterTheme.MINIGAME, false);
        setGameState(state);
        setSkySunSpawner(null);

        brains.clear();
        roster.clear();
        zombieReadyAtTick.clear();
        outcome = Outcome.IN_PROGRESS;

        roster.putAll(rosterForStage(stage.getStageNumber()));
        roster.keySet().removeIf(alias -> ZombieRegistry.getTemplate(alias) == null);
        if (roster.isEmpty()) {
            throw new IllegalStateException(
                "No roster zombie for this stage is available in the ZombieRegistry.");
        }

        for (int row = 1; row <= board.getLaneCount(); row++) {
            brains.add(new Brain(row));
        }

        plantSun = START_SUN;
        zombieSun = START_SUN;

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
        state.getBoard().tickSuns(state);
        accrueSun(state);
        updateBrains(state);
        checkEnd(state);
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

    private void accrueSun(GameState state) {
        int tick = state.getTickCounter();
        if (tick % PLANT_SUN_INTERVAL_TICKS == 0) {
            plantSun += SUN_INCOME;
        }
        if (tick % ZOMBIE_SUN_INTERVAL_TICKS == 0) {
            zombieSun += SUN_INCOME;
        }
    }

    private void updateBrains(GameState state) {
        for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
            if (zombie.isDead() || zombie.getX() > 0) {
                continue;
            }
            int lane = zombie.getLane();
            if (lane < 0 || lane >= brains.size()) {
                continue;
            }
            Brain brain = brains.get(lane);
            if (!brain.isEaten()) {
                brain.eat();
                state.logEvent("The zombies ate the brain in row " + brain.getRow() + "!\n");
            }
        }
    }

    private void checkEnd(GameState state) {
        if (state.isFinished()) {
            return;
        }
        if (brains.stream().allMatch(Brain::isEaten)) {
            outcome = Outcome.ZOMBIE_WON;
            state.setFinished(true);
            state.setWon(true);
            state.logEvent("All brains are eaten. The zombie player wins!\n");
        } else if (state.getTickCounter() >= matchDurationTicks) {
            outcome = Outcome.PLANT_WON;
            state.setFinished(true);
            state.setWon(false);
            state.logEvent("Time is up and a brain survived. The plant player wins!\n");
        }
    }

    public Zombie placeZombie(MatchRole role, String zombieName, int x, int y) {
        requireRole(role, MatchRole.ZOMBIE);
        ensureRunning();

        String alias = resolveZombieAlias(zombieName);
        if (alias == null) {
            throw new IllegalArgumentException(
                "Zombie " + zombieName + " is not available in this stage.");
        }
        GameState state = getGameState();
        Board board = state.getBoard();

        int cost = roster.get(alias);
        int cooldownRemaining = getZombieCooldownTicks(alias);
        if (cooldownRemaining > 0) {
            throw new IllegalStateException(
                alias + " is recharging for " + cooldownRemaining + " more ticks.");
        }
        if (cost > zombieSun) {
            throw new IllegalStateException(
                "Not enough sun to place " + alias + " (costs " + cost
                    + ", you have " + zombieSun + ").");
        }
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
        zombieSun -= cost;
        zombieReadyAtTick.put(alias, state.getTickCounter() + getZombieCooldownTotalTicks(alias));
        state.logEvent("Zombie " + alias + " placed at (" + x + ", " + y
            + ") for " + cost + " sun.\n");
        checkEnd(state);
        return zombie;
    }

    public Plant placePlant(MatchRole role, String plantName, int x, int y) {
        requireRole(role, MatchRole.PLANT);
        ensureRunning();

        PlantData data = resolvePlant(plantName);
        if (data == null) {
            throw new IllegalArgumentException("Plant " + plantName + " is not available.");
        }
        GameState state = getGameState();
        Board board = state.getBoard();

        if (y < 1 || y > board.getLaneCount()) {
            throw new IllegalArgumentException("Coordinates are outside the map.");
        }
        if (x < PLANT_START_COLUMN || x > PLANT_END_COLUMN) {
            throw new IllegalArgumentException(
                "Plants can only be placed between column " + PLANT_START_COLUMN
                    + " and " + PLANT_END_COLUMN + ".");
        }
        if (data.cost() > plantSun) {
            throw new IllegalStateException(
                "Not enough sun to plant " + data.name() + " (costs " + data.cost()
                    + ", you have " + plantSun + ").");
        }
        Tile tile = board.getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null || !tile.isOccupiable()) {
            throw new IllegalStateException("That tile is not free.");
        }

        Plant plant = PlantFactory.create(data.name());
        plant.setPosX(x - 1);
        plant.setPosY(y - 1);
        tile.setPlant(plant);
        plant.getPlantType().onPlanted(plant, state);
        plantSun -= data.cost();
        state.logEvent("Plant " + data.name() + " placed at (" + x + ", " + y
            + ") for " + data.cost() + " sun.\n");
        return plant;
    }

    public Plant pluckPlant(MatchRole role, int x, int y) {
        requireRole(role, MatchRole.PLANT);
        ensureRunning();

        GameState state = getGameState();
        Board board = state.getBoard();

        if (y < 1 || y > board.getLaneCount()) {
            throw new IllegalArgumentException("Coordinates are outside the map.");
        }
        if (x < PLANT_START_COLUMN || x > PLANT_END_COLUMN) {
            throw new IllegalArgumentException(
                "Plants only exist between column " + PLANT_START_COLUMN
                    + " and " + PLANT_END_COLUMN + ".");
        }

        Tile tile = board.getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null || tile.getPlant() == null) {
            throw new IllegalStateException("There is no plant to remove on that tile.");
        }

        Plant plant = tile.getPlant();
        board.removePlant(plant);
        state.logEvent("Plant removed from (" + x + ", " + y + ").\n");
        return plant;
    }

    public Plant feedPlant(MatchRole role, int x, int y) {
        requireRole(role, MatchRole.PLANT);
        ensureRunning();

        GameState state = getGameState();
        Board board = state.getBoard();

        if (y < 1 || y > board.getLaneCount()) {
            throw new IllegalArgumentException("Coordinates are outside the map.");
        }
        if (x < PLANT_START_COLUMN || x > PLANT_END_COLUMN) {
            throw new IllegalArgumentException(
                "Plants only exist between column " + PLANT_START_COLUMN
                    + " and " + PLANT_END_COLUMN + ".");
        }

        Tile tile = board.getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null || tile.getPlant() == null) {
            throw new IllegalStateException("There is no plant to feed on that tile.");
        }

        Plant plant = tile.getPlant();
        plant.feed(state);
        state.logEvent("Plant fed at (" + x + ", " + y + ").\n");
        return plant;
    }

    public int getZombieCooldownTicks(String zombieName) {
        String alias = resolveZombieAlias(zombieName);
        if (alias == null) {
            throw new IllegalArgumentException("Zombie " + zombieName + " is not available.");
        }
        int readyAt = zombieReadyAtTick.getOrDefault(alias, 0);
        return Math.max(0, readyAt - getGameState().getTickCounter());
    }

    public int getZombieCooldownTotalTicks(String zombieName) {
        String alias = resolveZombieAlias(zombieName);
        if (alias == null) {
            throw new IllegalArgumentException("Zombie " + zombieName + " is not available.");
        }
        int cost = roster.get(alias);
        int seconds = Math.max(3, Math.min(12, 2 + cost / 25));
        return seconds * Math.max(1, getGameState().getTicksPerSecond());
    }

    public boolean isZombieReady(String zombieName) {
        return getZombieCooldownTicks(zombieName) == 0;
    }

    public boolean isFinished() {
        return outcome != Outcome.IN_PROGRESS
            || (getGameState() != null && getGameState().isFinished());
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public MatchRole getWinnerRole() {
        return switch (outcome) {
            case ZOMBIE_WON -> MatchRole.ZOMBIE;
            case PLANT_WON -> MatchRole.PLANT;
            case IN_PROGRESS -> null;
        };
    }

    public int getRemainingTicks() {
        if (getGameState() == null) {
            return matchDurationTicks;
        }
        return Math.max(0, matchDurationTicks - getGameState().getTickCounter());
    }

    public int getMatchDurationTicks() {
        return matchDurationTicks;
    }

    public int getPlantSun() {
        return plantSun;
    }

    public int getZombieSun() {
        return zombieSun;
    }

    public List<Brain> getBrains() {
        return Collections.unmodifiableList(brains);
    }

    public int getRemainingBrainCount() {
        return (int) brains.stream().filter(b -> !b.isEaten()).count();
    }

    public Map<String, Integer> getRoster() {
        return Collections.unmodifiableMap(roster);
    }

    public MinigameStage getStage() {
        return stage;
    }

    private static void requireRole(MatchRole actual, MatchRole required) {
        if (actual != required) {
            throw new IllegalStateException(
                "This action requires the " + required + " role, but was sent as " + actual + ".");
        }
    }

    private String resolveZombieAlias(String requestedName) {
        return roster.keySet().stream()
            .filter(alias -> alias.equalsIgnoreCase(requestedName))
            .findFirst()
            .orElse(null);
    }

    private static PlantData resolvePlant(String requestedName) {
        return PlantRegistry.getAll().stream()
            .filter(data -> data.name().equalsIgnoreCase(requestedName))
            .findFirst()
            .orElse(null);
    }

    private void ensureRunning() {
        GameState state = getGameState();
        if (state == null) {
            throw new IllegalStateException("Match has not been loaded.");
        }
        if (state.isFinished()) {
            throw new IllegalStateException("This match is already finished.");
        }
    }

    private static MinigameStage findIZombieStage(int stageNumber) {
        return MinigameStage.getStages(MinigameType.IZOMBIE).stream()
            .filter(candidate -> candidate.getStageNumber() == stageNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "I, Zombie stage must be 1, 2, or 3."));
    }

    private static MinigameStage validateIZombieStage(MinigameStage stage) {
        Objects.requireNonNull(stage, "Minigame stage cannot be null.");
        if (stage.getMinigameType() != MinigameType.IZOMBIE) {
            throw new IllegalArgumentException(
                "Multiplayer I, Zombie requires an IZOMBIE stage, not "
                    + stage.getMinigameType() + ".");
        }
        return stage;
    }

    /**
     * Returns the zombie choices shown to the zombie player for a stage.
     *
     * <p>The online client uses the same source of truth as the authoritative
     * server. Keeping this public prevents the client UI from drifting away
     * from the aliases and costs that {@link #placeZombie} validates.</p>
     */
    public static Map<String, Integer> rosterForStage(int stageNumber) {
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
        return Collections.unmodifiableMap(roster);
    }

    public enum Outcome {
        IN_PROGRESS,
        PLANT_WON,
        ZOMBIE_WON
    }
}
