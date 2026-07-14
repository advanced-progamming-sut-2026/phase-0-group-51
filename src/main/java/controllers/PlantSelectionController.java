package controllers;

import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.games.Game;

import java.util.List;
import java.util.Set;

public class PlantSelectionController {
    private final UserRepository userRepository;
    public PlantSelectionController() {
        this.userRepository = new UserRepository();
    }
    public Result showAllPlants(){
        StringBuilder sb = new StringBuilder();
        for (PlantData data : PlantRegistry.getAll()) {
            sb.append(data.name()).append("\n");}
        return new Result(true,sb.toString(), null);
    }
    public Result showAvailablePlants(){
        Set<Integer> unlockedPlantsId = PlantRepository.loadUnlockedPlants(App.getInstance().getLoggedInUser().getId());
        StringBuilder sb = new StringBuilder();
        for(Integer id: unlockedPlantsId){
            PlantData data = PlantRegistry.getById(id);
            if(data!=null){
                sb.append(data.name()).append("\n");
            }
        }
        return new Result(true,sb.toString(),null);
    }
    public Result addPlant(String plantType){
        PlantData plant = PlantRegistry.getByName(plantType);
        if (plant == null) {
            return new Result(false, "Plant does not exist.", null);
        }
        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(App.getInstance().getLoggedInUser().getId());
        if (!unlocked.contains(plant.id())) {
            return new Result(false, "Plant is locked.", null);
        }
        List<PlantData> selected = App.getInstance().getCurrentGame().getSelectedPlantsForThisGame();
        if (selected.contains(plant)) {
            return new Result(false, "Plant already selected.", null);
        }
        if (selected.size() >= 8) { //بعدا ظرفیت انتخاب گیاه برای هر مرحله چک بشه
            return new Result(false, "Plant selection is full.", null);
        }
        selected.add(plant);
        return new Result(true, "Plant added successfully.", null);
    }

public Result removePlant(String plantType){
    PlantData plant = PlantRegistry.getByName(plantType);
    if (plant == null) {
        return new Result(false, "Plant does not exist.", null);
    }
    List<PlantData> selected =App.getInstance().getCurrentGame().getSelectedPlantsForThisGame();
    if (!selected.remove(plant)) {
        return new Result(false, "Plant is not selected.", null);
    }
    return new Result(true, "Plant removed successfully.", null);
}
public Result boostPlant(String plantType){
    PlantData plant = PlantRegistry.getByName(plantType);
    if (plant == null) {
        return new Result(false, "Plant does not exist.", null);
    }
    Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(App.getInstance().getLoggedInUser().getId());
    if (!unlocked.contains(plant.id())) {
        return new Result(false, "Plant is locked.", null);
    }
    if (App.getInstance().getLoggedInUser().getGems()<2) {
        return new Result(false, "Not enough gems.", null);
    }
    if (PlantBoostRepository.hasBoost(App.getInstance().getLoggedInUser().getId(), plant.id())) {
        return new Result(false, "This plant already has a stored boost.", null);
    }
    App.getInstance().getLoggedInUser().setGems(App.getInstance().getLoggedInUser().getGems() - 2);
    userRepository.updateStats(App.getInstance().getLoggedInUser());
    PlantBoostRepository.addBoost(App.getInstance().getLoggedInUser().getId(), plant.id());
    return new Result(true, "Plant boosted successfully.", null);
}
public Result startGame(){
    if (App.getInstance().getCurrentGame().getSelectedPlantsForThisGame().isEmpty()) {
        return new Result(false,
                "Select some plants before start the game.", null);
    }
    Game currentGame = App.getInstance().getCurrentGame();
     currentGame.loadLevel();
     currentGame.start();
    App.getInstance().setCurrentMenu(Menu.GAME_VIEW);
    return new Result(true,
            "Game started successfully.", null);
}
public Result showCurrentMenu(){
    return new Result(true
            ,"You are now in the plant selection menu.\n",null);
}
public void exitMenu(){
    App.getInstance().setCurrentMenu(Menu.GAME_MENU);
}


}
