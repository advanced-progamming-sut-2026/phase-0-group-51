package controllers;

import models.App;
import models.Result;
import models.enums.Menu;
import models.leaderBoard.LeaderBoard;
import network.client.ClientNetworkManager;
import network.protocol.leaderboard.LeaderboardEntryDto;
import network.protocol.leaderboard.LeaderboardResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LeaderboardMenuController {
    private final ClientNetworkManager networkManager;

    public LeaderboardMenuController() {
        this.networkManager = null;
    }

    public LeaderboardMenuController(
            ClientNetworkManager networkManager
    ) {
        this.networkManager = networkManager;
    }

    public Result showLeaderboard() {
        return failure(
                "Leaderboard data is server-backed. "
                        + "Open the graphical leaderboard.\n"
        );
    }

    public Result showLeaderboard(String column, boolean ascending) {
        return showLeaderboard();
    }

    public CompletableFuture<List<LeaderBoard>>
    loadLeaderboardEntriesAsync() {
        return loadLeaderboardEntriesAsync("score", false);
    }

    public CompletableFuture<List<LeaderBoard>>
    loadLeaderboardEntriesAsync(
            String column,
            boolean ascending
    ) {
        if (networkManager == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "Leaderboard network manager is unavailable."
                    )
            );
        }

        return networkManager.ensureConnectedAsync()
                .thenCompose(ignored -> requestLeaderboard())
                .thenApply(response -> {
                    if (response == null || !response.isSuccess()) {
                        throw new CompletionException(
                                new IllegalStateException(
                                        response == null
                                                ? "Leaderboard could not be loaded."
                                                : response.getMessage()
                                )
                        );
                    }

                    List<LeaderBoard> entries = new ArrayList<>();
                    for (LeaderboardEntryDto dto : response.getEntries()) {
                        if (dto != null) {
                            entries.add(toModel(dto));
                        }
                    }

                    return sortLeaderboardEntries(
                            entries,
                            column,
                            ascending
                    );
                });
    }

    public List<LeaderBoard> sortLeaderboardEntries(
            List<LeaderBoard> values,
            String column,
            boolean ascending
    ) {
        List<LeaderBoard> entries = values == null
                ? new ArrayList<>()
                : new ArrayList<>(values);

        Comparator<LeaderBoard> comparator = comparatorFor(column);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        comparator = comparator.thenComparing(
                LeaderBoard::username,
                String.CASE_INSENSITIVE_ORDER
        );
        entries.sort(comparator);
        return entries;
    }

    public Result showCurrentMenu() {
        return success("You are now in the Leaderboard menu.\n");
    }

    public Result exitMenu() {
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
        return success("You returned to the Main Menu.\n");
    }

    private CompletableFuture<LeaderboardResponse> requestLeaderboard() {
        try {
            return networkManager
                    .getLeaderboardClientService()
                    .getLeaderboard();
        } catch (IOException | RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private LeaderBoard toModel(LeaderboardEntryDto dto) {
        String lastCompleted = dto.getLastCompleted();
        if (lastCompleted == null || lastCompleted.isBlank()) {
            lastCompleted = "None";
        }

        return new LeaderBoard(
                dto.getUsername(),
                lastCompleted,
                Math.max(0, dto.getCompletedChapter()),
                Math.max(0, dto.getCompletedLevel()),
                Math.max(0, dto.getMinigamesCompleted()),
                Math.max(0, dto.getDailyQuestsCompleted()),
                Math.max(0, dto.getNonDailyQuestsCompleted()),
                Math.max(0, dto.getHighestScore())
        );
    }

    private Comparator<LeaderBoard> comparatorFor(String column) {
        String normalized = column == null
                ? "score"
                : column.toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "username" -> Comparator.comparing(
                    LeaderBoard::username,
                    String.CASE_INSENSITIVE_ORDER
            );
            case "progress" -> Comparator.comparingInt(
                    LeaderBoard::progressRank
            );
            case "minigames" -> Comparator.comparingInt(
                    LeaderBoard::minigamesCompleted
            );
            case "quests" -> Comparator.comparingInt(
                    entry -> entry.dailyQuestsCompleted()
                            + entry.nonDailyQuestsCompleted()
            );
            case "daily-quests" -> Comparator.comparingInt(
                    LeaderBoard::dailyQuestsCompleted
            );
            case "non-daily-quests" -> Comparator.comparingInt(
                    LeaderBoard::nonDailyQuestsCompleted
            );
            case "score" -> Comparator.comparingInt(
                    LeaderBoard::highestScore
            );
            default -> throw new IllegalArgumentException(
                    "Unknown leaderboard column."
            );
        };
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
}
