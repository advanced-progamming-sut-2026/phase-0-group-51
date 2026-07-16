package views;

import controllers.CollectionMenuController;
import models.Result;
import models.enums.commands.CollectionMenuCommands;

import java.util.Scanner;

public class CollectionMenu implements AppMenu{
    private final CollectionMenuController controller = new CollectionMenuController();
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(CollectionMenuCommands.CURRENT_MENU_REGEX.matches(line)){
            System.out.println("You are in Collection Menu.");
        } else if (CollectionMenuCommands.SHOW_ALL_PLANTS_REGEX.matches(line)) {
            Result result = controller.showAllPlants();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_PLANTS_REGEX.matches(line)) {
            Result result = controller.showPlants();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_A_PLANT_REGEX.matches(line)) {
            handleShowAPlant(line);
        } else if (CollectionMenuCommands.SHOW_ZOMBIES_REGEX.matches(line)) {
            Result result = controller.showZombies();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_ALL_ZOMBIES_REGEX.matches(line)) {
            Result result = controller.showAllZombies();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_A_ZOMBIE_REGEX.matches(line)) {
            handleShowAZombie(line);
        } else if (CollectionMenuCommands.UPGRADE_PLANT_REGEX.matches(line)) {
            handleUpgrade(line);
        } else if (CollectionMenuCommands.PURCHASE_PLANT_REGEX.matches(line)) {
            handlePurchase(line);
        } else if (CollectionMenuCommands.EXIT_MENU_REGEX.matches(line)) {
            models.App.getInstance().setCurrentMenu(models.enums.Menu.GAME_MENU);
            System.out.println("You returned to the Game Menu.");
        } else {
            invalidCommand();
        }
    }

    private void handlePurchase(String line) {
        String plantName = CollectionMenuCommands.PURCHASE_PLANT_REGEX.getGroup(line, "plantName");
        Result result = controller.purchase(plantName);
        System.out.println(result.message());
    }

    private void handleUpgrade(String line) {
        String plantName = CollectionMenuCommands.UPGRADE_PLANT_REGEX.getGroup(line, "plantName");
        Result result = controller.upgrade(plantName);
        System.out.println(result.message());
    }

    private void handleShowAZombie(String line) {
        String zombieName = CollectionMenuCommands.SHOW_A_ZOMBIE_REGEX.getGroup(line, "zombieName");
        Result result = controller.showAZombie(zombieName);
        System.out.println(result.message());
    }

    private void handleShowAPlant(String line) {
        String plantName = CollectionMenuCommands.SHOW_A_PLANT_REGEX.getGroup(line, "plantName");
        Result result = controller.showAPlant(plantName);
        System.out.println(result.message());
    }


}
