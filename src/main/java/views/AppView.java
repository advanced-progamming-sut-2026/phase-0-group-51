package views;

import models.enums.Menu;
import models.App;

import java.util.Scanner;

public class AppView {
    public static void run() {
        Scanner scanner = new Scanner(System.in);
        do {
            App.getInstance().getCurrentMenu().checkCommand(scanner);
        } while (App.getInstance().getCurrentMenu() != Menu.ExitMenu);
    }
}
