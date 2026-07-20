package models.minigames.wallnutBowling;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import lombok.Getter;
import lombok.Setter;
import models.Board.Board;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;
import models.games.ZombieWaveManager;
import models.minigames.MinigameStage;
import models.minigames.MinigameType;

import java.util.*;
@Getter
@Setter
public class WallnutBowling extends Game {
    private static final int CONVEYOR_SECONDS = 12;
    private static final int MAX_CONVEYOR_SIZE = 8;
    private static final int DEFAULT_NORMAL_ZOMBIE_HEALTH = 190;
    private static final int DEFAULT_CHERRY_BOMB_DAMAGE = 1800;
    private final MinigameStage stage;
    private final Random random;
    private final Deque<WallnutType> conveyorBelt = new ArrayDeque<>();
    private final List<RollingWallnut> rollingWallnuts = new ArrayList<>();
    private int redLineColumn;
    private int nextConveyorDeliveryTick;
    private double wallnutSpeedTilesPerSecond;
    public WallnutBowling(int stageNumber) {
        this(findStage(stageNumber), new Random());
    }
    public WallnutBowling(int stageNumber, Random random) {
        this(findStage(stageNumber), random);
    }
    public WallnutBowling(MinigameStage stage) {
        this(stage, new Random());
    }
    public WallnutBowling(MinigameStage stage, Random random) {
        this.stage = validateStage(stage);
        this.random = Objects.requireNonNull(random, "Random cannot be null.");
    }
    private static MinigameStage validateStage(MinigameStage stage) {
        Objects.requireNonNull(stage, "Minigame stage cannot be null.");
        if (stage.getMinigameType() != MinigameType.WALLNUT_BOWLING) {
            throw new IllegalArgumentException("WallnutBowling requires a WALLNUT_BOWLING stage.");
        }
        return stage;
    }
    @Override
    public void loadLevel() {
        if (ZombieRegistry.getTemplates().isEmpty()) {
            throw new IllegalStateException("ZombieRegistry is empty.");
        }

        StageConfig config = StageConfig.forStage(stage.getStageNumber());
        Board board = new Board();
        GameState state = new GameState(board, ChapterTheme.MINIGAME, true);
        state.setSun(0);
        setGameState(state);
        setSkySunSpawner(null);

        redLineColumn = config.redLineColumn();
        wallnutSpeedTilesPerSecond = config.wallnutSpeedTilesPerSecond();
        conveyorBelt.clear();
        rollingWallnuts.clear();

        List<ZombieType> allowedZombies = resolveAllowedZombies(config.allowedZombies());
        ZombieWaveManager waveManager = new ZombieWaveManager(
                state, allowedZombies, config.totalWaves(), config.baseDifficulty(), true, random);
        waveManager.setFirstWaveDelayTicks(3 * state.getTicksPerSecond());
        state.setZombieWaveManager(waveManager);
        deliverConveyorWallnut();
        nextConveyorDeliveryTick = CONVEYOR_SECONDS * state.getTicksPerSecond();
    }
    private List<ZombieType> resolveAllowedZombies(List<ZombieType> preferred) {
        List<ZombieType> available = preferred.stream()
                .filter(type -> ZombieRegistry.getTemplate(type.getAlias()) != null)
                .toList();
        if (!available.isEmpty()) return available;

        available = Arrays.stream(ZombieType.values())
                .filter(type -> ZombieRegistry.getTemplate(type.getAlias()) != null)
                .toList();
        if (available.isEmpty()) {
            throw new IllegalStateException("No zombies are available for Wall-nut Bowling.");
        }
        return available;
    }
    @Override
    public void start() {
     // bayad khali bashe
    }

