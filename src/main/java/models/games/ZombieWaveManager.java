package models.games;

import data.loader.ZombieRegistry;
import lombok.Getter;
import lombok.Setter;
import models.zombie.Zombie;
import models.zombie.ZombieType;
import models.items.Wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

@Getter
@Setter
public class ZombieWaveManager {

    private final GameState gs;
    private final List<ZombieType> allowedAliases; // this chapter zombies
    private final int totalWaves;
    private final float baseDifficulty;        // wave 1
    private final Random random;

    private boolean started;
    private boolean tornadoFinalWave = false;
    private IntConsumer onWaveStart = null;    //(graves, water level, icy wind)

    private final List<Wave> waves = new ArrayList<>();
    private Wave currentWave = null;
    private float currentDifficulty = 0f;
    private int firstWaveDelayTicks = 0;

    public ZombieWaveManager(GameState gs, List<ZombieType> allowedAliases,
                             int totalWaves, float baseDifficulty) {
        this(gs, allowedAliases, totalWaves, baseDifficulty, true, new Random());
    }

    public ZombieWaveManager(GameState gs, List<ZombieType> allowedAliases,
                             int totalWaves, float baseDifficulty,
                             boolean autoStart, Random random) {
        this.gs = gs;
        this.allowedAliases = allowedAliases;
        this.totalWaves = totalWaves;
        this.baseDifficulty = baseDifficulty;
        this.random = random;
        this.started = autoStart;
    }


    public void start() {
        started = true;
    }


    public void releaseTheNuke() {
        for (Zombie zombie : new ArrayList<>(gs.getZombiesInTheGame())) {
            zombie.killInstantly(gs);
        }
    }

    public void onTick() {
        if (!started || allWavesSent()) {
            return;
        }
        if (currentWave == null) {
            if (firstWaveDelayTicks > 0) {
                firstWaveDelayTicks--;
                return;
            }
            startNextWave();
            return;
        }
        if (currentWave.isBroken()) {
            startNextWave();
        }
    }

    public boolean allWavesSent() {
        return waves.size() >= totalWaves;
    }

    //player wins
    public boolean isLevelCleared() {
        if (!allWavesSent()) {
            return false;
        }
        for (Zombie zombie : gs.getZombiesInTheGame()) {
            if (!zombie.isDead()) {
                return false;
            }
        }
        return true;
    }

    public int getCurrentWaveNumber() {
        return currentWave == null ? 0 : currentWave.getWaveNumber();
    }



    private void startNextWave() {
        int number = waves.size() + 1;
        boolean finalWave = number == totalWaves;

        if (number == 1) {
            currentDifficulty = baseDifficulty;
        } else if (finalWave) {
            currentDifficulty *= 2f;    //super-wave
        } else {
            currentDifficulty *= 1.25f;
        }

        if (finalWave) {
            gs.logEvent("The final wave has come.\n");
        } else {
            gs.logEvent("Wave " + number + " started.\n");
        }

        currentWave = new Wave(number, currentDifficulty, finalWave);
        waves.add(currentWave);
        if (onWaveStart != null) {
            onWaveStart.accept(number);
        }
        spawnZombies(currentWave);
    }

    private void spawnZombies(Wave wave) {
        float remaining = wave.getDifficulty();
        int lanes = gs.getBoard().getLaneCount();
        int spawnColumn = gs.getBoard().getColumnCount() - 1;

        while (true) {
            Zombie zombie = pickAffordableZombie(remaining);
            if (zombie == null) {
                break;
            }

            int lane = random.nextInt(lanes);
            float x = spawnColumn;
            if (wave.isFinalWave() && tornadoFinalWave && random.nextBoolean()) {
                int movedColumns = 1 + random.nextInt(4);
                x -= movedColumns;
                gs.logEvent("A tornado moved " + zombie.getAlias() + " "
                        + movedColumns + " columns forward.\n");
            }

            zombie.setGlowing(random.nextInt(100) < 5);
            zombie.setLane(lane);
            zombie.setX(Math.max(0f, x));
            gs.addZombie(zombie);
            wave.addZombie(zombie);
            remaining -= zombie.getWavePointCost();

            gs.logEvent("Zombie " + zombie.getAlias()
                + " spawned at wave " + wave.getWaveNumber()
                + " in lane " + (lane + 1)
                + " which cost " + zombie.getWavePointCost() + ".\n");
        }
    }

    private Zombie pickAffordableZombie(float remainingBudget) {
        List<Zombie> affordable = new ArrayList<>();
        long weightSum = 0;
        for (ZombieType alias : allowedAliases) {
            Zombie template = ZombieRegistry.getTemplate(alias.getAlias());
            if (template == null) {
                continue;
            }
            Zombie candidate = template.copy();
            if (candidate.getWavePointCost() <= remainingBudget) {
                affordable.add(candidate);
                weightSum += Math.max(1, candidate.getWeight());
            }
        }
        if (affordable.isEmpty() || weightSum <= 0) {
            return null;
        }
        long roll = (long) (random.nextDouble() * weightSum);
        for (Zombie candidate : affordable) {
            roll -= Math.max(1, candidate.getWeight());
            if (roll < 0) {
                return candidate;
            }
        }
        return affordable.get(affordable.size() - 1);
    }
}
