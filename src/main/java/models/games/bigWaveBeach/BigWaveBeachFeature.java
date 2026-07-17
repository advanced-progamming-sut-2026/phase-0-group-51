package models.games.bigWaveBeach;

import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantTag;
import models.games.GameState;
import models.games.ZombieWaveManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BigWaveBeachFeature {
    private static final int MIN_WATER_COLUMNS = 3;
    private static final int MAX_WATER_COLUMNS = 6;
    private static final int LOW_SHORE_TILE_COUNT = 4;
    private static final int BACKWATER_SPAWN_CHANCE_PERCENT = 65;
    private final GameState state;
    private final ZombieWaveManager waveManager;
    private final Board board;
    private final Random random;
    private final List<Tile> lowShoreTiles = new ArrayList<>();

    public BigWaveBeachFeature(GameState state, ZombieWaveManager waveManager) {
        this(state, waveManager, new Random());
    }

    BigWaveBeachFeature(GameState state, ZombieWaveManager waveManager, Random random) {
        this.state = state;
        this.waveManager = waveManager;
        this.board = state.getBoard();
        this.random = random;
    }

    public void initialize() {
        chooseLowShoreTiles();
        applyWaterColumnCount(MIN_WATER_COLUMNS, 0);
        waveManager.setOnWaveStart(this::onWaveStart);
    }

    private void onWaveStart(int waveNumber) {
        int waterColumns = chooseNextWaterColumnCount();
        applyWaterColumnCount(waterColumns, waveNumber);
        spawnFromLowShores(waveNumber);
    }

    private int chooseNextWaterColumnCount() {
        int current = board.getWaterColumnCount();
        int next;
        do {next = MIN_WATER_COLUMNS + random.nextInt(MAX_WATER_COLUMNS - MIN_WATER_COLUMNS + 1);
        } while (next == current);

        return next;
    }

    private void applyWaterColumnCount(int waterColumns, int waveNumber) {
        int boundedColumns = Math.max(
                MIN_WATER_COLUMNS,
                Math.min(MAX_WATER_COLUMNS, waterColumns)
        );
        int leftmostWaterColumn = board.getColumnCount() - boundedColumns;
        board.setWaterLevel(leftmostWaterColumn);
        drownDryPlants();

        String moment = waveNumber <= 0
                ? "at the start of the level"
                : "at the start of zombie wave " + waveNumber;
        state.logEvent(
                "The tide changed " + moment + ": the rightmost "
                        + boundedColumns + " columns are water. "
                        + "The water wave cannot pass column "
                        + (board.getColumnCount() - MAX_WATER_COLUMNS + 1)
                        + ".\n"
        );
    }

    private void drownDryPlants() {
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                if (tile == null || !tile.isWater() || !tile.hasTopPlant()) {
                    continue;
                }
                Plant plant = tile.getTopPlant();
                boolean isWet = plant.hasTag(PlantTag.WATER) || tile.hasLilyPad();
                if (!isWet) {
                    state.logEvent(
                            "The rising wave drowned " + plant.getName() + " at (" + (column + 1) +
                                    ", " + (lane + 1) + ").\n");
                    plant.die(state);
                }
            }
        }
    }

    private void chooseLowShoreTiles() {
        lowShoreTiles.clear();
        List<Tile> candidates = new ArrayList<>();
        int firstPossibleWaterColumn = board.getColumnCount() - MAX_WATER_COLUMNS;
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = firstPossibleWaterColumn; column < board.getColumnCount(); column++) {
                candidates.add(board.getTile(lane, column));
            }
        }
        Collections.shuffle(candidates, random);
        int count = Math.min(LOW_SHORE_TILE_COUNT, candidates.size());
        for (int i = 0; i < count; i++) {
            Tile tile = candidates.get(i);
            tile.setLowShore(true);
            lowShoreTiles.add(tile);
        }
    }

    private void spawnFromLowShores(int waveNumber) {
        if (random.nextInt(100) >= BACKWATER_SPAWN_CHANCE_PERCENT) {
            return;
        }
        List<Tile> available = new ArrayList<>();
        for (Tile tile : lowShoreTiles) {
            if (tile.isWater() && board.getZombieInPosition(tile.getLane(), tile.getColumn()) == null) {
                available.add(tile);
            }
        }
        if (available.isEmpty()) {
            return;
        }
        Collections.shuffle(available, random);
        int spawnCount = Math.min(1 + random.nextInt(2), available.size());
        for (int i = 0; i < spawnCount; i++) {
            waveManager.spawnZombieFromBackwater(available.get(i), waveNumber);
        }
    }
}
