package network.client;

import models.enums.LootType;
import network.client.service.AccountClientService;
import network.client.service.GameplayAccountClientService;
import network.client.service.GreenHouseClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.greenhouse.GreenHousePotDto;
import network.protocol.greenhouse.GreenHouseResponse;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class GreenHouseClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private GreenHouseClientMain() {
    }

    public static void main(String[] args) {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "greenhouse-test-" + suffix;
        String email = "greenhouse" + suffix + "@example.com";

        try (NetworkClient client = new NetworkClient()) {
            client.connect(HOST, GameServer.DEFAULT_PORT);

            AccountClientService accountService =
                    new AccountClientService(client);
            GreenHouseClientService greenHouseService =
                    new GreenHouseClientService(client);
            GameplayAccountClientService gameplayService =
                    new GameplayAccountClientService(client);

            testUnauthenticatedRejected(greenHouseService);
            register(accountService, username, email);
            login(accountService, username);
            testInitialGreenHouse(greenHouseService);
            testPlantGrowCollect(
                    greenHouseService,
                    gameplayService
            );
            testPotLootPersists(
                    greenHouseService,
                    gameplayService
            );

            System.out.println();
            System.out.println(
                    "[PASS] All greenhouse tests passed."
            );
        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Greenhouse test failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private static void testUnauthenticatedRejected(
            GreenHouseClientService service
    ) throws Exception {
        GreenHouseResponse response = service.getGreenHouse()
                .get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Unauthenticated greenhouse request must fail."
        );

        System.out.println(
                "[PASS] Unauthenticated greenhouse request rejected"
        );
    }

    private static void register(
            AccountClientService service,
            String username,
            String email
    ) throws Exception {
        RegisterResponse response = service.register(
                new RegisterRequest(
                        username,
                        PASSWORD,
                        PASSWORD,
                        "Greenhouse Tester",
                        email,
                        "male",
                        1,
                        "coffee",
                        "coffee"
                )
        ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Registration failed: " + response.getMessage()
        );

        System.out.println("[PASS] Test account registered");
    }

    private static void login(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response = service.login(
                new LoginRequest(
                        username,
                        PASSWORD,
                        false
                )
        ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Login failed: " + response.getMessage()
        );

        System.out.println("[PASS] Test account logged in");
    }

    private static void testInitialGreenHouse(
            GreenHouseClientService service
    ) throws Exception {
        GreenHouseResponse response = service.getGreenHouse()
                .get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Greenhouse load failed: " + response.getMessage()
        );
        require(
                response.getPots().size() == 12,
                "A new account must have exactly 12 greenhouse pots."
        );
        require(
                countUnlocked(response) == 4,
                "A new account must start with exactly four unlocked pots."
        );

        System.out.println(
                "[PASS] Initial greenhouse loaded from server"
        );
    }

    private static void testPlantGrowCollect(
            GreenHouseClientService greenHouseService,
            GameplayAccountClientService gameplayService
    ) throws Exception {
        GreenHouseResponse planted = greenHouseService.plant(1, 1)
                .get(5, TimeUnit.SECONDS);

        require(
                planted.isSuccess(),
                "Planting failed: " + planted.getMessage()
        );
        require(
                pot(planted, 1, 1).getPlantId() != null,
                "The planted pot must contain a plant on the server."
        );

        GreenHouseResponse tooEarly = greenHouseService.collect(1, 1)
                .get(5, TimeUnit.SECONDS);
        require(
                !tooEarly.isSuccess(),
                "A growing plant must not be collectable yet."
        );

        for (int i = 0; i < 10; i++) {
            require(
                    gameplayService.collectLoot(LootType.GEM)
                            .get(5, TimeUnit.SECONDS)
                            .isSuccess(),
                    "Could not grant gems for greenhouse test."
            );
        }

        GreenHouseResponse grown = greenHouseService.grow(1, 1)
                .get(5, TimeUnit.SECONDS);
        require(
                grown.isSuccess(),
                "Instant growth failed: " + grown.getMessage()
        );

        GreenHouseResponse collected = greenHouseService.collect(1, 1)
                .get(5, TimeUnit.SECONDS);
        require(
                collected.isSuccess(),
                "Harvest failed: " + collected.getMessage()
        );
        require(
                pot(collected, 1, 1).getPlantId() == null,
                "Harvested pot must be empty on the server."
        );

        System.out.println(
                "[PASS] Plant/grow/collect are server-backed"
        );
    }

    private static void testPotLootPersists(
            GreenHouseClientService greenHouseService,
            GameplayAccountClientService gameplayService
    ) throws Exception {
        GreenHouseResponse before = greenHouseService.getGreenHouse()
                .get(5, TimeUnit.SECONDS);
        int unlockedBefore = countUnlocked(before);

        require(
                gameplayService.collectLoot(LootType.POT)
                        .get(5, TimeUnit.SECONDS)
                        .isSuccess(),
                "Pot loot could not be collected."
        );

        GreenHouseResponse after = greenHouseService.getGreenHouse()
                .get(5, TimeUnit.SECONDS);

        require(
                countUnlocked(after) == unlockedBefore + 1,
                "Pot loot must unlock exactly one greenhouse pot."
        );

        System.out.println(
                "[PASS] Pot loot is visible in server greenhouse"
        );
    }

    private static GreenHousePotDto pot(
            GreenHouseResponse response,
            int row,
            int column
    ) {
        return response.getPots().stream()
                .filter(value -> value.getRow() == row
                        && value.getColumn() == column)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Greenhouse pot was missing from response."
                ));
    }

    private static int countUnlocked(GreenHouseResponse response) {
        return (int) response.getPots().stream()
                .filter(GreenHousePotDto::isUnlocked)
                .count();
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
