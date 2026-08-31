package network.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import models.minigames.iZombie.multiplayer.MatchRole;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.match.*;
import network.protocol.matchmaking.MatchFoundDto;
import network.server.ClientConnection;
import network.server.match.MatchManager;
import network.server.match.PlayerChannel;
import network.server.matchmaking.MatchmakingStateRegistry;
import network.server.presence.ConnectionRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class MatchNetworkService {
    private static final int DEFAULT_STAGE_NUMBER = 1;
    private static final long ACTION_TIMEOUT_SECONDS = 2;
    private final ConnectionRegistry connectionRegistry;
    private final MatchmakingStateRegistry matchmakingStates;
    private final MatchManager matchManager = new MatchManager();
    private final NetworkJsonCodec codec = new NetworkJsonCodec();


    private final Map<String, ActiveMatchInfo> activeMatches = new ConcurrentHashMap<>();



    private final Map<String, CompletableFuture<ActionResultDto>> pendingActions = new ConcurrentHashMap<>();

    public MatchNetworkService(
            ConnectionRegistry connectionRegistry,
            MatchmakingStateRegistry matchmakingStates
    ) {

        this.connectionRegistry =
                Objects.requireNonNull(
                        connectionRegistry,
                        "connectionRegistry cannot be null"
                );

        this.matchmakingStates =
                Objects.requireNonNull(
                        matchmakingStates,
                        "matchmakingStates cannot be null"
                );
    }

    public boolean createMatch(
            String firstUsername,
            String secondUsername
    ) {

        return createMatch(
                firstUsername,
                secondUsername,
                DEFAULT_STAGE_NUMBER
        );
    }


    public boolean createMatch(
            String firstUsername,
            String secondUsername,
            int stageNumber
    ) {

        if (firstUsername == null
                || secondUsername == null
                || firstUsername.isBlank()
                || secondUsername.isBlank()
                || firstUsername.equalsIgnoreCase(
                secondUsername
        )) {

            return false;
        }


        ClientConnection firstConnection =
                connectionRegistry.getConnection(
                        firstUsername
                );

        ClientConnection secondConnection =
                connectionRegistry.getConnection(
                        secondUsername
                );


        if (firstConnection == null
                || secondConnection == null) {

            return false;
        }


        if (matchManager.isInMatch(firstUsername)
                || matchManager.isInMatch(secondUsername)) {

            return false;
        }


        boolean firstIsPlant =
                ThreadLocalRandom
                        .current()
                        .nextBoolean();


        PlayerChannel firstChannel =
                new NetworkPlayerChannel(
                        firstUsername,
                        secondUsername,
                        firstConnection
                );


        PlayerChannel secondChannel =
                new NetworkPlayerChannel(
                        secondUsername,
                        firstUsername,
                        secondConnection
                );


        try {

            if (firstIsPlant) {

                matchManager.createMatch(
                        firstChannel,
                        secondChannel,
                        stageNumber
                );

            } else {

                matchManager.createMatch(
                        secondChannel,
                        firstChannel,
                        stageNumber
                );
            }


            return true;

        } catch (RuntimeException exception) {

            System.err.println(
                    "[MATCH] Could not create match: "
                            + exception.getMessage()
            );

            return false;
        }
    }

    public NetworkMessage handleAction(
            ClientConnection connection,
            NetworkMessage message
    ) {

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


        if (!matchManager.isInMatch(
                username
        )) {

            return actionResponse(
                    message.getRequestId(),
                    ActionResultDto.rejected(
                            null,
                            "You are not in a match."
                    )
            );
        }


        GameActionDto action;


        try {

            action =
                    codec.decodePayload(
                            message.getPayload(),
                            GameActionDto.class
                    );

        } catch (
                JsonProcessingException exception
        ) {

            return actionResponse(
                    message.getRequestId(),
                    ActionResultDto.rejected(
                            null,
                            "Invalid action payload."
                    )
            );
        }


        if (action == null
                || action.getClientActionId() == null
                || action.getClientActionId().isBlank()) {

            return actionResponse(
                    message.getRequestId(),
                    ActionResultDto.rejected(
                            null,
                            "clientActionId is required."
                    )
            );
        }


        String key =
                actionKey(
                        username,
                        action.getClientActionId()
                );


        CompletableFuture<ActionResultDto>
                future =
                new CompletableFuture<>();


        if (pendingActions.putIfAbsent(
                key,
                future
        ) != null) {

            return actionResponse(
                    message.getRequestId(),
                    ActionResultDto.rejected(
                            action.getClientActionId(),
                            "Duplicate clientActionId."
                    )
            );
        }


        try {

            matchManager.submitAction(
                    username,
                    action
            );


            ActionResultDto result =
                    future.get(
                            ACTION_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );


            return actionResponse(
                    message.getRequestId(),
                    result
            );

        } catch (TimeoutException exception) {

            return actionResponse(
                    message.getRequestId(),
                    ActionResultDto.rejected(
                            action.getClientActionId(),
                            "Timed out waiting for action result."
                    )
            );

        } catch (Exception exception) {

            return actionResponse(
                    message.getRequestId(),
                    ActionResultDto.rejected(
                            action.getClientActionId(),
                            "Could not process action."
                    )
            );

        } finally {

            pendingActions.remove(
                    key,
                    future
            );
        }
    }

    public void handleDisconnect(
            String username
    ) {

        if (username == null
                || username.isBlank()) {

            return;
        }


        matchManager.handleDisconnect(
                username
        );
    }

    public ActiveMatchInfo getActiveMatch(
            String username
    ) {

        if (username == null) {
            return null;
        }


        return activeMatches.get(
                username
        );
    }


    public boolean isInMatch(
            String username
    ) {

        return username != null
                && matchManager.isInMatch(
                username
        );
    }


    private final class NetworkPlayerChannel implements PlayerChannel {
        private final String username;
        private final String opponentUsername;
        private final ClientConnection connection;


        private NetworkPlayerChannel(
                String username,
                String opponentUsername,
                ClientConnection connection
        ) {

            this.username =
                    username;

            this.opponentUsername =
                    opponentUsername;

            this.connection =
                    connection;
        }


        @Override
        public String playerId() {

            return username;
        }


        @Override
        public void send(
                Object message
        ) {

            if (message == null) {
                return;
            }

            if (message instanceof MatchStartDto start) {

                MatchRole role;

                try {

                    role =
                            MatchRole.valueOf(
                                    start.getRole()
                            );

                } catch (Exception exception) {

                    System.err.println(
                            "[MATCH] Invalid role: "
                                    + start.getRole()
                    );

                    return;
                }


                activeMatches.put(
                        username,
                        new ActiveMatchInfo(
                                start.getMatchId(),
                                opponentUsername,
                                role
                        )
                );

                sendPush(
                        connection,
                        MessageType.MATCHMAKING_MATCH_FOUND,
                        new MatchFoundDto(
                                start.getMatchId(),
                                opponentUsername,
                                start.getRole()
                        )
                );


                sendPush(
                        connection,
                        MessageType.MATCH_START,
                        start
                );


                return;
            }


            if (message instanceof MatchSnapshot snapshot) {

                sendPush(
                        connection,
                        MessageType.MATCH_SNAPSHOT,
                        snapshot
                );

                return;
            }


            if (message instanceof MatchEndedDto ended) {

                sendPush(
                        connection,
                        MessageType.MATCH_ENDED,
                        ended
                );


                cleanupMatch(
                        ended.getMatchId()
                );


                return;
            }


            if (message instanceof ActionResultDto result) {

                completeActionResult(
                        username,
                        result
                );
            }
        }
    }

    private void completeActionResult(
            String username,
            ActionResultDto result
    ) {

        if (result == null
                || result.getClientActionId() == null) {

            return;
        }


        String key =
                actionKey(
                        username,
                        result.getClientActionId()
                );


        CompletableFuture<ActionResultDto>
                future =
                pendingActions.get(
                        key
                );


        if (future != null) {

            future.complete(
                    result
            );
        }
    }


    private String actionKey(
            String username,
            String actionId
    ) {

        return username
                + "\u0000"
                + actionId;
    }


    private void cleanupMatch(
            String matchId
    ) {

        if (matchId == null) {
            return;
        }


        List<String> players = new ArrayList<>();


        for (
                Map.Entry<String, ActiveMatchInfo>
                        entry :
                activeMatches.entrySet()
        ) {

            if (matchId.equals(
                    entry.getValue().matchId()
            )) {

                players.add(
                        entry.getKey()
                );
            }
        }


        for (String username : players) {

            activeMatches.remove(
                    username
            );

            matchmakingStates.clear(
                    username
            );


            String prefix =
                    username + "\u0000";


            pendingActions
                    .keySet()
                    .removeIf(
                            key ->
                                    key.startsWith(
                                            prefix
                                    )
                    );
        }


        System.out.println(
                "[MATCH] Cleaned match "
                        + matchId
        );
    }

    private void sendPush(
            ClientConnection connection,
            MessageType type,
            Object payload
    ) {

        if (connection == null) {
            return;
        }


        try {

            connection.send(
                    new NetworkMessage(
                            type,
                            null,
                            codec.encodePayload(
                                    payload
                            )
                    )
            );

        } catch (IOException exception) {

            System.err.println(
                    "[MATCH] Could not send "
                            + type
                            + ": "
                            + exception.getMessage()
            );
        }
    }


    private NetworkMessage actionResponse(
            String requestId,
            ActionResultDto result
    ) {

        try {

            return new NetworkMessage(
                    MessageType.MATCH_ACTION_RESPONSE,
                    requestId,
                    codec.encodePayload(
                            result
                    )
            );

        } catch (
                JsonProcessingException exception
        ) {

            return NetworkMessage.error(
                    requestId,
                    "Could not encode action result."
            );
        }
    }


    private String authenticatedUsername(
            ClientConnection connection
    ) {

        if (connection == null || connection.getSession() == null || !connection.getSession().isAuthenticated()) {
            return null;
        }


        String username =
                connection
                        .getSession()
                        .getUsername();


        if (username == null || username.isBlank()) {

            return null;
        }


        return username.trim();
    }


    public record ActiveMatchInfo(
            String matchId,
            String opponentUsername,
            MatchRole role
    ) {
    }
}
