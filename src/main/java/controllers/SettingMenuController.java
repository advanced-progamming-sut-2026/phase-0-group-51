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
    public Result changeDifficulty(String difficultyLevel){
        if (!validation.isDifficultyLevelValid(difficultyLevel)){
            return new Result(false,"Please enter a difficulty level from 1 to 5.",null);
        }
        User user = App.getInstance().getLoggedInUser();
        user.setDifficultyLevel(validation.dl);
        repository.updateDifficulty(user.getUsername(), validation.dl);
        return new Result(true, "Difficulty level changed successfully.", null);
    }
    public Result exitMenu(){
        App.getInstance().setCurrentMenu(Menu.MainMenu);
        return new Result(true,"",null);
    }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the setting menu.\n",null);
    }
}
