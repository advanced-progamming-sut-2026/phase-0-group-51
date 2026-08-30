package network.client.service;

import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.match.ActionResultDto;
import network.protocol.match.GameActionDto;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.match.GameActionDto;
import network.protocol.match.ActionResultDto;
import network.protocol.matchmaking.*;
import network.protocol.reaction.ReactionSendDto;
import network.protocol.reaction.ReactionSendResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
public class MatchClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public MatchClientService(
            NetworkClient networkClient
    ) {
        if (networkClient == null) {
            throw new IllegalArgumentException(
                    "networkClient cannot be null"
            );
        }

        this.networkClient = networkClient;
    }

    public CompletableFuture<QueueResponse>
    joinRandomQueue() throws IOException {

        return send(
                MessageType.MATCHMAKING_QUEUE_REQUEST,
                MessageType.MATCHMAKING_QUEUE_RESPONSE,
                new QueueRequest(),
                QueueResponse.class
        );
    }

    public CompletableFuture<QueueResponse>
    leaveRandomQueue() throws IOException {

        return send(
                MessageType.MATCHMAKING_QUEUE_LEAVE_REQUEST,
                MessageType.MATCHMAKING_QUEUE_LEAVE_RESPONSE,
                null,
                QueueResponse.class
        );
    }

    public CompletableFuture<InviteResponse>
    challenge(
            String username
    ) throws IOException {

        return send(
                MessageType.MATCHMAKING_INVITE_REQUEST,
                MessageType.MATCHMAKING_INVITE_RESPONSE,
                new InviteRequest(username),
                InviteResponse.class
        );
    }

    public CompletableFuture<InviteDecisionResponse>
    respondToInvite(
            String challenger,
            boolean accepted
    ) throws IOException {

        return send(
                MessageType.MATCHMAKING_INVITE_DECISION,
                MessageType.MATCHMAKING_INVITE_DECISION_RESPONSE,
                new InviteDecision(
                        challenger,
                        accepted
                ),
                InviteDecisionResponse.class
        );
    }

    public CompletableFuture<ActionResultDto>
    sendAction(
            GameActionDto action
    ) throws IOException {

        return send(
                MessageType.MATCH_ACTION_REQUEST,
                MessageType.MATCH_ACTION_RESPONSE,
                action,
                ActionResultDto.class
        );
    }

    public CompletableFuture<ReactionSendResponse>
    sendReaction(
            ReactionSendDto reaction
    ) throws IOException {

        return send(
                MessageType.REACTION_SEND_REQUEST,
                MessageType.REACTION_SEND_RESPONSE,
                reaction,
                ReactionSendResponse.class
        );
    }

    private <T> CompletableFuture<T> send(
            MessageType requestType,
            MessageType expectedResponse,
            Object payloadObject,
            Class<T> responseClass
    ) throws IOException {

        String payload;

        try {
            payload =
                    payloadObject == null
                            ? null
                            : codec.encodePayload(
                            payloadObject
                    );
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(
                    exception
            );
        }

        NetworkMessage request =
                new NetworkMessage(
                        requestType,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(request)
                .thenApply(response -> {

                    if (response.getType()
                            == MessageType.ERROR) {

                        throw new CompletionException(
                                new IOException(
                                        response.getPayload()
                                )
                        );
                    }

                    if (response.getType()
                            != expectedResponse) {

                        throw new CompletionException(
                                new IOException(
                                        "Unexpected response: "
                                                + response.getType()
                                )
                        );
                    }

                    try {
                        return codec.decodePayload(
                                response.getPayload(),
                                responseClass
                        );
                    } catch (
                            JsonProcessingException exception
                    ) {
                        throw new CompletionException(
                                exception
                        );
                    }
                });
    }
}
