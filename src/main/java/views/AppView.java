package views;

import models.App;
import models.enums.Menu;

import java.util.Scanner;

public class AppView {
    public static void run() {
        App app = new App();
        Scanner scanner = new Scanner(System.in);
        do {
            app.getCurrentMenu().checkCommand(scanner);
        } while (app.getCurrentMenu() != Menu.ExitMenu);
    }
}
