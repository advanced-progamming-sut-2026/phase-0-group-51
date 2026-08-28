package network.client;

import network.client.service.AccountClientService;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class ClientNetworkManager implements Closeable {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 5050;

    private final String host;
    private final int port;

    private NetworkClient networkClient;
    private AccountClientService accountClientService;

    public ClientNetworkManager() {
        this(
                System.getProperty(
                        "pvz.server.host",
                        DEFAULT_HOST
                ),
                readPort()
        );
    }

    public ClientNetworkManager(
            String host,
            int port
    ) {
        this.host = host;
        this.port = port;
    }

    public CompletableFuture<Void> ensureConnectedAsync() {
        if (isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        });
    }

    private synchronized void ensureConnected()
            throws IOException {
        if (isConnected()) {
            return;
        }

        closeCurrentClient();

        NetworkClient newClient =
                new NetworkClient();

        try {
            newClient.connect(host, port);
        } catch (IOException exception) {
            newClient.close();
            throw exception;
        }

        networkClient = newClient;
        accountClientService =
                new AccountClientService(newClient);
    }

    public synchronized boolean isConnected() {
        return networkClient != null
                && networkClient.isConnected();
    }

    public synchronized AccountClientService
    getAccountClientService() {
        if (!isConnected()
                || accountClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }

        return accountClientService;
    }

    public synchronized NetworkClient getNetworkClient() {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }

        return networkClient;
    }

    @Override
    public synchronized void close() {
        closeCurrentClient();
    }

    private void closeCurrentClient() {
        if (networkClient != null) {
            networkClient.close();
        }

        networkClient = null;
        accountClientService = null;
    }

    private static int readPort() {
        String value = System.getProperty(
                "pvz.server.port"
        );

        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return DEFAULT_PORT;
        }
    }
}