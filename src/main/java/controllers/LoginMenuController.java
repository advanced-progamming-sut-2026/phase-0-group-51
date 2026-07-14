package controllers;

import data.database.GreenHouseRepository;
import data.database.UserRepository;
import lombok.Getter;
import models.App;
import models.greenHouse.GreenHouse;
import models.Result;
import models.User;
import models.enums.Menu;
import models.enums.SecurityQuestions;
@Getter
public class LoginMenuController {
    private User resetPasswordUser;
    private boolean waitingForNewPassword ;
    public UserRepository repository;
    public LoginMenuController(){
        this.repository=new UserRepository();
        waitingForNewPassword=false; ;}
    public Result login(String username, String password, boolean stayLoggedIn) {
            User user = repository.getUserByUsername(username);
            if (user == null) {
                return new Result(false, "Username does not exist.\n", null);}

            String passwordHash = HashUtil.hashPassword(password);

            if (!user.getPasswordHash().equals(passwordHash)) {
                return new Result(false, "Password is incorrect.\n", null);
            }
            GreenHouse greenHouse = GreenHouseRepository.load(user.getId());
            user.setGreenHouse(greenHouse);
            App.getInstance().setLoggedInUser(user);
            repository.setStayLoggedIn(user.getId(), stayLoggedIn);
            return new Result(true, "Login successful.\n", user);
        }

    public Result forgetPassword(String username , String email){
        User user = repository.getUserByUsername(username);
        if(user == null){
            return new Result(false,"Username does not exist",null);
        }
        if(!user.getEmail().equals(email)){
            return new Result(false,"Email is incorrect.",null);
        }
        resetPasswordUser = user;
        return new Result(true, "Please answer your security question:\n"
                + SecurityQuestions.getQuestion(user.getSecurityQuestion()), user);
    }
    public Result answerQuestion(String answer){
        if (resetPasswordUser == null) {
            return new Result(false, "No forgot password request found.", null);
        }
        if (!resetPasswordUser.getAnswer().equalsIgnoreCase(answer)) {
            resetPasswordUser = null;
            return new Result(false, "Security answer is incorrect.Returning back...\n", null);
        }
        waitingForNewPassword = true;
        return new Result(true, "Enter your new password:", null);
    }
    public Result setNewPassword(String newPass){
        String hash = HashUtil.hashPassword(newPass);
        repository.updatePassword(resetPasswordUser.getUsername(), hash);
        waitingForNewPassword = false;
        resetPasswordUser = null;

        return new Result(true, "Password changed successfully.", null);
    }
    public Result exitMenu(){
        App.getInstance().setCurrentMenu(Menu.SIGN_UP_MENU);
        return new Result(true,"",null);
    }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the login menu.\n",null);
    }
    public Result enterMenu(String menuName){
        if(!menuName.equalsIgnoreCase("main")){
            return new Result(false,"You can only enter the main menu from the login menu.\n",null);
        }
        App.getInstance().currentMenu = Menu.MAIN_MENU;
        return new Result(true,"",null);

    }
}
