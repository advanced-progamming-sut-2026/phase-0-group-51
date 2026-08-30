package network.client;

import models.App;
import models.User;
import models.enums.Menu;
import network.protocol.auth.UserProfileDto;

public final class ClientAuthState {
    private ClientAuthState() {
    }

    public static void applyLogin(
            UserProfileDto profile
    ) {
        if (profile == null) {
            throw new IllegalArgumentException(
                    "User profile cannot be null."
            );
        }

        User user = toUser(profile);

        ClientPlantOwnershipState.clear();
        ClientAdventureProgressState.clear();
        ClientGreenHouseState.clear();
        ClientShopState.clear();
        ClientQuestState.clear();
        ClientMinigameState.clear();

        App app = App.getInstance();
        app.setLoggedInUser(user);
        app.setCurrentMenu(Menu.MAIN_MENU);
    }

    public static void clear() {
        ClientPlantOwnershipState.clear();
        ClientAdventureProgressState.clear();
        ClientGreenHouseState.clear();
        ClientShopState.clear();
        ClientQuestState.clear();
        ClientMinigameState.clear();

        App app = App.getInstance();

        app.setLoggedInUser(null);
        app.setCurrentGame(null);
        app.setCurrentMenu(Menu.SIGN_UP_MENU);

        Menu.resetAllViews();
    }

    private static User toUser(
            UserProfileDto profile
    ) {
        User user = new User(
                profile.getId(),
                profile.getUsername(),
                profile.getEmail(),

                null,

                profile.getGender(),
                profile.getNickname(),

                0,
                null,

                profile.getCoins(),
                profile.getGems(),
                profile.getPlantFoodNum(),

                profile.getMostMeowPoint(),
                profile.getMaxPoint(),

                profile.getGamesPlayed(),
                profile.getMiniGamesPlayed(),

                profile.getLastWonGame(),
                profile.getDifficultyLevel()
        );

        user.setQuestDailyNum(
                profile.getQuestDailyNum()
        );

        user.setQuestNonDailyNum(
                profile.getQuestNonDailyNum()
        );

        return user;
    }
}