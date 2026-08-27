package network.client;

import network.client.service.AccountClientService;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public class RegisterClientMain {
    private static final String HOST = "127.0.0.1";

    private RegisterClientMain() {
    }

    public static void main(String[] args) {
        String suffix =
                String.valueOf(System.currentTimeMillis());

        String username = "phase3test-" + suffix;
        String email =
                "phase3test" + suffix + "@example.com";

        try (NetworkClient client = new NetworkClient()) {
            client.connect(
                    HOST,
                    GameServer.DEFAULT_PORT
            );

            AccountClientService service =
                    new AccountClientService(client);

            testValidRegistration(
                    service,
                    username,
                    email
            );

            testDuplicateUsername(
                    service,
                    username,
                    suffix
            );

            testDuplicateEmail(
                    service,
                    email,
                    suffix
            );

            testWeakPassword(
                    service,
                    suffix
            );

            testPasswordMismatch(
                    service,
                    suffix
            );

            System.out.println();
            System.out.println(
                    "[PASS] Registration tests completed."
            );
        } catch (Exception exception) {
            System.err.println(
                    "[FAIL] Registration test failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private static void testValidRegistration(
            AccountClientService service,
            String username,
            String email
    ) throws Exception {
        RegisterResponse response = register(
                service,
                request(
                        username,
                        email,
                        "ValidPass1!",
                        "ValidPass1!"
                )
        );

        require(
                response.isSuccess(),
                "Valid registration should succeed."
        );

        System.out.println(
                "[PASS] Valid registration"
        );
    }

    private static void testDuplicateUsername(
            AccountClientService service,
            String username,
            String suffix
    ) throws Exception {
        RegisterResponse response = register(
                service,
                request(
                        username,
                        "different" + suffix
                                + "@example.com",
                        "ValidPass1!",
                        "ValidPass1!"
                )
        );

        require(
                !response.isSuccess(),
                "Duplicate username should fail."
        );

        System.out.println(
                "[PASS] Duplicate username rejected"
        );
    }

    private static void testDuplicateEmail(
            AccountClientService service,
            String email,
            String suffix
    ) throws Exception {
        RegisterResponse response = register(
                service,
                request(
                        "different-" + suffix,
                        email,
                        "ValidPass1!",
                        "ValidPass1!"
                )
        );

        require(
                !response.isSuccess(),
                "Duplicate email should fail."
        );

        System.out.println(
                "[PASS] Duplicate email rejected"
        );
    }

    private static void testWeakPassword(
            AccountClientService service,
            String suffix
    ) throws Exception {
        RegisterResponse response = register(
                service,
                request(
                        "weak-" + suffix,
                        "weak" + suffix + "@example.com",
                        "abc",
                        "abc"
                )
        );

        require(
                !response.isSuccess(),
                "Weak password should fail."
        );

        System.out.println(
                "[PASS] Weak password rejected"
        );
    }

    private static void testPasswordMismatch(
            AccountClientService service,
            String suffix
    ) throws Exception {
        RegisterResponse response = register(
                service,
                request(
                        "mismatch-" + suffix,
                        "mismatch" + suffix
                                + "@example.com",
                        "ValidPass1!",
                        "DifferentPass1!"
                )
        );

        require(
                !response.isSuccess(),
                "Password mismatch should fail."
        );

        System.out.println(
                "[PASS] Password mismatch rejected"
        );
    }

    private static RegisterRequest request(
            String username,
            String email,
            String password,
            String passwordConfirm
    ) {
        return new RegisterRequest(
                username,
                password,
                passwordConfirm,
                "Network Tester",
                email,
                "male",
                1,
                "coffee",
                "coffee"
        );
    }

    private static RegisterResponse register(
            AccountClientService service,
            RegisterRequest request
    ) throws Exception {
        return service.register(request)
                .get(5, TimeUnit.SECONDS);
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