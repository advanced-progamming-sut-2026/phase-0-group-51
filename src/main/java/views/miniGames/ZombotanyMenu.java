package views.miniGames;

import controllers.miniGamesController.ZombotanyController;
import models.Result;
import models.enums.commands.minigames.ZombotanyCommands;
import views.AppMenu;

import java.util.Scanner;
import java.util.regex.Matcher;

public class ZombotanyMenu implements AppMenu {
    private final ZombotanyController controller;

    public ZombotanyMenu() {
        this.controller = new ZombotanyController();
    }

    @Override
    public void check(Scanner scanner) {
        String input = scanner.nextLine().trim();
        if (ZombotanyCommands.PLACE_PLANT.matches(input)) {
            handlePlacePlant(input);
        } else if (ZombotanyCommands.ADVANCE_TIME.matches(input)) {
            handleAdvanceTime(input);
        } else if (ZombotanyCommands.SHOW_PLANTS.matches(input)) {
            System.out.println(controller.showPlants().message());
        } else if (ZombotanyCommands.SHOW_STATUS.matches(input)) {
            System.out.println(controller.showStatus().message());
        } else if (ZombotanyCommands.SHOW_MAP.matches(input)) {
            System.out.println(controller.showMap().message());
        } else if (ZombotanyCommands.CURRENT_MENU.matches(input)) {
            System.out.println(controller.showCurrentMenu().message());
        } else if (ZombotanyCommands.EXIT_MENU.matches(input)) {
            System.out.println(controller.exitMenu().message());
        } else {
            invalidCommand();
        }
    }

    private void handlePlacePlant(String input) {
        Matcher matcher = ZombotanyCommands.PLACE_PLANT.getMatcher(input);
        if (matcher == null) {
            invalidCommand();
            return;
        }
        String plantName;
        Integer x;
        Integer y;
        try {
            plantName = matcher.group("plantName").trim();
            x = Integer.parseInt(matcher.group("x"));
            y = Integer.parseInt(matcher.group("y"));
        } catch (RuntimeException exception) {
            invalidCommand();
            return;
        }
        if (plantName.isEmpty()) {
            invalidCommand();
            return;
        }
        Result result = controller.placePlant(plantName, x, y);
        System.out.println(result.message());
    }

    private void handleAdvanceTime(String input) {
        Matcher matcher = ZombotanyCommands.ADVANCE_TIME.getMatcher(input);
        if (matcher == null) {
            invalidCommand();
            return;
        }
        int count;
        try {
            count = Integer.parseInt(matcher.group("count"));
        } catch (NumberFormatException exception) {
            invalidCommand();
            return;
        }
        Result result = controller.advanceTime(count);
        System.out.println(result.message());
    }
}
