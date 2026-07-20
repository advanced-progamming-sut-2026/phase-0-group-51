package views.miniGames;

import controllers.miniGamesController.BeghouledController;
import models.Result;
import models.enums.commands.minigames.BeghouledCommands;
import views.AppMenu;

import java.util.Scanner;
import java.util.regex.Matcher;

public class BeghouledMenu implements AppMenu {
    private final BeghouledController controller = new BeghouledController();
    @Override
    public void check(Scanner scanner) {
        String input = scanner.nextLine().trim();
        if (BeghouledCommands.SWAP_PLANTS.matches(input)) {
            handleSwap(input);
        } else if (BeghouledCommands.UPGRADE_PLANTS.matches(input)) {
            handleUpgrade(input);
        } else if (BeghouledCommands.ADVANCE_TIME.matches(input)) {
            handleAdvanceTime(input);
        } else if (BeghouledCommands.SHOW_STATUS.matches(input)) {
            print(controller.showStatus());
        } else if (BeghouledCommands.SHOW_MAP.matches(input)) {
            print(controller.showMap());
        } else if (BeghouledCommands.SHOW_UPGRADES.matches(input)) {
            print(controller.showUpgrades());
        } else if (BeghouledCommands.CURRENT_MENU.matches(input)) {
            print(controller.showCurrentMenu());
        } else if (BeghouledCommands.EXIT_MENU.matches(input)) {
            print(controller.exitMenu());
        } else {
            invalidCommand();
        }
    }

    private void handleSwap(String input) {
        Matcher matcher = BeghouledCommands.SWAP_PLANTS.getMatcher(input);
        Integer x1 = parseInteger(matcher, "x1");
        Integer y1 = parseInteger(matcher, "y1");
        Integer x2 = parseInteger(matcher, "x2");
        Integer y2 = parseInteger(matcher, "y2");
        if (x1 == null || y1 == null || x2 == null || y2 == null) {
            return;
        }
        print(controller.swapPlants(x1, y1, x2, y2));
    }

    private void handleUpgrade(String input) {
        Matcher matcher = BeghouledCommands.UPGRADE_PLANTS.getMatcher(input);
        if (matcher == null) {
            invalidCommand();
            return;
        }
        try {
            String fromPlant = matcher.group("fromPlant").trim();
            String toPlant = matcher.group("toPlant").trim();
            if (fromPlant.isEmpty() || toPlant.isEmpty()) {
                invalidCommand();
                return;
            }
            print(controller.upgradePlants(fromPlant, toPlant));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            invalidCommand();
        }
    }

    private void handleAdvanceTime(String input) {
        Matcher matcher = BeghouledCommands.ADVANCE_TIME.getMatcher(input);
        Integer count = parseInteger(matcher, "count");
        if (count != null) {
            print(controller.advanceTime(count));
        }
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

