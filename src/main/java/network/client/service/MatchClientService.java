package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;

import network.protocol.match.ActionResultDto;
import network.protocol.match.GameActionDto;

import network.protocol.matchmaking.InviteDecision;
import network.protocol.matchmaking.InviteDecisionResponse;
import network.protocol.matchmaking.InviteRequest;
import network.protocol.matchmaking.InviteResponse;
import network.protocol.matchmaking.QueueRequest;
import network.protocol.matchmaking.QueueResponse;

import network.protocol.reaction.ReactionSendDto;
import network.protocol.reaction.ReactionSendResponse;

import java.io.IOException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;


public final class MatchClientService {

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

        this.networkClient =
                networkClient;
    }

    public CompletableFuture<QueueResponse>
    joinRandomQueue()
            throws IOException {

        return send(
                MessageType.MATCHMAKING_QUEUE_REQUEST,
                MessageType.MATCHMAKING_QUEUE_RESPONSE,
                new QueueRequest(),
                QueueResponse.class
        );
    }


    public CompletableFuture<QueueResponse>
    leaveRandomQueue()
            throws IOException {

        return send(
                MessageType.MATCHMAKING_QUEUE_LEAVE_REQUEST,
                MessageType.MATCHMAKING_QUEUE_LEAVE_RESPONSE,
                null,
                QueueResponse.class
        );
    }

    public CompletableFuture<InviteResponse>
    challenge(
            String targetUsername
    ) throws IOException {

        return send(
                MessageType.MATCHMAKING_INVITE_REQUEST,
                MessageType.MATCHMAKING_INVITE_RESPONSE,
                new InviteRequest(
                        targetUsername
                ),
                InviteResponse.class
        );
    }


    public CompletableFuture<InviteDecisionResponse>
    respondToInvite(
            String challengerUsername,
            boolean accepted
    ) throws IOException {

        return send(
                MessageType.MATCHMAKING_INVITE_DECISION,
                MessageType.MATCHMAKING_INVITE_DECISION_RESPONSE,
                new InviteDecision(
                        challengerUsername,
                        accepted
                ),
                InviteDecisionResponse.class
        );
    }


    public CompletableFuture<ActionResultDto>
    sendAction(
            GameActionDto action
    ) throws IOException {

        if (action == null) {

            throw new IllegalArgumentException(
                    "action cannot be null"
            );
        }

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

        if (reaction == null) {

            throw new IllegalArgumentException(
                    "reaction cannot be null"
            );
        }

        return send(
                MessageType.REACTION_SEND_REQUEST,
                MessageType.REACTION_SEND_RESPONSE,
                reaction,
                ReactionSendResponse.class
        );
    }

    private <T> CompletableFuture<T> send(
            MessageType requestType,
            MessageType expectedResponseType,
            Object payloadObject,
            Class<T> responseClass
    ) throws IOException {

        if (requestType == null) {

            throw new IllegalArgumentException(
                    "requestType cannot be null"
            );
        }

        if (expectedResponseType == null) {

            throw new IllegalArgumentException(
                    "expectedResponseType cannot be null"
            );
        }

        if (responseClass == null) {

            throw new IllegalArgumentException(
                    "responseClass cannot be null"
            );
        }


        final String payload;


        try {

            payload =
                    payloadObject == null
                            ? null
                            : codec.encodePayload(
                            payloadObject
                    );

        } catch (
                JsonProcessingException exception
        ) {

            return CompletableFuture
                    .failedFuture(
                            exception
                    );
        }


        NetworkMessage request =
                new NetworkMessage(
                        requestType,
                        UUID.randomUUID()
                                .toString(),
                        payload
                );


        return networkClient
                .sendRequest(
                        request
                )
                .thenApply(
                        response -> {

                            if (response == null) {

                                throw new CompletionException(
                                        new IOException(
                                                "Server returned no response."
                                        )
                                );
                            }


                            if (response.getType()
                                    == MessageType.ERROR) {

                                throw new CompletionException(
                                        new IOException(
                                                response.getPayload()
                                        )
                                );
                            }


                            if (response.getType()
                                    != expectedResponseType) {

                                throw new CompletionException(
                                        new IOException(
                                                "Unexpected response type: "
                                                        + response.getType()
                                                        + ". Expected: "
                                                        + expectedResponseType
                                        )
                                );
                            }


                            if (response.getPayload()
                                    == null) {

                                throw new CompletionException(
                                        new IOException(
                                                "Server response payload is missing."
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
                        }
                );
    }
}