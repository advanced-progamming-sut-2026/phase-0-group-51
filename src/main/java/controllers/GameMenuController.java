package controllers;

import Data.database.PlantRepository;
import Data.database.ProgressRepository;
import Data.database.UserRepository;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.Level;

public class GameMenuController {
    private static final int[] CHAPTER_ONE_LEVEL_ONE_PLANTS = {
            1, 6, 7, 9, 25, 30, 44, 55
    };

    public Result handleEnterChapter(String chapter) {
        int requestedChapterIndex = -1;
        ChapterTheme[] themes = ChapterTheme.values();
        for (int i = 0; i < themes.length; i++) {
            String themeString = themes[i].name().replace("_", " ");
            if (themeString.equalsIgnoreCase(chapter)) {
                requestedChapterIndex = i;
                break;
            }
        }
        if (requestedChapterIndex==-1) {
            return new Result(false, "Chapter not found. " +
                    "Valid chapters: Ancient Egypt, Frostbite Caves, Big Wave Beach, Dark Ages.\n", null);
        }
        ProgressRepository progress = new ProgressRepository();
        int[] progressItem = progress.getCurrentProgress(App.loggedInUser.getId());
        int unlockedChapterIndex = progressItem[0] - 1;
        int unlockedLevelIndex = progressItem[1] - 1;
        if (requestedChapterIndex > unlockedChapterIndex) {
            return new Result(false, "This chapter is locked for you\n!", null);
        }
        int requestedLevelIndex = 0;
        if (requestedChapterIndex == unlockedChapterIndex) {
            requestedLevelIndex = unlockedLevelIndex;
        }
        unlockChapterOneLevelOnePlants(requestedChapterIndex, requestedLevelIndex);
        ChapterTheme requestedTheme = themes[requestedChapterIndex];
        Level requestedLevel = requestedTheme.getLevels().get(requestedLevelIndex);
        Game newGame = new Game();
        newGame.setCurrentChapterIndex(requestedChapterIndex);
        newGame.setCurrentLevelIndex(requestedLevelIndex);
        App.getInstance().setCurrentGame(newGame);
        if (!requestedLevel.type().usesPlantSelection()) {
            return startLevelDirectly(newGame, requestedTheme, requestedLevel);
        }
        App.getInstance().setCurrentMenu(Menu.PlantSelection_Menu);
        return new Result(true, "Entered " + themes[requestedChapterIndex].getName()
                + " Level " + (requestedLevelIndex + 1)
                + ".\nYou are now in the Plant Selection Menu.", null);
    }
    private Result startLevelDirectly(Game game, ChapterTheme theme, Level level) {
        try {
            game.loadLevel();
            game.start();
            App.getInstance().setCurrentMenu(Menu.GAME_VIEW);
            String firstPlant = game.getConveyorBeltPlants().isEmpty() ? "none" :
                    game.getConveyorBeltPlants().get(0).name();
            return new Result(
                    true,
                    "Entered " + theme.getName() + " Level " + level.levelNumber() + ".\n"
                            + "so plant selection was skipped.\n"
                            + "First conveyor plant: "
                            + firstPlant + ".\n"
                            + "A new random unlocked plant " + "arrives every 12 seconds.\n",
                    null
            );
        } catch (RuntimeException exception) {
            App.getInstance().setCurrentGame(null);
            App.getInstance().setCurrentMenu(Menu.GAME_MENU);
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "The level could not be initialized.";
            }
            return new Result(false, "Could not start the Conveyor Belt level: " + message + "\n", null);
        }
    }
    public void handleGreenhouse() {
        App.getInstance().setCurrentMenu(Menu.GREENHOUSE_MENU);
    }

    public void handleTravellog() {
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
    }

    public void leaderboard() {
        App.getInstance().setCurrentMenu(Menu.LEADERBOARD_MENU);
    }

    public Result cheatAdd(int amount, String kind) {
        if(amount <= 0){
            return new Result(false, "Please enter a positive amount." , null);
        } else if (kind.equalsIgnoreCase("coin")) {
            User user = App.loggedInUser;
            user.setCoins(user.getCoins() + amount);
            UserRepository userRepository = new UserRepository();
            userRepository.updateStats(user);
            return new Result(true, "Successfully added coins." , null);
        } else if (kind.equalsIgnoreCase("diamond")) {
            User user = App.loggedInUser;
            user.setGems(user.getGems() + amount);
            UserRepository userRepository = new UserRepository();
            userRepository.updateStats(user);
            return new Result(true, "Successfully added gems." , null);
        } else{
            return new Result(false, "You can only add coin or diamond please specify." , null);
        }
    }

    public Result handleEnterMenu(String menuName) {
        if(menuName.equalsIgnoreCase("Collection")){
            App.getInstance().setCurrentMenu(Menu.COLLECTION_MENU);
            return new Result(true,"You are now in the Collection menu.",null);
        }else{
            return new Result(false, "You can only go to the Collection menu!",null);
        }
    }
    private void unlockChapterOneLevelOnePlants(int chapterIndex, int levelIndex) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null || chapterIndex != 0 || levelIndex != 0) {
            return;
        }
        PlantRepository.unlockPlants(user.getId(), CHAPTER_ONE_LEVEL_ONE_PLANTS);
    }
}
