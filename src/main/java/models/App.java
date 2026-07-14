package models;

import Data.database.DataBaseManager;
import Data.database.UserRepository;
import Data.loader.PlantLoader;
import Data.loader.ZombieRegistry;
import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.enums.Menu;
import models.games.Game;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class App {
    private static App instance;
    public Menu currentMenu;
    private Game currentGame;
    public static User loggedInUser;
    public ArrayList<User> users = new ArrayList<>();
    private App(){
        DataBaseManager.initializeDatabase();
        PlantLoader.load();
        ZombieRegistry.load();

        UserRepository repository = new UserRepository();
        User rememberedUser = repository.getRememberedUser();

        if (rememberedUser != null) {
            loggedInUser = rememberedUser;
            currentMenu = Menu.MAIN_MENU;
        } else {
            currentMenu = Menu.SIGN_UP_MENU;
        }
    }
    public static App getInstance(){
        if(instance==null){instance = new App ();}
        return instance;
    }
    public User getLoggedInUser(){
        return loggedInUser;
    }
    public void setLoggedInUser(User user){
        loggedInUser = user;
    }
}
