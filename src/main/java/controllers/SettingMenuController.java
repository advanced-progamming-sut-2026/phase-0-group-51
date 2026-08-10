package controllers;

import Data.database.UserRepository;
import controllers.validation.SettingMenuValidation;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;

public class SettingMenuController {
    public SettingMenuValidation validation;
    public UserRepository repository;
    public SettingMenuController(){
        this.validation = new SettingMenuValidation();
        this.repository = new UserRepository();
    }
    public Result changeDifficulty(String difficultyLevel) {
        if (!validation.isDifficultyLevelValid(difficultyLevel)) {
            return new Result(false, "Please enter a difficulty level from 1 to 5.", null);
        }
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return new Result(false, "You must be logged in to change difficulty.", null);
        }
        int newDifficulty = validation.dl;
        boolean saved = repository.updateDifficulty(user.getUsername(), newDifficulty);
        if (!saved) {
            return new Result(false, "Could not save difficulty level.", null);
        }
        user.setDifficultyLevel(newDifficulty);
        return new Result(true, "Difficulty level changed successfully.", null);
    }
    public Result exitMenu(){
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
        return new Result(true,"",null);
    }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the setting menu.\n",null);
    }
}
