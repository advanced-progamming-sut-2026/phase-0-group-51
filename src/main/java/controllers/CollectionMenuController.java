package controllers;

import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import models.App;
import models.Result;
import models.User;
import models.Zombie.Behavior.ZombieBehavior;
import models.Zombie.Zombie;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionMenuController {

    private String printZombie(Zombie zombie) {
        StringBuilder output = new StringBuilder();
        output.append("[")
                .append(zombie.getAlias())
                .append("] ")
                .append(" | HP: ")
                //.append(zombie.getMaxHitpoints())
                .append(" | Speed: ")
                .append(zombie.getBaseSpeed())
                .append(" | Eat DPS: ")
                .append(zombie.getBaseEatDps())
                .append(" | Wave Cost: ")
                .append(zombie.getWavePointCost())
                .append(" | Weight: ")
                .append(zombie.getWeight())
                .append(" | Behaviors: ")
                .append(formatBehaviors(zombie.getBehaviors()));

        return output.toString();
    }

    private String formatBehaviors(List<ZombieBehavior> behaviors) {
        if (behaviors.isEmpty()) {
            return "none";
        }
        StringBuilder result = new StringBuilder();
        for (ZombieBehavior behavior : behaviors) {
            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(behavior.getClass().getSimpleName());
        }
        return result.toString();
    }

    public Result showAllPlants() {
        StringBuilder output = new StringBuilder();
        output.append("======All Plants======\n");
        for (PlantData plant : PlantRegistry.getAll()) {
            // i didn't implement the level's printing just yet :)
            output.append(printPlant(plant));
            output.append("\n");
        }
        return new Result(true, output.toString(), null);
    }

    public Result showPlants() {
        User user = App.loggedInUser;
        Set<Integer> plantIds = PlantRepository.loadUnlockedPlants(user.getId());
        //Map<Integer, Integer> levels = PlantRepository.loadPlantLevels(user.getId());
        List<PlantData> plants = new ArrayList<>(PlantRegistry.getAll());
        plants.sort(Comparator.comparingInt(PlantData::id));

        StringBuilder output = new StringBuilder();
        output.append("======Your Plants======\n");
        for (PlantData data : plants) {
            if (plantIds.contains(data.id())) {
                output.append(printPlant(data));
                output.append("\n");
            }
        }
        return new Result(true, output.toString(), null);
    }

    private String printPlant(PlantData data) {
        StringBuilder output = new StringBuilder();
        output.append("[")
                .append(String.format("%02d", data.id()))
                .append("] ")
                .append(data.name())
//                .append(" | Lv.")
//                .append(orDefault)
                .append(" | Cost: ")
                .append(data.cost())
                .append(" | Dmg: ")
                .append(data.damage())
                .append(" | HP: ")
                .append(data.baseHp())
                .append(" | Tags: ")
                .append(data.tags());

        return output.toString();
    }

    public Result showZombies() {
        return null;
    }

    public Result showAllZombies() {
        return null;
    }

    public Result purchase(String plantName) {
        User user = App.loggedInUser;
        Set<Integer> plantIds = PlantRepository.loadUnlockedPlants(user.getId());
        PlantData plant = PlantRegistry.getByName(plantName);
        if (plant == null) {
            return new Result(false, "No such plant. try writing the name again:)", null);
        } else if (plantIds.contains(plant.id())) {
            return new Result(false, "You already have this plant.", null);
        } else if(2000 > user.getCoins()){
            return new Result(false, "You don't have enough coins.", null);
        } else{
            user.setCoins(user.getCoins() - 2000);
            UserRepository userRepository = new UserRepository();
            userRepository.updateStats(user);
            PlantRepository.unlockPlant(user.getId(), plant.id());
            return new Result(true, plant.name() + " is added to your plants collection.", null);
        }
    }

    public Result upgrade(String plantName) {
        User user = App.loggedInUser;
        PlantData plant = PlantRegistry.getByName(plantName);
        if (plant == null) {
            return new Result(false, "No such plant. try writing the name again:)", null);
        } else if (!PlantRepository.loadUnlockedPlants(user.getId()).contains(plant.id())) {
            return new Result(false, "You don't have "+ plant.name() + " yet.", null);
        }
        Map<Integer, Integer> levels = PlantRepository.loadPlantLevels(user.getId());
        int currentLevel = levels.getOrDefault(plant.id(), 1);

        if (currentLevel >= 4) {
            return new Result(false, plant.name() + " is already at max level.", null);
        }
//        else if () {
//        for validating the needed seed packet or coins
        else{
            // az user kam mikonim meghdar khaste ro
            int newLevel = currentLevel + 1;
            PlantRepository.savePlantLevel(user.getId(), plant.id(), newLevel);
            return new Result(true, plant.name() + " is now upgraded.", null);
        }
    }

    public Result showAZombie(String zombieName) {
        return null;
    }

    public Result showAPlant(String plantName) {
        List<PlantData> plants = new ArrayList<>(PlantRegistry.getAll());

        StringBuilder output = new StringBuilder();
        for(PlantData plant : plants) {
            if(plant.name().equalsIgnoreCase(plantName)) {
                output.append(printPlant(plant));
            }
        }
        return new Result(true, output.toString(), null);
    }
}
