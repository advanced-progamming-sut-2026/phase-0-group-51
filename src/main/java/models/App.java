package models;

import Data.loader.PlantRegistry;

import Data.database.DataBaseManager;
import Data.database.GreenHouseRepository;
import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantLoader;
import Data.loader.QuestLoader;
import Data.loader.ZombieRegistry;
import lombok.Getter;
import lombok.Setter;
import models.enums.Menu;
import models.games.Game;
import models.greenHouse.GreenHouse;

import java.util.ArrayList;

@Getter
@Setter
public class App {
    public Menu currentMenu;
    private Game currentGame;
    public static User loggedInUser;
    public ArrayList<User> users = new ArrayList<>();
//    private App() {
//        DataBaseManager.initializeDatabase();
//        QuestLoader.loadQuestsToDatabase();
//        PlantLoader.load();
//        ZombieRegistry.load();
//        UserRepository repository = new UserRepository();
//        User rememberedUser = repository.getRememberedUser();
//        if (rememberedUser != null) {
//
//            GreenHouse greenHouse =
//                    GreenHouseRepository.load(rememberedUser.getId());
//
//            rememberedUser.setGreenHouse(greenHouse);
//
//            PlantRepository.unlockPlantsAndReturnNew(
//                    rememberedUser.getId(),
//                    PlantRegistry.getStarterPlantIds()
//            );
//
//            loggedInUser = rememberedUser;
//            currentMenu = Menu.MAIN_MENU;
//
//        } else {
//            currentMenu = Menu.SIGN_UP_MENU;
//        }
//    }
    private App() {
        DataBaseManager.initializeDatabase();
        QuestLoader.loadQuestsToDatabase();

        PlantLoader.load();
        ZombieRegistry.load();

        loggedInUser = null;
        currentMenu = Menu.SIGN_UP_MENU;
    }

    private static final class AppHolder {
        private static final App INSTANCE = new App();
        private AppHolder() {}
    }

    public static App getInstance() {
        return AppHolder.INSTANCE;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User user) {
        loggedInUser = user;
    }
}
