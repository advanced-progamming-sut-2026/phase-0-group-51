package network.client;

import network.client.service.AccountClientService;
import network.client.service.ProfileClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.profile.ProfileDataDto;
import network.protocol.profile.ProfilePasswordChangeRequest;
import network.protocol.profile.ProfileResponse;
import network.protocol.profile.ProfileUpdateRequest;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class ProfileClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String OLD_PASSWORD = "ValidPass1!";
    private static final String NEW_PASSWORD = "NewValid2!";

    private ProfileClientMain() {
    }

    public static void main(String[] args) {
        String suffix =
                String.valueOf(System.currentTimeMillis());

        String username =
                "profile-test-" + suffix;

        String blockerUsername =
                "profile-blocker-" + suffix;

        String updatedUsername =
                username + "-new";

        String email =
                "profile" + suffix + "@example.com";

        String blockerEmail =
                "profileblocker" + suffix + "@example.com";

        String updatedEmail =
                "profileupdated" + suffix + "@example.com";

        try (NetworkClient client = new NetworkClient()) {
            client.connect(
                    HOST,
                    GameServer.DEFAULT_PORT
            );

            AccountClientService accountService =
                    new AccountClientService(client);

            ProfileClientService profileService =
                    new ProfileClientService(client);

            register(
                    accountService,
                    username,
                    email,
                    "Profile Tester"
            );

            register(
                    accountService,
                    blockerUsername,
                    blockerEmail,
                    "Blocker Tester"
            );

            login(
                    accountService,
                    username,
                    OLD_PASSWORD,
                    true
            );

            testGetProfile(
                    profileService,
                    username,
                    email
            );

            testDuplicateUsernameRejected(
                    profileService,
                    blockerUsername,
                    email
            );

            testInvalidEmailRejected(
                    profileService,
                    username,
                    "invalid-email"
            );

            testValidUpdate(
                    profileService,
                    updatedUsername,
                    updatedEmail
            );

            testGetProfile(
                    profileService,
                    updatedUsername,
                    updatedEmail
            );

            testWrongCurrentPassword(
                    profileService
            );

            testPasswordChange(
                    profileService
            );

            accountService.logout()
                    .get(5, TimeUnit.SECONDS);

            testOldPasswordRejected(
                    accountService,
                    updatedUsername
            );

            testNewPasswordAccepted(
                    accountService,
                    updatedUsername
            );

            System.out.println();
            System.out.println(
                    "[PASS] All profile tests passed."
            );
        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Profile test failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private static void register(
            AccountClientService service,
            String username,
            String email,
            String nickname
    ) throws Exception {
        RegisterResponse response =
                service.register(
                        new RegisterRequest(
                                username,
                                OLD_PASSWORD,
                                OLD_PASSWORD,
                                nickname,
                                email,
                                "male",
                                1,
                                "coffee",
                                "coffee"
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Registration failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Registered " + username
        );
    }

    private static LoginResponse login(
            AccountClientService service,
            String username,
            String password,
            boolean rememberMe
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                password,
                                rememberMe
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Login failed: " + response.getMessage()
        );

        System.out.println(
                "[PASS] Logged in as " + username
        );

        return response;
    }

    private static void testGetProfile(
            ProfileClientService service,
            String expectedUsername,
            String expectedEmail
    ) throws Exception {
        ProfileResponse response =
                service.getProfile()
                        .get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Profile load failed: "
                        + response.getMessage()
        );

        ProfileDataDto profile =
                response.getProfile();

        require(
                profile != null,
                "Profile data is missing."
        );

        require(
                expectedUsername.equals(
                        profile.getUsername()
                ),
                "Wrong profile username."
        );

        require(
                expectedEmail.equals(
                        profile.getEmail()
                ),
                "Wrong profile email."
        );

        System.out.println(
                "[PASS] Profile loaded from server"
        );
    }

    private static void testDuplicateUsernameRejected(
            ProfileClientService service,
            String duplicateUsername,
            String currentEmail
    ) throws Exception {
        ProfileResponse response =
                service.updateProfile(
                        new ProfileUpdateRequest(
                                duplicateUsername,
                                "Profile Tester",
                                currentEmail
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Duplicate username should be rejected."
        );

        System.out.println(
                "[PASS] Duplicate username rejected"
        );
    }

    private static void testInvalidEmailRejected(
            ProfileClientService service,
            String currentUsername,
            String invalidEmail
    ) throws Exception {
        ProfileResponse response =
                service.updateProfile(
                        new ProfileUpdateRequest(
                                currentUsername,
                                "Profile Tester",
                                invalidEmail
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Invalid email should be rejected."
        );

        System.out.println(
                "[PASS] Invalid email rejected"
        );
    }

    private static void testValidUpdate(
            ProfileClientService service,
            String username,
            String email
    ) throws Exception {
        ProfileResponse response =
                service.updateProfile(
                        new ProfileUpdateRequest(
                                username,
                                "Updated Tester",
                                email
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Valid profile update failed: "
                        + response.getMessage()
        );

        require(
                response.getProfile() != null,
                "Updated profile was not returned."
        );

        require(
                username.equals(
                        response.getProfile()
                                .getUsername()
                ),
                "Username was not updated."
        );

        require(
                "Updated Tester".equals(
                        response.getProfile()
                                .getNickname()
                ),
                "Nickname was not updated."
        );

        require(
                email.equals(
                        response.getProfile()
                                .getEmail()
                ),
                "Email was not updated."
        );

        System.out.println(
                "[PASS] Username/nickname/email updated"
        );
    }

    private static void testWrongCurrentPassword(
            ProfileClientService service
    ) throws Exception {
        ProfileResponse response =
                service.changePassword(
                        new ProfilePasswordChangeRequest(
                                "WrongPass1!",
                                NEW_PASSWORD
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Wrong current password should fail."
        );

        System.out.println(
                "[PASS] Wrong current password rejected"
        );
    }

    private static void testPasswordChange(
            ProfileClientService service
    ) throws Exception {
        ProfileResponse response =
                service.changePassword(
                        new ProfilePasswordChangeRequest(
                                OLD_PASSWORD,
                                NEW_PASSWORD
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "Password change failed: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] Password changed on server"
        );
    }

    private static void testOldPasswordRejected(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                OLD_PASSWORD,
                                false
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                !response.isSuccess(),
                "Old password should be rejected."
        );

        System.out.println(
                "[PASS] Old password rejected"
        );
    }

    private static void testNewPasswordAccepted(
            AccountClientService service,
            String username
    ) throws Exception {
        LoginResponse response =
                service.login(
                        new LoginRequest(
                                username,
                                NEW_PASSWORD,
                                false
                        )
                ).get(5, TimeUnit.SECONDS);

        require(
                response.isSuccess(),
                "New password should work: "
                        + response.getMessage()
        );

        System.out.println(
                "[PASS] New password accepted"
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
