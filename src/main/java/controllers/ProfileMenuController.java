package controllers;

import Data.database.UserRepository;
import controllers.validation.ProfileMenuValidation;
import controllers.validation.SignUpValidation;
import lombok.Getter;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.games.Game;


public class ProfileMenuController {
    private final ProfileMenuValidation validation;
    private final SignUpValidation validationS;
    private final UserRepository repository;

    public ProfileMenuController() {
        this.validation = new ProfileMenuValidation();
        this.validationS = new SignUpValidation();
        this.repository = new UserRepository();
    }

    public Result changeUsername(String newUsername) {
        User user = App.getInstance().getLoggedInUser();
        if (validation.areUsernamesTheSame(user.getUsername(), newUsername)) {
            return new Result(false, "New username is the same as current username.\n", null);
        }
        if (repository.usernameExists(newUsername)) {
            return new Result(false,
                    "Username already exists.\n", null);
        }
        if(!validationS.isUsernameValid(newUsername)) {
            return new Result(false,
                    "Username can only contain A-Za-z letters, numbers, and the symbol -.\n",null);}
            repository.updateUsername(user.getId(), newUsername);
            user.setUsername(newUsername);
        return new Result(true, "Username changed successfully.", null);
    }

    public Result changeNickname(String newNickname) {
        User user = App.getInstance().getLoggedInUser();
        if (validation.areNicknamesTheSame(user.getNickname(), newNickname)) {
            return new Result(false, "New nickname is the same as current nickname.\n", null);
        }
        if(!validationS.isNicknameLengthValid(newNickname)){
            return new Result(false,"Nickname length must be between 3 and 30 characters.\n",null);
        }
        repository.updateNickname(user.getId(), newNickname);
        user.setNickname(newNickname);
        return new Result(true, "Nickname changed successfully.", null);
    }

    public Result changeEmail(String newEmail) {
        User user = App.getInstance().getLoggedInUser();
        if (validation.areEmailsTheSame(user.getEmail(), newEmail)) {
            return new Result(false, "New email is the same as current email.", null);
        }
        if (!validationS.hasExactlyOneAtSign(newEmail)){
            return new Result(false,"Email must contain exactly one @.\n",null); }
        if(validationS.hasInvalidChar(newEmail)){
            return new Result(false,"Email contains invalid characters.\n",null);
        }
        String[] parts = newEmail.split("@");
        if(!validationS.isFirstPartEmailValid(parts[0])){
            return new Result(false,
                    "The first part of email must start and end with a letter or digit," +
                            " may only contain letters, digits, '_', '-' and '.'(not consecutive)\n",null);}
        if (!validationS.isSecondPartEmailValid(parts[1])){
            return new Result(false,
                    "The second part of email must start and end with a letter or digit and be separated by one dot.\n",null);
        }
        repository.updateEmail(user.getId(), newEmail);
        user.setEmail(newEmail);
        return new Result(true, "Email changed successfully.", null);
    }

    public Result changePassword(String newPassword, String oldPassword) {
        String oldPasswordHash = HashUtil.hashPassword(oldPassword);
        String newPasswordHash = HashUtil.hashPassword(newPassword);
        User user = App.getInstance().getLoggedInUser();
        boolean isNewPassValid = true;
        if (validation.arePasswordsTheSame(oldPasswordHash, newPasswordHash)) {
            isNewPassValid = false;
            return new Result(false, "New password must be different from current password.\n", false);
        }
        if (!validation.arePasswordsTheSame(oldPasswordHash, user.getPasswordHash())) {
            isNewPassValid = false;
            return new Result(false, "Current password is incorrect.\n", false);
        }
        StringBuilder sb = new StringBuilder();
        if (!validationS.isPasswordValid(newPassword)) {
            isNewPassValid = false;
            return new Result(false,
                    "Password can only contain A-Za-z letters, numbers, and special symbols.\n", null);
        }
        if (!validationS.isPasswordStrong(newPassword)) {
            isNewPassValid = false;
            sb.append("Password is too weak.\n");
            if (!validationS.isWeakPasswordLongEnough(newPassword))
                sb.append("It must be at least 8 characters long!\n");
            if (!validationS.hasWeakPasswordUpperCaseLetter(newPassword))
                sb.append("It must contain at least one uppercase letter.\n");
            if (!validationS.hasWeakPasswordLowerCaseLetter(newPassword))
                sb.append("It must contain at least one lowercase letter.\n");
            if (!validationS.hasWeakPasswordNum(newPassword))
                sb.append("It must contain at least one number.\n");
            if (!validationS.hasWeakPasswordSpecialSymbol(newPassword))
                sb.append("It must contain at least one special symbol.\n"); }
            if (isNewPassValid) {
                repository.updatePassword(user.getUsername(), newPasswordHash);
                user.setPasswordHash(newPasswordHash);
                sb.append("Password changed successfully.");
            }
            return new Result(isNewPassValid, sb.toString(), null);
        }
        public Result profileShowInfo () {
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
        public Result showCurrentMenu () {
            return new Result(true, "You are now in the profile menu.\n", null);
        }
        public void exitMenu () {
            App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
        }

    }
