package views;

import controllers.LoginMenuController;
import models.Result;
import models.enums.commands.LoginMenuCommands;
import java.util.Scanner;
import java.util.regex.Matcher;

public class LoginMenu implements AppMenu{
    private final LoginMenuController controller;
    public LoginMenu(){this.controller = new LoginMenuController();}
    @Override
    public void check(Scanner scanner ) {
        String line = scanner.nextLine().trim();
        if(controller.isWaitingForNewPassword()){
            Result result = controller.setNewPassword(line.trim());
            System.out.println(result.message());
        }
        else if(LoginMenuCommands.loginRegex.matches(line)){
            handleLogin(line);
        }
        else if(LoginMenuCommands.exitMenuRegex.matches(line)) {
            Result result = controller.exitMenu();
            System.out.println(result.message());
        }
        else if(LoginMenuCommands.forgetPasswordRegex.matches(line)){
            handleForgetPassword(line);
        }
        else if(LoginMenuCommands.answerQuestionRegex.matches(line)){
            handleAnswerQuestion(line);
        }
        else if(LoginMenuCommands.enterMenuRegex.matches(line)) {
            handleEnterMenu(line);
        }
        else if(LoginMenuCommands.currentMenuRegex.matches(line)) {
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        }
        else invalidCommand();
    }
    public void handleLogin(String input){
        Matcher matcher = LoginMenuCommands.loginRegex.getMatcher(input);
        String username= matcher.group("username").trim();
        String password = matcher.group("password").trim();
        boolean stayLoggedIn = matcher.group("stay") != null;
        Result result = controller.login(username,password,stayLoggedIn);
        System.out.println(result.message());
    }
    public void handleForgetPassword(String input){
        Matcher matcher = LoginMenuCommands.forgetPasswordRegex.getMatcher(input);
        String username = matcher.group(1).trim();
        String email = matcher.group(2).trim();
        Result result = controller.forgetPassword(username,email);
        System.out.println(result.message());
    }
    public void handleAnswerQuestion(String input){
        Matcher matcher = LoginMenuCommands.answerQuestionRegex.getMatcher(input);
        String answer = matcher.group(1).trim();
        Result result = controller.answerQuestion(answer);
        System.out.println(result.message());
    }
    public void handleEnterMenu(String input){
        Matcher matcher = LoginMenuCommands.enterMenuRegex.getMatcher(input);
        String menuName = matcher.group("menuName");
        Result result = controller.enterMenu(menuName);
        System.out.println(result.message());
    }
}


