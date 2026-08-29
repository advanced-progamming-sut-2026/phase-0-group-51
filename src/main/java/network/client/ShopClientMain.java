package network.client;

import models.enums.LootType;
import network.client.service.AccountClientService;
import network.client.service.GameplayAccountClientService;
import network.client.service.GreenHouseClientService;
import network.client.service.ShopClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.greenhouse.GreenHouseResponse;
import network.protocol.shop.ShopDailyOfferDto;
import network.protocol.shop.ShopPlantStateDto;
import network.protocol.shop.ShopResponse;
import network.server.GameServer;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

public final class ShopClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private ShopClientMain() {
    }

    public static void main(String[] args) {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "shop-test-" + suffix;
        String email = "shop" + suffix + "@example.com";

        try (NetworkClient client = new NetworkClient()) {
            client.connect(HOST, GameServer.DEFAULT_PORT);

            AccountClientService accountService =
                    new AccountClientService(client);
            ShopClientService shopService =
                    new ShopClientService(client);
            GameplayAccountClientService gameplayService =
                    new GameplayAccountClientService(client);
            GreenHouseClientService greenHouseService =
                    new GreenHouseClientService(client);

            testUnauthenticatedRejected(shopService);
            register(accountService, username, email);
            login(accountService, username);
            testInitialShop(shopService);
            grantTestCurrency(gameplayService);
            testPotPurchase(shopService, greenHouseService);
            testPlantFoodPurchase(shopService);
            testRandomSeeds(shopService);
            testSelectedSeeds(shopService);
            testCoinConversion(shopService);
            testDailyOffer(shopService);
            testInvalidSelectedPlantRejected(shopService);

            System.out.println();
            System.out.println("[PASS] All shop tests passed.");
        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Shop test failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private static void testUnauthenticatedRejected(
            ShopClientService service
    ) throws Exception {
        ShopResponse response = service.getShop()
                .get(5, TimeUnit.SECONDS);
        require(
                !response.isSuccess(),
                "Unauthenticated shop request must fail."
        );
        System.out.println(
                "[PASS] Unauthenticated shop request rejected"
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
                        "Shop Tester",
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
                new LoginRequest(username, PASSWORD, false)
        ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Login failed: " + response.getMessage()
        );
        System.out.println("[PASS] Test account logged in");
    }

    private static void testInitialShop(
            ShopClientService service
    ) throws Exception {
        ShopResponse response = service.getShop()
                .get(5, TimeUnit.SECONDS);

        require(response.isSuccess(), response.getMessage());
        require(
                response.getCatalogue().size() == 5,
                "Permanent shop must contain five catalogue items."
        );
        require(
                response.getDailyOffer() != null,
                "A new account must receive a daily offer."
        );
        require(
                !response.getPlants().isEmpty(),
                "Shop must return the account's unlocked starter plants."
        );

        System.out.println(
                "[PASS] Shop catalogue/daily offer loaded from server"
        );
    }

    private static void grantTestCurrency(
            GameplayAccountClientService service
    ) throws Exception {
        for (int i = 0; i < 100; i++) {
            require(
                    service.collectLoot(LootType.COIN)
                            .get(5, TimeUnit.SECONDS)
                            .isSuccess(),
                    "Could not grant test coins."
            );
        }
        for (int i = 0; i < 20; i++) {
            require(
                    service.collectLoot(LootType.GEM)
                            .get(5, TimeUnit.SECONDS)
                            .isSuccess(),
                    "Could not grant test gems."
            );
        }
        System.out.println("[PASS] Test currency granted through server");
    }

    private static void testPotPurchase(
            ShopClientService service,
            GreenHouseClientService greenHouseService
    ) throws Exception {
        GreenHouseResponse before = greenHouseService.getGreenHouse()
                .get(5, TimeUnit.SECONDS);
        int unlockedBefore = (int) before.getPots().stream()
                .filter(value -> value.isUnlocked())
                .count();

        ShopResponse response = service.purchase(1, 1, null)
                .get(5, TimeUnit.SECONDS);
        require(response.isSuccess(), response.getMessage());

        GreenHouseResponse after = greenHouseService.getGreenHouse()
                .get(5, TimeUnit.SECONDS);
        int unlockedAfter = (int) after.getPots().stream()
                .filter(value -> value.isUnlocked())
                .count();

        require(
                unlockedAfter == unlockedBefore + 1,
                "Pot purchase must unlock exactly one server greenhouse pot."
        );
        System.out.println("[PASS] Flower Pot purchase is server-backed");
    }

    private static void testPlantFoodPurchase(
            ShopClientService service
    ) throws Exception {
        ShopResponse before = service.getShop().get(5, TimeUnit.SECONDS);
        ShopResponse response = service.purchase(2, 1, null)
                .get(5, TimeUnit.SECONDS);

        require(response.isSuccess(), response.getMessage());
        require(
                response.getPlantFood() == before.getPlantFood() + 3,
                "Plant Food purchase must add three stored Plant Food."
        );
        require(
                response.getGems() == before.getGems() - 3,
                "Plant Food price must be calculated by the server."
        );
        System.out.println("[PASS] Plant Food purchase is server-backed");
    }

    private static void testRandomSeeds(
            ShopClientService service
    ) throws Exception {
        ShopResponse before = service.getShop().get(5, TimeUnit.SECONDS);
        int packetsBefore = totalPackets(before);

        ShopResponse response = service.purchase(3, 1, null)
                .get(5, TimeUnit.SECONDS);
        require(response.isSuccess(), response.getMessage());
        require(
                totalPackets(response) == packetsBefore + 5,
                "Mystery Seeds must add exactly five seed packets."
        );
        require(
                response.getCoins() == before.getCoins() - 1000,
                "Mystery Seeds price must be charged on the server."
        );
        System.out.println("[PASS] Random Seed Packet purchase is server-backed");
    }

    private static void testSelectedSeeds(
            ShopClientService service
    ) throws Exception {
        ShopResponse before = service.getShop().get(5, TimeUnit.SECONDS);
        int plantId = before.getPlants().get(0).getPlantId();
        int packetsBefore = plant(before, plantId).getSeedPackets();

        ShopResponse response = service.purchase(4, 1, plantId)
                .get(5, TimeUnit.SECONDS);
        require(response.isSuccess(), response.getMessage());
        require(
                plant(response, plantId).getSeedPackets()
                        == packetsBefore + 10,
                "Specific Seeds must add ten packets to the selected plant."
        );
        require(
                response.getGems() == before.getGems() - 5,
                "Specific Seeds price must be charged on the server."
        );
        System.out.println("[PASS] Selected Seed Packet purchase is server-backed");
    }

    private static void testCoinConversion(
            ShopClientService service
    ) throws Exception {
        ShopResponse before = service.getShop().get(5, TimeUnit.SECONDS);
        ShopResponse response = service.purchase(5, 1, null)
                .get(5, TimeUnit.SECONDS);

        require(response.isSuccess(), response.getMessage());
        require(
                response.getGems() == before.getGems() - 5,
                "Coin conversion must deduct five gems."
        );
        require(
                response.getCoins() == before.getCoins() + 500,
                "Coin conversion must add five hundred coins."
        );
        System.out.println("[PASS] Coin conversion is server-backed");
    }

    private static void testDailyOffer(
            ShopClientService service
    ) throws Exception {
        ShopResponse before = service.getShop().get(5, TimeUnit.SECONDS);
        ShopDailyOfferDto offer = before.getDailyOffer();
        require(offer != null, "Daily offer is missing.");

        int packetsBefore = plant(before, offer.getPlantId())
                .getSeedPackets();

        ShopResponse response = service.buyDailyOffer()
                .get(5, TimeUnit.SECONDS);
        require(response.isSuccess(), response.getMessage());
        require(
                response.getCoins()
                        == before.getCoins() - offer.getFinalPrice(),
                "Daily offer must charge the server-calculated final price."
        );
        require(
                plant(response, offer.getPlantId()).getSeedPackets()
                        == packetsBefore + 10,
                "Daily offer must add ten seed packets."
        );
        require(
                response.getDailyOffer() != null
                        && response.getDailyOffer().isPurchased(),
                "Daily offer must be marked purchased on the server."
        );

        ShopResponse second = service.buyDailyOffer()
                .get(5, TimeUnit.SECONDS);
        require(
                !second.isSuccess(),
                "The same daily offer must not be purchasable twice."
        );

        System.out.println("[PASS] Daily offer is server-backed and single-use");
    }

    private static void testInvalidSelectedPlantRejected(
            ShopClientService service
    ) throws Exception {
        ShopResponse before = service.getShop().get(5, TimeUnit.SECONDS);
        ShopResponse response = service.purchase(
                4,
                1,
                Integer.MAX_VALUE
        ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Server must reject a fabricated selected plant id."
        );
        require(
                response.getCoins() == before.getCoins()
                        && response.getGems() == before.getGems(),
                "Rejected purchase must not change the wallet."
        );
        System.out.println("[PASS] Fabricated plant selection rejected by server");
    }

    private static int totalPackets(ShopResponse response) {
        return response.getPlants().stream()
                .mapToInt(ShopPlantStateDto::getSeedPackets)
                .sum();
    }

    private static ShopPlantStateDto plant(
            ShopResponse response,
            int plantId
    ) {
        Map<Integer, ShopPlantStateDto> byId = response.getPlants()
                .stream()
                .collect(Collectors.toMap(
                        ShopPlantStateDto::getPlantId,
                        Function.identity()
                ));
        ShopPlantStateDto state = byId.get(plantId);
        if (state == null) {
            throw new IllegalStateException(
                    "Plant state missing from shop response: " + plantId
            );
        }
        return state;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
