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
        if(CollectionMenuCommands.currentMenuRegex.matches(line)){
            System.out.println("You are in Collection Menu.");
        } else if (CollectionMenuCommands.SHOW_ALL_PLANTS.matches(line)) {
            Result result = controller.showAllPlants();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_PLANTS.matches(line)) {
            Result result = controller.showPlants();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_A_PLANT.matches(line)) {
            handleShowAPlant(line);
        } else if (CollectionMenuCommands.SHOW_ZOMBIES.matches(line)) {
            Result result = controller.showZombies();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_ALL_ZOMBIES.matches(line)) {
            Result result = controller.showAllZombies();
            System.out.println(result.message());
        } else if (CollectionMenuCommands.SHOW_A_ZOMBIE.matches(line)) {
            handleShowAZombie(line);
        } else if (CollectionMenuCommands.UPGRADE_PLANT.matches(line)) {
            handleUpgrade(line);
        } else if (CollectionMenuCommands.PURCHASE_PLANT.matches(line)) {
            handlePurchase(line);
        }
    }

    private void handlePurchase(String line) {
        String plantName = CollectionMenuCommands.PURCHASE_PLANT.getGroup(line, "plantName");
        Result result = controller.purchase(plantName);
        System.out.println(result.message());
    }

    private void handleUpgrade(String line) {
        String plantName = CollectionMenuCommands.UPGRADE_PLANT.getGroup(line, "plantName");
        Result result = controller.upgrade(plantName);
        System.out.println(result.message());
    }

    private void handleShowAZombie(String line) {
        String zombieName = CollectionMenuCommands.SHOW_A_ZOMBIE.getGroup(line, "zombieName");
        Result result = controller.showAZombie(zombieName);
        System.out.println(result.message());
    }

    private void handleShowAPlant(String line) {
        String plantName = CollectionMenuCommands.SHOW_A_PLANT.getGroup(line, "plantName");
        Result result = controller.showAPlant(plantName);
        System.out.println(result.message());
    }


}
