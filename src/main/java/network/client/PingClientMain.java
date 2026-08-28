package network.client;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PingClientMain {
    private static final int PING_COUNT = 5;

    private PingClientMain() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = parsePort(args);

        try (NetworkClient client = new NetworkClient()) {
            client.connect(host, port);
            System.out.println("Connected to " + host + ":" + port + ".");
            runPings(client);
            System.out.println("[PASS] All ping/pong checks succeeded.");
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException exception) {
            System.err.println("[FAIL] Ping test failed: " + exception.getMessage());
        }
    }

    private static void runPings(NetworkClient client)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        for (int index = 1; index <= PING_COUNT; index++) {
            String payload = "phase3-milestone1-" + index;
            NetworkMessage response = client.ping(payload).get(3, TimeUnit.SECONDS);
            validateResponse(response, payload);
            System.out.println("PONG " + index + ": requestId=" + response.getRequestId());
            Thread.sleep(500L);
        }
    }

    private static void validateResponse(NetworkMessage response, String expectedPayload) throws IOException {
        if (response.getType() != MessageType.PONG) {
            throw new IOException("Expected PONG but received " + response.getType() + ".");
        }
        if (!expectedPayload.equals(response.getPayload())) {
            throw new IOException("PONG payload did not match the PING payload.");
        }
        if (response.getRequestId() == null || response.getRequestId().isBlank()) {
            throw new IOException("PONG did not contain the original requestId.");
        }
    }

    private static int parsePort(String[] args) {
        if (args.length < 2) {
            return 5050;
        }
        try {
            return Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            return 5050;
        }
    }
}
