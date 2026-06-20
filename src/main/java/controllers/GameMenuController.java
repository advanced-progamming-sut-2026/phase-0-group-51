package controllers;

import Data.database.UserRepository;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;

public class GameMenuController {
    public Result handleEnterChapter(String chapter) {
        return null;
    }

    public void handleGreenhouse() {
        App.getInstance().setCurrentMenu(Menu.GREENHOUSE_MENU);
    }

    public void handleTravellog() {
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
    }

    public void leaderboard() {
        App.getInstance().setCurrentMenu(Menu.LEADERBOARD_MENU);
    }

    public Result cheatAdd(int amount, String kind) {
        if(amount <= 0){
            return new Result(false, "Please enter a positive amount." , null);
        } else if (kind.equalsIgnoreCase("coin")) {
            User user = App.loggedInUser;
            user.setCoins(user.getCoins() + amount);
            UserRepository userRepository = new UserRepository();
            userRepository.updateStats(user);
            return new Result(true, "Successfully added coins." , null);
        } else if (kind.equalsIgnoreCase("diamond")) {
            User user = App.loggedInUser;
            user.setGems(user.getGems() + amount);
            UserRepository userRepository = new UserRepository();
            userRepository.updateStats(user);
            return new Result(true, "Successfully added gems." , null);
        } else{
            return new Result(false, "You can only add coin or diamond please specify." , null);
        }
    }

    public Result handleEnterMenu(String menuName) {
        if(menuName.equalsIgnoreCase("Collection")){
            App.getInstance().setCurrentMenu(Menu.COLLECTION_MENU);
            return new Result(true,"You are now in the Collection menu.",null);
        }else{
            return new Result(false, "You can only go to the Collection menu!",null);
        }
    }
}
