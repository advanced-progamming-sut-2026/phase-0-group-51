package models.games;

import Data.loader.ZombieRegistry;
import lombok.Getter;
import lombok.Setter;
import models.Board.Tile;
import models.Zombie.Behavior.SandstormTransportBehavior;
import models.Zombie.Behavior.SnowstormTransportBehavior;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.items.Wave;
import models.quests.QuestKillSourceType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;

@Getter
@Setter
public class ZombieWaveManager {

    private static final float BACKWATER_MAX_ZOMBIE_COST = 700f;

    private static final int SANDSTORM_TRANSPORT_TICKS = 10;
    private static final int SNOWSTORM_TRANSPORT_TICKS = 10;

    private static final float ZOMBIE_SPAWN_X_OFFSET = 1.3f;

    private static final int MIN_SPAWN_GAP_TICKS = 30;
    private static final int MAX_SPAWN_GAP_TICKS = 60;

    private final GameState gs;

    private final List<ZombieType> allowedAliases;

    private final int totalWaves;

    private final float baseDifficulty;

    private final Random random;

    private final boolean endless;

    private final float maxDifficulty;

    // Normal levels keep random wave generation, but every allowed zombie
    // must appear at least once before the level ends.
    private final List<ZombieType> coverageOrder;
    private final Set<String> coveredZombieAliases =
        new HashSet<>();

    private boolean started;

    private boolean tornadoFinalWave = false;

    private List<Integer> snowstormLanesForNextWave = List.of();

    private IntConsumer onWaveStart = null;

    private final List<Wave> waves =
        new ArrayList<>();

    private final Deque<PendingZombieSpawn> pendingSpawns =
        new ArrayDeque<>();

    private final Deque<PendingSpecialSpawn> pendingSpecialSpawns =
        new ArrayDeque<>();

    private Wave currentWave = null;

    private float currentDifficulty = 0f;

    private int firstWaveDelayTicks = 0;

    private int spawnDelayTicks = 0;

    private int lastSpawnLane = -1;
    private int consecutiveLaneSpawns = 0;

    public ZombieWaveManager(
        GameState gs,
        List<ZombieType> allowedAliases,
        int totalWaves,
        float baseDifficulty
    ) {
        this(
            gs,
            allowedAliases,
            totalWaves,
            baseDifficulty,
            true,
            new Random(),
            false,
            Float.MAX_VALUE
        );
    }

    public ZombieWaveManager(
        GameState gs,
        List<ZombieType> allowedAliases,
        int totalWaves,
        float baseDifficulty,
        boolean autoStart,
        Random random
    ) {
        this(
            gs,
            allowedAliases,
            totalWaves,
            baseDifficulty,
            autoStart,
            random,
            false,
            Float.MAX_VALUE
        );
    }

    private ZombieWaveManager(
        GameState gs,
        List<ZombieType> allowedAliases,
        int totalWaves,
        float baseDifficulty,
        boolean autoStart,
        Random random,
        boolean endless,
        float maxDifficulty
    ) {
        this.gs =
            Objects.requireNonNull(
                gs,
                "GameState cannot be null."
            );

        this.allowedAliases =
            List.copyOf(
                Objects.requireNonNull(
                    allowedAliases,
                    "Allowed zombies cannot be null."
                )
            );

        this.random =
            Objects.requireNonNull(
                random,
                "Random cannot be null."
            );

        if (allowedAliases.isEmpty()) {
            throw new IllegalArgumentException(
                "Allowed zombies cannot be empty."
            );
        }

        if (!endless && totalWaves <= 0) {
            throw new IllegalArgumentException(
                "Total waves must be positive."
            );
        }

        if (baseDifficulty <= 0) {
            throw new IllegalArgumentException(
                "Base difficulty must be positive."
            );
        }

        if (maxDifficulty < baseDifficulty) {
            throw new IllegalArgumentException(
                "Maximum difficulty cannot be less than base difficulty."
            );
        }

        this.totalWaves = totalWaves;
        this.baseDifficulty = baseDifficulty;
        this.started = autoStart;
        this.endless = endless;
        this.maxDifficulty = maxDifficulty;

        List<ZombieType> uniqueCoverage =
            new ArrayList<>(
                new LinkedHashSet<>(
                    this.allowedAliases
                )
            );

        // Shuffle once per level. This avoids always guaranteeing the same
        // zombie types in the same early waves while remaining deterministic
        // when a seeded Random is supplied.
        Collections.shuffle(
            uniqueCoverage,
            this.random
        );

        this.coverageOrder =
            List.copyOf(uniqueCoverage);
    }

