package models;

import Data.database.DataBaseManager;
import Data.database.UserRepository;
import Data.loader.PlantLoader;
import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.enums.Menu;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class App {
    private static App instance;
    public Menu currentMenu;
    public static User loggedInUser;
    public ArrayList<User> users = new ArrayList<>();
    public List<Plant> allPlants = new ArrayList<>();
    public List<Zombie> allZombies = new ArrayList<>();
    public List<String> securityQuestions = new ArrayList<>();

    private App(){
        DataBaseManager.initializeDatabase();
        PlantLoader.load();

        UserRepository repository = new UserRepository();
        User rememberedUser = repository.getRememberedUser();

        if (rememberedUser != null) {
            loggedInUser = rememberedUser;
            currentMenu = Menu.MainMenu;
        } else {
            currentMenu = Menu.SignUpMenu;
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
