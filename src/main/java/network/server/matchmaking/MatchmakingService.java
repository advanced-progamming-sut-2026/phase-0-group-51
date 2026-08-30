package network.server.matchmaking;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.matchmaking.InviteDecision;
import network.protocol.matchmaking.InviteDecisionResponse;
import network.protocol.matchmaking.InviteReceived;
import network.protocol.matchmaking.InviteRequest;
import network.protocol.matchmaking.InviteResponse;
import network.protocol.matchmaking.MatchFoundDto;
import network.protocol.matchmaking.QueueResponse;
import network.server.ClientConnection;
import network.server.presence.ConnectionRegistry;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;


public final class MatchmakingService {

    private final ConnectionRegistry connectionRegistry;
    private final RandomQueue randomQueue;
    private final InviteManager inviteManager;
    private final MatchmakingStateRegistry states;

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();


    public MatchmakingService(
            ConnectionRegistry connectionRegistry,
            RandomQueue randomQueue,
            InviteManager inviteManager,
            MatchmakingStateRegistry states
    ) {

        this.connectionRegistry =
                Objects.requireNonNull(
                        connectionRegistry,
                        "connectionRegistry cannot be null"
                );

        this.randomQueue =
                Objects.requireNonNull(
                        randomQueue,
                        "randomQueue cannot be null"
                );

        this.inviteManager =
                Objects.requireNonNull(
                        inviteManager,
                        "inviteManager cannot be null"
                );

        this.states =
                Objects.requireNonNull(
                        states,
                        "states cannot be null"
                );
    }

    public synchronized NetworkMessage handleQueueRequest(
            ClientConnection connection,
            NetworkMessage message
    ) {

        String username =
                authenticatedUsername(connection);


        if (username == null) {

            return NetworkMessage.error(
                    message.getRequestId(),
                    "You must be logged in."
            );
        }

        if (!states.isIdle(username)) {

            return queueResponse(
                    MessageType.MATCHMAKING_QUEUE_RESPONSE,
                    message.getRequestId(),
                    false,
                    "You are already busy.",
                    states.get(username)
                            == MatchmakingState.QUEUED
            );
        }


        if (randomQueue.contains(username)) {

            states.set(
                    username,
                    MatchmakingState.QUEUED
            );

            return queueResponse(
                    MessageType.MATCHMAKING_QUEUE_RESPONSE,
                    message.getRequestId(),
                    false,
                    "You are already waiting in the queue.",
                    true
            );
        }


        String opponent;

        while (true) {

            opponent =
                    randomQueue.dequeue();


            if (opponent == null) {
                break;
            }


            if (opponent.equalsIgnoreCase(username)) {

                states.clear(opponent);
                continue;
            }

            if (!connectionRegistry.isOnline(opponent)) {

                states.clear(opponent);
                continue;
            }


            if (states.get(opponent)
                    != MatchmakingState.QUEUED) {

                continue;
            }


            break;
        }

        if (opponent == null) {

            randomQueue.enqueue(username);

            states.set(
                    username,
                    MatchmakingState.QUEUED
            );


            return queueResponse(
                    MessageType.MATCHMAKING_QUEUE_RESPONSE,
                    message.getRequestId(),
                    true,
                    "Waiting for an opponent...",
                    true
            );
        }

        states.set(
                username,
                MatchmakingState.IN_MATCH
        );

        states.set(
                opponent,
                MatchmakingState.IN_MATCH
        );

        boolean created =
                createTemporaryMatch(
                        username,
                        opponent
                );


        if (!created) {

            states.clear(username);
            states.clear(opponent);

            return queueResponse(
                    MessageType.MATCHMAKING_QUEUE_RESPONSE,
                    message.getRequestId(),
                    false,
                    "Could not start the match.",
                    false
            );
        }


        return queueResponse(
                MessageType.MATCHMAKING_QUEUE_RESPONSE,
                message.getRequestId(),
                true,
                "Opponent found.",
                false
        );
    }


    public synchronized NetworkMessage handleQueueLeave(
            ClientConnection connection,
            NetworkMessage message
    ) {

        String username =
                authenticatedUsername(connection);


        if (username == null) {

            return NetworkMessage.error(
                    message.getRequestId(),
                    "You must be logged in."
            );
        }


        if (states.get(username)
                != MatchmakingState.QUEUED) {

            return queueResponse(
                    MessageType.MATCHMAKING_QUEUE_LEAVE_RESPONSE,
                    message.getRequestId(),
                    false,
                    "You are not in the matchmaking queue.",
                    false
            );
        }


        randomQueue.remove(username);
        states.clear(username);


        return queueResponse(
                MessageType.MATCHMAKING_QUEUE_LEAVE_RESPONSE,
                message.getRequestId(),
                true,
                "You left the matchmaking queue.",
                false
        );
    }

