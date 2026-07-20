package models.games.specialLevelConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SaveOurSeedsConfig(
        List<ProtectedPlantPlacement> protectedPlants,
        RandomPlacementRule randomPlacementRule) {
    public SaveOurSeedsConfig(List<ProtectedPlantPlacement> protectedPlants) {
        this(protectedPlants, null);
    }

    public SaveOurSeedsConfig {
        protectedPlants = protectedPlants == null
                ? List.of()
                : List.copyOf(protectedPlants);

        if (!protectedPlants.isEmpty() && randomPlacementRule != null) {
            throw new IllegalArgumentException(
                    "Use either fixed or random protected-plant placement, not both."
            );
        }

        Set<String> occupiedCoordinates = new HashSet<>();
        for (ProtectedPlantPlacement placement : protectedPlants) {
            if (placement == null) {
                throw new IllegalArgumentException(
                        "Protected plant placement cannot be null."
                );
            }
            String coordinate = placement.column() + ":" + placement.row();
            if (!occupiedCoordinates.add(coordinate)) {
                throw new IllegalArgumentException(
                        "Two protected plants cannot share the same tile."
                );
            }
        }
    }

    public static SaveOurSeedsConfig none() {
        return new SaveOurSeedsConfig(List.of(), null);
    }

    public static SaveOurSeedsConfig protect(ProtectedPlantPlacement... placements) {
        if (placements == null) {
            return none();
        }
        return new SaveOurSeedsConfig(Arrays.asList(placements), null);
    }

    public static SaveOurSeedsConfig randomPlants(
            int plantId,
            int count,
            int minimumColumn,
            int excludedRightColumns) {
        return randomPlants(
                plantId,
                count,
                1,
                minimumColumn,
                excludedRightColumns,
                true
        );
    }

    public static SaveOurSeedsConfig randomPlants(
            int plantId,
            int count,
            int level,
            int minimumColumn,
            int excludedRightColumns,
            boolean distinctRows) {
        return new SaveOurSeedsConfig(
                List.of(),
                new RandomPlacementRule(
                        plantId,
                        count,
                        level,
                        minimumColumn,
                        excludedRightColumns,
                        distinctRows
                )
        );
    }

    public boolean isConfigured() {
        return !protectedPlants.isEmpty() || randomPlacementRule != null;
    }

    public boolean usesRandomPlacement() {
        return randomPlacementRule != null;
    }

    public static ProtectedPlantPlacement plant(int plantId, int column, int row) {
        return new ProtectedPlantPlacement(plantId, column, row, 1);
    }

    public static ProtectedPlantPlacement plant(int plantId, int column, int row, int level) {
        return new ProtectedPlantPlacement(plantId, column, row, level);
    }

    public record RandomPlacementRule(
            int plantId,
            int count,
            int level,
            int minimumColumn,
            int excludedRightColumns,
            boolean distinctRows) {
        public RandomPlacementRule {
            if (plantId <= 0) {
                throw new IllegalArgumentException(
                        "Protected plant id must be positive."
                );
            }
            if (count <= 0) {
                throw new IllegalArgumentException(
                        "Random protected plant count must be positive."
                );
            }
            if (level <= 0) {
                throw new IllegalArgumentException(
                        "Protected plant level must be positive."
                );
            }
            if (minimumColumn <= 0) {
                throw new IllegalArgumentException(
                        "Minimum protected plant column must be positive."
                );
            }
            if (excludedRightColumns < 0) {
                throw new IllegalArgumentException(
                        "Excluded right columns cannot be negative."
                );
            }
        }
    }

    public record ProtectedPlantPlacement(int plantId, int column, int row, int level) {
        public ProtectedPlantPlacement {
            if (plantId <= 0) {
                throw new IllegalArgumentException(
                        "Protected plant id must be positive."
                );
            }
            if (column <= 0 || row <= 0) {
                throw new IllegalArgumentException(
                        "Protected plant coordinates are one-based and must be positive."
                );
            }
            if (level <= 0) {
                throw new IllegalArgumentException(
                        "Protected plant level must be positive."
                );
            }
        }
    }
}
