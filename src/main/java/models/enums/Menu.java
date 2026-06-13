package models.enums;

import views.*;

import java.util.Scanner;

public enum Menu {
    LoginMenu(new LoginMenu()),
    SignUpMenu(new SignUpMenu()),
    CoinWalletMenu(new CoinWalletMenu()),
    ProfileMenu(new ProfileMenu()),
    MainMenu(new MainMenu),
    GameMenu(new GameMenu()),
    SettingMenu(new SettingMenu()),
    Network(new NetworkMenu()),
    NewsMenu(new NewsMenu()),
    ExitMenu(new ExitMenu());
    private final AppMenu menu;
    Menu(AppMenu menu){
        this.menu = menu;
    }

    public void checkCommand(Scanner scanner){
        this.menu.check(scanner);
    }
}
