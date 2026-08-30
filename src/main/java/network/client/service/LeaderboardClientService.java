package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.leaderboard.LeaderboardResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LeaderboardClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public LeaderboardClientService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<LeaderboardResponse> getLeaderboard()
            throws IOException {
        NetworkMessage request = new NetworkMessage(
                MessageType.LEADERBOARD_GET_REQUEST,
                UUID.randomUUID().toString(),
                null
        );

        return networkClient.sendRequest(request)
                .thenApply(this::decodeResponse);
    }

    private LeaderboardResponse decodeResponse(
            NetworkMessage response
    ) {
        if (response == null) {
            throw new CompletionException(
                    new IOException("Server returned no response.")
            );
        }

        if (response.getType() == MessageType.ERROR) {
            return new LeaderboardResponse(
                    false,
                    response.getPayload(),
                    java.util.List.of()
            );
        }

        if (response.getType()
                != MessageType.LEADERBOARD_GET_RESPONSE) {
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
                    LeaderboardResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}
