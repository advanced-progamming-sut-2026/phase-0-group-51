package network.client;

import network.client.service.AccountClientService;
import network.client.service.PlantOwnershipClientService;
import network.client.service.GameplayAccountClientService;
import network.client.service.GreenHouseClientService;
import network.client.service.ShopClientService;
import network.client.service.QuestClientService;
import network.client.service.MinigameClientService;

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
    private PlantOwnershipClientService plantOwnershipClientService;
    private GameplayAccountClientService gameplayAccountClientService;
    private GreenHouseClientService greenHouseClientService;
    private ShopClientService shopClientService;
    private QuestClientService questClientService;
    private MinigameClientService minigameClientService;

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
        plantOwnershipClientService =
                new PlantOwnershipClientService(newClient);
        gameplayAccountClientService =
                new GameplayAccountClientService(newClient);
        greenHouseClientService =
                new GreenHouseClientService(newClient);
        shopClientService =
                new ShopClientService(newClient);
        questClientService =
                new QuestClientService(newClient);
        minigameClientService =
                new MinigameClientService(newClient);
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

    public synchronized PlantOwnershipClientService
    getPlantOwnershipClientService() {
        if (!isConnected()
                || plantOwnershipClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }

        return plantOwnershipClientService;
    }

    public synchronized GameplayAccountClientService
    getGameplayAccountClientService() {
        if (!isConnected()
                || gameplayAccountClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }

        return gameplayAccountClientService;
    }

    public synchronized GreenHouseClientService
    getGreenHouseClientService() {
        if (!isConnected()
                || greenHouseClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }

        return greenHouseClientService;
    }

    public synchronized ShopClientService getShopClientService() {
        if (!isConnected() || shopClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }
        return shopClientService;
    }

    public synchronized QuestClientService getQuestClientService() {
        if (!isConnected() || questClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }
        return questClientService;
    }

    public synchronized MinigameClientService getMinigameClientService() {
        if (!isConnected() || minigameClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }
        return minigameClientService;
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
        plantOwnershipClientService = null;
        gameplayAccountClientService = null;
        greenHouseClientService = null;
        shopClientService = null;
        questClientService = null;
        minigameClientService = null;
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