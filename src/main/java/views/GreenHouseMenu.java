package views;

import controllers.GreenHouseMenuController;
import models.Result;
import models.enums.commands.GreenHouseMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class GreenHouseMenu implements AppMenu{
    private final GreenHouseMenuController controller;
    public GreenHouseMenu(){this.controller = new GreenHouseMenuController();}
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(GreenHouseMenuCommands.CURRENT_MENU_REGEX.matches(line)){
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        }
        else if(GreenHouseMenuCommands.EXIT_MENU_REGEX.matches(line)){
            Result result = controller.exitMenu();
            System.out.println(result.message());
        }
        else if(GreenHouseMenuCommands.SHOW_GREENHOUSE_REGEX.matches(line)){
            Result result = controller.showGreenHouse();
            System.out.println(result.message());
        }
        else if(GreenHouseMenuCommands.PLANT_REGEX.matches(line)){
           handlePlantPot(line);
        }
        else if(GreenHouseMenuCommands.COLLECT_REGEX.matches(line)){
           handleCollectPlant(line);
        }
        else if(GreenHouseMenuCommands.GROW_REGEX.matches(line)){
           handleGrowPlant(line);
        }
        else if(GreenHouseMenuCommands.ENTER_SHOP_REGEX.matches(line)){
            controller.enterShop();
        }
        else invalidCommand();
    }
    public void handlePlantPot(String input){
        Matcher matcher = GreenHouseMenuCommands.PLANT_REGEX.getMatcher(input);
        String x = matcher.group("x");
        String y = matcher.group("y");
        Result result = controller.plantPot(x,y);
        System.out.println(result.message());
    }
    public void handleCollectPlant(String input){
        Matcher matcher = GreenHouseMenuCommands.COLLECT_REGEX.getMatcher(input);
        String x = matcher.group("x");
        String y = matcher.group("y");
        Result result = controller.collectPlant(x,y);
        System.out.println(result.message());
    }
    public void handleGrowPlant(String input){
        Matcher matcher = GreenHouseMenuCommands.GROW_REGEX.getMatcher(input);
        String x = matcher.group("x");
        String y = matcher.group("y");
        Result result = controller.growPlant(x,y);
        System.out.println(result.message());
    }
}
