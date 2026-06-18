package views;

import controllers.SettingMenuController;
import models.Result;
import models.enums.commands.SettingMenuCommands;
import java.util.Scanner;
import java.util.regex.Matcher;

public class SettingMenu implements AppMenu{
    private final SettingMenuController controller;
    public SettingMenu(){this.controller=new SettingMenuController();}
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
     if(SettingMenuCommands.changeDifficultyLevel.matches(line)){
         handleChangeDifficulty(line);
     }
     else if(SettingMenuCommands.exitMenuRegex.matches(line)){
         Result result = controller.exitMenu();
         System.out.println(result.message());
     }
     else if(SettingMenuCommands.currentMenuRegex.matches(line)){
         Result result = controller.showCurrentMenu();
         System.out.println(result.message());
     }
     else invalidCommand();
    }
    public void handleChangeDifficulty(String input){
        Matcher matcher = SettingMenuCommands.changeDifficultyLevel.getMatcher(input);
        String difficultyLevel = matcher.group(1);
        Result result = controller.changeDifficulty(difficultyLevel);
        System.out.println(result.message());
    }
}
