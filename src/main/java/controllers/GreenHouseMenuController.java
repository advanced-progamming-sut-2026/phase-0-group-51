package controllers;

import data.database.GreenHouseRepository;
import data.database.PlantBoostRepository;
import data.database.PlantRepository;
import data.database.UserRepository;
import data.loader.PlantData;
import data.loader.PlantRegistry;
import controllers.validation.GreenHouseMenuValidation;
import models.App;
import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;
import models.greenHouse.GreenHousePlantHelper;
import models.Result;
import models.User;
import models.enums.Menu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GreenHouseMenuController  {
    private final Random random = new Random();
    private final GreenHouseMenuValidation validation;
    private final UserRepository repository;
    public GreenHouseMenuController(){
        this.validation = new GreenHouseMenuValidation();
        this.repository = new UserRepository();
    }
    public Result showCurrentMenu(){
        return new Result(true
                ,"You are now in the greenhouse.\n",null);
    }
    public Result exitMenu(){
        App.getInstance().setCurrentMenu(Menu.GAME_MENU);
        return new Result(true,"Going back to the game menu...\n",null);
    }

    public Result showGreenHouse(){
        User user = App.getInstance().getLoggedInUser();
        GreenHouse greenHouse = user.getGreenHouse();
        StringBuilder sb = new StringBuilder();
        for(int row=1;row<=GreenHouse.ROWS;row++){
            for(int column=1;column<=GreenHouse.COLUMNS;column++){
                FlowerPot pot = greenHouse.getPot(row,column);
                if(!pot.isUnlocked()){
                    sb.append("[LOCKED]");
                } else if (pot.isEmpty()) {
                    sb.append("[EMPTY]");
                }
                else if(pot.isReady()){
                    sb.append("[").append(pot.getPlantName()).append(" READY]");
                }
                 else{
                    sb.append("[").append(pot.getPlantName());
                    sb.append(" ").append(pot.getRemainingTimeString());
                    sb.append("]");
                }
                if (column != GreenHouse.COLUMNS) {
                    sb.append("   ");
                }
            }
            sb.append("\n");
        }
        return new Result(true,sb.toString(),null);
    }
    public Result plantPot(String x,String y){
        if(!validation.isNumberXValid(x) || !validation.isNumberYValid(y))
            return new Result(false,"Please enter valid x and y\n",null);
        int row = validation.y;
        int column = validation.x;
        User user = App.getInstance().getLoggedInUser();
        GreenHouse greenHouse = user.getGreenHouse();
        FlowerPot pot = greenHouse.getPot(row,column);
        if(!pot.isUnlocked()) return new Result(false,
                "This flower pot is locked!\n",null);
        if(!pot.isEmpty()) return new Result(false,
                "This flower pot already contains a plant!\n",null);
        int plantId = chooseRandomPlant(user);
        pot.setPlantId(plantId);
        pot.setPlantedAt(LocalDateTime.now());
        GreenHouseRepository.updatePot(user.getId(), pot);
        return new Result(
                true,
                "Plant planted successfully.\n", null
        );
    }
    public Result collectPlant(String x, String y){
        if(!validation.isNumberXValid(x) || !validation.isNumberYValid(y))
            return new Result(false,"Please enter valid x and y\n",null);
        User user = App.getInstance().getLoggedInUser();
        FlowerPot pot = user.getGreenHouse().getPot(validation.y,validation.x);
        if (pot.isEmpty()) {
            return new Result(false,
                    "This flower pot is empty.\n", null);
        }
        if (!pot.isReady()) {
            return new Result(false,
                    "This plant is not ready for collection.\n", null);
        }
        Integer plantId = pot.getPlantId();
        if (plantId == FlowerPot.MARIGOLD_ID) {
            user.setCoins(user.getCoins() + 500);
            repository.updateStats(user);
        } else {
            PlantBoostRepository.addBoost(user.getId(), plantId);
        }
        pot.clear();
        GreenHouseRepository.updatePot(user.getId(), pot);
        return new Result(true,
                "Plant collected successfully.\n", null);
    }
    public Result growPlant(String x, String y){
        if(!validation.isNumberXValid(x) || !validation.isNumberYValid(y))
            return new Result(false,"Please enter valid x and y\n",null);
        int row = validation.y;
        int column = validation.x;
        User user = App.getInstance().getLoggedInUser();
        FlowerPot pot = user.getGreenHouse().getPot(row,column);
        if(!pot.isUnlocked())
            return new Result(false,  "This flower pot is locked!\n",null);
        if(pot.isEmpty())
            return new Result(false, "This flower pot does not contain a plant!\n",null);
        if (pot.isReady())
            return new Result(false, "This plant is already ready to collect!\n",null);
        long gemsNeeded = pot.getCeilRemainingHours();
        if (user.getGems() < gemsNeeded) {
            return new Result(false,
                    "You do not have enough gems!\n", null);
        }
        user.setGems(user.getGems() - (int) gemsNeeded);
        pot.finishGrowing();
        repository.updateStats(user);
        GreenHouseRepository.updatePot(user.getId(), pot);
        return new Result(true,
                "The plant grew instantly.\n", null);
    }
    public void enterShop(){
        App.getInstance().setCurrentMenu(Menu.SHOP_MENU);
    }
    private int chooseRandomPlant(User user) {
        if (random.nextBoolean()) {
            return FlowerPot.MARIGOLD_ID;
        }
        Set<Integer> unlockedPlants = PlantRepository.loadUnlockedPlants(user.getId());
        List<Integer> candidates = new ArrayList<>();
        for (Integer plantId : unlockedPlants) {
            PlantData plant = PlantRegistry.get(plantId);
            if (plant != null &&   GreenHousePlantHelper.canAppearInGreenHouse(plant)) {
                candidates.add(plantId);
            }
        }
        if (candidates.isEmpty()) {
            return FlowerPot.MARIGOLD_ID;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}
