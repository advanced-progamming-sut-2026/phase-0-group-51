package network.client;

import models.minigames.MinigameType;
import network.client.service.AccountClientService;
import network.client.service.MinigameClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.minigame.MinigameProgressDto;
import network.protocol.minigame.MinigameProgressResponse;
import network.protocol.minigame.ScoringResultResponse;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class MinigameAccountClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private MinigameAccountClientMain() {
    }

    public static void main(String[] args) {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "minigame-test-" + suffix;
        String email = "minigame" + suffix + "@example.com";

        try (NetworkClient client = new NetworkClient()) {
            client.connect(HOST, GameServer.DEFAULT_PORT);

            AccountClientService accountService =
                    new AccountClientService(client);
            MinigameClientService minigameService =
                    new MinigameClientService(client);

            testUnauthenticatedRejected(minigameService);
            register(accountService, username, email);
            login(accountService, username);
            testInitialProgress(minigameService);
            testLockedStageRejected(minigameService);
            testStageUnlockingAndCounter(minigameService);
            testScoringStats(minigameService);

            System.out.println();
            System.out.println(
                    "[PASS] All minigame account tests passed."
            );
        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Minigame account test failed: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private static void testUnauthenticatedRejected(
            MinigameClientService service
    ) throws Exception {
        MinigameProgressResponse response = service.getProgress()
                .get(5, TimeUnit.SECONDS);
        require(
                !response.isSuccess(),
                "Unauthenticated minigame request must fail."
        );
        System.out.println(
                "[PASS] Unauthenticated minigame request rejected"
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
                        "Minigame Tester",
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

    private static void testInitialProgress(
            MinigameClientService service
    ) throws Exception {
        MinigameProgressResponse response = service.getProgress()
                .get(5, TimeUnit.SECONDS);
        require(response.isSuccess(), response.getMessage());
        require(
                response.getProgress().size()
                        == MinigameType.values().length,
                "Server must return every minigame type."
        );
        for (MinigameProgressDto item : response.getProgress()) {
            require(
                    item.getHighestUnlockedStage() == 1,
                    item.getType() + " must start with stage 1 unlocked."
            );
            require(
                    item.getHighestCompletedStage() == 0,
                    item.getType() + " must start with no completed stage."
            );
        }
        require(
                response.getMiniGamesPlayed() == 0,
                "New account must start with zero completed minigames."
        );
        System.out.println(
                "[PASS] Initial minigame progress loaded from server"
        );
    }

    private static void testLockedStageRejected(
            MinigameClientService service
    ) throws Exception {
        MinigameProgressResponse response = service.completeStage(
                MinigameType.VASEBREAKER,
                2
        ).get(5, TimeUnit.SECONDS);
        require(
                !response.isSuccess(),
                "Server must reject completion of a locked stage."
        );
        System.out.println(
                "[PASS] Locked minigame stage completion rejected"
        );
    }

    private static void testStageUnlockingAndCounter(
            MinigameClientService service
    ) throws Exception {
        MinigameProgressResponse stage1 = service.completeStage(
                MinigameType.VASEBREAKER,
                1
        ).get(5, TimeUnit.SECONDS);
        require(stage1.isSuccess(), stage1.getMessage());
        require(
                progress(stage1, MinigameType.VASEBREAKER)
                        .getHighestUnlockedStage() == 2,
                "Stage 1 completion must unlock stage 2."
        );

        MinigameProgressResponse stage2 = service.completeStage(
                MinigameType.VASEBREAKER,
                2
        ).get(5, TimeUnit.SECONDS);
        require(stage2.isSuccess(), stage2.getMessage());
        require(
                progress(stage2, MinigameType.VASEBREAKER)
                        .getHighestUnlockedStage() == 3,
                "Stage 2 completion must unlock stage 3."
        );

        MinigameProgressResponse stage3 = service.completeStage(
                MinigameType.VASEBREAKER,
                3
        ).get(5, TimeUnit.SECONDS);
        require(stage3.isSuccess(), stage3.getMessage());
        require(
                progress(stage3, MinigameType.VASEBREAKER)
                        .getHighestCompletedStage() == 3,
                "Stage 3 completion must complete the minigame."
        );
        require(
                stage3.getMiniGamesPlayed() == 1,
                "Completing all three stages must increment mini_games_played once."
        );

        MinigameProgressResponse repeated = service.completeStage(
                MinigameType.VASEBREAKER,
                3
        ).get(5, TimeUnit.SECONDS);
        require(repeated.isSuccess(), repeated.getMessage());
        require(
                repeated.getMiniGamesPlayed() == 1,
                "Repeating stage 3 must not increment mini_games_played again."
        );

        System.out.println(
                "[PASS] Stage unlocking and completed-minigame counter are server-backed"
        );
    }

    private static void testScoringStats(
            MinigameClientService service
    ) throws Exception {
        ScoringResultResponse first = service.submitScoringResult(
                1234,
                true
        ).get(5, TimeUnit.SECONDS);
        require(first.isSuccess(), first.getMessage());
        require(
                first.getDailyBest() == 1234,
                "First MeowPoint result must become today's best."
        );
        require(
                first.getMostMeowPoint() == 1234
                        && first.getMaxPoint() == 1234,
                "Best score statistics must be stored on the server."
        );

        int gamesAfterFirst = first.getGamesPlayed();

        ScoringResultResponse lower = service.submitScoringResult(
                500,
                false
        ).get(5, TimeUnit.SECONDS);
        require(lower.isSuccess(), lower.getMessage());
        require(
                lower.getDailyBest() == 1234,
                "A lower score must not replace today's best."
        );
        require(
                lower.getMostMeowPoint() == 1234
                        && lower.getMaxPoint() == 1234,
                "A lower score must not reduce stored best statistics."
        );
        require(
                lower.getGamesPlayed() == gamesAfterFirst + 1,
                "Each MeowPoint run must increment games_played once."
        );

        System.out.println(
                "[PASS] MeowPoint score statistics are server-backed"
        );
    }

    private static MinigameProgressDto progress(
            MinigameProgressResponse response,
            MinigameType type
    ) {
        return response.getProgress().stream()
                .filter(item -> item != null && item.getType() == type)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Missing progress for " + type
                        )
                );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
