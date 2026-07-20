package models.enums;

import views.*;
import views.miniGames.*;

import java.util.Scanner;
import java.util.function.Supplier;

public enum Menu {
    LOGIN_MENU(LoginMenu::new),
    SIGN_UP_MENU(SignUpMenu::new),
    PROFILE_MENU(ProfileMenu::new),
    MAIN_MENU(MainMenu::new),
    GAME_MENU(GameMenu::new),
    GAME_VIEW(GameView::new),
    SETTING_MENU(SettingMenu::new),
    NETWORK_MENU(NetworkMenu::new),
    NEWS_MENU(NewsMenu::new),
    SHOP_MENU(ShopMenu::new),
    COLLECTION_MENU(CollectionMenu::new),
    GREENHOUSE_MENU(GreenHouseMenu::new),
    TRAVELLOG_MENU(TravelLogMenu::new),
    LEADERBOARD_MENU(LeaderboardMenu::new),
    PlantSelection_Menu(PlantSelectionMenu::new),
    VASE_BREAKER(VaseBreakerMenu::new),
    WALLNUT_BOWLING(WallnutBowllingMenu::new),
    IZOMBIE(IZombieMenu::new),
    BEGHOULDED(BeghouledMenu::new),
    ZOMBOTANY(ZombotanyMenu::new),
    EXIT_MENU(ExitMenu::new);

    private final Supplier<AppMenu> menuFactory;
    private AppMenu menu;

    Menu(Supplier<AppMenu> menuFactory) {
        this.menuFactory = menuFactory;
    }

    private AppMenu getOrCreateMenu() {
        if (menu == null) {
            menu = menuFactory.get();
        }

        return menu;
    }

    public void checkCommand(Scanner scanner) {
        getOrCreateMenu().check(scanner);
    }

    public void resetView() {
        menu = null;
    }

    public static void resetAllViews() {
        for (Menu menuValue : values()) {
            menuValue.resetView();
        }
    }
}
