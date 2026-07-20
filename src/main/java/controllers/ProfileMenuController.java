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
    private final SignUpValidation signUpValidation;
    private final UserRepository repository;

    public ProfileMenuController() {
        this.validation = new ProfileMenuValidation();
        this.signUpValidation = new SignUpValidation();
        this.repository = new UserRepository();
    }

    public Result changeUsername(String newUsername) {
        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        if (validation.areUsernamesTheSame(user.getUsername(), newUsername)) {
            return new Result(
                    false,
                    "New username is the same as current username.\n",
                    null
            );
        }
        if (!signUpValidation.isUsernameValid(newUsername)) {
            return new Result(
                    false,
                    "Username can only contain A-Za-z letters, numbers, and the symbol -.\n",
                    null
            );
        }
        if (repository.usernameExists(newUsername)) {
            return new Result(false,
                    "Username already exists.\n", null);
        }
        if (!repository.updateUsername(user.getId(), newUsername)) {
            return new Result(false, "Username could not be saved.\n", null);
        }
            user.setUsername(newUsername);
        return new Result(true, "Username changed successfully.\n", null);
    }

    public Result changeNickname(String newNickname) {
        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        if (validation.areNicknamesTheSame(user.getNickname(), newNickname)) {
            return new Result(false, "New nickname is the same as current nickname.\n", null);
        }
        if (!signUpValidation.isNicknameLengthValid(newNickname)) {
            return new Result(false,"Nickname length must be between 3 and 30 characters.\n",null);
        }
        if (!repository.updateNickname(user.getId(), newNickname)) {
            return new Result(false, "Nickname could not be saved.\n", null);
        }
        user.setNickname(newNickname);
        return new Result(true, "Nickname changed successfully.\n", null);
    }

    public Result changeEmail(String newEmail) {
        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        if (validation.areEmailsTheSame(user.getEmail(), newEmail)) {
            return new Result(
                    false,
                    "New email is the same as current email.\n",
                    null
            );
        }
        Result formatError = validateEmail(newEmail);
        if (formatError != null) {
            return formatError;
        }
        if (repository.emailExistsForAnotherUser(newEmail, user.getId())) {
            return new Result(false, "Email already exists.\n", null);
        }
        if (!repository.updateEmail(user.getId(), newEmail)) {
            return new Result(false, "Email could not be saved.\n", null);
        }
        user.setEmail(newEmail);
        return new Result(true, "Email changed successfully.\n", null);
    }

    public Result changePassword(String newPassword, String oldPassword) {
        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        String oldPasswordHash = HashUtil.hashPassword(oldPassword);
        if (!validation.arePasswordsTheSame(oldPasswordHash, user.getPasswordHash())) {
            return new Result(false, "Current password is incorrect.\n", null);
        }
        String newPasswordHash = HashUtil.hashPassword(newPassword);
        if (validation.arePasswordsTheSame(newPasswordHash, user.getPasswordHash())) {
            return new Result(
                    false, "New password must be different from current password.\n", null
            );
        }

        StringBuilder errors = new StringBuilder();
        if (!signUpValidation.isPasswordValid(newPassword)) {
            return new Result(
                    false, "Password can only contain A-Za-z letters, numbers, and special symbols.\n", null
            );
        }
        if (!signUpValidation.isPasswordStrong(newPassword)) {
            errors.append("Password is too weak.\n");
            if (!signUpValidation.isWeakPasswordLongEnough(newPassword)) {
                errors.append("It must be at least 8 characters long!\n");
            }
            if (!signUpValidation.hasWeakPasswordUpperCaseLetter(newPassword)) {
                errors.append("It must contain at least one uppercase letter.\n");
            }
            if (!signUpValidation.hasWeakPasswordLowerCaseLetter(newPassword)) {
                errors.append("It must contain at least one lowercase letter.\n");
        }
            if (!signUpValidation.hasWeakPasswordNum(newPassword)) {
                errors.append("It must contain at least one number.\n");
            }
            if (!signUpValidation.hasWeakPasswordSpecialSymbol(newPassword)) {
                errors.append("It must contain at least one special symbol.\n");
        }
            return new Result(false, errors.toString(), null);
        }
        if (!repository.updatePassword(user.getUsername(), newPasswordHash)) {
            return new Result(false, "Password could not be saved.\n", null);
        }
                user.setPasswordHash(newPasswordHash);
        return new Result(true, "Password changed successfully.\n", null);
            }

        public Result profileShowInfo () {
        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
            int passedLevels = repository.getPassedLevels(user.getId());
        StringBuilder output = new StringBuilder();
        output.append("Username: ").append(user.getUsername()).append('\n');
        output.append("Nickname: ").append(user.getNickname()).append('\n');
        output.append("Games Played: ").append(user.getGamesPlayed()).append('\n');
        output.append("Coins: ").append(user.getCoins()).append('\n');
        output.append("Gems: ").append(user.getGems()).append('\n');
        output.append("Passed Levels: ").append(passedLevels).append('\n');
        output.append("Most Meow Point: ").append(user.getMostMeowPoint()).append('\n');
        return new Result(true, output.toString(), null);
    }

        public Result showCurrentMenu () {
        if (currentUser() == null) {
            return loginRequired();
        }
            return new Result(true, "You are now in the profile menu.\n", null);
        }
        public void exitMenu () {
            App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
        }

    private User currentUser() {
        return App.getInstance().getLoggedInUser();
    }

    private Result loginRequired() {
        return new Result(false, "You must log in first.\n", null);
    }

    private Result validateEmail(String email) {
        if (!signUpValidation.hasExactlyOneAtSign(email)) {
            return new Result(false, "Email must contain exactly one @.\n", null);
        }
        if (signUpValidation.hasInvalidChar(email)) {
            return new Result(false, "Email contains invalid characters.\n", null);
        }
        String[] parts = email.split("@", -1);
        if (parts.length != 2 || !signUpValidation.isFirstPartEmailValid(parts[0])) {
            return new Result(
                    false,
                    "The first part of email must start and end with a letter or digit, "
                            + "and may only contain letters, digits, '_', '-' and '.'.\n",
                    null
            );
        }
        if (!signUpValidation.isSecondPartEmailValid(parts[1])) {
            return new Result(
                    false,
                    "The domain must be valid and contain a suffix of at least two letters.\n",
                    null
            );
        }
        return null;
    }
    }