    @Override
    public void onTick() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) return;
        state.addTick(1);
        updateConveyor();
        state.getZombieWaveManager().onTick();
        updateRollingWallnuts();
        for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
            zombie.onTick(state);
        }
        state.tickMowers();
        checkEndState();
    }
    @Override
    public void forward(int requestedTicks) {
        if (requestedTicks < 0) {
            throw new IllegalArgumentException("Tick count cannot be negative.");
        }
        ensureLoaded();
        for (int i = 0; i < requestedTicks && !getGameState().isFinished(); i++) {
            onTick();
        }
    }
    public RollingWallnut rollNextWallnut(int x, int y) {
        ensureRunning();
        Board board = getGameState().getBoard();
        if (x < 1 || x > redLineColumn) {
            throw new IllegalArgumentException(
                    "Walnuts may only be planted in columns 1 through "
                            + redLineColumn + ", before the red line."
            );
        }
        if (y < 1 || y > board.getLaneCount()) {
            throw new IllegalArgumentException("Row must be between 1 and "
                    + board.getLaneCount() + ".");
        }
        if (conveyorBelt.isEmpty()) {
            throw new IllegalStateException("The conveyor belt is empty.");
        }
        WallnutType type = conveyorBelt.removeFirst();
        RollingWallnut wallnut = new RollingWallnut(
                type, x - 1.0, y - 1.0, DEFAULT_NORMAL_ZOMBIE_HEALTH, DEFAULT_CHERRY_BOMB_DAMAGE,
                wallnutSpeedTilesPerSecond
        );
        rollingWallnuts.add(wallnut);
        getGameState().logEvent(type.getName() + " started rolling from ("
                + x + ", " + y + ").\n");
        return wallnut;
    }
    private void updateConveyor() {
        int currentTick = getGameState().getTickCounter();
        int interval = CONVEYOR_SECONDS * getGameState().getTicksPerSecond();
        while (currentTick >= nextConveyorDeliveryTick) {
            deliverConveyorWallnut();
            nextConveyorDeliveryTick += interval;
        }
    }

    private void deliverConveyorWallnut() {
        if (conveyorBelt.size() >= MAX_CONVEYOR_SIZE) {
            getGameState().logEvent("The conveyor belt is full; its next walnut was skipped.\n");
            return;
        }
        WallnutType delivered = randomWallnutType();
        conveyorBelt.addLast(delivered);
        getGameState().logEvent(delivered.getName()
                + " arrived on the conveyor belt.\n");
    }

    private WallnutType randomWallnutType() {
        int roll = random.nextInt(100);
        return switch (stage.getStageNumber()) {
            case 1 -> roll < 70 ? WallnutType.BOWLING
                    : roll < 95 ? WallnutType.EXPLODE
                    : WallnutType.BIG_WALLNUT;
            case 2 -> roll < 60 ? WallnutType.BOWLING
                    : roll < 85 ? WallnutType.EXPLODE
                    : WallnutType.BIG_WALLNUT;
            default -> roll < 55 ? WallnutType.BOWLING
                    : roll < 80 ? WallnutType.EXPLODE
                    : WallnutType.BIG_WALLNUT;
        };
    }

    private void updateRollingWallnuts() {
        GameState state = getGameState();
        Board board = state.getBoard();
        for (RollingWallnut wallnut : new ArrayList<>(rollingWallnuts)) {
            if (wallnut.isRemoved()) {
                continue;
            }
            wallnut.move(state.getTicksPerSecond());
            wallnut.reflectFromEdge(0, board.getLaneCount() - 1.0);
            List<Zombie> candidates = new ArrayList<>(state.getZombiesInTheGame());
            candidates.removeIf(zombie -> zombie.isDead() || !wallnut.hasCollision(zombie));
            candidates.sort(Comparator.comparingDouble(Zombie::getX));
            for (Zombie zombie : candidates) {
                boolean hit = wallnut.onHit(zombie, state);
                if (hit && wallnut.getWallnutType() != WallnutType.BIG_WALLNUT) {
                    break;
                }
            }
            if (!wallnut.isRemoved() && wallnut.getX() > board.getColumnCount() + 0.5) {
                wallnut.markRemoved();
            }
        }
        rollingWallnuts.removeIf(RollingWallnut::isRemoved);
    }

    private void checkEndState() {
        GameState state = getGameState();

        if (state.isFinished()) {
            return;
        }

        if (state.getZombieWaveManager().isLevelCleared()) {
            state.setFinished(true);
            state.setWon(true);

            state.logEvent(
                    "All bowling waves were cleared. "
                            + "You won Wall-nut Bowling stage "
                            + stage.getStageNumber()
                            + "!\n"
            );

            return;
        }

        if (state.checkLoseCondition()) {
            state.setFinished(true);
            state.setWon(false);
        }
    }

    private void ensureLoaded() {
        if (getGameState() == null) {
            throw new IllegalStateException("Wall-nut Bowling has not been loaded.");
        }
    }

    private void ensureRunning() {
        ensureLoaded();
        if (getGameState().isFinished()) {
            throw new IllegalStateException("This Wall-nut Bowling stage is already finished.");
        }
    }

    public List<WallnutType> getConveyorBelt() {
        return Collections.unmodifiableList(new ArrayList<>(conveyorBelt));
    }

    public List<RollingWallnut> getRollingWallnuts() {
        return Collections.unmodifiableList(rollingWallnuts);
    }

    public int getLivingZombieCount() {
        if (getGameState() == null) return 0;
        return (int) getGameState().getZombiesInTheGame().stream()
                .filter(zombie -> !zombie.isDead())
                .count();
    }

    private static MinigameStage findStage(int stageNumber) {
        return MinigameStage.getStages(MinigameType.WALLNUT_BOWLING).stream()
                .filter(candidate -> candidate.getStageNumber() == stageNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wall-nut Bowling stage must be 1, 2, or 3."
                ));
    }

    private record StageConfig(
            int totalWaves,
            float baseDifficulty,
            int redLineColumn,
            double wallnutSpeedTilesPerSecond,
            List<ZombieType> allowedZombies
    ) {
        private static StageConfig forStage(int stageNumber) {
            return switch (stageNumber) {
                case 1 -> new StageConfig(
                        3, 1000f, 3, 3.2,
                        List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1, ZombieType.IMP)
                );
                case 2 -> new StageConfig(
                        4, 1500f, 3, 3.1,
                        List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1,
                                ZombieType.ARMOR_2, ZombieType.IMP)
                );
                case 3 -> new StageConfig(
                        5, 2000f, 3, 3.0,
                        List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1,
                                ZombieType.ARMOR_2, ZombieType.ARMOR_4,
                                ZombieType.DARK_ARMOR_3, ZombieType.IMP)
                );
                default -> throw new IllegalArgumentException(
                        "Wall-nut Bowling stage must be 1, 2, or 3."
                );
            };
        }
    }
}
