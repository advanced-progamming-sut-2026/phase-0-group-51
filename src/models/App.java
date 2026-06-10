package models;

import models.plant.Plant;
import models.zombie.Zombie;
import models.enums.Menu;

import java.util.ArrayList;
import java.util.List;

public class App {
    //private App instance; سینگلتون بعدا پیاده سازی بشه
    public Menu currentMenu;
    public User loggedInUser;
    public ArrayList<User> users = new ArrayList<>();
    public List<Plant> allPlants = new ArrayList<>();
    public List<Zombie> allZombies = new ArrayList<>();
    public List<String> securityQuestions = new ArrayList<>();


    public Menu getCurrentMenu() {
        return this.currentMenu;
    }
}
