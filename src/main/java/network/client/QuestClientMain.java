package network.client;

import network.client.service.AccountClientService;
import network.client.service.QuestClientService;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;
import network.protocol.quests.QuestEntryDto;
import network.protocol.quests.QuestResponse;
import network.server.GameServer;

import java.util.concurrent.TimeUnit;

public final class QuestClientMain {
    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "ValidPass1!";

    private QuestClientMain() {
    }

    public static void main(String[] args) {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "quest-test-" + suffix;
        String email = "quest" + suffix + "@example.com";

        try (NetworkClient client = new NetworkClient()) {
            client.connect(HOST, GameServer.DEFAULT_PORT);

            AccountClientService accountService =
                    new AccountClientService(client);
            QuestClientService questService =
                    new QuestClientService(client);

            QuestResponse unauth = questService.getQuests()
                    .get(5, TimeUnit.SECONDS);
            require(!unauth.isSuccess(),
                    "Unauthenticated quest request must fail.");
            System.out.println(
                    "[PASS] Unauthenticated quest request rejected"
            );

            RegisterResponse register = accountService.register(
                    new RegisterRequest(
                            username,
                            PASSWORD,
                            PASSWORD,
                            "Quest Tester",
                            email,
                            "male",
                            1,
                            "coffee",
                            "coffee"
                    )
            ).get(5, TimeUnit.SECONDS);
            require(register.isSuccess(),
                    "Registration failed: " + register.getMessage());
            System.out.println("[PASS] Test account registered");

            LoginResponse login = accountService.login(
                    new LoginRequest(username, PASSWORD, false)
            ).get(5, TimeUnit.SECONDS);
            require(login.isSuccess(),
                    "Login failed: " + login.getMessage());
            System.out.println("[PASS] Test account logged in");

            QuestResponse initial = questService.getQuests()
                    .get(5, TimeUnit.SECONDS);
            require(initial.isSuccess(),
                    "Quest load failed: " + initial.getMessage());
            require(!initial.getEntries().isEmpty(),
                    "Server returned no quest definitions.");
            System.out.println("[PASS] Quest list loaded from server");

            QuestEntryDto sunQuest = initial.getEntries().stream()
                    .filter(entry -> entry.getQuestId() == 1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Daily Sun Collector quest was not assigned."
                    ));

            int amountNeeded = Math.max(
                    1,
                    sunQuest.getTargetAmount() - sunQuest.getProgress()
            );
            QuestResponse progressed = questService.recordSunCollected(
                    amountNeeded
            ).get(5, TimeUnit.SECONDS);
            require(progressed.isSuccess(),
                    "Sun quest progress failed: " + progressed.getMessage());

            QuestEntryDto completed = progressed.getEntries().stream()
                    .filter(entry -> entry.getQuestId() == 1)
                    .findFirst()
                    .orElseThrow();
            require(completed.isCompleted(),
                    "Daily Sun Collector did not complete on server.");
            System.out.println("[PASS] Quest progress saved on server");

            int coinsBefore = progressed.getCoins();
            QuestResponse claimed = questService.claimQuest(1)
                    .get(5, TimeUnit.SECONDS);
            require(claimed.isSuccess(),
                    "Quest claim failed: " + claimed.getMessage());
            require(claimed.getCoins() > coinsBefore,
                    "Quest coin reward was not saved on server.");
            System.out.println("[PASS] Quest reward claimed on server");

            QuestResponse duplicate = questService.claimQuest(1)
                    .get(5, TimeUnit.SECONDS);
            require(!duplicate.isSuccess(),
                    "Quest reward must not be claimable twice.");
            System.out.println("[PASS] Duplicate quest claim rejected");

            System.out.println();
            System.out.println(
                    "[PASS] All server-backed quest tests passed."
            );
        } catch (Exception exception) {
            System.err.println();
            System.err.println(
                    "[FAIL] Quest test failed: " + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
