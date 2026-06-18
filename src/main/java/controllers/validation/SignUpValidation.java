package controllers.validation;

import models.enums.commands.SignUpMenuCommands;

public class SignUpValidation {
    public int questionNum;
    public boolean isUsernameValid(String username){
        return SignUpMenuCommands.usernameRegex.matches(username);
    }
    public boolean isPasswordValid(String password){
        return SignUpMenuCommands.passwordRegex.matches(password);
    }
    public boolean isPasswordStrong(String password){
        return SignUpMenuCommands.strongPasswordRegex.matches(password);
    }
    public boolean isWeakPasswordLongEnough(String password){
        return password.length()>=8;
    }
    public boolean hasWeakPasswordUpperCaseLetter(String password){
        return password.matches(".*[A-Z].*");
    }
    public boolean hasWeakPasswordLowerCaseLetter(String password){
        return password.matches(".*[a-z].*");
    }
    public boolean hasWeakPasswordNum(String password){
        return password.matches(".*\\d.*");
    }
    public boolean hasWeakPasswordSpecialSymbol(String password){
        return SignUpMenuCommands.specialSymbolsRegex.matches(password);
    }
    public boolean are2passwordsSame(String pass, String pass2){
        return pass.equals(pass2);
    }
    public boolean isNicknameLengthValid(String username) {
        return username.length() >= 3 && username.length() <= 30;
    }
    public boolean hasExactlyOneAtSign(String email){
        return email.matches("^[^@]+@[^@]+$");
    }
    public boolean isFirstPartEmailValid(String firstPart) {
        return SignUpMenuCommands.emailFirstPartRegex.matches(firstPart);
    }
    public boolean isSecondPartEmailValid(String secondPart) {
        return SignUpMenuCommands.emailSecondPartRegex.matches(secondPart);
    }
    public boolean hasInvalidChar(String email){
        return email.matches("^.*[^a-zA-Z0-9._@-].*$");
    }
    public boolean isGenderValid(String gender){
        return gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("female");
    }
    public boolean isQuestionNumValid(String qNum){
        try {
             questionNum = Integer.parseInt(qNum);
            return questionNum >= 1 && questionNum <= 10;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public boolean are2answersSame(String answer,String answer2){
        return answer.equalsIgnoreCase(answer2);
    }
    public boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
