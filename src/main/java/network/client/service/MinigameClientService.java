package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import models.minigames.MinigameType;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.minigame.MinigameCompleteRequest;
import network.protocol.minigame.MinigameProgressResponse;
import network.protocol.minigame.ScoringResultRequest;
import network.protocol.minigame.ScoringResultResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class MinigameClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public MinigameClientService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<MinigameProgressResponse> getProgress()
            throws IOException {
        NetworkMessage request = new NetworkMessage(
                MessageType.MINIGAME_PROGRESS_GET_REQUEST,
                UUID.randomUUID().toString(),
                null
        );
        return networkClient.sendRequest(request)
                .thenApply(this::decodeProgressResponse);
    }

    public CompletableFuture<MinigameProgressResponse> completeStage(
            MinigameType type,
            int stageNumber
    ) throws IOException {
        try {
            String payload = codec.encodePayload(
                    new MinigameCompleteRequest(type, stageNumber)
            );
            NetworkMessage request = new NetworkMessage(
                    MessageType.MINIGAME_COMPLETE_REQUEST,
                    UUID.randomUUID().toString(),
                    payload
            );
            return networkClient.sendRequest(request)
                    .thenApply(this::decodeProgressResponse);
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public CompletableFuture<ScoringResultResponse> submitScoringResult(
            int score,
            boolean won
    ) throws IOException {
        try {
            String payload = codec.encodePayload(
                    new ScoringResultRequest(score, won)
            );
            NetworkMessage request = new NetworkMessage(
                    MessageType.SCORING_RESULT_REQUEST,
                    UUID.randomUUID().toString(),
                    payload
            );
            return networkClient.sendRequest(request)
                    .thenApply(this::decodeScoringResponse);
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private MinigameProgressResponse decodeProgressResponse(
            NetworkMessage message
    ) {
        if (message == null) {
            throw new CompletionException(
                    new IOException("Server returned no minigame response.")
            );
        }
        if (message.getType() == MessageType.ERROR) {
            return new MinigameProgressResponse(
                    false,
                    message.getPayload(),
                    java.util.List.of(),
                    0
            );
        }
        if (message.getType() != MessageType.MINIGAME_PROGRESS_GET_RESPONSE
                && message.getType() != MessageType.MINIGAME_COMPLETE_RESPONSE) {
            throw new CompletionException(
                    new IOException(
                            "Unexpected minigame response type: "
                                    + message.getType()
                    )
            );
        }
        try {
            return codec.decodePayload(
                    message.getPayload(),
                    MinigameProgressResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }

    private ScoringResultResponse decodeScoringResponse(
            NetworkMessage message
    ) {
        if (message == null) {
            throw new CompletionException(
                    new IOException("Server returned no scoring response.")
            );
        }
        if (message.getType() == MessageType.ERROR) {
            return new ScoringResultResponse(
                    false,
                    message.getPayload(),
                    0,
                    0,
                    0,
                    0
            );
        }
        if (message.getType() != MessageType.SCORING_RESULT_RESPONSE) {
            throw new CompletionException(
                    new IOException(
                            "Unexpected scoring response type: "
                                    + message.getType()
                    )
            );
        }
        try {
            return codec.decodePayload(
                    message.getPayload(),
                    ScoringResultResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}
