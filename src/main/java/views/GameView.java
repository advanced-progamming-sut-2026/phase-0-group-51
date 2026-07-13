package views;

import controllers.GamingController;
import models.Result;
import models.enums.commands.GameCommands;
import models.enums.commands.GreenHouseMenuCommands;
import models.enums.commands.MainMenuCommands;
import models.games.GameState;

import java.util.Scanner;
import java.util.regex.Matcher;

public class GameView implements AppMenu{
    private final GamingController controller;
    private GameState gameState;
    public GameView(){
        this.controller = new GamingController();
        this.gameState = new GameState(); //علی الحساب
    }
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(GameCommands.SHOW_SUN_AMOUNT_REGEX.matches(line)){
            Result result = controller.showSunAmount(gameState);
            System.out.println(result.message());
        } else if(GameCommands.CHEAT_ADD_SUN_REGEX.matches(line)) {
           handleCheatAddSun(line);
        }
        else if(GameCommands.PLANT_COLLECT_SUN_REGEX.matches(line)){
            handleCollectSun(line);
        }
        else if(GameCommands.ADVANCE_TIME_REGEX.matches(line)){
            handleAdvanceTime(line);
        }
        else invalidCommand();
    }
    public void handleCheatAddSun(String input){
        Matcher matcher = GameCommands.CHEAT_ADD_SUN_REGEX.getMatcher(input);
        int n=0;
        try {
            n = Integer.parseInt(matcher.group("count"));
        } catch (NumberFormatException e) {
            invalidCommand();
        }
        Result result = controller.cheatAddSun(gameState,n);
        System.out.println(result.message());
    }
    public void handleCollectSun(String input){
        Matcher matcher = GameCommands.PLANT_COLLECT_SUN_REGEX.getMatcher(input);
        int x=0,y=0;
        try {
            x = Integer.parseInt(matcher.group("x"));
        } catch (NumberFormatException e) {
            invalidCommand();
        }
        try {
            y = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException e) {
            invalidCommand();
        }
        Result result = controller.collectSun(gameState,x,y);
        System.out.println(result.message());
    }
    public void handleAdvanceTime(String input){
        Matcher matcher = GameCommands.ADVANCE_TIME_REGEX.getMatcher(input);
        int count=0;
        try {
            count = Integer.parseInt(matcher.group("count"));
        } catch (NumberFormatException e) {
            invalidCommand();
        }
        Result result = controller.advanceTime(count);
        System.out.println(result.message());
    }
}
