package network.client;

import network.client.service.AccountClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.LogoutResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class RememberMeClientMain {

    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private RememberMeClientMain() {
    }

    public static void main(String[] args) {
        String suffix =
                String.valueOf(System.currentTimeMillis());

        String username =
                "remember-test-" + suffix;

        String email =
                "remember" + suffix + "@example.com";

        String savedToken;

        try {
            // -------------------------------------------------
            // TEST 1:
            // Register account
            // Login WITHOUT remember me
            // Token must be null
            // -------------------------------------------------

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

                testNormalLoginHasNoToken(
                        service,
                        username
                );

                testLogout(service);
            }

            // -------------------------------------------------
            // TEST 2:
            // Login WITH remember me
            // Token must exist
            //
            // IMPORTANT:
            // We deliberately close this socket WITHOUT logout.
            // -------------------------------------------------

            try (NetworkClient client =
                         new NetworkClient()) {

                client.connect(
                        HOST,
                        GameServer.DEFAULT_PORT
                );

                AccountClientService service =
                        new AccountClientService(client);

                savedToken =
                        testRememberMeLogin(
                                service,
                                username
                        );

                System.out.println(
                        "[PASS] Closing first remembered "
                                + "connection WITHOUT logout"
                );

                // Do NOT call service.logout() here.
            }

            // -------------------------------------------------
            // TEST 3:
            // New TCP connection
            // Resume using saved token
            // -------------------------------------------------

            try (NetworkClient client =
                         new NetworkClient()) {

                client.connect(
                        HOST,
                        GameServer.DEFAULT_PORT
                );

                AccountClientService service =
                        new AccountClientService(client);

                testResumeSession(
                        service,
                        savedToken,
                        username
                );

                // Explicit logout MUST revoke the token.
                testLogout(service);
            }

            // -------------------------------------------------
            // TEST 4:
            // Try using the SAME token after logout.
            // It MUST now fail.
            // -------------------------------------------------

            try (NetworkClient client =
                         new NetworkClient()) {

                client.connect(
                        HOST,
                        GameServer.DEFAULT_PORT
                );

                AccountClientService service =
                        new AccountClientService(client);

                testRevokedTokenRejected(
                        service,
                        savedToken
                );
            }

            System.out.println();
            System.out.println(
                    "[PASS] All remember-me/session tests passed."
            );

        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Remember-me test failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
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
                        PASSWORD,
                        PASSWORD,
                        "Remember Tester",
                        email,
                        "male",
                        1,
                        "coffee",
                        "coffee"
                );

        RegisterResponse response =
                service.register(request)
                        .get(
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

    private static void testNormalLoginHasNoToken(
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
                "Normal login failed: "
                        + response.getMessage()
        );

        require(
                response.getSessionToken() == null,
                "Normal login must NOT create "
                        + "a persistent session token."
        );

        System.out.println(
                "[PASS] Normal login succeeded"
        );

        System.out.println(
                "[PASS] rememberMe=false returned no token"
        );
    }

    private static String testRememberMeLogin(
            AccountClientService service,
            String username
    ) throws Exception {

        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                PASSWORD,
                                true
                        )
                ).get(
                        5,
                        TimeUnit.SECONDS
                );

        require(
                response.isSuccess(),
                "Remember-me login failed: "
                        + response.getMessage()
        );

        String token =
                response.getSessionToken();

        require(
                token != null
                        && !token.isBlank(),
                "rememberMe=true must return "
                        + "a session token."
        );

        System.out.println(
                "[PASS] Remember-me login succeeded"
        );

        System.out.println(
                "[PASS] Server returned a session token"
        );

        return token;
    }

    private static void testResumeSession(
            AccountClientService service,
            String token,
            String expectedUsername
    ) throws Exception {

        LoginResponse response =
                service.resumeSession(token)
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                response.isSuccess(),
                "Session resume should succeed: "
                        + response.getMessage()
        );

        require(
                response.getUser() != null,
                "Resumed session must return user profile."
        );

        require(
                expectedUsername.equals(
                        response.getUser()
                                .getUsername()
                ),
                "Resumed session returned wrong user."
        );

        System.out.println(
                "[PASS] Session resumed on new TCP connection"
        );

        System.out.println(
                "[PASS] Restored user = "
                        + response.getUser()
                        .getUsername()
        );
    }

    private static void testLogout(
            AccountClientService service
    ) throws Exception {

        LogoutResponse response =
                service.logout()
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                response.isSuccess(),
                "Logout failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Explicit logout succeeded"
        );
    }

    private static void testRevokedTokenRejected(
            AccountClientService service,
            String token
    ) throws Exception {

        LoginResponse response =
                service.resumeSession(token)
                        .get(
                                5,
                                TimeUnit.SECONDS
                        );

        require(
                !response.isSuccess(),
                "Token must NOT work after explicit logout."
        );

        System.out.println(
                "[PASS] Old token rejected after logout"
        );

        System.out.println(
                "       server response = "
                        + response.getMessage()
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