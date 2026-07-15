package controllers;

import Data.database.LeaderBoardRepository;
import models.App;
import models.Result;
import models.enums.Menu;
import models.leaderBoard.LeaderBoard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LeaderboardMenuController {
    private final LeaderBoardRepository repository = new LeaderBoardRepository();

    public Result showLeaderboard() {
        return showLeaderboard("progress", false);
    }

    public Result showLeaderboard(String column, boolean ascending) {
        List<LeaderBoard> entries;
        try {
            entries = new ArrayList<>(repository.getAllEntries());
        } catch (IllegalStateException exception) {
            return failure(exception.getMessage() + "\n");
        }

        Comparator<LeaderBoard> comparator = comparatorFor(column);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(
                LeaderBoard::username,
                String.CASE_INSENSITIVE_ORDER
        );
        entries.sort(comparator);

        return success(formatLeaderboard(entries, column, ascending));
    }

    public Result showCurrentMenu() {
        return success("You are now in the Leaderboard menu.\n");
    }

    public Result exitMenu() {
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
        return success("You returned to the Main Menu.\n");
    }

    private Comparator<LeaderBoard> comparatorFor(String column) {
        return switch (column.toLowerCase(Locale.ROOT)) {
            case "username" -> Comparator.comparing(
                    LeaderBoard::username,
                    String.CASE_INSENSITIVE_ORDER
            );
            case "progress" -> Comparator.comparingInt(LeaderBoard::progressRank);
            case "minigames" -> Comparator.comparingInt(
                    LeaderBoard::minigamesCompleted
            );
            case "daily-quests" -> Comparator.comparingInt(
                    LeaderBoard::dailyQuestsCompleted
            );
            case "non-daily-quests" -> Comparator.comparingInt(
                    LeaderBoard::nonDailyQuestsCompleted
            );
            case "score" -> Comparator.comparingInt(LeaderBoard::highestScore);
            default -> throw new IllegalArgumentException(
                    "Unknown leaderboard column."
            );
        };
    }

    private String formatLeaderboard(
            List<LeaderBoard> entries,
            String column,
            boolean ascending
    ) {
        StringBuilder output = new StringBuilder();
        output.append("===== LEADERBOARD =====\n")
                .append("Sorted by ")
                .append(column)
                .append(ascending ? " ascending\n" : " descending\n")
                .append(String.format(
                        Locale.US,
                        "%-4s %-18s %-22s %10s %12s %16s %12s%n",
                        "Rank",
                        "Username",
                        "Last completed",
                        "Minigames",
                        "Daily quests",
                        "Non-daily quests",
                        "High score"
                ))
                .append("---------------------------------------------------------------------------------------\n");

        if (entries.isEmpty()) {
            output.append("No registered users found.\n");
            return output.toString();
        }

        for (int i = 0; i < entries.size(); i++) {
            LeaderBoard entry = entries.get(i);
            output.append(String.format(
                    Locale.US,
                    "%-4d %-18s %-22s %10d %12d %16d %12d%n",
                    i + 1,
                    entry.username(),
                    entry.lastCompleted(),
                    entry.minigamesCompleted(),
                    entry.dailyQuestsCompleted(),
                    entry.nonDailyQuestsCompleted(),
                    entry.highestScore()
            ));
        }

        return output.toString();
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
}
