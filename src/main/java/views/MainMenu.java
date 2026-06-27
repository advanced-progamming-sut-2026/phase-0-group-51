package views;

import controllers.MainMenuController;
import models.Result;
import models.enums.commands.MainMenuCommands;
import models.enums.commands.SignUpMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class MainMenu implements AppMenu{
    private final MainMenuController controller;
    public MainMenu() {
        this.controller = new MainMenuController();
    }
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
    if(MainMenuCommands.LOGOUT_REGEX.matches(line)){
        controller.logout();
    } else if(MainMenuCommands.CURRENT_MENU_REGEX.matches(line)) {
        Result result = controller.showCurrentMenu();
        System.out.println(result.message());
    } else if(MainMenuCommands.ENTER_MENU_REGEX.matches(line)){
        handleEnterMenu(line);
    }
    else invalidCommand();
    }
    public void handleEnterMenu(String input){
        Matcher matcher = SignUpMenuCommands.ENTER_MENU_REGEX.getMatcher(input);
        String menuName = matcher.group(1).trim();
        Result result = controller.enterMenu(menuName);
        System.out.println(result.message());
    }
}
