package views.miniGames;

import controllers.miniGamesController.VaseBreakerController;
import models.Result;
import models.enums.commands.TravelLogMenuCommands;
import models.enums.commands.VaseBreakerCommands;
import views.AppMenu;

import java.util.Scanner;
import java.util.regex.Matcher;

public class VaseBreakerMenu implements AppMenu {
    private final VaseBreakerController controller;
    public VaseBreakerMenu() {
        this.controller = new VaseBreakerController();
    }
    @Override
    public void check(Scanner scanner) {
        String input = scanner.nextLine().trim();
        if (VaseBreakerCommands.BREAK_VASE.matches(input)) {
            handleBreakVase(input);
        } else if (VaseBreakerCommands.PICK_SEED_PACKET.matches(input)) {
            handlePickSeedPacket(input);
        } else if (VaseBreakerCommands.PLANT_PACKET.matches(input)) {
            handlePlantPacket(input);
        } else if (VaseBreakerCommands.ADVANCE_TIME.matches(input)) {
            handleAdvanceTime(input);
        } else if (VaseBreakerCommands.SHOW_STATUS.matches(input)) {
            Result result = controller.showStatus();
            System.out.println(result.message());
        } else if (VaseBreakerCommands.CURRENT_MENU.matches(input)) {
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        } else if (VaseBreakerCommands.EXIT_MENU.matches(input)) {
            Result result = controller.exitMenu();
            System.out.println(result.message());
        } else {
            invalidCommand();
        }
    }
    private void handleBreakVase(String input) {
        Matcher matcher = VaseBreakerCommands.BREAK_VASE.getMatcher(input);
        int[] coordinates = parseCoordinates(matcher);
        if (coordinates == null) {return;}
        Result result = controller.breakVase(coordinates[0],coordinates[1]);
        System.out.println(result.message());
    }

    private void handlePickSeedPacket(String input) {
        Matcher matcher = VaseBreakerCommands.PICK_SEED_PACKET.getMatcher(input);
        int[] coordinates = parseCoordinates(matcher);
        if (coordinates == null) {
            return;
        }
        Result result = controller.pickUpSeedPacket(coordinates[0],coordinates[1]);
        System.out.println(result.message());
    }

    private void handlePlantPacket(String input) {
        Matcher matcher = VaseBreakerCommands.PLANT_PACKET.getMatcher(input);
        if (matcher == null) {
            invalidCommand();
            return;
        }
        int[] coordinates = parseCoordinates(matcher);
        if (coordinates == null) {
            return;
        }
        String plantType;
        try {plantType = matcher.group("plantType").trim();
        } catch (IllegalArgumentException exception) {
            invalidCommand();
            return;
        }
        if (plantType.isEmpty()) {
            invalidCommand();
            return;
        }
        Result result = controller.plantPacket(plantType, coordinates[0], coordinates[1]);
        System.out.println(result.message());
    }

    private void handleAdvanceTime(String input) {
        Matcher matcher = VaseBreakerCommands.ADVANCE_TIME.getMatcher(input);
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
