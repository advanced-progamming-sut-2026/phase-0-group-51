package network.client;

import network.client.service.AccountClientService;
import network.client.service.ProfileClientService;
import network.client.service.NewsClientService;
import network.client.service.PlantOwnershipClientService;
import network.client.service.GameplayAccountClientService;
import network.client.service.GreenHouseClientService;
import network.client.service.ShopClientService;
import network.client.service.QuestClientService;
import network.client.service.MinigameClientService;
import network.client.service.LeaderboardClientService;

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
    private ProfileClientService profileClientService;
    private NewsClientService newsClientService;
    private PlantOwnershipClientService plantOwnershipClientService;
    private GameplayAccountClientService gameplayAccountClientService;
    private GreenHouseClientService greenHouseClientService;
    private ShopClientService shopClientService;
    private QuestClientService questClientService;
    private MinigameClientService minigameClientService;
    private LeaderboardClientService leaderboardClientService;

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
        profileClientService =
                new ProfileClientService(newClient);
        newsClientService =
                new NewsClientService(newClient);
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
        leaderboardClientService =
                new LeaderboardClientService(newClient);
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

    public synchronized ProfileClientService getProfileClientService() {
        if (!isConnected() || profileClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }
        return profileClientService;
    }

    public synchronized NewsClientService getNewsClientService() {
        if (!isConnected() || newsClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }
        return newsClientService;
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

    public synchronized LeaderboardClientService
    getLeaderboardClientService() {
        if (!isConnected() || leaderboardClientService == null) {
            throw new IllegalStateException(
                    "Client is not connected to server."
            );
        }
        return leaderboardClientService;
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
        profileClientService = null;
        newsClientService = null;
        plantOwnershipClientService = null;
        gameplayAccountClientService = null;
        greenHouseClientService = null;
        shopClientService = null;
        questClientService = null;
        minigameClientService = null;
        leaderboardClientService = null;
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