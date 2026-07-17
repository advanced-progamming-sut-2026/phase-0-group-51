package views.miniGames;

import controllers.miniGamesController.IZombieController;
import models.Result;
import models.enums.commands.minigames.IZombieCommands;
import views.AppMenu;

import java.util.Scanner;
import java.util.regex.Matcher;

public class IZombieMenu implements AppMenu {
    private final IZombieController controller;
    public IZombieMenu() {
        this.controller = new IZombieController();
    }
    @Override
    public void check(Scanner scanner) {
        String input = scanner.nextLine().trim();
        if (IZombieCommands.PLACE_ZOMBIE.matches(input)) {
            handlePlaceZombie(input);
        } else if (IZombieCommands.SHOW_ROSTER.matches(input)) {
            Result result = controller.showRoster();
            System.out.println(result.message());
        } else if (IZombieCommands.ADVANCE_TIME.matches(input)) {
            handleAdvanceTime(input);
        } else if (IZombieCommands.SHOW_STATUS.matches(input)) {
            Result result = controller.showStatus();
            System.out.println(result.message());
        } else if (IZombieCommands.SHOW_MAP.matches(input)) {
            Result result = controller.showMap();
            System.out.println(result.message());
        } else if (IZombieCommands.CURRENT_MENU.matches(input)) {
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        } else if (IZombieCommands.EXIT_MENU.matches(input)) {
            Result result = controller.exitMenu();
            System.out.println(result.message());
        } else {
            invalidCommand();
        }
    }

    private void handlePlaceZombie(String input) {
        Matcher matcher = IZombieCommands.PLACE_ZOMBIE.getMatcher(input);
        if (matcher == null) {
            invalidCommand();
            return;
        }
        int[] coordinates = parseCoordinates(matcher);
        if (coordinates == null) {
            return;
        }
        String zombieName;
        try {
            zombieName = matcher.group("zombieName").trim();
        } catch (IllegalArgumentException exception) {
            invalidCommand();
            return;
        }
        if (zombieName.isEmpty()) {
            invalidCommand();
            return;
        }
        Result result = controller.placeZombie(zombieName, coordinates[0], coordinates[1]);
        System.out.println(result.message());
    }

    private void handleAdvanceTime(String input) {
        Matcher matcher = IZombieCommands.ADVANCE_TIME.getMatcher(input);
        Integer count = parseInteger(matcher, "count");
        if (count == null) {
            return;
        }
        Result result = controller.advanceTime(count);
        System.out.println(result.message());
    }

    private int[] parseCoordinates(Matcher matcher) {
        if (matcher == null) {
            invalidCommand();
            return null;
        }
        Integer x = parseInteger(matcher, "x");
        Integer y = parseInteger(matcher, "y");
        if (x == null || y == null) {
            return null;
        }
        return new int[]{x, y};
    }

    private Integer parseInteger(Matcher matcher, String groupName) {
        if (matcher == null) {
            invalidCommand();
            return null;
        }
        try {
            return Integer.parseInt(
                matcher.group(groupName)
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            invalidCommand();
            return null;
        }
    }
}
