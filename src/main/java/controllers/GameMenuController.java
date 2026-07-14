package controllers;

import data.database.ProgressRepository;
import data.database.UserRepository;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.games.ChapterTheme;
import models.games.Game;

public class GameMenuController {
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
        Game newGame = new Game();
        newGame.setCurrentChapterIndex(requestedChapterIndex);
        newGame.setCurrentLevelIndex(requestedLevelIndex);
        App.getInstance().setCurrentGame(newGame);
        App.getInstance().setCurrentMenu(Menu.PlantSelection_Menu);
        return new Result(true, "Entered " + themes[requestedChapterIndex].getName()
                + " Level " + (requestedLevelIndex + 1)
                + ".\nYou are now in the Plant Selection Menu.", null);
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
}
