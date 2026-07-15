package models.games.frostbite;

import Data.loader.ZombieRegistry;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FrostbiteCavesFeature {
    private final GameState state;
    private final FrostbiteLevelConfig config;
    private final Random random;

    public FrostbiteCavesFeature(GameState state, FrostbiteLevelConfig config) {
        this(state, config, new Random());
    }

    FrostbiteCavesFeature(GameState state, FrostbiteLevelConfig config, Random random) {
        this.state = state;
        this.config = config;
        this.random = random;
    }

    public void initialize() {
        placeIceFloors();
        spawnFrozenZombies();
    }

    public void onWaveStart(int waveNumber) {
        if (config.icyWindChance() <= 0 || random.nextDouble() >= config.icyWindChance()) {
            return;
        }
        List<Integer> lanes = chooseWindLanes();
        for (int lane : lanes) {
            state.getBoard().addFrostToLane(lane, state, "icy wind");
        }
        state.logEvent("Icy wind wooopshed rows " + formatLanes(lanes) + " at wave " + waveNumber + ".\n");
    }

    private void placeIceFloors() {
        Board board = state.getBoard();
        for (FrostbiteLevelConfig.IceFloorPlacement placement : config.iceFloors()) {
            board.placeIceFloor(
                    placement.laneIndex(),
                    placement.columnIndex(),
                    placement.direction()
            );
        }
    }

    private void spawnFrozenZombies() {
        for (FrostbiteLevelConfig.FrozenZombiePlacement placement : config.frozenZombies()) {
            Zombie zombie = ZombieRegistry.spawn(placement.zombieType().getAlias());
            zombie.setLane(placement.laneIndex());
            zombie.setX(placement.columnIndex());
            zombie.freezeInIce();
            state.addZombie(zombie);
            state.logEvent("Frozen zombie " + zombie.getAlias() + " started at ("
                    + (placement.columnIndex() + 1) + ", " + (placement.laneIndex() + 1) + ").\n");
        }
    }

    private List<Integer> chooseWindLanes() {
        int laneCount = state.getBoard().getLaneCount();
        int minimum = Math.min(config.minimumWindLanes(), laneCount);
        int maximum = Math.min(config.maximumWindLanes(), laneCount);
        int count = minimum;
        if (maximum > minimum) {
            count += random.nextInt(maximum - minimum + 1);
        }
        List<Integer> lanes = new ArrayList<>();
        for (int lane = 0; lane < laneCount; lane++) {
            lanes.add(lane);
        }
        Collections.shuffle(lanes, random);
        return new ArrayList<>(lanes.subList(0, count));
    }

    private String formatLanes(List<Integer> lanes) {
        List<Integer> userLanes = lanes.stream()
                .map(lane -> lane + 1)
                .sorted()
                .toList();
        return userLanes.toString();
    }
}
