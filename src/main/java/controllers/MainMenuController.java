package controllers;

import Data.database.NewsRepository;
import Data.database.UserRepository;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.games.ScoringGame;

public class MainMenuController {
    private final UserRepository repository;
    private final NewsRepository newsRepository;
    public MainMenuController() {
        this.repository = new UserRepository();
        this.newsRepository = new NewsRepository();
    }
    public Result logout(){
        App app = App.getInstance();
        if (app.getLoggedInUser() == null) {
            return new Result(false, "No user is currently logged in.\n", null);
        }
        boolean cleared = repository.clearStayLoggedIn();
        if (!cleared) {
            return new Result(false,
                    "Logout failed because the saved session " + "could not be cleared.\n",
                    null
            );
        }
        app.setLoggedInUser(null);
        app.setCurrentGame(null);
        app.setCurrentMenu(Menu.SIGN_UP_MENU);
        Menu.resetAllViews();
        return new Result(
                true, "Logout successful.\n", null
        );
    }
    public Result showCurrentMenu(){
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return new Result(false, "You must log in first.\n", null);
        }
        int unreadCount = newsRepository.countUnreadNews(user.getId());
        String newsOption = unreadCount > 0
                ? "news [NEW: " + unreadCount + "]"
                : "";
        String message =
                "You are now in the main menu.\n"  + newsOption + "\n";
        return new Result(true, message, null);
    }
    public Result enterMenu(String menuName){
        switch (menuName.toLowerCase()){
            case "game"-> App.getInstance().setCurrentMenu(Menu.GAME_MENU);
            case "setting"-> App.getInstance().setCurrentMenu(Menu.SETTING_MENU);
            case "network"-> App.getInstance().setCurrentMenu(Menu.NETWORK_MENU);
            case "news"-> App.getInstance().setCurrentMenu(Menu.NEWS_MENU);
            case "leaderboard" -> App.getInstance().setCurrentMenu(Menu.LEADERBOARD_MENU);
            case "profile" -> App.getInstance().setCurrentMenu(Menu.PROFILE_MENU);
            case "score-game" -> { App.getInstance().setCurrentGame(new ScoringGame());
                App.getInstance().setCurrentMenu(Menu.PlantSelection_Menu);
                return new Result(
                        true, "Daily Scoring Game opened.\n"
                                + "Select your plants, then use start game.\n",
                        null
                );}
            default -> {
                return new Result(false,
                 "You can only enter score-game,game,setting,news,network,profile menus from the main menu.\n",null);
            }
        }
         return new Result(true,"",null);
    }
}
