package network.client;

import models.App;
import models.User;
import models.minigames.MinigameType;
import network.protocol.minigame.MinigameProgressDto;
import network.protocol.minigame.MinigameProgressResponse;

import java.util.EnumMap;
import java.util.Map;

public final class ClientMinigameState {
    private static boolean loaded;
    private static final Map<MinigameType, MinigameProgressDto> progress =
            new EnumMap<>(MinigameType.class);

    private ClientMinigameState() {
    }

    public static synchronized void apply(
            MinigameProgressResponse response
    ) {
        if (response == null || !response.isSuccess()) {
            return;
        }

        progress.clear();
        if (response.getProgress() != null) {
            for (MinigameProgressDto item : response.getProgress()) {
                if (item != null && item.getType() != null) {
                    progress.put(item.getType(), item);
                }
            }
        }
        loaded = true;

        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            user.setMiniGamesPlayed(response.getMiniGamesPlayed());
        }
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized int highestUnlockedStage(
            MinigameType type
    ) {
        MinigameProgressDto value = progress.get(type);
        return value == null ? 1 : Math.max(1, value.getHighestUnlockedStage());
    }

    public static synchronized int highestCompletedStage(
            MinigameType type
    ) {
        MinigameProgressDto value = progress.get(type);
        return value == null ? 0 : Math.max(0, value.getHighestCompletedStage());
    }

    public static synchronized boolean isStageUnlocked(
            MinigameType type,
            int stageNumber
    ) {
        return loaded
                && type != null
                && stageNumber >= 1
                && stageNumber <= highestUnlockedStage(type);
    }

    public static synchronized void noteLocalWin(
            MinigameType type,
            int stageNumber
    ) {
        if (!loaded
                || type == null
                || stageNumber < 1
                || stageNumber > 3) {
            return;
        }

        int oldUnlocked = highestUnlockedStage(type);
        int oldCompleted = highestCompletedStage(type);
        int newCompleted = Math.max(oldCompleted, stageNumber);
        int newUnlocked = Math.max(
                oldUnlocked,
                stageNumber < 3 ? stageNumber + 1 : stageNumber
        );

        progress.put(
                type,
                new MinigameProgressDto(
                        type,
                        newUnlocked,
                        newCompleted
                )
        );
    }

    public static synchronized void clear() {
        loaded = false;
        progress.clear();
    }
}
