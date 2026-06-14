package models;

import Data.database.DataBaseManager;
import Data.loader.PlantLoader;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.enums.Menu;

import java.util.ArrayList;
import java.util.List;

public class App {
    //private App instance; سینگلتون بعدا پیاده سازی بشه
    public Menu currentMenu;
    public static User loggedInUser;
    public ArrayList<User> users = new ArrayList<>();
    public List<Plant> allPlants = new ArrayList<>();
    public List<Zombie> allZombies = new ArrayList<>();
    public List<String> securityQuestions = new ArrayList<>();
    public Menu getCurrentMenu() {
        return this.currentMenu;
    }
    private App(){
        DataBaseManager.initializeDatabase();
        PlantLoader.load();
    }
}
