package models.minigames.vaseBreaker;
import data.loader.PlantData;
import data.loader.PlantRegistry;
import data.loader.ZombieRegistry;
import lombok.Getter;
import lombok.Setter;
import models.Board.Board;
import models.Board.Tile;
import models.minigames.MinigameStage;
import models.minigames.MinigameType;
import models.plant.Plant;
import models.plant.PlantFactory;
import models.plant.PlantTag;
import models.zombie.Zombie;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;
import models.zombie.ZombieType;

import java.util.*;

@Getter
public class VaseBreaker extends Game {
    private final MinigameStage stage;
    private final Random random;
    private final List<Vase> vases = new ArrayList<>();
    private final List<DroppedSeedPacket> droppedSeedPackets = new ArrayList<>();
    private final Map<String, Integer> packetInventory = new LinkedHashMap<>();
    private final List<Brain> brains = new ArrayList<>();
    private static MinigameStage findVasebreakerStage(int stageNumber) {
        return MinigameStage.getStages(MinigameType.VASEBREAKER).stream()
                .filter(candidate -> candidate.getStageNumber() == stageNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vasebreaker stage must be 1, 2, or 3."));
    }

    public VaseBreaker(int stageNumber) {
        this(findVasebreakerStage(stageNumber), new Random());
    }

    public VaseBreaker(int stageNumber, Random random) {
        this(findVasebreakerStage(stageNumber), random);
    }

    public VaseBreaker(MinigameStage stage) {
        this(stage, new Random());
    }

    public VaseBreaker(MinigameStage stage, Random random) {
        this.stage = validateVasebreakerStage(stage);
        this.random = Objects.requireNonNull(random, "Random cannot be null.");
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
        state.setSun(0);
        setGameState(state);
        setSkySunSpawner(null);
        vases.clear();
        droppedSeedPackets.clear();
        packetInventory.clear();
        brains.clear();
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
        expireSeedPackets();
        state.getBoard().tickPlants(state);
        state.getBoard().tickProjectiles(state);
        for (Zombie zombie :
                new ArrayList<>(state.getZombiesInTheGame())) {
            zombie.onTick(state);
        }
        updateBrains();
        endState();
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
                        "The zombie ate the brain in row " + brain.getRow() + "; LOSER!!!\n"
                );
            }
            state.setFinished(true);
            state.setWon(false);
            return;
        }
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
    public BreakOutcome breakVase(int x, int y) {
        ensureRunning();
        Vase vase = getVaseAt(x, y);
        if (vase == null) {
            throw new IllegalArgumentException(("There is no vase at (" + x + ", " + y + ")."));
        }
        if (vase.isBroken()) {
            throw new IllegalStateException(("The vase at (" + x + ", " + y + ") is already broken."));
        }
        if (!vase.breakVase()) {
            throw new IllegalStateException("The vase is already broken.");
        }
        Zombie spawnedZombie = null;
        if (vase.getContentType() == VaseContentType.ZOMBIE ||
                vase.getContentType() == VaseContentType.GARGANTUAR) {
            spawnedZombie = ZombieRegistry.spawn(vase.getZombieAlias());
            spawnedZombie.setLane(y - 1);
            spawnedZombie.setX(x - 1);
        }
        vase.breakVase();
        if (vase.getContentType() == VaseContentType.SEED_PACKET) {
            int expiresAt = getGameState().getTickCounter() + packetLifetimeTicks();
            droppedSeedPackets.add(new DroppedSeedPacket(vase.getPlantName(), x, y, expiresAt));
        } else if (spawnedZombie != null) {
            getGameState().addZombie(spawnedZombie);
        }
        endState();
        return new BreakOutcome(vase, vase.getContentType(),
                vase.getPlantName() != null ? vase.getPlantName() : vase.getZombieAlias());
    }

    public String pickUpSeedPacket(int x, int y) {
        ensureRunning();
        int currentTick = getGameState().getTickCounter();
        DroppedSeedPacket packet = droppedSeedPackets.stream()
                .filter(p -> p.getX() == x && p.getY() == y && p.isActive(currentTick))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "There is no active seed packet at (" + x + ", " + y + ")."));

        packet.pickUp();
        packetInventory.merge(packet.getPlantName(), 1, Integer::sum);
        droppedSeedPackets.remove(packet);
        return packet.getPlantName();
    }

    public Plant plantFromPacket(String plantName, int x, int y) {
        ensureRunning();
        String inventoryName = resolvePlantName(plantName);
        if (inventoryName == null || packetInventory.getOrDefault(inventoryName, 0) <= 0) {
            throw new IllegalStateException("You do not have a " + plantName + " seed packet.");
        }

        Tile tile = getGameState().getBoard().getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null) {
            throw new IllegalArgumentException("Coordinates are outside the map.");
        }
        if (hasUnbrokenVaseAt(x, y)) {
            throw new IllegalStateException("Break the vase at (" + x + ", " + y + ") first.");
        }
        if (!tile.isOccupiable()) {
            throw new IllegalStateException("The tile at (" + x + ", " + y + ") is not occupiable.");
        }

        Plant plant = PlantFactory.create(inventoryName);
        plant.setPosX(x - 1);
        plant.setPosY(y - 1);
        tile.setPlant(plant);
        plant.getPlantType().onPlanted(plant, getGameState());
        consumePacket(inventoryName);
        return plant;
    }

    public boolean hasWon() {
        if (getGameState() == null) {
            return false;
        }
        boolean everyVaseBroken = vases.stream().allMatch(Vase::isBroken);
        boolean noLivingZombies = getGameState().getZombiesInTheGame().stream()
                .noneMatch(zombie -> !zombie.isDead());
        return everyVaseBroken && noLivingZombies;
    }

    public Vase getVaseAt(int x, int y) {
        return vases.stream()
                .filter(v -> v.getX() == x && v.getY() == y)
                .findFirst()
                .orElse(null);
    }

    public boolean hasUnbrokenVaseAt(int x, int y) {
        Vase vase = getVaseAt(x, y);
        return vase != null && !vase.isBroken();
    }

    public int getRemainingVaseCount() {
        return (int) vases.stream().filter(v -> !v.isBroken()).count();
    }

    public int getLivingZombieCount() {
        if (getGameState() == null) {
            return 0;
        }
        return (int) getGameState().getZombiesInTheGame().stream()
                .filter(z -> !z.isDead()).count();
    }

    public List<Vase> getVases() {
        return Collections.unmodifiableList(vases);
    }

    public List<DroppedSeedPacket> getDroppedSeedPackets() {
        return Collections.unmodifiableList(droppedSeedPackets);
    }

    public Map<String, Integer> getPacketInventory() {
        return Collections.unmodifiableMap(packetInventory);
    }
    private void generateLevel() {
        LevelConfig config = LevelConfig.forStage(stage.getStageNumber());
        List<Coordinate> positions = vasePositions(config.totalVases());
        Collections.shuffle(positions, random);

        int index = 0;
        for (int i = 0; i < config.plantVases(); i++) {
            Coordinate c = positions.get(index++);
            vases.add(Vase.plantVase(c.x(), c.y(), randomPlantName()));
        }
        for (int i = 0; i < config.gargantuarVases(); i++) {
            Coordinate c = positions.get(index++);
            vases.add(Vase.gargantuarVase(
                    c.x(), c.y(), requiredZombieAlias(ZombieType.GARGANTUAR)));
        }
        for (int i = 0; i < config.simpleZombieVases(); i++) {
            Coordinate c = positions.get(index++);
            vases.add(Vase.simpleZombie(c.x(), c.y(), randomOrdinaryZombieAlias()));
        }
        for (int i = 0; i < config.simpleSeedVases(); i++) {
            Coordinate c = positions.get(index++);
            vases.add(Vase.simpleSeedPacket(c.x(), c.y(), randomPlantName()));
        }
        while (index < config.totalVases()) {
            Coordinate c = positions.get(index++);
            vases.add(Vase.empty(c.x(), c.y()));
        }

        vases.sort(Comparator.comparingInt(Vase::getY).thenComparingInt(Vase::getX));
    }

    private List<Coordinate> vasePositions(int totalVases) {
        if (totalVases % 5 != 0) {
            throw new IllegalStateException("Vase count must fill complete board columns.");
        }
        int usedColumns = totalVases / 5;
        int firstColumn = 10 - usedColumns;

        List<Coordinate> positions = new ArrayList<>();
        for (int y = 1; y <= 5; y++) {
            for (int x = firstColumn; x <= 9; x++) {
                positions.add(new Coordinate(x, y));
            }
        }
        if (totalVases > positions.size()) {
            throw new IllegalStateException("Not enough board positions for the vases.");
        }
        return new ArrayList<>(positions.subList(0, totalVases));
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

    private String randomOrdinaryZombieAlias() {
        int difficulty = stage.getDifficulty();
        List<ZombieType> candidates;

        if (difficulty <= 1) {
            candidates = List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1);
        } else if (difficulty == 2) {
            candidates = List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1,
                    ZombieType.ARMOR_2, ZombieType.IMP);
        } else {
            candidates = List.of(ZombieType.DEFAULT, ZombieType.ARMOR_1,
                    ZombieType.ARMOR_2, ZombieType.ARMOR_4,
                    ZombieType.DARK_ARMOR_3, ZombieType.IMP);
        }

        List<String> available = candidates.stream()
                .map(ZombieType::getAlias)
                .filter(alias -> ZombieRegistry.getTemplate(alias) != null)
                .toList();

        if (available.isEmpty()) {
            available = ZombieRegistry.getTemplates().keySet().stream()
                    .filter(alias -> !alias.equalsIgnoreCase(ZombieType.GARGANTUAR.getAlias()))
                    .toList();
        }
        if (available.isEmpty()) {
            throw new IllegalStateException("No ordinary zombie is available.");
        }
        return available.get(random.nextInt(available.size()));
    }

    private String requiredZombieAlias(ZombieType type) {
        String alias = type.getAlias();
        if (ZombieRegistry.getTemplate(alias) == null) {
            throw new IllegalStateException("Required zombie is missing: " + alias);
        }
        return alias;
    }

    public int packetLifetimeTicks() {
        int difficulty = stage.getDifficulty();
        if (difficulty <= 1) {
            return 150; // 15 seconds
        }
        if (difficulty == 2) {
            return 100; // 10 seconds
        }
        return 70; // 7 seconds
    }
    private void endState() {
        GameState state = getGameState();
        if (state == null || state.isFinished()) {
            return;
        }
        if (hasWon()) {
            state.setFinished(true);
            state.setWon(true);
            state.logEvent("All vases are broken and all zombies are defeated. You won Vasebreaker level:\n"
                    + stage.getStageNumber() + "!\n");
        }
    }

    private void expireSeedPackets() {
        int currentTick = getGameState().getTickCounter();
        List<DroppedSeedPacket> expired = droppedSeedPackets.stream()
                .filter(packet -> packet.isExpired(currentTick))
                .toList();

        for (DroppedSeedPacket packet : expired) {
            getGameState().logEvent("The " + packet.getPlantName()
                    + " seed packet at (" + packet.getX() + ", " + packet.getY()
                    + ") disappeared.\n");
        }
        droppedSeedPackets.removeAll(expired);
    }

    private String resolvePlantName(String requestedName) {
        return packetInventory.keySet().stream()
                .filter(name -> name.equalsIgnoreCase(requestedName))
                .findFirst()
                .orElse(null);
    }

    private void consumePacket(String plantName) {
        int remaining = packetInventory.getOrDefault(plantName, 0) - 1;
        if (remaining <= 0) {
            packetInventory.remove(plantName);
        } else {
            packetInventory.put(plantName, remaining);
        }
    }

    private void ensureRunning() {
        if (getGameState() == null) {
            throw new IllegalStateException("Vasebreaker has not been loaded.");
        }
        if (getGameState().isFinished()) {
            throw new IllegalArgumentException("This Vasebreaker stage is already finished.");
        }
    }


    private static MinigameStage validateVasebreakerStage(MinigameStage stage) {
        Objects.requireNonNull(stage, "Minigame stage cannot be null.");

        if (stage.getMinigameType() != MinigameType.VASEBREAKER) {
            throw new IllegalArgumentException(
                    "VaseBreaker requires a VASEBREAKER stage, not "
                            + stage.getMinigameType() + ".");
        }
        return stage;
    }

    public record BreakOutcome(Vase vase, VaseContentType contentType, String contentName) {}

    private record Coordinate(int x, int y) {}

    private record LevelConfig(int totalVases,
                               int plantVases,
                               int gargantuarVases,
                               int simpleZombieVases,
                               int simpleSeedVases) {
        private static LevelConfig forStage(int stageNumber) {
            return switch (stageNumber) {
                case 1 -> new LevelConfig(15, 3, 0, 5, 3);
                case 2 -> new LevelConfig(20, 4, 1, 8, 3);
                case 3 -> new LevelConfig(25, 5, 2, 11, 3);
                default -> throw new IllegalArgumentException("Vasebreaker stage must be 1, 2, or 3.");
            };
        }
    }
    public List<Brain> getBrains() {
        return Collections.unmodifiableList(brains);
    }
}
