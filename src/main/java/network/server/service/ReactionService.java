package network.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;

import network.protocol.reaction.ReactionReceivedDto;
import network.protocol.reaction.ReactionSendDto;
import network.protocol.reaction.ReactionSendResponse;

import network.server.ClientConnection;
import network.server.presence.ConnectionRegistry;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class ReactionService {

    private static final long REACTION_COOLDOWN_NANOS =
            TimeUnit.SECONDS.toNanos(1);


    private final ConnectionRegistry connectionRegistry;

    private final MatchNetworkService matchNetworkService;

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();


    private final Map<String, Long> lastReactionTime =
            new ConcurrentHashMap<>();


    public ReactionService(
            ConnectionRegistry connectionRegistry,
            MatchNetworkService matchNetworkService
    ) {

        this.connectionRegistry =
                Objects.requireNonNull(
                        connectionRegistry,
                        "connectionRegistry cannot be null"
                );


        this.matchNetworkService =
                Objects.requireNonNull(
                        matchNetworkService,
                        "matchNetworkService cannot be null"
                );
    }


    public NetworkMessage handleSend(
            ClientConnection connection,
            NetworkMessage message
    ) {

        if (message == null) {

            return NetworkMessage.error(
                    null,
                    "Reaction request is missing."
            );
        }


        String username =
                authenticatedUsername(
                        connection
                );


        if (username == null) {

            return NetworkMessage.error(
                    message.getRequestId(),
                    "You must be logged in."
            );
        }


        MatchNetworkService.ActiveMatchInfo matchInfo =
                matchNetworkService.getActiveMatch(
                        username
                );


        if (matchInfo == null
                || !matchNetworkService.isInMatch(username)) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "You are not in an active match."
            );
        }


        if (message.getPayload() == null
                || message.getPayload().isBlank()) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Reaction payload is required."
            );
        }


        final ReactionSendDto request;


        try {

            request =
                    codec.decodePayload(
                            message.getPayload(),
                            ReactionSendDto.class
                    );

        } catch (JsonProcessingException exception) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Invalid reaction payload."
            );
        }


        if (request == null) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Invalid reaction request."
            );
        }


        if (request.matchId() == null
                || request.matchId().isBlank()) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "matchId is required."
            );
        }

        if (!matchInfo.matchId().equals(
                request.matchId()
        )) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Reaction does not belong to your active match."
            );
        }


        if (request.reactionId() == null) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "reactionId is required."
            );
        }


        String opponentUsername =
                matchInfo.opponentUsername();


        if (opponentUsername == null
                || opponentUsername.isBlank()) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Opponent is unavailable."
            );
        }


        MatchNetworkService.ActiveMatchInfo opponentMatch =
                matchNetworkService.getActiveMatch(
                        opponentUsername
                );


        if (opponentMatch == null
                || !matchInfo.matchId().equals(
                opponentMatch.matchId()
        )) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Opponent is no longer in this match."
            );
        }


        ClientConnection opponentConnection =
                connectionRegistry.getConnection(
                        opponentUsername
                );


        if (opponentConnection == null) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Opponent is offline."
            );
        }

        if (!tryAcquireReactionSlot(
                username
        )) {

            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "You are sending reactions too quickly."
            );
        }


        ReactionReceivedDto received =
                new ReactionReceivedDto(
                        matchInfo.matchId(),
                        username,
                        request.reactionId()
                );


        boolean delivered =
                sendReactionPush(
                        opponentConnection,
                        received
                );


        if (!delivered) {

            lastReactionTime.remove(
                    username
            );


            return reactionResponse(
                    message.getRequestId(),
                    false,
                    "Could not deliver reaction to opponent."
            );
        }


        return reactionResponse(
                message.getRequestId(),
                true,
                "Reaction sent."
        );
    }

    private synchronized boolean tryAcquireReactionSlot(
            String username
    ) {

        long now =
                System.nanoTime();


        Long previous =
                lastReactionTime.get(
                        username
                );


        if (previous != null) {

            long elapsed =
                    now - previous;


            if (elapsed
                    < REACTION_COOLDOWN_NANOS) {

                return false;
            }
        }


        lastReactionTime.put(
                username,
                now
        );


        return true;
    }


    private boolean sendReactionPush(
            ClientConnection opponentConnection,
            ReactionReceivedDto reaction
    ) {

        try {

            String payload =
                    codec.encodePayload(
                            reaction
                    );


            opponentConnection.send(
                    new NetworkMessage(
                            MessageType.REACTION_RECEIVED,
                            null,
                            payload
                    )
            );


            return true;

        } catch (
                IOException exception
        ) {

            System.err.println(
                    "[REACTION] Could not deliver reaction: "
                            + exception.getMessage()
            );


            return false;
        }
    }


    private NetworkMessage reactionResponse(
            String requestId,
            boolean success,
            String text
    ) {

        try {

            return new NetworkMessage(
                    MessageType.REACTION_SEND_RESPONSE,
                    requestId,
                    codec.encodePayload(
                            new ReactionSendResponse(
                                    success,
                                    text
                            )
                    )
            );

        } catch (JsonProcessingException exception) {

            return NetworkMessage.error(
                    requestId,
                    "Could not create reaction response."
            );
        }
    }


    private String authenticatedUsername(
            ClientConnection connection
    ) {

        if (connection == null
                || connection.getSession() == null
                || !connection
                .getSession()
                .isAuthenticated()) {

            return null;
        }


        String username =
                connection
                        .getSession()
                        .getUsername();


        if (username == null
                || username.isBlank()) {

            return null;
        }


        return username.trim();
    }


    public void handleDisconnect(
            String username
    ) {

        if (username == null
                || username.isBlank()) {

            return;
        }


        lastReactionTime.remove(
                username.trim()
        );
    }
}