    public static ZombieWaveManager endless(
        GameState state,
        List<ZombieType> allowedZombies,
        float baseDifficulty,
        float maxDifficulty,
        Random random
    ) {
        return new ZombieWaveManager(
            state,
            allowedZombies,
            Integer.MAX_VALUE,
            baseDifficulty,
            true,
            random,
            true,
            maxDifficulty
        );
    }

    public void start() {
        started = true;
    }

    public void releaseTheNuke() {
        for (
            Zombie zombie :
            new ArrayList<>(
                gs.getZombiesInTheGame()
            )
        ) {
            zombie.killInstantly(
                gs,
                QuestKillSourceType.CHEAT
            );
        }
    }

    public void onTick() {
        if (!started) {
            return;
        }

        tickPendingSpecialSpawns();

        if (!pendingSpawns.isEmpty()) {
            tickPendingSpawns();
            return;
        }

        if (currentWave == null) {
            if (firstWaveDelayTicks > 0) {
                firstWaveDelayTicks--;
                return;
            }

            if (!allWavesSent()) {
                startNextWave();
            }

            return;
        }

        if (
            currentWave.isBroken()
                && !allWavesSent()
        ) {
            startNextWave();
        }
    }

    public boolean allWavesSent() {
        return !endless
            && waves.size() >= totalWaves;
    }

    public boolean isLevelCleared() {
        if (endless) {
            return false;
        }

        if (!allWavesSent()) {
            return false;
        }

        if (!pendingSpawns.isEmpty()
            || !pendingSpecialSpawns.isEmpty()) {
            return false;
        }

        for (
            Zombie zombie :
            gs.getZombiesInTheGame()
        ) {
            if (!zombie.isDead()) {
                return false;
            }
        }

        return true;
    }

    public int getCurrentWaveNumber() {
        return currentWave == null
            ? 0
            : currentWave.getWaveNumber();
    }
    public float getCurrentWaveProgress() {
        if (currentWave == null) {
            return 0f;
        }

        int spawnedCount = currentWave.getZombies().size();
        int pendingCount = pendingSpawns.size();
        int totalZombieCount = spawnedCount + pendingCount;
        float spawnProgress = 0f;
        if (totalZombieCount > 0) {
            spawnProgress = spawnedCount / (float) totalZombieCount;
        }

        long totalHealth = currentWave.getInitialTotalHealth();
        long remainingHealth = currentWave.remainingHealth();
        for (PendingZombieSpawn pending : pendingSpawns) {
            Zombie zombie = pending.zombie();
            totalHealth += zombie.getMaxHitpoints();
            remainingHealth += zombie.getMaxHitpoints();
        }

        float damageProgress = 0f;
        if (totalHealth > 0) {
            float healthDestroyed =
                1f
                    - remainingHealth
                    / (float) totalHealth;


            float requiredDamage =
                currentWave.isFinalWave()
                    ? 1f
                    : 0.75f;

            damageProgress =
                healthDestroyed
                    / requiredDamage;

            damageProgress =
                Math.max(
                    0f,
                    Math.min(
                        1f,
                        damageProgress
                    )
                );
        }

        final float SPAWN_WEIGHT = 0.20f;
        final float DAMAGE_WEIGHT = 0.80f;

        float progress =
            spawnProgress * SPAWN_WEIGHT
                + damageProgress * DAMAGE_WEIGHT;

        return Math.max(
            0f,
            Math.min(
                1f,
                progress
            )
        );
    }
    private void startNextWave() {
        int number =
            waves.size() + 1;

        boolean finalWave =
            !endless
                && number == totalWaves;

        if (number == 1) {
            currentDifficulty =
                baseDifficulty;

            gs.getQuestTracker()
                .recordFirstWaveStart(
                    gs.getTickCounter()
                );

        } else if (finalWave) {

            currentDifficulty *= 2f;

        } else {

            currentDifficulty =
                Math.min(
                    maxDifficulty,
                    currentDifficulty * 1.25f
                );
        }

        if (endless) {
            gs.logEvent(
                "Endless wave "
                    + number
                    + " started.\n"
            );

        } else if (finalWave) {

            gs.logEvent(
                "The final wave has come.\n"
            );

        } else {

            gs.logEvent(
                "Wave "
                    + number
                    + " started.\n"
            );
        }

        currentWave =
            new Wave(
                number,
                currentDifficulty,
                finalWave
            );

        waves.add(
            currentWave
        );

        if (onWaveStart != null) {
            onWaveStart.accept(
                number
            );
        }

        prepareWaveSpawns(
            currentWave
        );
    }

