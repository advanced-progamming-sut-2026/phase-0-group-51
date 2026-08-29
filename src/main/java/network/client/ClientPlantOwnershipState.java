package network.client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ClientPlantOwnershipState {
    private static volatile Set<Integer> unlockedPlantIds =
            Set.of();
    private static volatile boolean loaded;

    private ClientPlantOwnershipState() {
    }

    public static synchronized void replaceWith(
            Collection<Integer> plantIds
    ) {
        LinkedHashSet<Integer> copy =
                new LinkedHashSet<>();

        if (plantIds != null) {
            copy.addAll(plantIds);
        }

        unlockedPlantIds =
                Collections.unmodifiableSet(copy);
        loaded = true;
    }

    public static Set<Integer> snapshot() {
        return unlockedPlantIds;
    }

    public static boolean isUnlocked(int plantId) {
        return unlockedPlantIds.contains(plantId);
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static synchronized void clear() {
        unlockedPlantIds = Set.of();
        loaded = false;
    }
}
