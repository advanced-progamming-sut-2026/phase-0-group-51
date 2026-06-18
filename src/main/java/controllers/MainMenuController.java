package controllers;

import models.App;
import models.Result;
import models.enums.Menu;

public class MainMenuController {
    public void logout(){
        App.getInstance().setCurrentMenu(Menu.SignUpMenu);
    }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the main menu.\n",null);
    }
    public Result enterMenu(String menuName){
        switch (menuName.toLowerCase()){
            case "game"-> App.getInstance().setCurrentMenu(Menu.GameMenu);
            case "setting"-> App.getInstance().setCurrentMenu(Menu.SettingMenu);
            case "network"-> App.getInstance().setCurrentMenu(Menu.NetworkMenu);
            case "news"-> App.getInstance().setCurrentMenu(Menu.NewsMenu);
            case "profile" -> App.getInstance().setCurrentMenu(Menu.ProfileMenu);
            default -> {
                return new Result(false,"You can only enter game,setting,news,network,profile menus from the main menu.\n",null);
            }
        }
         return new Result(true,"",null);
    }
}
