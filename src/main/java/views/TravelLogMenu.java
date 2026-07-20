package views;

import controllers.TravelLogController;
import controllers.miniGamesController.*;
import models.Result;
import models.enums.commands.TravelLogMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class TravelLogMenu implements AppMenu{
    private final VaseBreakerController vaseBreakerController = new VaseBreakerController();
    private final WallnutBowlingController wallnutBowlingController = new WallnutBowlingController();
    private final IZombieController iZombieController = new IZombieController();
    private final BeghouledController beghouledController = new BeghouledController();
    private final ZombotanyController zombotanyController = new ZombotanyController();
    private final TravelLogController controller = new TravelLogController();
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if (TravelLogMenuCommands.CHANGE_PAGE.matches(line)) {
            Matcher matcher = TravelLogMenuCommands.CHANGE_PAGE.getMatcher(line);
            print(controller.changePage(matcher.group("page")));
        }  else if (TravelLogMenuCommands.SHOW_QUESTS.matches(line)) {
            print(controller.showCurrentPage());
        }  else if (TravelLogMenuCommands.CLAIM_QUEST.matches(line)) {
            Matcher matcher = TravelLogMenuCommands.CLAIM_QUEST.getMatcher(line);
            Integer id = parseInteger(matcher, "id");
            if (id != null) print(controller.claimQuest(id));
        }  else if (TravelLogMenuCommands.START_VASEBREAKER.matches(line)) {
            startVasebreaker(line);
        }  else if (TravelLogMenuCommands.START_WALLNUT_BOWLING.matches(line)) {
            startWallnutBowling(line);
        }  else if (TravelLogMenuCommands.START_I_ZOMBIE.matches(line)) {
            startIZombie(line);
        }  else if (TravelLogMenuCommands.START_BEGHOULDED.matches(line)) {
            startBeghouled(line);
        }  else if (TravelLogMenuCommands.START_ZOMBOTANY.matches(line)) {
            startZombotany(line);
        }  else if (TravelLogMenuCommands.CURRENT_MENU_REGEX.matches(line)) {
            print(controller.showCurrentMenu());
        }  else if (TravelLogMenuCommands.EXIT_MENU_REGEX.matches(line)) {
            print(controller.exitMenu());
        }
        else {
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


    private void startIZombie(String line) {
        Matcher matcher = TravelLogMenuCommands.START_I_ZOMBIE.getMatcher(line);
        Integer stage = parseStage(matcher);
        if (stage != null) print(iZombieController.startStage(stage));
    }

    private void startBeghouled(String line) {
        Matcher matcher = TravelLogMenuCommands.START_BEGHOULDED.getMatcher(line);
        Integer stage = parseStage(matcher);
        if (stage != null) print(beghouledController.startStage(stage));
    }

    private void startZombotany(String line) {
        Matcher matcher = TravelLogMenuCommands.START_ZOMBOTANY.getMatcher(line);
        Integer stage = parseStage(matcher);
        if (stage != null) print(zombotanyController.startStage(stage));
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

    private Integer parseInteger(Matcher matcher, String group) {
        if (matcher == null) {
            invalidCommand();
            return null;
        }
        try {return Integer.parseInt(matcher.group(group));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            invalidCommand();
            return null;
        }
    }

}
