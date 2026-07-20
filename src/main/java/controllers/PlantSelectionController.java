package controllers;

import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.App;
import models.Plant.PlantTag;
import models.Result;
import models.User;
import models.enums.Menu;
import models.games.Game;
import models.games.LevelType;
import models.games.ScoringGame;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class PlantSelectionController {
    private final UserRepository userRepository;
    public PlantSelectionController() {
        this.userRepository = new UserRepository();
    }
    public Result showAllPlants(){
        Game game = App.getInstance().getCurrentGame();
        StringBuilder result = new StringBuilder();
        PlantRegistry.getAll().stream()
                .sorted(Comparator.comparingInt(PlantData::id))
                .filter(plant -> !isForbiddenForCurrentLevel(plant))
                .forEach(plant -> result
                        .append(plant.name())
                        .append(selectionStatus(game, plant))
                        .append("\n"));

        return new Result(true, result.toString(), null);
            }

    public Result showAvailablePlants(){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) {
            return new Result(false, "No level is selected.", null);}

        if (game.isForcedLoadoutMode()) {
            StringBuilder forcedPlants = new StringBuilder();
            for (PlantData plant : game.getSelectedPlantsForThisGame()) {
                forcedPlants.append(plant.name()).append(" [forced]\n");
        }
            return new Result(true, forcedPlants.toString(), null);
    }

        Set<Integer> unlockedPlantIds = PlantRepository.loadUnlockedPlants(
                App.getInstance().getLoggedInUser().getId()
        );
        StringBuilder result = new StringBuilder();

        unlockedPlantIds.stream()
                .map(PlantRegistry::getById)
                .filter(plant -> plant != null)
                .sorted(Comparator.comparingInt(PlantData::id))
                .filter(plant -> !isForbiddenForCurrentLevel(plant))
                .filter(plant -> !game.isPlantLockedByFamilyMode(plant))
                .forEach(plant -> result
                        .append(plant.name())
                        .append(game.isFamilyChoicePlant(plant)
                                ? " [family choice]"
                                : "")
                        .append("\n"));

        return new Result(true, result.toString(), null);
    }

    public Result addPlant(String plantType){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) return new Result(false, "No level is selected.", null);
        PlantData plant = PlantRegistry.getByName(plantType);
        if (plant == null) {
            return new Result(false, "Plant does not exist.", null);
        }
        if (game.isForcedLoadoutMode()) {
            return new Result(
                    false, "All eight plant slots are filled and locked in Forced Plants mode."
                    , null);
        }

        if (isForbiddenForCurrentLevel(plant)) {
            return new Result(
                    false, "Sun-producing and water-only plants cannot be selected "
                            + "in the dry Plant What You Get level.", null
            );
        }
        if (game.isPlantLockedByFamilyMode(plant)) {
            PlantData allowedPlant = game.getAllowedPlantForFamily(plant);
            String allowedName = allowedPlant == null
                    ? "another plant" : allowedPlant.name();
            return new Result(
                    false,
                    plant.name() + " is locked in Family Lock mode. Only "
                            + allowedName + " is available from the " + plant.category() + " family.",
                    null
            );
        }

        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(App.getInstance().getLoggedInUser().getId());
        if (!unlocked.contains(plant.id())) {
            return new Result(false, "Plant is locked.", null);
        }
        List<PlantData> selected = game.getSelectedPlantsForThisGame();
        if (selected.contains(plant)) {
            return new Result(false, "Plant already selected.", null);
        }
        if (selected.size() >= 8) {
            return new Result(false, "Plant selection is full.", null);
        }
        selected.add(plant);
        return new Result(true, "Plant added successfully.", null);
    }
