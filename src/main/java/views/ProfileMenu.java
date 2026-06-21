package views;

import controllers.ProfileMenuController;
import models.Result;
import models.enums.commands.ProfileMenuCommands;
import java.util.Scanner;
import java.util.regex.Matcher;

public class ProfileMenu implements AppMenu{
    private final ProfileMenuController controller;
    public ProfileMenu(){this.controller = new ProfileMenuController();}
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
        if(ProfileMenuCommands.SHOW_INFO_REGEX.matches(line)){
            Result result = controller.profileShowInfo();
            System.out.println(result.message());
        }
        else if(ProfileMenuCommands.CHANGE_USERNAME_REGEX.matches(line)){
            handleChangeUsername(line);
        }
        else if(ProfileMenuCommands.CHANGE_NICKNAME_REGEX.matches(line)){
            handleChangeNickname(line);
        }
        else if(ProfileMenuCommands.CHANGE_EMAIL_REGEX.matches(line)){
            handleChangeEmail(line);
        }
        else if(ProfileMenuCommands.CHANGE_PASSWORD_REGEX.matches(line)){
            handleChangePassword(line);
        }
        else if(ProfileMenuCommands.EXIT_MENU_REGEX.matches(line)){
            controller.exitMenu();
        }
        else if(ProfileMenuCommands.CURRENT_MENU_REGEX.matches(line)){
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        }
        else invalidCommand();
    }
    public void handleChangeUsername(String input){
        Matcher matcher = ProfileMenuCommands.CHANGE_USERNAME_REGEX.getMatcher(input);
        String newUsername = matcher.group(1).trim();
        Result result = controller.changeUsername(newUsername);
        System.out.println(result.message());
    }

    public void handleChangeNickname(String input){
        Matcher matcher = ProfileMenuCommands.CHANGE_NICKNAME_REGEX.getMatcher(input);
        String newNickname = matcher.group(1).trim();
        Result result = controller.changeNickname(newNickname);
        System.out.println(result.message());
    }
    public void handleChangeEmail(String input){
        Matcher matcher = ProfileMenuCommands.CHANGE_EMAIL_REGEX.getMatcher(input);
        String newEmail = matcher.group(1).trim();
        Result result = controller.changeEmail(newEmail);
        System.out.println(result.message());
    }
    public void handleChangePassword(String input){
        Matcher matcher = ProfileMenuCommands.CHANGE_PASSWORD_REGEX.getMatcher(input);
        String newPassword = matcher.group(1).trim();
        String oldPassword = matcher.group(2).trim();
        Result result = controller.changePassword(newPassword, oldPassword);
        System.out.println(result.message());
       
    
    }
}
