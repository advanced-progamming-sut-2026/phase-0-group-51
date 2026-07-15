package models.enums;

import views.*;
import views.miniGames.VaseBreakerMenu;
import views.miniGames.WallnutBowllingMenu;

import java.util.Scanner;

public enum Menu {
    LOGIN_MENU(new LoginMenu()),
    SIGN_UP_MENU(new SignUpMenu()),
    PROFILE_MENU(new ProfileMenu()),
    MAIN_MENU(new MainMenu()),
    GAME_MENU(new GameMenu()),
    GAME_VIEW(new GameView()),
    SETTING_MENU(new SettingMenu()),
    NETWORK_MENU(new NetworkMenu()),
    NEWS_MENU(new NewsMenu()),
    SHOP_MENU(new ShopMenu()),
    COLLECTION_MENU(new CollectionMenu()),
    GREENHOUSE_MENU(new GreenHouseMenu()),
    TRAVELLOG_MENU(new TravelLogMenu()),
    LEADERBOARD_MENU(new LeaderboardMenu()),
    PlantSelection_Menu(new PlantSelectionMenu()),
    VASE_BREAKER(new VaseBreakerMenu()),
    WALLNUT_BOWLING(new WallnutBowllingMenu()),
    EXIT_MENU(new ExitMenu());
    private final AppMenu menu;
    Menu(AppMenu menu){
        this.menu = menu;
    }

    public void checkCommand(Scanner scanner){
        this.menu.check(scanner);
    }
}
