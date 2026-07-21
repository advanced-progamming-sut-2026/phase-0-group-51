package views;

import controllers.NetworkMenuController;
import models.Result;
import models.enums.commands.NetworkMenuCommands;

import java.util.Scanner;

public class NetworkMenu implements AppMenu{
        private final NetworkMenuController controller;
    public NetworkMenu() {
            this.controller = new NetworkMenuController();
        }
        @Override
        public void check(Scanner scanner) {
            String line = scanner.nextLine().trim();
            if (NetworkMenuCommands.CURRENT_MENU_REGEX.matches(line)) {
                Result result = controller.showCurrentMenu();
                System.out.println(result.message());
            } else if (NetworkMenuCommands.EXIT_MENU_REGEX.matches(line)) {
                Result result = controller.exitMenu();
                System.out.println(result.message());
            } else {
                invalidCommand();
            }
        }
    }