public Result removePlant(String plantType){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) {
            return new Result(false, "No level is selected.", null);
        }

    PlantData plant = PlantRegistry.getByName(plantType);
    if (plant == null) {
        return new Result(false, "Plant does not exist.", null);
    }

        if (game.isForcedLockedPlant(plant)) {
            return new Result(
                    false,
                    plant.name() + " is locked in a forced slot and cannot be removed.",
                    null
            );
        }

        List<PlantData> selected = game.getSelectedPlantsForThisGame();
    if (!selected.remove(plant)) {
        return new Result(false, "Plant is not selected.", null);
    }
    return new Result(true, "Plant removed successfully.", null);
}
// for testing all plants
public Result unlockAllPlantsForTesting() {
    User user = App.getInstance().getLoggedInUser();
    if (user == null) {
        return new Result(false, "You must be logged in.", null);
    }

    Set<Integer> unlockedBefore = PlantRepository.loadUnlockedPlants(user.getId());
    for (PlantData plant : PlantRegistry.getAll()) {
        if (!unlockedBefore.contains(plant.id())) {
            PlantRepository.unlockPlant(user.getId(), plant.id());
        }
    }

    Set<Integer> unlockedAfter = PlantRepository.loadUnlockedPlants(user.getId());
    int newlyUnlocked = unlockedAfter.size() - unlockedBefore.size();
    if (newlyUnlocked == 0) {
        return new Result(
                true,
                "CHEAT: All plants are already unlocked for testing.",
                null
        );
    }

    return new Result(
            true,
            "CHEAT: " + newlyUnlocked
                    + " plants were unlocked for testing. All registered plants "
                    + "are now available in plant selection.",
            null
    );
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
    Game currentGame = App.getInstance().getCurrentGame();
    if (currentGame == null) {return new Result(false, "No level is selected.", null);}
    if (currentGame.getSelectedLevel().type()
            == LevelType.LOVE_YOUR_PLANTS) {
        return new Result(
                true, "Game started successfully.\n"
                        + "Special rule: you lose when " + currentGame.getSelectedLevel().plantLossLimit()
                        + " plants are destroyed or eaten.\n", null
        );
    }
        if (currentGame.isLockedPlantsLevel()
                && !currentGame.hasChosenLockedPlantsMode()) {
            return new Result(
                    false,
                    "Choose Family Lock or Forced Plants before starting the level.",
                    null
            );
        }

        if (currentGame.isForcedLoadoutMode()
                && currentGame.getSelectedPlantsForThisGame().size() != 8) {
            return new Result(
                    false,
                    "Forced Plants mode requires exactly eight locked plants.",
                    null
            );
        }

        if (currentGame.getSelectedPlantsForThisGame().stream()
                .anyMatch(currentGame::isPlantLockedByFamilyMode)) {
            return new Result(
                    false,
                    "Remove every plant that is locked by Family Lock mode.",
                    null
            );
        }

    if (currentGame.getSelectedPlantsForThisGame().stream()
            .anyMatch(this::isForbiddenForCurrentLevel)) {
        return new Result(
                false,
                "Remove all sun-producing and water-only plants before starting "
                        + "Plant What You Get.",
                null
        );
    }
    if (currentGame.getSelectedPlantsForThisGame().isEmpty()) {
        return new Result(false,
                    "Select some plants before starting the game.",
                    null
            );
    }

        try {
     currentGame.loadLevel();
     currentGame.start();
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "The level could not be initialized.";
            }
            return new Result(false, message, null);
        }

    App.getInstance().setCurrentMenu(Menu.GAME_VIEW);
    if (currentGame.getGameState().isTimedBattleActive()) {
        return new Result(
                true,
                "Timed Battle started. Complete both objectives before time runs out.\n"
                        + currentGame.getGameState().timedBattleStatusLine(),
                null
        );
    }
    if (currentGame.getGameState().isSaveOurSeedsActive()) {
        return new Result(
                true,
                "Save Our Seeds started. "
                        + currentGame.getGameState().getSaveOurSeedsStatus()
                        + " Losing any protected plant loses the level immediately. "
                        + "Use 'show map' to see the warning rows and E markers.",
                null
        );
    }
    if (currentGame.isPreparingPlantWhatYouGet()) {
        return new Result(
                true,
                "Plant What You Get preparation started with "
                        + currentGame.getGameState().getSun()
                        + " sun. The lawn is dry and zombie waves are paused. "
                        + "Plant without recharge, then use 'start zombie waves'.",
                null
        );
    }
    return new Result(true, "Game started successfully.", null);
}
public Result showCurrentMenu(){
        Game game = App.getInstance().getCurrentGame();
        if (game != null && game.hasChosenLockedPlantsMode()) {
            return new Result(
                    true,
                    "You are in the Plant Selection Menu. Locked Plants mode: "
                            + game.getLockedPlantsMode().getCommandName() + ".\n",
                    null
            );
}
        return new Result(
                true,
                "You are now in the Plant Selection Menu.\n",
                null
        );
    }

public void exitMenu(){
    Game currentGame = App.getInstance().getCurrentGame();
    boolean scoringGame = currentGame instanceof ScoringGame;
    App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(
                scoringGame ? Menu.MAIN_MENU : Menu.GAME_MENU
        );
    }

    private String selectionStatus(Game game, PlantData plant) {
        if (game == null) {
            return "";
        }
        if (game.isForcedLoadoutMode()) {
            return game.isForcedLockedPlant(plant)
                    ? " [forced]"
                    : " [locked in Forced Plants mode]";
        }
        if (game.isPlantLockedByFamilyMode(plant)) {
            PlantData allowedPlant = game.getAllowedPlantForFamily(plant);
            String allowedName = allowedPlant == null
                    ? "another plant"
                    : allowedPlant.name();
            return " [locked; " + allowedName + " is the family choice]";
        }
        if (game.isFamilyChoicePlant(plant)) {
            return " [family choice]";
        }
        return "";
}

private boolean isForbiddenForCurrentLevel(PlantData plant) {
    Game game = App.getInstance().getCurrentGame();
    if (game == null || plant == null) {
        return false;
    }
    int chapterIndex = game.getCurrentChapterIndex();
    int levelIndex = game.getCurrentLevelIndex();
        if (chapterIndex < 0 || chapterIndex >= game.getChapters().size()) {
        return false;
    }
        if (levelIndex < 0
                || levelIndex >= game.getChapters().get(chapterIndex).getLevels().size()) {
            return false;
        }

    LevelType type = game.getChapters().get(chapterIndex).getLevels().get(levelIndex)
            .type();
    return type == LevelType.PLANT_WHAT_YOU_GET && (isSunProducer(plant)
            || plant.id() == 58 || plant.tags().contains(PlantTag.WATER));
}

private boolean isSunProducer(PlantData plant) {
    String category = plant.category() == null ? ""
            : plant.category().replaceAll("[^A-Za-z]", "").toLowerCase();
    return category.equals("sunproducer")
            || plant.tags().contains(PlantTag.SUN)
            || (plant.id() >= 1 && plant.id() <= 5);
}


}
