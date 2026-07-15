package models.games.frostbite;

import models.Zombie.ZombieType;

import java.util.List;

public record FrostbiteLevelConfig(
        double icyWindChance,
        int minimumWindLanes,
        int maximumWindLanes,
        List<IceFloorPlacement> iceFloors,
        List<FrozenZombiePlacement> frozenZombies
) {
    public FrostbiteLevelConfig {
        if (icyWindChance < 0 || icyWindChance > 1) {
            throw new IllegalArgumentException("Icy wind chance must be between 0 and 1");
        }
        if (minimumWindLanes < 0 || maximumWindLanes < minimumWindLanes) {
            throw new IllegalArgumentException("Invalid icy wind lane limits");
        }
        iceFloors = iceFloors == null ? List.of() : List.copyOf(iceFloors);
        frozenZombies = frozenZombies == null ? List.of() : List.copyOf(frozenZombies);
    }

    public static FrostbiteLevelConfig none() {
        return new FrostbiteLevelConfig(0, 0, 0, List.of(), List.of());
    }

    public record IceFloorPlacement(
            int laneIndex,
            int columnIndex,
            IceFloorDirection direction
    ) {
    }

    public record FrozenZombiePlacement(
            ZombieType zombieType,
            int laneIndex,
            int columnIndex
    ) {
    }
}
