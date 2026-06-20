package models.enums;

import views.*;

import java.util.Scanner;

public enum Menu {
    LoginMenu(new LoginMenu()),
    SignUpMenu(new SignUpMenu()),
    ProfileMenu(new ProfileMenu()),
    MainMenu(new MainMenu()),
    GameMenu(new GameMenu()),
    SettingMenu(new SettingMenu()),
    NetworkMenu(new NetworkMenu()),
    NewsMenu(new NewsMenu()),
    COLLECTION_MENU(new CollectionMenu()),
    GREENHOUSE_MENU(new GreenHouseMenu()),
    TRAVELLOG_MENU(new TravelLogMenu()),
    LEADERBOARD_MENU(new LeaderboardMenu()),
    ExitMenu(new ExitMenu());
    private final AppMenu menu;
    Menu(AppMenu menu){
        this.menu = menu;
    }

    public void checkCommand(Scanner scanner){
        this.menu.check(scanner);
    }
}
