package views;

import controllers.miniGamesController.VaseBreakerController;
import controllers.miniGamesController.WallnutBowlingController;
import models.Result;
import models.enums.commands.TravelLogMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class TravelLogMenu implements AppMenu{
    private final VaseBreakerController vaseBreakerController = new VaseBreakerController();
    private final WallnutBowlingController wallnutBowlingController = new WallnutBowlingController();

    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if (TravelLogMenuCommands.START_VASEBREAKER.matches(line)) {
            startVasebreaker(line);
        } else if (TravelLogMenuCommands.START_WALLNUT_BOWLING.matches(line)) {
            startWallnutBowling(line);
        }  else {
            invalidCommand();
        }
    }
    private void startVasebreaker(String line) {
        Matcher matcher = TravelLogMenuCommands.START_VASEBREAKER.getMatcher(line);
        Integer stage = parseStage(matcher);
        if (stage == null) return;
        print(vaseBreakerController.startStage(stage));
    }

    private void startWallnutBowling(String line) {
        Matcher matcher = TravelLogMenuCommands.START_WALLNUT_BOWLING.getMatcher(line);
        Integer stage = parseStage(matcher);
        if (stage == null) return;
        print(wallnutBowlingController.startStage(stage));
    }

    private Integer parseStage(Matcher matcher) {
        if (matcher == null) {
            invalidCommand();
            return null;}
        try {
            return Integer.parseInt(matcher.group("stage"));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            invalidCommand();
            return null;
        }
    }

    private void print(Result result) {
        System.out.print(result.message());
    }

    public void handleChangePage(String input) {

    }
}
