package network.client;

import Data.loader.PlantRegistry;
import network.client.service.AccountClientService;
import network.client.service.PlantOwnershipClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.LogoutResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.plants.PlantOwnershipResponse;
import network.server.GameServer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class PlantOwnershipClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private PlantOwnershipClientMain() {
    }

    public static void main(String[] args) {
        String suffix =
                String.valueOf(
                        System.currentTimeMillis()
                );

        String username =
                "ownership-test-" + suffix;

        String email =
                "ownership" + suffix
                        + "@example.com";

        try (NetworkClient client =
                     new NetworkClient()) {

            client.connect(
                    HOST,
                    GameServer.DEFAULT_PORT
            );

            AccountClientService accountService =
                    new AccountClientService(client);

            PlantOwnershipClientService ownershipService =
                    new PlantOwnershipClientService(client);

            testUnauthenticatedRejected(
                    ownershipService
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

            testStarterPlantsLoaded(
                    ownershipService
            );

            logout(accountService);

            testUnauthenticatedRejected(
                    ownershipService
            );

            System.out.println();
            System.out.println(
                    "[PASS] All plant ownership tests passed."
            );

        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Plant ownership test failed: "
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
        RegisterRequest request =
                new RegisterRequest(
                        username,
                        PASSWORD,
                        PASSWORD,
                        "Ownership Tester",
                        email,
                        "male",
                        1,
                        "coffee",
                        "coffee"
                );

        RegisterResponse response =
                service.register(request)
                        .get(5, TimeUnit.SECONDS);

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
                ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Login failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Test account logged in"
        );
    }

    private static void testStarterPlantsLoaded(
            PlantOwnershipClientService service
    ) throws Exception {
        PlantOwnershipResponse response =
                service.getOwnership()
                        .get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Ownership request failed: "
                        + response.getMessage()
        );

        Set<Integer> actual =
                new HashSet<>(
                        response.getUnlockedPlantIds()
                );

        Collection<Integer> starters =
                PlantRegistry.getStarterPlantIds();

        require(
                actual.containsAll(starters),
                "Starter plants are missing. Expected at least "
                        + starters
                        + " but server returned "
                        + actual
        );

        System.out.println(
                "[PASS] Server returned starter plants: "
                        + actual
        );
    }

    private static void testUnauthenticatedRejected(
            PlantOwnershipClientService service
    ) throws Exception {
        PlantOwnershipResponse response =
                service.getOwnership()
                        .get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Ownership request must fail "
                        + "when not logged in."
        );

        System.out.println(
                "[PASS] Unauthenticated ownership request rejected"
        );
    }

    private static void logout(
            AccountClientService service
    ) throws Exception {
        LogoutResponse response =
                service.logout()
                        .get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Logout failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Logout succeeded"
        );
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
