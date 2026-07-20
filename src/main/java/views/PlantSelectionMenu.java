package views;

import controllers.PlantSelectionController;
import models.Result;
import models.enums.commands.PlantSelectionMenuCommands;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

public class PlantSelectionMenu implements AppMenu{
    private final PlantSelectionController controller;
    public PlantSelectionMenu(){this.controller = new PlantSelectionController();}
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(PlantSelectionMenuCommands.ADD_PLANT_REGEX.matches(line)){
            handleAddPlant(line);
        }
        else if(PlantSelectionMenuCommands.REMOVE_PLANT_REGEX.matches(line)){
            handleRemovePlant(line);
        }
        else if(PlantSelectionMenuCommands.BOOST_PLANT_REGEX.matches(line)){
            handleBoostPlant(line);
        }
        // for testing the plants
        else if (PlantSelectionMenuCommands.UNLOCK_ALL_PLANTS_CHEAT_REGEX.matches(line)) {
            Result result = controller.unlockAllPlantsForTesting();
            System.out.println(result.message());
        }
        else if (PlantSelectionMenuCommands.SHOW_ALL_PLANT_REGEX.matches(line)) {
            Result result = controller.showAllPlants();
            System.out.println(result.message());
        } else if(PlantSelectionMenuCommands.SHOW_AVAILABLE_PLANT_REGEX.matches(line)){
            Result result = controller.showAvailablePlants();
            System.out.println(result.message());
        } else if (PlantSelectionMenuCommands.START_GAME_REGEX.matches(line)) {
            Result result = controller.startGame();
            System.out.println(result.message());
        } else if (PlantSelectionMenuCommands.CURRENT_MENU_REGEX.matches(line)) {
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        } else if (PlantSelectionMenuCommands.EXIT_MENU_REGEX.matches(line)){
            controller.exitMenu();
        }
        else invalidCommand();
    }
    public void handleAddPlant(String input){
        Matcher matcher = PlantSelectionMenuCommands.ADD_PLANT_REGEX.getMatcher(input);
        String type = matcher.group("type");
        Result result = controller.addPlant(type);
        System.out.println(result.message());
    }
    public void handleRemovePlant(String input){
        Matcher matcher = PlantSelectionMenuCommands.REMOVE_PLANT_REGEX.getMatcher(input);
        String type = matcher.group("type");
        Result result = controller.removePlant(type);
        System.out.println(result.message());
    }
    public void handleBoostPlant(String input){
        Matcher matcher = PlantSelectionMenuCommands.BOOST_PLANT_REGEX.getMatcher(input);
        String type = matcher.group("type");
        Result result = controller.boostPlant(type);
        System.out.println(result.message());
    }
}