    public void setSnowstormLanesForNextWave(
        List<Integer> lanes
    ) {
        if (lanes == null
            || lanes.isEmpty()) {
            snowstormLanesForNextWave =
                List.of();
            return;
        }

        int laneCount =
            gs.getBoard()
                .getLaneCount();

        snowstormLanesForNextWave =
            lanes.stream()
                .filter(
                    lane ->
                        lane != null
                            && lane >= 0
                            && lane < laneCount
                )
                .distinct()
                .toList();
    }

    private void prepareWaveSpawns(
        Wave wave
    ) {
        pendingSpawns.clear();

        spawnDelayTicks = 0;

        List<Integer> activeSnowstormLanes =
            snowstormLanesForNextWave;

        snowstormLanesForNextWave =
            List.of();

        int preparedZombieCount = 0;

        float remaining =
            wave.getDifficulty();

        int lanes =
            gs.getBoard()
                .getLaneCount();

        int spawnColumn =
            gs.getBoard()
                .getColumnCount() - 1;

        float spawnX =
            spawnColumn
                + ZOMBIE_SPAWN_X_OFFSET;

        // Endless mode has no level ending, so it stays fully random.
        // Normal levels reserve enough unseen zombie types per wave so that
        // every allowed type is guaranteed to appear by the final wave.
        if (!endless) {
            List<ZombieType> uncovered =
                getUncoveredAllowedTypes();

            int wavesRemainingIncludingThis =
                Math.max(
                    1,
                    totalWaves
                        - wave.getWaveNumber()
                        + 1
                );

            int guaranteedThisWave =
                uncovered.isEmpty()
                    ? 0
                    : (
                    uncovered.size()
                    + wavesRemainingIncludingThis
                    - 1
                ) / wavesRemainingIncludingThis;

            for (
                int i = 0;
                i < guaranteedThisWave
                    && i < uncovered.size();
                i++
            ) {
                ZombieType guaranteedType =
                    uncovered.get(i);

                Zombie guaranteedZombie =
                    createZombieForCoverage(
                        guaranteedType
                    );

                if (guaranteedZombie == null) {
                    continue;
                }

                queuePreparedZombie(
                    guaranteedZombie,
                    wave,
                    activeSnowstormLanes,
                    preparedZombieCount,
                    lanes,
                    spawnColumn,
                    spawnX
                );

                markCovered(
                    guaranteedZombie
                );

                remaining =
                    Math.max(
                        0f,
                        remaining
                            - Math.max(
                            0f,
                            guaranteedZombie
                                .getWavePointCost()
                        )
                    );

                preparedZombieCount++;
            }
        }

        while (true) {

            Zombie zombie =
                pickAffordableZombie(
                    remaining
                );

            if (zombie == null) {
                break;
            }

            queuePreparedZombie(
                zombie,
                wave,
                activeSnowstormLanes,
                preparedZombieCount,
                lanes,
                spawnColumn,
                spawnX
            );

            markCovered(
                zombie
            );

            remaining -=
                zombie.getWavePointCost();

            preparedZombieCount++;
        }
    }

