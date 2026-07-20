package models.games.darkAges;

import java.util.List;

public enum LockedPlantsMode {
    FAMILY(
            "family",
            List.of(
                    "Sun Producer",
                    "Shooter",
                    "Explosive",
                    "Wall-nut",
                    "Strike-through"
            ),
            List.of()
    ),
    FORCED(
            "forced",
            List.of(),
            List.of(
                    3,   // Sun-shroom
                    23,  // Puff-shroom
                    24,  // Fume-shroom
                    26,  // Kernel-pult
                    32,  // Cherry Bomb
                    44,  // Wall-nut
                    53,  // Magnet-shroom
                    60   // Grave Buster
            )
    );
    private final String commandName;
    private final List<String> restrictedFamilies;
    private final List<Integer> forcedPlantIds;

    LockedPlantsMode(
            String commandName,
            List<String> restrictedFamilies,
            List<Integer> forcedPlantIds
    ) {
        this.commandName = commandName;
        this.restrictedFamilies = List.copyOf(restrictedFamilies);
        this.forcedPlantIds = List.copyOf(forcedPlantIds);
    }

    public String getCommandName() {
        return commandName;
    }

    public List<String> getRestrictedFamilies() {
        return restrictedFamilies;
    }

    public List<Integer> getForcedPlantIds() {
        return forcedPlantIds;
    }

    public static LockedPlantsMode fromCommand(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.trim().toLowerCase()) {
            case "1", "family" -> FAMILY;
            case "2", "forced" -> FORCED;
            default -> null;
        };
    }

    public static String normalizeFamily(String family) {
        if (family == null) {
            return "";
        }

        return family
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase();
    }
}
