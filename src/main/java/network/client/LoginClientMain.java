package network.client;

import network.client.service.AccountClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.LogoutResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class LoginClientMain {
    private static final String HOST = "127.0.0.1";

    private LoginClientMain() {
    }

    public static void main(String[] args) {
        String suffix =
                String.valueOf(System.currentTimeMillis());

        String username = "login-test-" + suffix;
        String email =
                "login" + suffix + "@example.com";

        try {
            createAndTestAccount(
                    username,
                    email
            );

            testNewConnection(username);

            System.out.println();
            System.out.println(
                    "[PASS] All login/session tests passed."
            );
        } catch (Exception exception) {
            System.err.println(
                    "[FAIL] Login/session test failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private static void createAndTestAccount(
            String username,
            String email
    ) throws Exception {
        try (NetworkClient client =
                     new NetworkClient()) {

            client.connect(
                    HOST,
                    GameServer.DEFAULT_PORT
            );

            AccountClientService service =
                    new AccountClientService(client);

            registerUser(
                    service,
                    username,
                    email
            );

            testCorrectLogin(service, username);
            testSecondLoginRejected(service, username);

            testLogout(service);
            testSecondLogoutRejected(service);

            testWrongPassword(service, username);
            testUnknownUsername(service);

            testCorrectLogin(service, username);
            testLogout(service);
        }
    }

    private static void registerUser(
            AccountClientService service,
            String username,
            String email
    ) throws Exception {
        RegisterRequest request =
                new RegisterRequest(
                        username,
                        "ValidPass1!",
                        "ValidPass1!",
                        "Login Tester",
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
                "Test user registration failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Test account registered"
        );
    }

    private static void testCorrectLogin(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                "ValidPass1!"
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Correct login should succeed: "
                        + response.getMessage()
        );

        require(
                response.getUser() != null,
                "Successful login must return user profile."
        );

        require(
                username.equals(
                        response.getUser().getUsername()
                ),
                "Returned username does not match."
        );

        require(
                response.getUser().getId() > 0,
                "Returned user id is invalid."
        );

        System.out.println(
                "[PASS] Correct login accepted"
        );

        System.out.println(
                "       authenticated user = "
                        + response.getUser().getUsername()
                        + " (id="
                        + response.getUser().getId()
                        + ")"
        );
    }

    private static void testSecondLoginRejected(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                "ValidPass1!"
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Second login on same connection "
                        + "should be rejected."
        );

        System.out.println(
                "[PASS] Second login on same "
                        + "connection rejected"
        );
    }

    private static void testLogout(
            AccountClientService service
    ) throws Exception {
        LogoutResponse response =
                service.logout()
                        .get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Logout should succeed."
        );

        System.out.println(
                "[PASS] Logout accepted"
        );
    }

    private static void testSecondLogoutRejected(
            AccountClientService service
    ) throws Exception {
        LogoutResponse response =
                service.logout()
                        .get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Second logout should fail."
        );

        System.out.println(
                "[PASS] Logout while unauthenticated rejected"
        );
    }

    private static void testWrongPassword(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                "WrongPass1!"
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Wrong password should fail."
        );

        require(
                response.getUser() == null,
                "Failed login must not return a profile."
        );

        System.out.println(
                "[PASS] Wrong password rejected"
        );
    }

    private static void testUnknownUsername(
            AccountClientService service
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                "user-does-not-exist",
                                "ValidPass1!"
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Unknown username should fail."
        );

        System.out.println(
                "[PASS] Unknown username rejected"
        );
    }

    private static void testNewConnection(
            String username
    ) throws Exception {
        try (NetworkClient client =
                     new NetworkClient()) {

            client.connect(
                    HOST,
                    GameServer.DEFAULT_PORT
            );

            AccountClientService service =
                    new AccountClientService(client);

            LoginResponse response =
                    service.login(
                            new LoginRequest(
                                    username,
                                    "ValidPass1!"
                            )
                    ).get(5, TimeUnit.SECONDS);

            require(
                    response.isSuccess(),
                    "Login from a new TCP connection "
                            + "should succeed."
            );

            System.out.println(
                    "[PASS] Same account logged in "
                            + "from a new connection"
            );

            LogoutResponse logout =
                    service.logout()
                            .get(5, TimeUnit.SECONDS);

            require(
                    logout.isSuccess(),
                    "Final logout should succeed."
            );
        }
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