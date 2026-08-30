package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.news.NewsResponse;
import network.protocol.news.ZombieDiscoverRequest;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class NewsClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public NewsClientService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<NewsResponse> getNews()
            throws IOException {
        return send(
                MessageType.NEWS_GET_REQUEST,
                MessageType.NEWS_GET_RESPONSE,
                null
        );
    }

    public CompletableFuture<NewsResponse> markAllRead()
            throws IOException {
        return send(
                MessageType.NEWS_MARK_ALL_READ_REQUEST,
                MessageType.NEWS_MARK_ALL_READ_RESPONSE,
                null
        );
    }

    public CompletableFuture<NewsResponse> discoverZombie(
            String zombieAlias
    ) throws IOException {
        try {
            return send(
                    MessageType.ZOMBIE_DISCOVER_REQUEST,
                    MessageType.ZOMBIE_DISCOVER_RESPONSE,
                    codec.encodePayload(
                            new ZombieDiscoverRequest(zombieAlias)
                    )
            );
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private CompletableFuture<NewsResponse> send(
            MessageType requestType,
            MessageType responseType,
            String payload
    ) throws IOException {
        NetworkMessage request = new NetworkMessage(
                requestType,
                UUID.randomUUID().toString(),
                payload
        );

        return networkClient.sendRequest(request)
                .thenApply(response -> {
                    if (response == null) {
                        throw new CompletionException(
                                new IOException("Server returned no response.")
                        );
                    }
                    if (response.getType() == MessageType.ERROR) {
                        throw new CompletionException(
                                new IOException(response.getPayload())
                        );
                    }
                    if (response.getType() != responseType) {
                        throw new CompletionException(
                                new IOException(
                                        "Unexpected response type: "
                                                + response.getType()
                                )
                        );
                    }
                    try {
                        return codec.decodePayload(
                                response.getPayload(),
                                NewsResponse.class
                        );
                    } catch (JsonProcessingException exception) {
                        throw new CompletionException(exception);
                    }
                });
    }
}
