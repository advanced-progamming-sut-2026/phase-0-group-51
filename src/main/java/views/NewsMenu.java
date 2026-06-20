package views;

import controllers.NewsMenuController;
import models.Result;
import models.enums.commands.NewsMenuCommands;

import java.util.Scanner;

public class NewsMenu implements AppMenu{
    private final NewsMenuController controller;
    public NewsMenu(){this.controller = new NewsMenuController();}
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(NewsMenuCommands.showAllNewsRegex.matches(line)){
            Result result = controller.showAllNews();
            System.out.println(result.message());
        }
        else if(NewsMenuCommands.showUnreadNewsRegex.matches(line)){
            Result result = controller.showUnreadNews();
            System.out.println(result.message());
        }
        else if(NewsMenuCommands.exitMenuRegex.matches(line)){
            controller.exitMenu();
        }
        else if(NewsMenuCommands.currentMenuRegex.matches(line)){
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        }
        else invalidCommand();

    }
}
