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
        String line = scanner.nextLine();
        if(ProfileMenuCommands.showInfoRegex.matches(line)){
            Result result = controller.profileShowInfo();
            System.out.println(result.message());
        }
        else if(ProfileMenuCommands.changeUsernameRegex.matches(line)){
            handleChangeUsername(line);
        }
        else if(ProfileMenuCommands.changeNicknameRegex.matches(line)){
            handleChangeNickname(line);
        }
        else if(ProfileMenuCommands.changeEmailRegex.matches(line)){
            handleChangeEmail(line);
        }
        else if(ProfileMenuCommands.changePasswordRegex.matches(line)){
            handleChangePassword(line);
        }
        else if(ProfileMenuCommands.exitMenuRegex.matches(line)){
            controller.exitMenu();
        }
        else if(ProfileMenuCommands.currentMenuRegex.matches(line)){
            Result result = controller.showCurrentMenu();
            System.out.println(result.message());
        }
        else invalidCommand();
    }
    public void handleChangeUsername(String input){
        Matcher matcher = ProfileMenuCommands.changeUsernameRegex.MatchRegex(input);
        String newUsername = matcher.group(1).trim();
        Result result = controller.changeUsername(newUsername);
        System.out.println(result.message());
    }

    public void handleChangeNickname(String input){
        Matcher matcher = ProfileMenuCommands.changeNicknameRegex.MatchRegex(input);
        String newNickname = matcher.group(1).trim();
        Result result = controller.changeNickname(newNickname);
        System.out.println(result.message());
    }
    public void handleChangeEmail(String input){
        Matcher matcher = ProfileMenuCommands.changeEmailRegex.MatchRegex(input);
        String newEmail = matcher.group(1).trim();
        Result result = controller.changeEmail(newEmail);
        System.out.println(result.message());
    }
    public void handleChangePassword(String input){
        Matcher matcher = ProfileMenuCommands.changePasswordRegex.MatchRegex(input);
        String newPassword = matcher.group(1).trim();
        String oldPassword = matcher.group(2).trim();
        Result result = controller.changePassword(newPassword, oldPassword);
        System.out.println(result.message());
    }
}
