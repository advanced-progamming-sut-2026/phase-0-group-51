package controllers;

import data.database.UserRepository;
import controllers.validation.ProfileMenuValidation;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;

public class ProfileMenuController {
    public ProfileMenuValidation validation;
    public UserRepository repository;
    public ProfileMenuController(){
        this.validation = new ProfileMenuValidation();
        this.repository=new UserRepository();
    }
    public Result changeUsername(String newUsername){
        User user = App.getInstance().getLoggedInUser();
        if(validation.areUsernamesTheSame(user.getUsername(),newUsername)){
            return new Result(false,"New username is the same as current username.\n",null);
        }
        if (repository.usernameExists(newUsername)) {return new Result(false,
                    "Username already exists.\n", null);
        }
        repository.updateUsername(user.getId(), newUsername);
        user.setUsername(newUsername);
        return new Result(true, "Username changed successfully.", null);
    }
    public Result changeNickname(String newNickname){
        User user = App.getInstance().getLoggedInUser();
        if(validation.areNicknamesTheSame(user.getNickname(),newNickname)){
            return new Result(false,"New nickname is the same as current nickname.\n",null);
        }
        repository.updateNickname(user.getId(), newNickname);
        user.setNickname(newNickname);
        return new Result(true, "Nickname changed successfully.", null);
    }
    public Result changeEmail(String newEmail){
        User user = App.getInstance().getLoggedInUser();
        if (validation.areEmailsTheSame(user.getEmail(),newEmail)) {
            return new Result(false, "New email is the same as current email.", null);
        }
        repository.updateEmail(user.getId(), newEmail);
        user.setEmail(newEmail);
        return new Result(true, "Email changed successfully.", null);
    }
    public Result changePassword(String newPassword, String oldPassword){
        String oldPasswordHash = HashUtil.hashPassword(oldPassword);
        String newPasswordHash = HashUtil.hashPassword(newPassword);
        User user = App.getInstance().getLoggedInUser();
        if(validation.arePasswordsTheSame(oldPasswordHash,newPasswordHash)){
            return new Result(false,"New password must be different from current password.\n",false);
        }
        if(!validation.arePasswordsTheSame(oldPasswordHash,user.getPasswordHash())){
            return new Result(false,"Current password is incorrect.\n",false);
        }
        repository.updatePassword(user.getUsername(), newPasswordHash);
        user.setPasswordHash(newPasswordHash);
        return new Result(true, "Password changed successfully.", null);
    }
    public Result profileShowInfo() {

        User user = App.getInstance().getLoggedInUser();

        int passedLevels = repository.getPassedLevels(user.getId());

        StringBuilder sb = new StringBuilder();

        sb.append("Username: ").append(user.getUsername()).append("\n");
        sb.append("Nickname: ").append(user.getNickname()).append("\n");
        sb.append("Games Played: ").append(user.getGamesPlayed()).append("\n");
        sb.append("Coins: ").append(user.getCoins()).append("\n");
        sb.append("Gems: ").append(user.getGems()).append("\n");
        sb.append("Passed Levels: ").append(passedLevels).append("\n");
        sb.append("Most Meow Point: ").append(user.getMostMeowPoint());

        return new Result(true, sb.toString(), null);
    }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the profile menu.\n",null);
    }
    public void exitMenu(){
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
    }
}
