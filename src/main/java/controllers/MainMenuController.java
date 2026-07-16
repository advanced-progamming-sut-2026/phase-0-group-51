package controllers;

import models.App;
import models.Result;
import models.enums.Menu;
import models.games.ScoringGame;

public class MainMenuController {
    public void logout(){
        App.getInstance().setCurrentMenu(Menu.SIGN_UP_MENU);
    }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the main menu.\n",null);
    }
    public Result enterMenu(String menuName){
        switch (menuName.toLowerCase()){
            case "game"-> App.getInstance().setCurrentMenu(Menu.GAME_MENU);
            case "setting"-> App.getInstance().setCurrentMenu(Menu.SETTING_MENU);
            case "network"-> App.getInstance().setCurrentMenu(Menu.NETWORK_MENU);
            case "news"-> App.getInstance().setCurrentMenu(Menu.NEWS_MENU);
            case "leaderboard" -> App.getInstance().setCurrentMenu(Menu.LEADERBOARD_MENU);
            case "profile" -> App.getInstance().setCurrentMenu(Menu.PROFILE_MENU);
            case "score-game" -> { App.getInstance().setCurrentGame(new ScoringGame());
                App.getInstance().setCurrentMenu(Menu.PlantSelection_Menu);
                return new Result(
                        true, "Daily Scoring Game opened.\n"
                                + "Select your plants, then use start game.\n",
                        null
                );}
            default -> {
                return new Result(false,
                 "You can only enter score-game,game,setting,news,network,profile menus from the main menu.\n",null);
            }
        }
         return new Result(true,"",null);
    }
}
