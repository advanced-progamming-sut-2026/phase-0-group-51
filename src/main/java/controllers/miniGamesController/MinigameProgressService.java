package controllers.miniGamesController;

import models.App;
import models.Result;
import models.User;
import models.minigames.MinigameType;
import network.client.ClientMinigameState;
import network.client.ClientNetworkManager;
import network.protocol.minigame.MinigameProgressResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class MinigameProgressService {
    private final ClientNetworkManager networkManager;

    public MinigameProgressService() {
        this(null);
    }

    public MinigameProgressService(
            ClientNetworkManager networkManager
    ) {
        this.networkManager = networkManager;
    }

    public Result checkStageAccess(
            MinigameType type,
            int stageNumber
    ) {
        if (type == null) {
            return new Result(false, "Minigame type is required.\n", null);
        }
        if (stageNumber < 1 || stageNumber > 3) {
            return new Result(
                    false,
                    type.getDisplayName()
                            + " stage must be 1, 2, or 3.\n",
                    null
            );
        }

        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return new Result(
                    false,
                    "You must log in before starting a minigame.\n",
                    null
            );
        }

        if (!ClientMinigameState.isLoaded()) {
            return new Result(
                    false,
                    "Minigame progress is still loading from the server.\n",
                    null
            );
        }

        if (!ClientMinigameState.isStageUnlocked(type, stageNumber)) {
            return new Result(
                    false,
                    type.getDisplayName()
                            + " stage " + stageNumber + " is locked.\n"
                            + "Complete stage " + (stageNumber - 1)
                            + " first.\n",
                    null
            );
        }

        return new Result(true, "", null);
    }

    public String recordWin(
            MinigameType type,
            int stageNumber
    ) {
        if (networkManager == null) {
            return "Minigame progress requires the server-backed graphical client.\n";
        }

        try {
            ClientMinigameState.noteLocalWin(type, stageNumber);
            recordWinAsync(type, stageNumber)
                    .whenComplete(
                            (response, throwable) -> {
                                if (throwable != null
                                        || response == null
                                        || !response.isSuccess()) {
                                    ClientMinigameState.clear();
                                }
                            }
                    );
            return "Minigame progress is being saved on the server.\n";
        } catch (RuntimeException exception) {
            ClientMinigameState.clear();
            return "Minigame progress could not be sent to the server.\n";
        }
    }

    public CompletableFuture<MinigameProgressResponse> recordWinAsync(
            MinigameType type,
            int stageNumber
    ) {
        if (networkManager == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "Server-backed minigame progress is unavailable."
                    )
            );
        }

        return networkManager.ensureConnectedAsync()
                .thenCompose(
                        ignored -> {
                            try {
                                return networkManager
                                        .getMinigameClientService()
                                        .completeStage(type, stageNumber);
                            } catch (IOException | RuntimeException exception) {
                                return CompletableFuture.failedFuture(exception);
                            }
                        }
                )
                .thenApply(
                        response -> {
                            if (response != null && response.isSuccess()) {
                                ClientMinigameState.apply(response);
                            }
                            return response;
                        }
                );
    }

    public String formatStages(
            int userId,
            MinigameType type
    ) {
        if (!ClientMinigameState.isLoaded()) {
            return "  Progress is loading from the server.\n";
        }

        int highestUnlocked =
                ClientMinigameState.highestUnlockedStage(type);
        int highestCompleted =
                ClientMinigameState.highestCompletedStage(type);

        StringBuilder output = new StringBuilder();
        for (int stage = 1; stage <= 3; stage++) {
            String status;
            if (stage <= highestCompleted) {
                status = "COMPLETED";
            } else if (stage <= highestUnlocked) {
                status = "UNLOCKED";
            } else {
                status = "LOCKED";
            }

            output.append("  Stage ")
                    .append(stage)
                    .append(" [")
                    .append(status)
                    .append("]\n");
        }
        return output.toString();
    }
}
