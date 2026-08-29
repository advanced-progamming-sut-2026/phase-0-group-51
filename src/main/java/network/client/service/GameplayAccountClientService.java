package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import models.enums.LootType;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.gameplay.AdventureLossResponse;
import network.protocol.gameplay.AdventureProgressResponse;
import network.protocol.gameplay.AdventureWinRequest;
import network.protocol.gameplay.AdventureWinResponse;
import network.protocol.gameplay.LootCollectRequest;
import network.protocol.gameplay.LootCollectResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class GameplayAccountClientService {
    private final NetworkClient networkClient;

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public GameplayAccountClientService(
            NetworkClient networkClient
    ) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<LootCollectResponse>
    collectLoot(
            LootType type
    ) throws IOException {
        String payload =
                codec.encodePayload(
                        new LootCollectRequest(type)
                );

        NetworkMessage message =
                new NetworkMessage(
                        MessageType.GAMEPLAY_LOOT_COLLECT_REQUEST,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        response -> decodeLootResponse(
                                response
                        )
                );
    }

    public CompletableFuture<AdventureLossResponse>
    recordAdventureLoss() throws IOException {
        NetworkMessage message =
                new NetworkMessage(
                        MessageType.ADVENTURE_LOSS_REQUEST,
                        UUID.randomUUID().toString(),
                        null
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        this::decodeAdventureLossResponse
                );
    }


    public CompletableFuture<AdventureProgressResponse>
    getAdventureProgress() throws IOException {
        NetworkMessage message =
                new NetworkMessage(
                        MessageType.ADVENTURE_PROGRESS_GET_REQUEST,
                        UUID.randomUUID().toString(),
                        null
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        this::decodeAdventureProgressResponse
                );
    }


    public CompletableFuture<AdventureWinResponse>
    recordAdventureWin(
            int completedChapter,
            int completedLevel
    ) throws IOException {
        String payload =
                codec.encodePayload(
                        new AdventureWinRequest(
                                completedChapter,
                                completedLevel
                        )
                );

        NetworkMessage message =
                new NetworkMessage(
                        MessageType.ADVENTURE_WIN_REQUEST,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        this::decodeAdventureWinResponse
                );
    }

    private LootCollectResponse decodeLootResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new LootCollectResponse(
                    false,
                    message.getPayload(),
                    null,
                    0,
                    0,
                    0
            );
        }

        if (message.getType()
                != MessageType.GAMEPLAY_LOOT_COLLECT_RESPONSE) {
            throw new CompletionException(
                    new IllegalStateException(
                            "Unexpected response type: "
                                    + message.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    LootCollectResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }

    private AdventureLossResponse
    decodeAdventureLossResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new AdventureLossResponse(
                    false,
                    message.getPayload(),
                    0
            );
        }

        if (message.getType()
                != MessageType.ADVENTURE_LOSS_RESPONSE) {
            throw new CompletionException(
                    new IllegalStateException(
                            "Unexpected response type: "
                                    + message.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    AdventureLossResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
    private AdventureProgressResponse
    decodeAdventureProgressResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new AdventureProgressResponse(
                    false,
                    message.getPayload(),
                    1,
                    1
            );
        }

        if (message.getType()
                != MessageType.ADVENTURE_PROGRESS_GET_RESPONSE) {
            throw new CompletionException(
                    new IllegalStateException(
                            "Unexpected response type: "
                                    + message.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    AdventureProgressResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }

    private AdventureWinResponse
    decodeAdventureWinResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new AdventureWinResponse(
                    false,
                    message.getPayload(),
                    0,
                    null,
                    1,
                    1,
                    false,
                    java.util.List.of(),
                    java.util.List.of()
            );
        }

        if (message.getType()
                != MessageType.ADVENTURE_WIN_RESPONSE) {
            throw new CompletionException(
                    new IllegalStateException(
                            "Unexpected response type: "
                                    + message.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    AdventureWinResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}
