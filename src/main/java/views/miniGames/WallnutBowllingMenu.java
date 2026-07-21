package views.miniGames;

import controllers.miniGamesController.WallnutBowlingController;
import models.Result;
import models.enums.commands.minigames.WallnutBowlingCommands;
import views.AppMenu;

import java.util.Scanner;
import java.util.regex.Matcher;

public class WallnutBowllingMenu implements AppMenu {
    private final WallnutBowlingController controller = new WallnutBowlingController();
    @Override
    public void check(Scanner scanner) {
        String input = scanner.nextLine().trim();
        if (WallnutBowlingCommands.ROLL_WALLNUT.matches(input)) {
            Matcher matcher = WallnutBowlingCommands.ROLL_WALLNUT.getMatcher(input);
            int[] coordinates = parseCoordinates(matcher);
            if (coordinates != null) {
                print(controller.rollWallnut(coordinates[0], coordinates[1]));
            }
        } else if (WallnutBowlingCommands.ADVANCE_TIME.matches(input)) {
            Matcher matcher = WallnutBowlingCommands.ADVANCE_TIME.getMatcher(input);
            Integer count = parseInteger(matcher, "count");
            if (count != null) print(controller.advanceTime(count));
        } else if (WallnutBowlingCommands.SHOW_CONVEYOR.matches(input)) {
            print(controller.showConveyor());
        } else if (WallnutBowlingCommands.COLLECT_LOOT.matches(input)) {
            Matcher matcher = WallnutBowlingCommands.COLLECT_LOOT.getMatcher(input);
            int[] coordinates = parseCoordinates(matcher);
            if (coordinates != null) {
                print(controller.collectLoot(coordinates[0], coordinates[1]));
            }
        } else if (WallnutBowlingCommands.SHOW_MAP.matches(input)) {
            print(controller.showMap());
        } else if (WallnutBowlingCommands.SHOW_STATUS.matches(input)) {
            print(controller.showStatus());
        } else if (WallnutBowlingCommands.CURRENT_MENU.matches(input)) {
            print(controller.showCurrentMenu());
        } else if (WallnutBowlingCommands.EXIT_MENU.matches(input)) {
            print(controller.exitMenu());
        } else {
            invalidCommand();
        }
    }

    private int[] parseCoordinates(Matcher matcher) {
        Integer x = parseInteger(matcher, "x");
        Integer y = parseInteger(matcher, "y");
        if (x == null || y == null) return null;
        return new int[]{x, y};
    }

    private Integer parseInteger(Matcher matcher, String groupName) {
        if (matcher == null) {
            invalidCommand();
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(groupName));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            invalidCommand();
            return null;
        }
    }

    private void print(Result result) {
        System.out.print(result.message());
    }
}