    private List<ZombieType> getUncoveredAllowedTypes() {
        List<ZombieType> uncovered =
            new ArrayList<>();

        for (
            ZombieType type :
            coverageOrder
        ) {
            if (type == null) {
                continue;
            }

            String alias =
                type.getAlias();

            if (alias == null
                || alias.isBlank()
                || coveredZombieAliases
                .contains(alias)) {
                continue;
            }

            // A missing registry template cannot be spawned. Do not let one
            // broken alias block the coverage of all valid zombie types.
            if (
                ZombieRegistry.getTemplate(alias)
                    == null
            ) {
                continue;
            }

            uncovered.add(type);
        }

        return uncovered;
    }

    private Zombie createZombieForCoverage(
        ZombieType type
    ) {
        if (type == null) {
            return null;
        }

        Zombie template =
            ZombieRegistry.getTemplate(
                type.getAlias()
            );

        if (template == null) {
            return null;
        }

        return template.copy();
    }

    private void markCovered(
        Zombie zombie
    ) {
        if (zombie == null
            || zombie.getAlias() == null
            || zombie.getAlias().isBlank()) {
            return;
        }

        coveredZombieAliases.add(
            zombie.getAlias()
        );
    }

    private void queuePreparedZombie(
        Zombie zombie,
        Wave wave,
        List<Integer> activeSnowstormLanes,
        int preparedZombieCount,
        int lanes,
        int spawnColumn,
        float spawnX
    ) {
        boolean snowstormTransport =
            !activeSnowstormLanes.isEmpty()
                && (
                preparedZombieCount == 0
                    || random.nextBoolean()
            );

        int lane =
            snowstormTransport
                ? activeSnowstormLanes.get(
                random.nextInt(
                    activeSnowstormLanes.size()
                )
            )
                : chooseSpawnLane(
                lanes
            );

        float x =
            spawnX;

        if (snowstormTransport) {
            int movedColumns =
                1
                    + random.nextInt(3);

            float targetX =
                Math.max(
                    0f,
                    spawnColumn
                        - movedColumns
                );

            zombie.addBehavior(
                new SnowstormTransportBehavior(
                    spawnX,
                    targetX,
                    SNOWSTORM_TRANSPORT_TICKS
                )
            );

            gs.logEvent(
                "A snowstorm is carrying "
                    + zombie.getAlias()
                    + " "
                    + movedColumns
                    + " columns forward in lane "
                    + (lane + 1)
                    + ".\n"
            );
        }

        if (
            wave.isFinalWave()
                && tornadoFinalWave
                && random.nextBoolean()
        ) {
            int movedColumns =
                1
                    + random.nextInt(4);

            float targetX =
                Math.max(
                    0f,
                    spawnX
                        - movedColumns
                );

            zombie.addBehavior(
                new SandstormTransportBehavior(
                    spawnX,
                    targetX,
                    SANDSTORM_TRANSPORT_TICKS
                )
            );

            gs.logEvent(
                "A sandstorm is carrying "
                    + zombie.getAlias()
                    + " "
                    + movedColumns
                    + " columns forward.\n"
            );
        }

        zombie.setGlowing(
            random.nextInt(100) < 5
        );

        pendingSpawns.addLast(
            new PendingZombieSpawn(
                zombie,
                lane,
                x
            )
        );
    }

