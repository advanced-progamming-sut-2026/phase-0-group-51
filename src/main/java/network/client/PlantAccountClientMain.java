package network.client;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.enums.LootType;
import models.shop.ShopItemType;
import network.client.service.AccountClientService;
import network.client.service.GameplayAccountClientService;
import network.client.service.ShopClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.gameplay.LootCollectResponse;
import network.protocol.shop.ShopItemDto;
import network.protocol.shop.ShopPlantStateDto;
import network.protocol.shop.ShopResponse;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class PlantAccountClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private PlantAccountClientMain() {
    }

    public static void main(String[] args) {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "plant-account-test-" + suffix;
        String email = "plantaccount" + suffix + "@example.com";

        try (NetworkClient client = new NetworkClient()) {
            client.connect(HOST, GameServer.DEFAULT_PORT);

            AccountClientService account = new AccountClientService(client);
            GameplayAccountClientService gameplay =
                    new GameplayAccountClientService(client);
            ShopClientService plants = new ShopClientService(client);

            register(account, username, email);
            login(account, username);

            grantCurrency(gameplay, LootType.COIN, 25);
            grantCurrency(gameplay, LootType.GEM, 15);

            ShopResponse snapshot = get(plants);
            int plantId = findUpgradeablePlant(snapshot);

            testSelectedPacketsAndUpgrade(plants, snapshot, plantId);
            testBoostLifecycle(plants, plantId);
            testPurchasedPlantFoodClaim(plants);

            System.out.println();
            System.out.println(
                    "[PASS] All plant account tests passed."
            );
        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Plant account test failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
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
                        "Plant Account Tester",
                        email,
                        "male",
                        1,
                        "coffee",
                        "coffee"
                )
        ).get(5, TimeUnit.SECONDS);

        require(response.isSuccess(),
                "Registration failed: " + response.getMessage());
        System.out.println("[PASS] Test account registered");
    }

    private static void login(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response = service.login(
                new LoginRequest(username, PASSWORD, false)
        ).get(5, TimeUnit.SECONDS);

        require(response.isSuccess(),
                "Login failed: " + response.getMessage());
        System.out.println("[PASS] Test account logged in");
    }

    private static void grantCurrency(
            GameplayAccountClientService service,
            LootType type,
            int count
    ) throws Exception {
        for (int i = 0; i < count; i++) {
            LootCollectResponse response = service.collectLoot(type)
                    .get(5, TimeUnit.SECONDS);
            require(response.isSuccess(),
                    "Could not grant test currency: " + type);
        }
    }

    private static ShopResponse get(
            ShopClientService service
    ) throws Exception {
        ShopResponse response = service.getShop()
                .get(5, TimeUnit.SECONDS);
        require(response.isSuccess(),
                "Could not load plant state: " + response.getMessage());
        return response;
    }

    private static int findUpgradeablePlant(
            ShopResponse response
    ) {
        for (ShopPlantStateDto state : response.getPlants()) {
            PlantData plant = PlantRegistry.getById(state.getPlantId());
            if (plant != null
                    && plant.upgrades() != null
                    && !plant.upgrades().isEmpty()) {
                return state.getPlantId();
            }
        }
        throw new IllegalStateException(
                "No unlocked upgradeable starter plant was found."
        );
    }

    private static void testSelectedPacketsAndUpgrade(
            ShopClientService service,
            ShopResponse snapshot,
            int plantId
    ) throws Exception {
        ShopPlantStateDto before = findPlant(snapshot, plantId);
        int selectedItemId = findItemId(
                snapshot,
                ShopItemType.SEED_PACKET_SELECTED
        );

        ShopResponse packetPurchase = service.purchase(
                selectedItemId,
                1,
                plantId
        ).get(5, TimeUnit.SECONDS);

        require(packetPurchase.isSuccess(),
                "Selected Seed Packet purchase failed: "
                        + packetPurchase.getMessage());

        ShopPlantStateDto withPackets = findPlant(
                packetPurchase,
                plantId
        );
        require(withPackets.getSeedPackets()
                        >= before.getSeedPackets() + 5,
                "Selected Seed Packets were not stored on the server.");

        ShopResponse upgraded = service.upgradePlant(plantId)
                .get(5, TimeUnit.SECONDS);
        require(upgraded.isSuccess(),
                "Plant upgrade failed: " + upgraded.getMessage());

        ShopPlantStateDto after = findPlant(upgraded, plantId);
        require(after.getLevel() == before.getLevel() + 1,
                "Plant level did not increase by one.");

        System.out.println(
                "[PASS] Server-backed seed packets and upgrade work"
        );
    }

    private static void testBoostLifecycle(
            ShopClientService service,
            int plantId
    ) throws Exception {
        ShopResponse boosted = service.buyBoost(plantId)
                .get(5, TimeUnit.SECONDS);
        require(boosted.isSuccess(),
                "Boost purchase failed: " + boosted.getMessage());
        require(findPlant(boosted, plantId).isBoosted(),
                "Boost was not returned in server plant state.");

        ShopResponse consumed = service.consumeBoost(plantId)
                .get(5, TimeUnit.SECONDS);
        require(consumed.isSuccess(),
                "Boost consumption failed: " + consumed.getMessage());
        require(!findPlant(consumed, plantId).isBoosted(),
                "Consumed boost still appears active on the server.");

        System.out.println(
                "[PASS] Stored boost purchase/consumption is server-backed"
        );
    }

    private static void testPurchasedPlantFoodClaim(
            ShopClientService service
    ) throws Exception {
        ShopResponse snapshot = get(service);
        int plantFoodItemId = findItemId(
                snapshot,
                ShopItemType.PLANT_FOOD
        );

        ShopResponse purchased = service.purchase(
                plantFoodItemId,
                1,
                null
        ).get(5, TimeUnit.SECONDS);
        require(purchased.isSuccess(),
                "Plant Food purchase failed: " + purchased.getMessage());
        require(purchased.getPlantFood() > 0,
                "Purchased Plant Food was not stored on the server.");

        int stored = purchased.getPlantFood();
        ShopResponse claimed = service.claimStoredPlantFood()
                .get(5, TimeUnit.SECONDS);

        require(claimed.isSuccess(),
                "Plant Food claim failed: " + claimed.getMessage());
        require(claimed.getClaimedPlantFood() == stored,
                "Claimed Plant Food does not match the stored amount.");
        require(claimed.getPlantFood() == 0,
                "Stored Plant Food was not cleared after claiming.");

        System.out.println(
                "[PASS] Purchased Plant Food is claimed once at game start"
        );
    }

    private static int findItemId(
            ShopResponse response,
            ShopItemType type
    ) {
        for (ShopItemDto item : response.getCatalogue()) {
            if (item.getType() == type) {
                return item.getId();
            }
        }
        throw new IllegalStateException(
                "Shop item was not found: " + type
        );
    }

    private static ShopPlantStateDto findPlant(
            ShopResponse response,
            int plantId
    ) {
        return response.getPlants().stream()
                .filter(state -> state.getPlantId() == plantId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Plant state was not returned: " + plantId
                ));
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
