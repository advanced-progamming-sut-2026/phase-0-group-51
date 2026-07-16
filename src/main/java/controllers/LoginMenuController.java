package controllers;

import Data.database.GreenHouseRepository;
import Data.database.UserRepository;
import controllers.validation.SignUpValidation;
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
    private boolean isNewPassValid;
    private final SignUpValidation validation;
    public LoginMenuController(){
        this.repository=new UserRepository();
        this.validation = new SignUpValidation();
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
        isNewPassValid=true;
        String hash = HashUtil.hashPassword(newPass);
        User user = App.getInstance().getLoggedInUser();
        isNewPassValid = true;
        StringBuilder sb = new StringBuilder();
        if (!validation.isPasswordValid(newPass)) {
            isNewPassValid = false;
            return new Result(false,
                    "Password can only contain A-Za-z letters, numbers, and special symbols.\n", null);
        }
        if (!validation.isPasswordStrong(newPass)) {
            isNewPassValid = false;
            sb.append("Password is too weak.\n");
            if (!validation.isWeakPasswordLongEnough(newPass))
                sb.append("It must be at least 8 characters long!\n");
            if (!validation.hasWeakPasswordUpperCaseLetter(newPass))
                sb.append("It must contain at least one uppercase letter.\n");
            if (!validation.hasWeakPasswordLowerCaseLetter(newPass))
                sb.append("It must contain at least one lowercase letter.\n");
            if (!validation.hasWeakPasswordNum(newPass))
                sb.append("It must contain at least one number.\n");
            if (!validation.hasWeakPasswordSpecialSymbol(newPass))
                sb.append("It must contain at least one special symbol.\n"); }
        if (isNewPassValid) {
            repository.updatePassword(resetPasswordUser.getUsername(), hash);
            waitingForNewPassword = false;
            resetPasswordUser = null;
            sb.append("Password changed successfully.");
        }
        return new Result(isNewPassValid, sb.toString(), null);
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
        if(App.getInstance().getLoggedInUser() != null){
        App.getInstance().currentMenu = Menu.MAIN_MENU;
        return new Result(true,"",null);}
        return new Result(false,"You Have To Login First!\n",null);

    }
}