    private void tickPendingSpawns() {
        if (pendingSpawns.isEmpty()) {
            return;
        }

        if (spawnDelayTicks > 0) {
            spawnDelayTicks--;
            return;
        }

        PendingZombieSpawn pending =
            pendingSpawns.removeFirst();

        Zombie zombie =
            pending.zombie();

        zombie.setLane(
            pending.lane()
        );

        zombie.setX(
            pending.x()
        );

        gs.addZombie(
            zombie
        );

        markCovered(
            zombie
        );

        if (currentWave != null) {
            currentWave.addZombie(
                zombie
            );
        }

        gs.logEvent(
            "Zombie "
                + zombie.getAlias()
                + " spawned at wave "
                + (
                currentWave == null
                    ? 0
                    : currentWave.getWaveNumber()
            )
                + " in lane "
                + (pending.lane() + 1)
                + " which cost "
                + zombie.getWavePointCost()
                + ".\n"
        );

        if (!pendingSpawns.isEmpty()) {
            spawnDelayTicks =
                randomSpawnGap();
        }
    }

    private int chooseSpawnLane(
        int laneCount
    ) {

        if (laneCount <= 1) {
            return 0;
        }

        int lane;

        do {
            lane = random.nextInt(laneCount);
        } while (
            lane == lastSpawnLane
                && consecutiveLaneSpawns >= 1
        );

        if (lane == lastSpawnLane) {
            consecutiveLaneSpawns++;
        } else {
            lastSpawnLane = lane;
            consecutiveLaneSpawns = 1;
        }

        return lane;
    }

    private int randomSpawnGap() {
        if (
            MAX_SPAWN_GAP_TICKS
                <= MIN_SPAWN_GAP_TICKS
        ) {
            return MIN_SPAWN_GAP_TICKS;
        }

        return MIN_SPAWN_GAP_TICKS
            + random.nextInt(
            MAX_SPAWN_GAP_TICKS
                - MIN_SPAWN_GAP_TICKS
                + 1
        );
    }

    private Zombie pickAffordableZombie(
        float remainingBudget
    ) {
        List<Zombie> affordable =
            new ArrayList<>();

        long weightSum = 0;

        for (
            ZombieType alias :
            allowedAliases
        ) {
            Zombie template =
                ZombieRegistry.getTemplate(
                    alias.getAlias()
                );

            if (template == null) {
                continue;
            }

            Zombie candidate =
                template.copy();

            float cost =
                candidate.getWavePointCost();

            if (
                cost > 0f
                    && cost <= remainingBudget
            ) {
                affordable.add(
                    candidate
                );

                weightSum +=
                    Math.max(
                        1,
                        candidate.getWeight()
                    );
            }
        }

        if (
            affordable.isEmpty()
                || weightSum <= 0
        ) {
            return null;
        }

        long roll =
            (long) (
                random.nextDouble()
                    * weightSum
            );

        for (
            Zombie candidate :
            affordable
        ) {
            roll -=
                Math.max(
                    1,
                    candidate.getWeight()
                );

            if (roll < 0) {
                return candidate;
            }
        }

        return affordable.get(
            affordable.size() - 1
        );
    }

    public void scheduleZombieFromGrave(
        Tile graveTile,
        int waveNumber,
        int delayTicks
    ) {
        scheduleSpecialSpawn(
            SpecialSpawnSource.GRAVE,
            graveTile,
            waveNumber,
            delayTicks
        );
    }

    public void scheduleZombieFromBackwater(
        Tile shoreTile,
        int waveNumber,
        int delayTicks
    ) {
        scheduleSpecialSpawn(
            SpecialSpawnSource.BACKWATER,
            shoreTile,
            waveNumber,
            delayTicks
        );
    }

    private void scheduleSpecialSpawn(
        SpecialSpawnSource source,
        Tile tile,
        int waveNumber,
        int delayTicks
    ) {
        if (tile == null) {
            return;
        }

        pendingSpecialSpawns.addLast(
            new PendingSpecialSpawn(
                source,
                tile,
                waveNumber,
                Math.max(1, delayTicks)
            )
        );
    }

