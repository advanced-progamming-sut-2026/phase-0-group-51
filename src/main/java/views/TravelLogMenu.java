package views;

import controllers.miniGamesController.VaseBreakerController;
import models.Result;
import models.enums.commands.TravelLogMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class TravelLogMenu implements AppMenu{
    private final VaseBreakerController vaseBreakerController = new VaseBreakerController();
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
 if (TravelLogMenuCommands.START_VASEBREAKER.matches(line)) {
            Matcher matcher = TravelLogMenuCommands.START_VASEBREAKER.getMatcher(line);
            if (matcher == null) {
                invalidCommand();
                return;
            }
            try {
                int stageNumber = Integer.parseInt(matcher.group("stage"));
                Result result = vaseBreakerController.startStage(stageNumber);
                System.out.print(result.message());
            } catch (IllegalArgumentException | IllegalStateException exception) {
                invalidCommand();
            }
            return;
        }
    }
    public void handleChangePage(String input){}
}
