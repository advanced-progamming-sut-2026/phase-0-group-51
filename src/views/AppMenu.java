package views;

import java.util.Scanner;

public interface AppMenu {
    public void check(Scanner scanner);
    public default void invalidCommand() {
        System.out.println("invalid command!");
    }
}