    private void tickPendingSpecialSpawns() {
        int pendingCount =
            pendingSpecialSpawns.size();

        for (int i = 0; i < pendingCount; i++) {
            PendingSpecialSpawn pending =
                pendingSpecialSpawns.removeFirst();

            if (pending.ticksRemaining() > 1) {
                pendingSpecialSpawns.addLast(
                    new PendingSpecialSpawn(
                        pending.source(),
                        pending.tile(),
                        pending.waveNumber(),
                        pending.ticksRemaining() - 1
                    )
                );
                continue;
            }

            if (pending.source() == SpecialSpawnSource.GRAVE) {
                spawnZombieFromGrave(
                    pending.tile(),
                    pending.waveNumber()
                );
            } else {
                spawnZombieFromBackwater(
                    pending.tile(),
                    pending.waveNumber()
                );
            }
        }
    }

    public Zombie spawnZombieFromGrave(
        Tile graveTile,
        int waveNumber
    ) {
        if (
            graveTile == null
                || !graveTile.hasGrave()
        ) {
            return null;
        }

        Zombie existingZombie =
            gs.getBoard()
                .getZombieInPosition(
                    graveTile.getLane(),
                    graveTile.getColumn()
                );

        if (existingZombie != null) {
            return null;
        }

        Zombie zombie =
            pickAffordableZombie(
                Float.MAX_VALUE
            );

        if (zombie == null) {
            return null;
        }

        zombie.setLane(
            graveTile.getLane()
        );

        zombie.setX(
            graveTile.getColumn()
        );

        zombie.setGlowing(
            random.nextInt(100) < 5
        );

        gs.addZombie(
            zombie
        );

        markCovered(
            zombie
        );

        if (currentWave != null) {
            currentWave.addZombie(
                zombie
            );
        }

        gs.logEvent(
            "Necromancy summoned "
                + zombie.getAlias()
                + " from the grave at ("
                + (graveTile.getColumn() + 1)
                + ", "
                + (graveTile.getLane() + 1)
                + ") during wave "
                + waveNumber
                + ".\n"
        );

        return zombie;
    }

    public Zombie spawnZombieFromBackwater(
        Tile shoreTile,
        int waveNumber
    ) {
        if (
            shoreTile == null
                || !shoreTile.isLowShore()
                || !shoreTile.isWater()
        ) {
            return null;
        }

        Zombie existingZombie =
            gs.getBoard()
                .getZombieInPosition(
                    shoreTile.getLane(),
                    shoreTile.getColumn()
                );

        if (existingZombie != null) {
            return null;
        }

        Zombie zombie =
            pickAffordableZombie(
                BACKWATER_MAX_ZOMBIE_COST
            );

        if (zombie == null) {
            return null;
        }

        zombie.setLane(
            shoreTile.getLane()
        );

        zombie.setX(
            shoreTile.getColumn()
        );

        zombie.setGlowing(
            random.nextInt(100) < 5
        );

        gs.addZombie(
            zombie
        );

        markCovered(
            zombie
        );

        if (currentWave != null) {
            currentWave.addZombie(
                zombie
            );
        }

        gs.logEvent(
            "Backwater spawned "
                + zombie.getAlias()
                + " from below the low shore at ("
                + (shoreTile.getColumn() + 1)
                + ", "
                + (shoreTile.getLane() + 1)
                + ") during wave "
                + waveNumber
                + ".\n"
        );

        return zombie;
    }

    private enum SpecialSpawnSource {
        GRAVE,
        BACKWATER
    }

    private record PendingSpecialSpawn(
        SpecialSpawnSource source,
        Tile tile,
        int waveNumber,
        int ticksRemaining
    ) {
    }

    private record PendingZombieSpawn(
        Zombie zombie,
        int lane,
        float x
    ) {
    }
}
