package views;

import controllers.SignUpMenuController;
import models.Result;
import models.enums.Menu;
import models.enums.commands.LoginMenuCommands;
import models.enums.commands.SignUpMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class SignUpMenu implements AppMenu{
    private final SignUpMenuController controller;
    public SignUpMenu(){this.controller= new SignUpMenuController();}
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine();
        if(SignUpMenuCommands.exitMenuRegex.matches(line)) {
            Result result = controller.exitMenu();
            System.out.println(result.message());}
        else if(SignUpMenuCommands.enterMenuRegex.matches(line)) {handleEnterMenu(line);}
        else if(SignUpMenuCommands.currentMenuRegex.matches(line)) {
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());}
        else if(SignUpMenuCommands.registerRegex.matches(line)){handleRegister(line);}
        else if(SignUpMenuCommands.pickSecurityQuestion.matches(line)){handlePickQuestion(line);}
        else invalidCommand();
    }
    public void handleRegister(String input) {
        StringBuilder sb = new StringBuilder();
        Result result;
        Matcher matcher = SignUpMenuCommands.registerRegex.MatchRegex(input);
        String username = matcher.group("username");
        result=controller.setUsername(username);
        sb.append(result.message());
        String password =matcher.group("password");
        result=controller.setPassword(password);
        sb.append(result.message());
        String passwordConfirm = matcher.group("passwordConfirm");
        result=controller.setPasswordConfirm(passwordConfirm,password);
        sb.append(result.message());
        String nickname = matcher.group("nickname");
        result=controller.setNickname(nickname);
        sb.append(result.message());
        String email = matcher.group("email");
        result=controller.setEmail(email);
        sb.append(result.message());
        String gender = matcher.group("gender");
        if (controller.isRegisterValid) {
            result = controller.setGender(gender);
            sb.append(result.message());
        }
        System.out.println(sb);
    }

    public void handleEnterMenu(String input){
        Matcher matcher = SignUpMenuCommands.enterMenuRegex.MatchRegex(input);
        String menuName = matcher.group(1).trim();
        Result result = controller.enterMenu(menuName);
        System.out.println(result.message());
    }

    public void handlePickQuestion(String input){
        Matcher matcher = SignUpMenuCommands.pickSecurityQuestion.MatchRegex(input);
        String questionNum = matcher.group(1).trim();
        String answer = matcher.group(2).trim().toLowerCase();
        String answerConfirm = matcher.group(3).trim();
        Result result = controller.setQuestion(questionNum,answer,answerConfirm);
        System.out.println(result.message());
    }
}
