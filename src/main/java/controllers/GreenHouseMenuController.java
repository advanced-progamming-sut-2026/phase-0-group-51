package controllers;

import Data.database.GreenHouseRepository;
import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
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

    public GreenHouseMenuController(){
        this.validation = new GreenHouseMenuValidation();
    }
    public Result showCurrentMenu(){
        if (currentUser() == null) {
            return loginRequired();
    }
        return new Result(true, "You are now in the greenhouse.\n", null);
    }

    public Result exitMenu(){
        App.getInstance().setCurrentMenu(Menu.GAME_MENU);
        return new Result(true,"Going back to the game menu...\n",null);
    }

    public Result showGreenHouse(){
        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        GreenHouse greenHouse = user.getGreenHouse();
        if (greenHouse == null) {
            return new Result(false, "Your greenhouse could not be loaded.\n", null);
        }

        StringBuilder output = new StringBuilder();
        for(int row=1;row<=GreenHouse.ROWS;row++){
            for(int column=1;column<=GreenHouse.COLUMNS;column++){
                FlowerPot pot = greenHouse.getPot(row,column);
                if(!pot.isUnlocked()){
                    output.append("[LOCKED]");
                } else if (pot.isEmpty()) {
                    output.append("[EMPTY]");
                } else if (pot.isReady()) {
                    output.append('[').append(pot.getPlantName()).append(" READY]");
                } else {
                    output.append('[').append(pot.getPlantName()).append(' ')
                            .append(pot.getRemainingTimeString()).append(']');
                }
                if (column != GreenHouse.COLUMNS) {
                    output.append("   ");
                }
            }
            output.append('\n');
        }
        return new Result(true, output.toString(), null);
    }
    public Result plantPot(String x,String y){
        if (!validation.isNumberXValid(x) || !validation.isNumberYValid(y)) {
            return new Result(false, "Please enter valid x and y.\n", null);
        }

        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        GreenHouse greenHouse = user.getGreenHouse();
        if (greenHouse == null) {
            return new Result(false, "Your greenhouse could not be loaded.\n", null);
        }

        int row = validation.y;
        int column = validation.x;
        FlowerPot pot = greenHouse.getPot(row,column);
        if (!pot.isUnlocked()) {
            return new Result(false, "This flower pot is locked!\n", null);
        }
        if (!pot.isEmpty()) {
            return new Result(
                    false,
                    "This flower pot already contains a plant!\n",
                    null
            );
        }

        int plantId = chooseRandomPlant(user);
        LocalDateTime plantedAt = LocalDateTime.now();
        boolean saved = GreenHouseRepository.plantPot(
                user.getId(), row, column, plantId, plantedAt
        );
        if (!saved) {
            return new Result(false, "The plant could not be saved.\n", null);
        }

        pot.setPlantId(plantId);
        pot.setPlantedAt(plantedAt);
        return new Result(
                true,
                "Plant planted successfully.\n", null
        );
    }
    public Result collectPlant(String x, String y){
        if (!validation.isNumberXValid(x) || !validation.isNumberYValid(y)) {
            return new Result(false, "Please enter valid x and y.\n", null);
        }

        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        GreenHouse greenHouse = user.getGreenHouse();
        if (greenHouse == null) {
            return new Result(false, "Your greenhouse could not be loaded.\n", null);
        }

        int row = validation.y;
        int column = validation.x;
        FlowerPot pot = greenHouse.getPot(row, column);
        if (!pot.isUnlocked()) {
            return new Result(false, "This flower pot is locked.\n", null);
        }
        if (pot.isEmpty()) {
            return new Result(false,
                    "This flower pot is empty.\n", null);
        }
        if (!pot.isReady()) {
            return new Result(false,
                    "This plant is not ready for collection.\n", null);
        }

        int plantId = pot.getPlantId();
        GreenHouseRepository.CollectResult result =
                GreenHouseRepository.collectPot(
                        user.getId(), row, column, plantId
                );
        if (result.status() != GreenHouseRepository.CollectStatus.SUCCESS) {
            return greenhouseCollectFailure(result.status());
        }

        if (plantId == FlowerPot.MARIGOLD_ID) {
            user.setCoins(result.newCoinBalance());
        }
        pot.clear();
        return new Result(true,
                "Plant collected successfully.\n", null);
    }
    public Result growPlant(String x, String y){
        if (!validation.isNumberXValid(x) || !validation.isNumberYValid(y)) {
            return new Result(false, "Please enter valid x and y.\n", null);
        }

        User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        GreenHouse greenHouse = user.getGreenHouse();
        if (greenHouse == null) {
            return new Result(false, "Your greenhouse could not be loaded.\n", null);
        }

        int row = validation.y;
        int column = validation.x;
        FlowerPot pot = greenHouse.getPot(row, column);
        if (!pot.isUnlocked()) {
            return new Result(false,  "This flower pot is locked!\n",null);
        }
        if (pot.isEmpty()) {
            return new Result(
                    false,
                    "This flower pot does not contain a plant!\n",
                    null
            );
        }
        if (pot.isReady()) {
            return new Result(
                    false,
                    "This plant is already ready to collect!\n",
                    null
            );
        }

        int gemsNeeded = Math.toIntExact(pot.getCeilRemainingHours());
        int growthHours = pot.getPlantId() == FlowerPot.MARIGOLD_ID ? 2 : 8;
        LocalDateTime readyPlantedAt = LocalDateTime.now().minusHours(growthHours);
        GreenHouseRepository.GrowResult result =
                GreenHouseRepository.growPotInstantly(
                        user.getId(),
                        row,
                        column,
                        gemsNeeded,
                        readyPlantedAt
                );

        return switch (result.status()) {
            case SUCCESS -> {
                user.setGems(result.newGemBalance());
                pot.setPlantedAt(readyPlantedAt);
                yield new Result(true, "The plant grew instantly.\n", null);
    }
            case NOT_ENOUGH_GEMS -> new Result(
                    false, "You do not have enough gems!\n", null
            );
            case POT_LOCKED -> new Result(
                    false, "This flower pot is locked!\n", null
            );
            case POT_EMPTY -> new Result(
                    false, "This flower pot does not contain a plant!\n", null
            );
            case POT_NOT_FOUND, DATABASE_ERROR -> new Result(
                    false, "The instant growth could not be saved.\n", null
            );
        };
    }

    public void enterShop(){
        App.getInstance().setCurrentMenu(Menu.SHOP_MENU);
    }

    private User currentUser() {
        return App.getInstance().getLoggedInUser();
    }

    private Result loginRequired() {
        return new Result(
                false,
                "You must log in before using the greenhouse.\n",
                null
        );
    }

    private Result greenhouseCollectFailure(
            GreenHouseRepository.CollectStatus status
    ) {
        return switch (status) {
            case POT_LOCKED -> new Result(
                    false, "This flower pot is locked.\n", null
            );
            case POT_EMPTY -> new Result(
                    false, "This flower pot is empty.\n", null
            );
            case POT_NOT_FOUND, DATABASE_ERROR -> new Result(
                    false, "The plant collection could not be saved.\n", null
            );
            case SUCCESS -> new Result(true, "", null);
        };
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
