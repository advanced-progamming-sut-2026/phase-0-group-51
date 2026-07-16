package views;

import controllers.GamingController;
import models.Result;
import models.enums.commands.GameCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class GameView implements AppMenu {
    private final GamingController controller = new GamingController();

    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();

        if (GameCommands.SHOW_SUN_AMOUNT_REGEX.matches(line)) {
            print(controller.showSunAmount());
        } else if (GameCommands.CHEAT_ADD_SUN_REGEX.matches(line)) {
            Matcher matcher = GameCommands.CHEAT_ADD_SUN_REGEX.getMatcher(line);
            print(controller.cheatAddSun(parseInteger(matcher, "count")));
        } else if (GameCommands.PLANT_COLLECT_SUN_REGEX.matches(line)) {
            handleCoordinates(GameCommands.PLANT_COLLECT_SUN_REGEX.getMatcher(line), controller::collectSun);
        } else if (GameCommands.ADVANCE_TIME_REGEX.matches(line)) {
            Matcher matcher = GameCommands.ADVANCE_TIME_REGEX.getMatcher(line);
            print(controller.advanceTime(parseInteger(matcher, "count")));
        } else if (GameCommands.PLANT_PLANT_REGEX.matches(line)) {
            Matcher matcher = GameCommands.PLANT_PLANT_REGEX.getMatcher(line);
            int[] coordinates = parseCoordinates(matcher);
            if (coordinates != null) {
                print(controller.plantPlant(
                        matcher.group("type").trim(),
                        coordinates[0],
                        coordinates[1]
                ));
            }
        } else if (GameCommands.PLUCK_PLANT_REGEX.matches(line)) {
            handleCoordinates(GameCommands.PLUCK_PLANT_REGEX.getMatcher(line), controller::pluckPlant);
        } else if (GameCommands.FEED_PLANT_REGEX.matches(line)) {
            handleCoordinates(GameCommands.FEED_PLANT_REGEX.getMatcher(line), controller::feedPlant);
        } else if (GameCommands.CHEAT_ADD_PLANT_FOOD_REGEX.matches(line)) {
            print(controller.cheatAddPlantFood());
        } else if (GameCommands.SHOW_TILE_STATUS_REGEX.matches(line)) {
            handleCoordinates(GameCommands.SHOW_TILE_STATUS_REGEX.getMatcher(line), controller::showTileStatus);
        } else if (GameCommands.SHOW_PLANT_STATUS_REGEX.matches(line)) {
            print(controller.showPlantStatus());
        }  else if (GameCommands.SHOW_CONVEYOR_REGEX.matches(line)) {
            print(controller.showConveyor());
        }else if (GameCommands.ZOMBIES_INFO_REGEX.matches(line)) {
            print(controller.zombiesInfo());
        } else if (GameCommands.CHEAT_REMOVE_COOLDOWN_REGEX.matches(line)) {
            print(controller.removeCooldowns());
        } else if (GameCommands.RELEASE_NUKE_REGEX.matches(line)) {
            print(controller.releaseNuke());
        } else if (GameCommands.CHEAT_SPAWN_ZOMBIE.matches(line)) {
            Matcher matcher = GameCommands.CHEAT_SPAWN_ZOMBIE.getMatcher(line);
            int[] coordinates = parseCoordinates(matcher);
            if (coordinates != null) {
                print(controller.spawnZombie(
                        matcher.group("zombieType").trim(),
                        coordinates[0],
                        coordinates[1]
                ));
            }
        } else if (GameCommands.SHOW_SCORE_REGEX.matches(line)) {
            print(controller.showScore());
        } else if (GameCommands.SHOW_SCORING_RULES_REGEX.matches(line)) {
            print(controller.showScoringRules());
        } else if (GameCommands.SHOW_MAP_REGEX.matches(line)) {
            print(controller.showMap());
        } else if (GameCommands.CURRENT_MENU_REGEX.matches(line)) {
            System.out.println("You are now in the game menu.");
        } else {
            invalidCommand();
        }
    }

    private void handleCoordinates(Matcher matcher, CoordinateAction action) {
        int[] coordinates = parseCoordinates(matcher);
        if (coordinates != null) {
            print(action.apply(coordinates[0], coordinates[1]));
        }
    }

    private int[] parseCoordinates(Matcher matcher) {
        if (matcher == null) {
            invalidCommand();
            return null;
        }
        try {
            return new int[]{
                    Integer.parseInt(matcher.group("x")),
                    Integer.parseInt(matcher.group("y"))
            };
        } catch (IllegalArgumentException exception) {
            invalidCommand();
            return null;
        }
    }

    private int parseInteger(Matcher matcher, String group) {
        if (matcher == null) {
            invalidCommand();
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(group));
        } catch (IllegalArgumentException exception) {
            invalidCommand();
            return 0;
        }
    }

    private void print(Result result) {
        System.out.println(result.message());
    }

    @FunctionalInterface
    private interface CoordinateAction {
        Result apply(int x, int y);
    }
}
