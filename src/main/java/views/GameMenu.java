package views;

import controllers.GameMenuController;
import models.App;
import models.Result;
import models.enums.Menu;
import models.enums.commands.GameCommands;
import models.enums.commands.GameMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class GameMenu implements AppMenu{
    private final GameMenuController controller = new GameMenuController();
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(GameMenuCommands.CURRENT_MENU_REGEX.matches(line)){
            System.out.println("You are in Game Menu.");
        } else if (GameMenuCommands.ENTER_CHAPTER_REGEX.matches(line)) {
            handleEnterChapter(line);
        }else if (GameMenuCommands.ENTER_LEVEL_REGEX.matches(line)) {
            handleEnterLevel(line);
        } else if (GameMenuCommands.LOCKED_PLANTS_MODE_REGEX.matches(line)) {
            handleLockedPlantsMode(line);
        } else if (GameMenuCommands.GREENHOUSE_REGEX.matches(line)) {
            controller.handleGreenhouse();
        } else if (GameMenuCommands.TRAVEL_LOG_REGEX.matches(line)) {
            controller.handleTravellog();
        } else if (GameMenuCommands.LEADERBOARD_REGEX.matches(line)) {
            controller.leaderboard();
        } else if (GameMenuCommands.COIN_WALLET_REGEX.matches(line)) {
            System.out.println("You have "+ App.loggedInUser.getCoins()+ " coins.");
        } else if (GameMenuCommands.GEM_WALLET_REGEX.matches(line)) {
            System.out.println("You have "+ App.loggedInUser.getGems()+ " gems.");
        // for testing chapter progression
        } else if (GameMenuCommands.CHEAT_UNLOCK_LEVEL_REGEX.matches(line)) {
            handleCheatUnlockLevel(line);
        } else if (GameMenuCommands.CHEAT_ADD_REGEX.matches(line)) {
            handleCheatAdd(line);
        } else if (GameMenuCommands.ENTER_MENU_REGEX.matches(line)) {
            handleEnterMenu(line);
        } else if (GameMenuCommands.EXIT_MENU_REGEX.matches(line)) {
            models.App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
            System.out.println("You returned to the Main Menu.");
        } else{
            invalidCommand();
        }
    }

    private void handleEnterMenu(String line) {
        String menuName = GameMenuCommands.ENTER_MENU_REGEX.getGroup(line, "menuName");
        Result result = controller.handleEnterMenu(menuName);
        System.out.println(result.message());
    }

    public void handleEnterChapter(String input){
        String chapter = GameMenuCommands.ENTER_CHAPTER_REGEX.getGroup(input, "chapterName");
        Result result = controller.handleEnterChapter(chapter);
        System.out.println(result.message());
    }
    public void handleEnterLevel(String input){
        Matcher matcher = GameMenuCommands.ENTER_LEVEL_REGEX.getMatcher(input);
        Result result = controller.enterLevel(parseInteger(matcher, "levelNumber"));
        System.out.println(result.message());
    }
    private void handleLockedPlantsMode(String input) {
        String mode = GameMenuCommands.LOCKED_PLANTS_MODE_REGEX.getGroup(input, "mode");
        Result result = controller.chooseLockedPlantsMode(mode);
        System.out.println(result.message());
    }

    private void handleCheatUnlockLevel(String input) {
        Matcher matcher = GameMenuCommands.CHEAT_UNLOCK_LEVEL_REGEX.getMatcher(input);
        if (matcher == null) {
            invalidCommand();
            return;
        }
        String chapterName = matcher.group("chapterName");
        int levelNumber = parseInteger(matcher, "levelNumber");
        Result result = controller.cheatUnlockLevel(chapterName, levelNumber);
        System.out.println(result.message());
    }
    public void handleCheatAdd(String input){
        int amount = Integer.parseInt(GameMenuCommands.CHEAT_ADD_REGEX.getGroup(input, "amount"));
        String kind = GameMenuCommands.CHEAT_ADD_REGEX.getGroup(input, "kind");
        Result result = controller.cheatAdd(amount, kind);
        System.out.println(result.message());
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
}
