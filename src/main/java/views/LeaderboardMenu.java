package views;
import controllers.LeaderboardMenuController;
import models.Result;
import models.enums.commands.LeaderBoardCommands;

import java.util.Scanner;

public class LeaderboardMenu implements AppMenu{
    private final LeaderboardMenuController controller = new LeaderboardMenuController();

    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if (LeaderBoardCommands.SHOW_LEADERBOARD.matches(line)) {
            print(controller.showLeaderboard());
        } else if (LeaderBoardCommands.SORT_LEADERBOARD.matches(line)) {
            String column = LeaderBoardCommands.SORT_LEADERBOARD.getGroup(line, "column");
            String order = LeaderBoardCommands.SORT_LEADERBOARD.getGroup(line, "order");
            print(controller.showLeaderboard(column, order.equalsIgnoreCase("asc")));
        } else if (LeaderBoardCommands.CURRENT_MENU.matches(line)) {
            print(controller.showCurrentMenu());
        } else if (LeaderBoardCommands.EXIT_MENU.matches(line)) {
            print(controller.exitMenu());
        } else {
            invalidCommand();
        }
    }

    private void print(Result result) {
        System.out.print(result.message());
    }
}
