package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.quests.QuestClaimRequest;
import network.protocol.quests.QuestResponse;
import network.protocol.quests.QuestRunSummary;
import network.protocol.quests.QuestSunProgressRequest;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class QuestClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public QuestClientService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<QuestResponse> getQuests()
            throws IOException {
        return send(
                MessageType.QUEST_GET_REQUEST,
                MessageType.QUEST_GET_RESPONSE,
                null
        );
    }

    public CompletableFuture<QuestResponse> claimQuest(int questId)
            throws IOException {
        try {
            return send(
                    MessageType.QUEST_CLAIM_REQUEST,
                    MessageType.QUEST_CLAIM_RESPONSE,
                    codec.encodePayload(new QuestClaimRequest(questId))
            );
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public CompletableFuture<QuestResponse> recordSunCollected(int amount)
            throws IOException {
        try {
            return send(
                    MessageType.QUEST_SUN_PROGRESS_REQUEST,
                    MessageType.QUEST_SUN_PROGRESS_RESPONSE,
                    codec.encodePayload(new QuestSunProgressRequest(amount))
            );
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public CompletableFuture<QuestResponse> recordAdventureRun(
            QuestRunSummary summary
    ) throws IOException {
        try {
            return send(
                    MessageType.QUEST_RUN_RECORD_REQUEST,
                    MessageType.QUEST_RUN_RECORD_RESPONSE,
                    codec.encodePayload(summary)
            );
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private CompletableFuture<QuestResponse> send(
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
                .thenApply(response -> decode(response, responseType));
    }

    private QuestResponse decode(
            NetworkMessage response,
            MessageType expectedType
    ) {
        if (response == null) {
            throw new CompletionException(
                    new IOException("Server returned no response.")
            );
        }

        if (response.getType() == MessageType.ERROR) {
            return new QuestResponse(
                    false,
                    response.getPayload(),
                    java.util.List.of(),
                    0,
                    0,
                    0,
                    0,
                    java.util.List.of()
            );
        }

        if (response.getType() != expectedType) {
            throw new CompletionException(
                    new IOException(
                            "Unexpected response type: " + response.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    response.getPayload(),
                    QuestResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}
