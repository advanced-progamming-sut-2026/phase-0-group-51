package network.client;

import network.client.service.AccountClientService;
import network.client.service.GameplayAccountClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.gameplay.AdventureLossResponse;
import network.protocol.gameplay.AdventureWinResponse;
import network.protocol.gameplay.LootCollectResponse;
import models.enums.LootType;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class GameplayAccountClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private GameplayAccountClientMain() {
    }

    public static void main(String[] args) {
        String suffix =
                String.valueOf(
                        System.currentTimeMillis()
                );

        String username =
                "gameplay-test-" + suffix;

        String email =
                "gameplay" + suffix
                        + "@example.com";

        try (NetworkClient client =
                     new NetworkClient()) {

            client.connect(
                    HOST,
                    GameServer.DEFAULT_PORT
            );

            AccountClientService accountService =
                    new AccountClientService(client);

            GameplayAccountClientService gameplayService =
                    new GameplayAccountClientService(client);

            testUnauthenticatedRejected(
                    gameplayService
            );

            register(
                    accountService,
                    username,
                    email
            );

            login(
                    accountService,
                    username
            );

            testCoinLoot(gameplayService);
            testGemLoot(gameplayService);
            testPlantFoodLoot(gameplayService);
            testPotLoot(gameplayService);
            testAdventureLoss(gameplayService);
            testAdventureWin(gameplayService);

            System.out.println();
            System.out.println(
                    "[PASS] All gameplay account tests passed."
            );

        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Gameplay account test failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private static void testUnauthenticatedRejected(
            GameplayAccountClientService service
    ) throws Exception {
        LootCollectResponse response =
                service.collectLoot(
                        LootType.COIN
                ).get(
                        5,
                        TimeUnit.SECONDS
                );

        require(
                !response.isSuccess(),
                "Unauthenticated loot request must fail."
        );

        AdventureLossResponse loss =
                service.recordAdventureLoss()
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                !loss.isSuccess(),
                "Unauthenticated loss request must fail."
        );

        System.out.println(
                "[PASS] Unauthenticated gameplay requests rejected"
        );
    }

    private static void register(
            AccountClientService service,
            String username,
            String email
    ) throws Exception {
        RegisterResponse response =
                service.register(
                        new RegisterRequest(
                                username,
                                PASSWORD,
                                PASSWORD,
                                "Gameplay Tester",
                                email,
                                "male",
                                1,
                                "coffee",
                                "coffee"
                        )
                ).get(
                        5,
                        TimeUnit.SECONDS
                );

        require(
                response.isSuccess(),
                "Registration failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Test account registered"
        );
    }

    private static void login(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                PASSWORD,
                                false
                        )
                ).get(
                        5,
                        TimeUnit.SECONDS
                );

        require(
                response.isSuccess(),
                "Login failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Test account logged in"
        );
    }

    private static void testCoinLoot(
            GameplayAccountClientService service
    ) throws Exception {
        LootCollectResponse first =
                collect(
                        service,
                        LootType.COIN
                );

        LootCollectResponse second =
                collect(
                        service,
                        LootType.COIN
                );

        require(
                second.getTotal()
                        == first.getTotal() + 50,
                "Coin loot must add exactly 50."
        );

        System.out.println(
                "[PASS] Coin loot saved on server"
        );
    }

    private static void testGemLoot(
            GameplayAccountClientService service
    ) throws Exception {
        LootCollectResponse first =
                collect(
                        service,
                        LootType.GEM
                );

        LootCollectResponse second =
                collect(
                        service,
                        LootType.GEM
                );

        require(
                second.getTotal()
                        == first.getTotal() + 1,
                "Gem loot must add exactly 1."
        );

        System.out.println(
                "[PASS] Gem loot saved on server"
        );
    }

    private static void testPlantFoodLoot(
            GameplayAccountClientService service
    ) throws Exception {
        LootCollectResponse first =
                collect(
                        service,
                        LootType.PLANT_FOOD
                );

        LootCollectResponse second =
                collect(
                        service,
                        LootType.PLANT_FOOD
                );

        require(
                second.getTotal()
                        == first.getTotal() + 1,
                "Plant Food loot must increment plant_food_num."
        );

        System.out.println(
                "[PASS] Plant Food uses its own server balance"
        );
    }

    private static void testPotLoot(
            GameplayAccountClientService service
    ) throws Exception {
        LootCollectResponse response =
                collect(
                        service,
                        LootType.POT
                );

        require(
                response.getUnlockedRow() > 0
                        && response.getUnlockedColumn() > 0,
                "Pot loot must return the unlocked pot coordinates."
        );

        System.out.println(
                "[PASS] Pot loot unlocked greenhouse pot on server"
        );
    }

    private static void testAdventureLoss(
            GameplayAccountClientService service
    ) throws Exception {
        AdventureLossResponse first =
                service.recordAdventureLoss()
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        AdventureLossResponse second =
                service.recordAdventureLoss()
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                first.isSuccess(),
                "First Adventure loss failed: "
                        + first.getMessage()
        );

        require(
                second.isSuccess(),
                "Second Adventure loss failed: "
                        + second.getMessage()
        );

        require(
                second.getGamesPlayed()
                        == first.getGamesPlayed() + 1,
                "Adventure loss must increment games_played by one."
        );

        System.out.println(
                "[PASS] Adventure losses saved on server"
        );
    }

    private static void testAdventureWin(
            GameplayAccountClientService service
    ) throws Exception {
        AdventureLossResponse before =
                service.recordAdventureLoss()
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                before.isSuccess(),
                "Could not establish games_played baseline."
        );

        AdventureWinResponse win =
                service.recordAdventureWin(1, 1)
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                win.isSuccess(),
                "Adventure win failed: "
                        + win.getMessage()
        );

        require(
                win.getGamesPlayed()
                        == before.getGamesPlayed() + 1,
                "Adventure win must increment games_played by one."
        );

        require(
                win.getLastWonGame() != null
                        && win.getLastWonGame()
                        .contains("Chapter 1 Level 1"),
                "Adventure win must update last_won_game."
        );

        require(
                !win.getUnlockedPlantIds().isEmpty(),
                "Adventure win must return server plant ownership."
        );

        AdventureWinResponse skipped =
                service.recordAdventureWin(2, 1)
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                !skipped.isSuccess(),
                "Server must reject a win for a locked later level."
        );

        System.out.println(
                "[PASS] Adventure wins/progress saved on server"
        );
    }

    private static LootCollectResponse collect(
            GameplayAccountClientService service,
            LootType type
    ) throws Exception {
        LootCollectResponse response =
                service.collectLoot(type)
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                response.isSuccess(),
                type
                        + " collection failed: "
                        + response.getMessage()
        );

        require(
                response.getType() == type,
                "Server returned wrong loot type."
        );

        return response;
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
