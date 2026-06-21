package controllers;

import Data.database.UserRepository;
import controllers.validation.SignUpValidation;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.enums.SecurityQuestions;

public class SignUpMenuController {
    public SignUpValidation validation;
    public UserRepository repository;
    public boolean isRegisterValid;
    private String username,passwordHash,nickname,email,gender,answer;
    private int securityQuestionNum;
    public SignUpMenuController(){
    this.validation = new SignUpValidation();
    this.repository=new UserRepository();
    this.isRegisterValid=true;}
    public Result setUsername (String username){
        isRegisterValid=true;
        if(!validation.isUsernameValid(username)) {
            isRegisterValid = false;
            return new Result(false,
                    "Username can only contain A-Za-z letters, numbers, and the symbol -.\n",null);}
        if(repository.usernameExists(username)){
            isRegisterValid=false;
            return new Result(false,"Username already exists!\n",null);
        }
        this.username = username;
        return new Result(true, "", null);
    }
    public Result setPassword (String pass){
        if(!validation.isPasswordValid(pass)){
            isRegisterValid = false;
            return new Result(false,
                    "Password can only contain A-Za-z letters, numbers, and special symbols.\n",null);
        }
        if(!validation.isPasswordStrong(pass)){
            isRegisterValid = false;
            StringBuilder sb = new StringBuilder();
            sb.append("Password is too weak.\n");
            if(!validation.isWeakPasswordLongEnough(pass)) sb.append("It must be at least 8 characters long!\n");
            if(!validation.hasWeakPasswordUpperCaseLetter(pass))
                sb.append("It must contain at least one uppercase letter.\n");
            if(!validation.hasWeakPasswordLowerCaseLetter(pass))
                sb.append("It must contain at least one lowercase letter.\n");
            if(!validation.hasWeakPasswordNum(pass))
                sb.append("It must contain at least one number.\n");
            if(!validation.hasWeakPasswordSpecialSymbol(pass))
                sb.append("It must contain at least one special symbol.\n");
            return new Result(false, sb.toString(),null);
        }
        this.passwordHash = HashUtil.hashPassword(pass);
        return new Result(true, "", null);
    }
    public Result setPasswordConfirm (String passConfirm,String pass){
        if(!validation.are2passwordsSame(passConfirm, pass)) {
            isRegisterValid = false;
            return new Result(false,"Passwords do not match.Try again or return back.\n",null);
        }
        if (this.passwordHash == null) {
            this.passwordHash = HashUtil.hashPassword(pass);
        }
        return new Result(true, "", null);
    }
    public Result setNickname(String nickname){
        if(!validation.isNicknameLengthValid(nickname)){
            isRegisterValid=false;
            return new Result(false,"Nickname length must be between 3 and 30 characters.\n",null);
        }
        this.nickname = nickname;
        return new Result(true, "", null);
    }
    public Result setEmail(String email){
        if (!validation.hasExactlyOneAtSign(email)){
            isRegisterValid = false;
            return new Result(false,"Email must contain exactly one @.\n",null); }
        if(validation.hasInvalidChar(email)){
            isRegisterValid = false;
            return new Result(false,"Email contains invalid characters.\n",null);
        }
        String[] parts = email.split("@");
        if(!validation.isFirstPartEmailValid(parts[0])){
            isRegisterValid = false;
        return new Result(false,
        "The first part of email must start and end with a letter or digit," +
                " may only contain letters, digits, '_', '-' and '.'(not consecutive)\n",null);}
        if (!validation.isSecondPartEmailValid(parts[1])){
            isRegisterValid=false;
        return new Result(false,
        "The second part of email must start and end with a letter or digit and be separated by dots.\n",null);
        }
        this.email = email;
        return new Result(true, "", null);
    }
    public Result setGender(String gender){
        if (!validation.isGenderValid(gender)){
            isRegisterValid = false;
            return new Result(false,"Please select a valid gender\n",null);
        }
        this.gender = gender;
        return new Result(true,
                "Please choose a security question and enter an answer without spaces.\n"
                        +SecurityQuestions.listOfSecurityQuestions(),null);
    }
    public Result setQuestion(String questionNum,String answer,String answerConfirm){
        if(!validation.isQuestionNumValid(questionNum)){
        isRegisterValid=false;
        return new Result(false,"Please enter a number from 1 to 10 .\n",null);}
        if (!validation.are2answersSame(answer,answerConfirm)){
            isRegisterValid=false;
        return new Result(false,"The answers do not match.\n",null);
        }
        this.securityQuestionNum = validation.questionNum;
        this.answer = answer;

        if (!allFieldsReady()) {
            isRegisterValid = false;
            return new Result(false, "Some registration fields are missing.\n", null);
        }
        User user = new User(username, email, passwordHash, gender, nickname, securityQuestionNum, answer);
        boolean success = repository.register(user);
        if (!success) {
            isRegisterValid = false;
            return new Result(false, "Registration failed\n", null);
        }
        resetFields();
        return new Result(true, "Registration completed successfully.\n", null);
    }
    private boolean allFieldsReady() {
        return validation.isNotBlank(username) && validation.isNotBlank(passwordHash) && validation.isNotBlank(nickname)
                && validation.isNotBlank(email) && validation.isNotBlank(gender)  && validation.isNotBlank(answer);
    }
    public Result exitMenu(){
        System.exit(0);
        return null;
    }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the signup menu.\n",null);
    }
    public Result enterMenu(String menuName){
        if(!menuName.equalsIgnoreCase("login")){
            return new Result(false,"You can only enter the login menu from the signup menu.\n",null);
        }
        App.getInstance().currentMenu = Menu.LoginMenu;
        return new Result(true,"",null);
    }
    private void resetFields() {
        isRegisterValid = true;username = null;
        passwordHash = null;nickname = null;
        email = null;gender = null;
        securityQuestionNum = 0; answer = null;
    }
}