    public synchronized NetworkMessage handleInviteRequest(
            ClientConnection connection,
            NetworkMessage message
    ) {

        String challenger =
                authenticatedUsername(connection);


        if (challenger == null) {

            return NetworkMessage.error(
                    message.getRequestId(),
                    "You must be logged in."
            );
        }


        if (!states.isIdle(challenger)) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "You are already busy."
            );
        }


        if (message.getPayload() == null) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "Target username is required."
            );
        }


        InviteRequest request;


        try {

            request =
                    codec.decodePayload(
                            message.getPayload(),
                            InviteRequest.class
                    );

        } catch (JsonProcessingException exception) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "Invalid challenge request."
            );
        }


        if (request == null
                || request.targetUsername() == null
                || request.targetUsername().isBlank()) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "Target username is required."
            );
        }


        String target =
                request.targetUsername().trim();


        if (challenger.equalsIgnoreCase(target)) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "You cannot challenge yourself."
            );
        }

        if (!connectionRegistry.isOnline(target)) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "User is offline or does not exist."
            );
        }


        if (!states.isIdle(target)) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "Target player is busy."
            );
        }


        boolean created =
                inviteManager.createInvite(
                        challenger,
                        target
                );


        if (!created) {

            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "One of the players already has a pending challenge."
            );
        }


        states.set(
                challenger,
                MatchmakingState.INVITING
        );

        states.set(
                target,
                MatchmakingState.INVITED
        );


        ClientConnection targetConnection =
                connectionRegistry.getConnection(
                        target
                );


        if (targetConnection == null) {

            inviteManager.removeUser(challenger);

            states.clear(challenger);
            states.clear(target);


            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "Target player disconnected."
            );
        }


        boolean delivered =
                sendPush(
                        targetConnection,
                        MessageType.MATCHMAKING_INVITE_RECEIVED,
                        new InviteReceived(
                                challenger
                        )
                );


        if (!delivered) {

            inviteManager.removeUser(challenger);

            states.clear(challenger);
            states.clear(target);


            return inviteResponse(
                    message.getRequestId(),
                    false,
                    "Could not deliver the challenge."
            );
        }


        return inviteResponse(
                message.getRequestId(),
                true,
                "Challenge sent to " + target + "."
        );
    }


    public synchronized NetworkMessage handleInviteDecision(
            ClientConnection connection,
            NetworkMessage message
    ) {

        String target =
                authenticatedUsername(connection);


        if (target == null) {

            return NetworkMessage.error(
                    message.getRequestId(),
                    "You must be logged in."
            );
        }


        if (message.getPayload() == null) {

            return inviteDecisionResponse(
                    message.getRequestId(),
                    false,
                    "Invite decision payload is required."
            );
        }


        InviteDecision decision;


        try {

            decision =
                    codec.decodePayload(
                            message.getPayload(),
                            InviteDecision.class
                    );

        } catch (JsonProcessingException exception) {

            return inviteDecisionResponse(
                    message.getRequestId(),
                    false,
                    "Invalid invite decision."
            );
        }


        if (decision == null
                || decision.challengerUsername() == null
                || decision.challengerUsername().isBlank()) {

            return inviteDecisionResponse(
                    message.getRequestId(),
                    false,
                    "Invalid challenger."
            );
        }


        String challenger =
                inviteManager.getChallenger(
                        target
                );

        if (challenger == null
                || !challenger.equals(
                decision.challengerUsername()
        )) {

            return inviteDecisionResponse(
                    message.getRequestId(),
                    false,
                    "This challenge is no longer active."
            );
        }


        inviteManager.removeUser(target);


        if (!decision.accepted()) {

            states.clear(target);
            states.clear(challenger);


            ClientConnection challengerConnection =
                    connectionRegistry.getConnection(
                            challenger
                    );


            if (challengerConnection != null) {

                sendTextPush(
                        challengerConnection,
                        MessageType.MATCHMAKING_INVITE_REJECTED,
                        "Challenge was rejected."
                );
            }


            return inviteDecisionResponse(
                    message.getRequestId(),
                    true,
                    "Challenge rejected."
            );
        }

        if (!connectionRegistry.isOnline(challenger)) {

            states.clear(target);
            states.clear(challenger);


            return inviteDecisionResponse(
                    message.getRequestId(),
                    false,
                    "Challenger is no longer online."
            );
        }


        states.set(
                challenger,
                MatchmakingState.IN_MATCH
        );

        states.set(
                target,
                MatchmakingState.IN_MATCH
        );


        boolean created =
                createTemporaryMatch(
                        challenger,
                        target
                );


        if (!created) {

            states.clear(challenger);
            states.clear(target);


            return inviteDecisionResponse(
                    message.getRequestId(),
                    false,
                    "Could not start the match."
            );
        }


        return inviteDecisionResponse(
                message.getRequestId(),
                true,
                "Challenge accepted."
        );
    }

    public synchronized void handleDisconnect(
            String username
    ) {

        if (username == null
                || username.isBlank()) {

            return;
        }


        MatchmakingState state =
                states.get(username);



        if (state == MatchmakingState.QUEUED) {

            randomQueue.remove(username);
            states.clear(username);

            return;
        }


        if (state == MatchmakingState.INVITING) {

            String target =
                    inviteManager.getTarget(
                            username
                    );


            inviteManager.removeUser(
                    username
            );

            states.clear(username);


            if (target != null) {

                states.clear(target);


                ClientConnection targetConnection =
                        connectionRegistry
                                .getConnection(
                                        target
                                );


                if (targetConnection != null) {

                    sendTextPush(
                            targetConnection,
                            MessageType.MATCHMAKING_INVITE_REJECTED,
                            "Challenge cancelled because the player disconnected."
                    );
                }
            }


            return;
        }


        if (state == MatchmakingState.INVITED) {

            String challenger =
                    inviteManager.getChallenger(
                            username
                    );


            inviteManager.removeUser(
                    username
            );

            states.clear(username);


            if (challenger != null) {

                states.clear(challenger);


                ClientConnection challengerConnection =
                        connectionRegistry
                                .getConnection(
                                        challenger
                                );


                if (challengerConnection != null) {

                    sendTextPush(
                            challengerConnection,
                            MessageType.MATCHMAKING_INVITE_REJECTED,
                            "Challenge cancelled because the player disconnected."
                    );
                }
            }


            return;
        }

        if (state == MatchmakingState.IN_MATCH) {

            states.clear(username);
        }
    }


    private boolean createTemporaryMatch(
            String firstPlayer,
            String secondPlayer
    ) {

        String matchId =
                UUID.randomUUID()
                        .toString();


        boolean firstIsPlants =
                Math.random() < 0.5;


        String firstRole =
                firstIsPlants
                        ? "PLANTS"
                        : "ZOMBIES";


        String secondRole =
                firstIsPlants
                        ? "ZOMBIES"
                        : "PLANTS";


        boolean firstSent =
                sendMatchFound(
                        firstPlayer,
                        matchId,
                        secondPlayer,
                        firstRole
                );


        boolean secondSent =
                sendMatchFound(
                        secondPlayer,
                        matchId,
                        firstPlayer,
                        secondRole
                );


        return firstSent
                && secondSent;
    }


    private boolean sendMatchFound(
            String username,
            String matchId,
            String opponent,
            String role
    ) {

        ClientConnection connection =
                connectionRegistry
                        .getConnection(
                                username
                        );


        if (connection == null) {
            return false;
        }


        return sendPush(
                connection,
                MessageType.MATCHMAKING_MATCH_FOUND,
                new MatchFoundDto(
                        matchId,
                        opponent,
                        role
                )
        );
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


    private NetworkMessage queueResponse(
            MessageType responseType,
            String requestId,
            boolean success,
            String text,
            boolean waiting
    ) {

        try {

            return new NetworkMessage(
                    responseType,
                    requestId,
                    codec.encodePayload(
                            new QueueResponse(
                                    success,
                                    text,
                                    waiting
                            )
                    )
            );

        } catch (JsonProcessingException exception) {

            return NetworkMessage.error(
                    requestId,
                    "Could not create queue response."
            );
        }
    }


    private NetworkMessage inviteResponse(
            String requestId,
            boolean success,
            String text
    ) {

        try {

            return new NetworkMessage(
                    MessageType.MATCHMAKING_INVITE_RESPONSE,
                    requestId,
                    codec.encodePayload(
                            new InviteResponse(
                                    success,
                                    text
                            )
                    )
            );

        } catch (JsonProcessingException exception) {

            return NetworkMessage.error(
                    requestId,
                    "Could not create invite response."
            );
        }
    }


    private NetworkMessage inviteDecisionResponse(
            String requestId,
            boolean success,
            String text
    ) {

        try {

            return new NetworkMessage(
                    MessageType.MATCHMAKING_INVITE_DECISION_RESPONSE,
                    requestId,
                    codec.encodePayload(
                            new InviteDecisionResponse(
                                    success,
                                    text
                            )
                    )
            );

        } catch (JsonProcessingException exception) {

            return NetworkMessage.error(
                    requestId,
                    "Could not create invite decision response."
            );
        }
    }


    private boolean sendPush(
            ClientConnection connection,
            MessageType type,
            Object payload
    ) {

        if (connection == null) {
            return false;
        }


        try {

            String encodedPayload =
                    payload == null
                            ? null
                            : codec.encodePayload(
                            payload
                    );


            connection.send(
                    new NetworkMessage(
                            type,
                            null,
                            encodedPayload
                    )
            );


            return true;

        } catch (IOException exception) {

            System.err.println(
                    "Could not send server push "
                            + type
                            + ": "
                            + exception.getMessage()
            );

            return false;
        }
    }


    private boolean sendTextPush(
            ClientConnection connection,
            MessageType type,
            String text
    ) {

        if (connection == null) {
            return false;
        }


        try {

            connection.send(
                    new NetworkMessage(
                            type,
                            null,
                            text
                    )
            );


            return true;

        } catch (IOException exception) {

            System.err.println(
                    "Could not send server push "
                            + type
                            + ": "
                            + exception.getMessage()
            );

            return false;
        }
    }
}