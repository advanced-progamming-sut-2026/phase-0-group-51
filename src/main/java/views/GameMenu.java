package views;

import controllers.GameMenuController;
import models.App;
import models.Result;
import models.enums.commands.GameMenuCommands;

import javax.swing.plaf.PanelUI;
import java.security.PublicKey;
import java.util.Scanner;

public class GameMenu implements AppMenu{
    private final GameMenuController controller = new GameMenuController();
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(GameMenuCommands.currentMenuRegex.matches(line)){
            System.out.println("You are in Game Menu.");
        } else if (GameMenuCommands.ENTER_CHAPTER.matches(line)) {
            handleEnterChapter(line);
        } else if (GameMenuCommands.GREENHOUSE.matches(line)) {
            controller.handleGreenhouse();
        } else if (GameMenuCommands.TRAVEL_LOG.matches(line)) {
            controller.handleTravellog();
        } else if (GameMenuCommands.LEADERBOARD.matches(line)) {
            controller.leaderboard();
        } else if (GameMenuCommands.COIN_WALLET.matches(line)) {
            System.out.println("You have "+ App.loggedInUser.getCoins()+ "coins.");
        } else if (GameMenuCommands.GEM_WALLET.matches(line)) {
            System.out.println("You have "+ App.loggedInUser.getGems()+ "gems.");
        } else if (GameMenuCommands.CHEAT_ADD.matches(line)) {
            handleCheatAdd(line);
        } else if (GameMenuCommands.enterMenuRegex.matches(line)) {
            handleEnterMenu(line);
        } else{
            invalidCommand();
        }
    }

    private void handleEnterMenu(String line) {
        String menuName = GameMenuCommands.enterMenuRegex.getGroup(line, "menuName");
        Result result = controller.handleEnterMenu(menuName);
        System.out.println(result.message());
    }

    public void handleEnterChapter(String input){
        String chapter = GameMenuCommands.ENTER_CHAPTER.getGroup(input, "chapterName");
        Result result = controller.handleEnterChapter(chapter);
        System.out.println(result.message());
    }
    public void handleCheatAdd(String input){
        int amount = Integer.parseInt(GameMenuCommands.CHEAT_ADD.getGroup(input, "amount"));
        String kind = GameMenuCommands.CHEAT_ADD.getGroup(input, "kind");
        Result result = controller.cheatAdd(amount, kind);
        System.out.println(result.message());
    }
}
