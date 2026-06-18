package views;

import java.util.Scanner;

public interface AppMenu {
     void check(Scanner scanner);
    default void invalidCommand() {
        System.out.println("invalid command!");
    }
}