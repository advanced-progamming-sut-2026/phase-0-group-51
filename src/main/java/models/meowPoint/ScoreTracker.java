package models.meowPoint;

import lombok.Getter;
import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;
import models.items.Mower;
import models.items.Wave;
import java.util.*;

@Getter
public final class ScoreTracker {
    private record SpawnInfo(int spawnTick, int waveNumber) {
    }
    private final Map<Zombie, SpawnInfo> activeZombies = new IdentityHashMap<>();
    private final Map<Integer, Integer> waveStartTicks = new HashMap<>();
    private final Set<Integer> rewardedWaves = new HashSet<>();

    private boolean finished;
    private int zombieValuePoints;
    private int quickKillBonus;
    private int simultaneousKillBonus;
    private int fastWaveBonus;
    private int gardenPreservationBonus;

    public void observeWaveAndSpawns(GameState state) {
        if (finished || state == null || state.getZombieWaveManager() == null) {
            return;
        }
        int currentWave = state.getZombieWaveManager().getCurrentWaveNumber();
        if (currentWave > 0) {
            waveStartTicks.putIfAbsent(currentWave, state.getTickCounter());
        }
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie == null || zombie.isDead() || !zombie.isQuestEligible()) {
                continue;
            }
            activeZombies.putIfAbsent(zombie,
                    new SpawnInfo(state.getTickCounter(), currentWave)
            );
        }
    }

    public void observeDeathsAndWaveCompletion(GameState state) {
        if (finished || state == null) {
            return;
        }
        Set<Zombie> currentlyPresent = Collections.newSetFromMap(new IdentityHashMap<>());
        currentlyPresent.addAll(state.getZombiesInTheGame());
        List<Map.Entry<Zombie, SpawnInfo>> deaths = new ArrayList<>();
        for (Map.Entry<Zombie, SpawnInfo> entry : activeZombies.entrySet()) {
            Zombie zombie = entry.getKey();
            if (zombie.isDead() || !currentlyPresent.contains(zombie)) {
                deaths.add(entry);
            }
        }

        int killsThisTick = 0;
        for (Map.Entry<Zombie, SpawnInfo> death : deaths) {
            recordKill(death.getKey(), death.getValue(), state);
            activeZombies.remove(death.getKey());
            killsThisTick++;
        }

        if (killsThisTick > 1) {
            simultaneousKillBonus += ScoringRules.SAME_TICK_EXTRA_KILL_BONUS
                    * killsThisTick * (killsThisTick - 1) / 2;
        }

        awardCompletedWaves(state);
    }

    public int currentTotal() {
        return zombieValuePoints
                + quickKillBonus
                + simultaneousKillBonus
                + fastWaveBonus
                + gardenPreservationBonus;
    }

    public String liveSummary() {
        return "===== CURRENT MEOWPOINT =====\n"
                + "Zombie value points: " + zombieValuePoints + "\n"
                + "Quick-kill bonus: " + quickKillBonus + "\n"
                + "Simultaneous-kill bonus: " + simultaneousKillBonus + "\n"
                + "Fast-wave bonus: " + fastWaveBonus + "\n"
                + "Current total: " + currentTotal() + "\n";
    }

    public ScoreBreakdown finish(GameState state, boolean won) {
        if (!finished) {
            gardenPreservationBonus = won ? calculateGardenPreservation(state) : 0;
            finished = true;
        }
        return new ScoreBreakdown(
                zombieValuePoints,
                quickKillBonus,
                simultaneousKillBonus,
                fastWaveBonus,
                gardenPreservationBonus,
                currentTotal(),
                won
        );
    }

    private void recordKill(Zombie zombie, SpawnInfo spawnInfo, GameState state) {
        zombieValuePoints += ScoringRules.ZOMBIE_BASE_POINTS
                + Math.max(0, Math.round(zombie.getWavePointCost()));

        int elapsedTicks = Math.max(0, state.getTickCounter() - spawnInfo.spawnTick());
        int windowTicks = ScoringRules.QUICK_KILL_WINDOW_SECONDS
                * Math.max(1, state.getTicksPerSecond());
        if (elapsedTicks < windowTicks) {
            int remainingTicks = windowTicks - elapsedTicks;
            quickKillBonus += ScoringRules.QUICK_KILL_MAX_BONUS
                    * remainingTicks / windowTicks;
        }
    }

    private void awardCompletedWaves(GameState state) {
        if (state.getZombieWaveManager() == null) {
            return;
        }

        for (Wave wave : state.getZombieWaveManager().getWaves()) {
            int number = wave.getWaveNumber();
            if (!wave.allDead() || rewardedWaves.contains(number)) {
                continue;
            }

            int startTick = waveStartTicks.getOrDefault(number, state.getTickCounter());
            int elapsedTicks = Math.max(0, state.getTickCounter() - startTick);
            int windowTicks = ScoringRules.FAST_WAVE_WINDOW_SECONDS
                    * Math.max(1, state.getTicksPerSecond());
            if (elapsedTicks < windowTicks) {
                int remainingTicks = windowTicks - elapsedTicks;
                fastWaveBonus += ScoringRules.FAST_WAVE_MAX_BONUS
                        * remainingTicks / windowTicks;
            }
            rewardedWaves.add(number);
        }
    }

    private int calculateGardenPreservation(GameState state) {
        if (state == null) {
            return 0;
        }

        int survivingPlants = 0;
        for (int lane = 0; lane < state.getBoard().getLaneCount(); lane++) {
            for (int column = 0; column < state.getBoard().getColumnCount(); column++) {
                Tile tile = state.getBoard().getTile(lane, column);
                if (tile != null
                        && tile.hasPlant()
                        && !tile.getPlant().isMarkedForRemoval()) {
                    survivingPlants++;
                }
            }
        }

        int unusedMowers = 0;
        for (Mower mower : state.getLawnMowers()) {
            if (!mower.isActivated() && !mower.isDestroyed()) {
                unusedMowers++;
            }
        }

        return ScoringRules.WIN_BONUS
                + unusedMowers * ScoringRules.UNUSED_MOWER_BONUS
                + survivingPlants * ScoringRules.SURVIVING_PLANT_BONUS
                + Math.max(0, state.getSun()) / ScoringRules.SUN_DIVISOR;
    }
}
