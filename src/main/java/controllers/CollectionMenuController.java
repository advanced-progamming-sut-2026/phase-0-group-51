package controllers;

import Data.database.NewsRepository;
import Data.database.PlantRepository;
import Data.database.PlantRepository.PurchaseResult;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.UpgradeData;
import Data.loader.ZombieRegistry;
import models.App;
import models.Result;
import models.User;
import models.Zombie.Behavior.ZombieBehavior;
import models.Zombie.Zombie;

import java.util.*;

public class CollectionMenuController {
    private final NewsRepository newsRepository = new NewsRepository();
    public Result showAllPlants() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before viewing the collection.\n");
        }

        StringBuilder output = new StringBuilder("====== ALL PLANTS ======\n");
        appendPlantGroup(
                output,
                user,
                "ADVENTURE PLANTS",
                PlantRegistry.getAdventurePlantIds()
        );
        appendPlantGroup(
                output,
                user,
                "PURCHASE-ONLY PLANTS",
                PlantRegistry.getPurchasablePlantIds()
        );
        return success(output.toString());
    }

    public Result showAdventurePlants() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before viewing the collection.\n");
        }
        StringBuilder output = new StringBuilder();
        appendPlantGroup(
                output,
                user,
                "ADVENTURE PLANTS",
                PlantRegistry.getAdventurePlantIds()
        );
        return success(output.toString());
    }

    public Result showPurchasablePlants() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before viewing the collection.\n");
        }
        StringBuilder output = new StringBuilder();
        appendPlantGroup(
                output,
                user,
                "PURCHASE-ONLY PLANTS",
                PlantRegistry.getPurchasablePlantIds()
        );
        return success(output.toString());
    }

    public Result showPlants() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before viewing the collection.\n");
        }

        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(user.getId());
        if (unlocked.isEmpty()) {
            return success("You have not unlocked any plants yet.\n");
            }

        Map<Integer, Integer> levels = PlantRepository.loadPlantLevels(user.getId());
        Map<Integer, Integer> seedPackets = PlantRepository.loadSeedPackets(user.getId());
        StringBuilder output = new StringBuilder("====== YOUR PLANTS ======\n");

        for (PlantData plant : sortedPlants()) {
            if (!unlocked.contains(plant.id())) {
                continue;
        }
            output.append(printPlant(
                    plant,
                    levels.getOrDefault(plant.id(), 1),
                    seedPackets.getOrDefault(plant.id(), 0),
                    true
            )).append('\n');
    }

        return success(output.toString());
    }

    public Result showZombies() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before viewing the collection.\n");
        }

        Set<String> discovered = newsRepository.getDiscoveredZombieAliases(user.getId());
        if (discovered.isEmpty()) {
            return success("You have not observed any zombies yet.\n");
        }

        StringBuilder output = new StringBuilder("====== OBSERVED ZOMBIES ======\n");
        int shown = 0;
        for (Zombie zombie : sortedZombieTemplates()) {
            if (containsIgnoreCase(discovered, zombie.getAlias())) {
                output.append(printZombie(zombie)).append('\n');
                shown++;
            }
        }

        if (shown == 0) {
            return success("You have not observed any currently registered zombies yet.\n");
        }
        return success(output.toString());
    }

    public Result showAllZombies() {
        List<Zombie> zombies = sortedZombieTemplates();
        if (zombies.isEmpty()) {
            return failure("No zombie definitions are currently loaded.\n");
        }

        StringBuilder output = new StringBuilder("====== ALL ZOMBIES ======\n");
        for (Zombie zombie : zombies) {
            output.append(printZombie(zombie)).append('\n');
        }
        return success(output.toString());
    }

    public Result purchase(String plantName) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before purchasing a plant.\n");
        }

        PlantData plant = PlantRegistry.getByName(cleanName(plantName));
        if (plant == null) {
            return failure("No plant named '" + cleanName(plantName) + "' was found.\n");
        }


        PlantRegistry.UnlockRule unlockRule = PlantRegistry.getUnlockRule(plant.id());
        if (!unlockRule.isPurchasable()) {
            return failure(
                    "You cannot purchase " + plant.name()
                            + " because it will get unlocked through the Adventure. "
                            + unlockRule.description() + ".\n"
            );
        }

        int purchaseCost = unlockRule.purchaseCost();
        PlantRepository.PurchaseResult result = PlantRepository.tryPurchasePlant(
                user.getId(), plant.id(), purchaseCost
            );

        return switch (result.status()) {
            case SUCCESS -> {
                user.setCoins(result.remainingCoins());
        newsRepository.createNewsForUser(
                user.getId(),
                "New plant unlocked: " + plant.name() + "."
        );
                yield success(
                plant.name() + " was added to your collection. "
                                + "You now have " + result.remainingCoins() + " coins.\n"
                );
            }
            case ALREADY_UNLOCKED -> failure(
                    "You already own " + plant.name() + ".\n"
            );
            case NOT_ENOUGH_COINS -> failure(
                    "You need " + purchaseCost + " coins to purchase "
                            + plant.name() + ", but you only have "
                            + result.remainingCoins() + ".\n"
            );
            case USER_NOT_FOUND -> failure(
                    "The logged-in user no longer exists.\n"
        );
            case DATABASE_ERROR -> failure(
                    "The purchase could not be saved in the database.\n"
            );
        };
    }

    public Result upgrade(String plantName) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before upgrading a plant.\n");}
        PlantData plant = PlantRegistry.getByName(cleanName(plantName));
        if (plant == null) {
            return failure("No plant named '" + cleanName(plantName) + "' was found.\n");}
        if (!PlantRepository.loadUnlockedPlants(user.getId()).contains(plant.id())) {
            return failure("You have not unlocked " + plant.name() + " yet.\n");}
        int currentLevel = PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(plant.id(), 1);
        int maximumLevel = maximumLevel(plant);
        if (currentLevel >= maximumLevel) {
            return failure(plant.name() + " is already at maximum level "
                    + maximumLevel + ".\n");}
        int targetLevel = currentLevel + 1;
        int coinCost = coinCostForLevel(targetLevel);
        int packetCost = seedPacketCostForLevel(targetLevel);
        PlantRepository.UpgradeResult result = PlantRepository.tryUpgradePlant(
                user.getId(), plant.id(), maximumLevel, coinCost, packetCost);
        return switch (result.status()) {
            case SUCCESS -> {
                user.setCoins(result.remainingCoins());
                String description = upgradeDescription(plant, result.newLevel());
                yield success(
                        plant.name() + " upgraded from level " + result.oldLevel()
                                + " to level " + result.newLevel() + ".\n"
                                + "Cost: " + coinCost + " coins and " + packetCost + " seed packets.\n"
                                + "Upgrade effect: " + description + "\n"
                                + "Remaining: " + result.remainingCoins() + " coins and "
                                + result.remainingSeedPackets() + " seed packets.\n");
            }
            case NOT_ENOUGH_COINS -> failure(
                    "Not enough coins to upgrade " + plant.name() + " to level "
                            + targetLevel + ". Required: " + coinCost + ", available: "
                            + result.remainingCoins() + ".\n");
            case NOT_ENOUGH_SEED_PACKETS -> failure(
                    "Not enough seed packets to upgrade " + plant.name() + " to level "
                            + targetLevel + ". Required: " + packetCost + ", available: "
                            + result.remainingSeedPackets() + ".\n");
            case MAX_LEVEL -> failure(
                    plant.name() + " is already at its maximum level.\n");
            case PLANT_NOT_UNLOCKED -> failure(
                    "You have not unlocked " + plant.name() + " yet.\n");
            case USER_NOT_FOUND -> failure("The logged-in user no longer exists.\n");
            case DATABASE_ERROR -> failure(
                    "The plant upgrade could not be saved in the database.\n"
            );
        };
    }
    public Result cheatUpgrade(String plantName) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before using the upgrade cheat.\n");
        }

        PlantData plant = PlantRegistry.getByName(cleanName(plantName));
        if (plant == null) {
            return failure(
                    "No plant named '" + cleanName(plantName) + "' was found.\n"
            );
        }

        if (!PlantRepository.loadUnlockedPlants(user.getId()).contains(plant.id())) {
            return failure(
                    "You must unlock " + plant.name()
                            + " before changing its level.\n"
            );
        }

        int currentLevel = PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(plant.id(), 1);
        int maximumLevel = maximumLevel(plant);
        if (currentLevel >= maximumLevel) {
            return failure(
                    plant.name() + " is already at maximum level "
                            + maximumLevel + ".\n"
            );
        }

        int newLevel = currentLevel + 1;
        PlantRepository.savePlantLevel(user.getId(), plant.id(), newLevel);

        int savedLevel = PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(plant.id(), currentLevel);
        if (savedLevel != newLevel) {
            return failure(
                    "The cheat upgrade could not be saved in the database.\n"
            );
        }

        return success(
                "CHEAT: " + plant.name() + " upgraded from level "
                        + currentLevel + " to level " + newLevel + " for free.\n"
                        + "No coins or seed packets were consumed.\n"
                        + "Upgrade effect: "
                        + upgradeDescription(plant, newLevel) + "\n"
        );
    }

    public Result showAZombie(String zombieName) {
        Zombie zombie = findZombieTemplate(cleanName(zombieName));
        if (zombie == null) {
            return failure("No zombie named '" + cleanName(zombieName) + "' was found.\n");
        }

        User user = App.getInstance().getLoggedInUser();
        boolean observed = user != null
                && newsRepository.hasDiscoveredZombie(user.getId(), zombie.getAlias());

        StringBuilder output = new StringBuilder("====== ZOMBIE DETAILS ======\n")
                .append("Collection status: ")
                .append(observed ? "OBSERVED" : "NOT OBSERVED")
                .append('\n')
                .append(printZombie(zombie))
                .append('\n');
        return success(output.toString());
    }

    public Result showAPlant(String plantName) {
        PlantData plant = PlantRegistry.getByName(cleanName(plantName));
        if (plant == null) {
            return failure("No plant named '" + cleanName(plantName) + "' was found.\n");
        }

        User user = App.getInstance().getLoggedInUser();
        boolean unlocked = user != null
                && PlantRepository.loadUnlockedPlants(user.getId()).contains(plant.id());
        int level = user == null ? 1 : PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(plant.id(), 1);
        int packets = user == null ? 0
                : PlantRepository.getSeedPackets(user.getId(), plant.id());

        StringBuilder output = new StringBuilder("====== PLANT DETAILS ======\n")
                .append(printPlant(plant, level, packets, unlocked))
                .append('\n')
                .append("Base ability: ").append(plant.baseAbility()).append('\n')
                .append("Plant Food: ").append(plant.plantFoodEffect()).append('\n');

        if (level < maximumLevel(plant)) {
            int targetLevel = level + 1;
            output.append("Next upgrade: Level ").append(targetLevel)
                    .append(" - ").append(upgradeDescription(plant, targetLevel)).append('\n')
                    .append("Required: ").append(coinCostForLevel(targetLevel))
                    .append(" coins and ").append(seedPacketCostForLevel(targetLevel))
                    .append(" seed packets.\n");
        } else {
            output.append("This plant is at maximum level.\n");
        }

        return success(output.toString());
    }

    private void appendPlantGroup(
            StringBuilder output,
            User user,
            String heading,
            Collection<Integer> plantIds
    ) {
        Set<Integer> includedIds = new HashSet<>(plantIds);
        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(user.getId());
        Map<Integer, Integer> levels = PlantRepository.loadPlantLevels(user.getId());
        Map<Integer, Integer> seedPackets = PlantRepository.loadSeedPackets(
                user.getId()
        );

        output.append("====== ").append(heading).append(" ======\n");
        for (PlantData plant : sortedPlants()) {
            if (!includedIds.contains(plant.id())) {
                continue;
            }
            output.append(printPlant(
                    plant,
                    levels.getOrDefault(plant.id(), 1),
                    seedPackets.getOrDefault(plant.id(), 0),
                    unlocked.contains(plant.id())
            )).append('\n');
        }
    }

    private List<PlantData> sortedPlants() {
        List<PlantData> plants = new ArrayList<>(PlantRegistry.getAll());
        plants.sort(Comparator.comparingInt(PlantData::id));
        return plants;
    }

    private String printPlant(
            PlantData plant,
            int level,
            int seedPackets,
            boolean unlocked
    ) {
        return "[" + String.format("%02d", plant.id()) + "] "
                + plant.name()
                + " | " + (unlocked ? "UNLOCKED" : "LOCKED")
                + " | Lv." + level + "/" + maximumLevel(plant)
                + " | Seed Packets: " + seedPackets
                + " | Cost: " + plant.cost()
                + " | Dmg: " + plant.damage()
                + " | HP: " + plant.baseHp()
                + " | Tags: " + plant.tags()
                + " | Unlock: "
                + PlantRegistry.getUnlockRule(plant.id()).description();
    }

    private List<Zombie> sortedZombieTemplates() {
        return ZombieRegistry.getTemplates().values().stream()
                .sorted(Comparator.comparing(Zombie::getAlias, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Zombie findZombieTemplate(String requestedName) {
        return ZombieRegistry.getTemplates().values().stream()
                .filter(zombie -> zombie.getAlias().equalsIgnoreCase(requestedName))
                .findFirst()
                .orElse(null);
    }

    private String printZombie(Zombie zombie) {
        return "[" + zombie.getAlias() + "]"
                + " | HP: " + zombie.getMaxHitpoints()
                + " | Speed: " + zombie.getBaseSpeed()
                + " | Eat DPS: " + zombie.getBaseEatDps()
                + " | Wave Cost: " + zombie.getWavePointCost()
                + " | Weight: " + zombie.getWeight()
                + " | Behaviors: " + formatBehaviors(zombie.getBehaviors());
    }

    private String formatBehaviors(List<ZombieBehavior> behaviors) {
        if (behaviors == null || behaviors.isEmpty()) {
            return "none";
        }
        return behaviors.stream()
                .map(behavior -> behavior.getClass().getSimpleName())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((first, second) -> first + ", " + second)
                .orElse("none");
            }

    private boolean containsIgnoreCase(Set<String> values, String candidate) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(candidate));
    }

    private int maximumLevel(PlantData plant) {
        return plant.upgrades() == null ? 1 : plant.upgrades().size() + 1;
    }

    private int seedPacketCostForLevel(int targetLevel) {
        return switch (targetLevel) {
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default -> 20 * Math.max(1, targetLevel - 3);
        };
    }

    private int coinCostForLevel(int targetLevel) {
        return switch (targetLevel) {
            case 2 -> 1000;
            case 3 -> 2000;
            case 4 -> 4000;
            default -> 4000 * Math.max(1, targetLevel - 3);
        };
    }

    private String upgradeDescription(PlantData plant, int targetLevel) {
        if (plant.upgrades() == null) {
            return "No additional upgrade data";
        }
        return plant.upgrades().stream()
                .filter(upgrade -> upgrade.level() == targetLevel)
                .map(UpgradeData::description)
                .findFirst()
                .orElse("Plant statistics improved");
    }

    private String cleanName(String name) {
        return name == null ? "" : name.trim();
    }

    private Result success(String message) {
        return new Result(true, message, null);
        }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
}


