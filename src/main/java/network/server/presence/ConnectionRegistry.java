package network.server.presence;

import network.server.ClientConnection;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionRegistry {

    public enum PresenceState {

        IDLE,
        QUEUED,
        INVITING,
        INVITED,
        IN_MATCH

    }


    private final Map<String, Set<ClientConnection>>
            connectionsByUsername =
            new ConcurrentHashMap<>();

    private final Map<ClientConnection, String>
            usernameByConnection =
            new ConcurrentHashMap<>();

    private final Map<String, PresenceState>
            stateByUsername =
            new ConcurrentHashMap<>();


    public void register(
            String username,
            ClientConnection connection
    ) {

        Objects.requireNonNull(
                connection,
                "connection cannot be null"
        );

        String normalized =
                normalizeUsername(username);

        if (normalized == null) {

            throw new IllegalArgumentException(
                    "username cannot be blank"
            );
        }

        String previousUsername =
                usernameByConnection.put(
                        connection,
                        normalized
                );

        if (
                previousUsername != null
                        && !previousUsername.equals(
                        normalized
                )
        ) {

            removeConnectionFromUsername(
                    previousUsername,
                    connection
            );
        }

        connectionsByUsername
                .computeIfAbsent(
                        normalized,
                        ignored ->
                                ConcurrentHashMap.newKeySet()
                )
                .add(connection);

        stateByUsername.putIfAbsent(
                normalized,
                PresenceState.IDLE
        );
    }


    public String unregister(
            ClientConnection connection
    ) {

        if (connection == null) {
            return null;
        }

        String username =
                usernameByConnection.remove(
                        connection
                );

        if (username == null) {
            return null;
        }

        removeConnectionFromUsername(
                username,
                connection
        );

        if (!isOnline(username)) {

            stateByUsername.remove(
                    username
            );
        }

        return username;
    }


    public void unregister(
            String username,
            ClientConnection connection
    ) {

        if (
                username == null
                        || connection == null
        ) {

            return;
        }

        String normalized =
                normalizeUsername(username);

        if (normalized == null) {
            return;
        }

        usernameByConnection.remove(
                connection,
                normalized
        );

        removeConnectionFromUsername(
                normalized,
                connection
        );

        if (!isOnline(normalized)) {

            stateByUsername.remove(
                    normalized
            );
        }
    }


    private void removeConnectionFromUsername(
            String username,
            ClientConnection connection
    ) {

        connectionsByUsername.computeIfPresent(
                username,
                (
                        key,
                        connections
                ) -> {

                    connections.remove(
                            connection
                    );

                    if (
                            connections.isEmpty()
                    ) {

                        return null;
                    }

                    return connections;
                }
        );
    }


    public boolean isOnline(
            String username
    ) {

        String normalized =
                normalizeUsername(username);

        if (normalized == null) {
            return false;
        }

        Set<ClientConnection> connections =
                connectionsByUsername.get(
                        normalized
                );

        return connections != null
                && !connections.isEmpty();
    }


    public ClientConnection getConnection(
            String username
    ) {

        Set<ClientConnection> connections =
                getConnectionsInternal(
                        username
                );

        if (
                connections == null
                        || connections.isEmpty()
        ) {

            return null;
        }

        return connections
                .stream()
                .findFirst()
                .orElse(null);
    }


    public Set<ClientConnection> getConnections(
            String username
    ) {

        Set<ClientConnection> connections =
                getConnectionsInternal(
                        username
                );

        if (
                connections == null
                        || connections.isEmpty()
        ) {

            return Collections.emptySet();
        }

        return Set.copyOf(
                connections
        );
    }


    private Set<ClientConnection> getConnectionsInternal(
            String username
    ) {

        String normalized =
                normalizeUsername(username);

        if (normalized == null) {
            return null;
        }

        return connectionsByUsername.get(
                normalized
        );
    }


    public String getUsername(
            ClientConnection connection
    ) {

        if (connection == null) {
            return null;
        }

        return usernameByConnection.get(
                connection
        );
    }


    public Set<String> getOnlineUsernames() {

        return Set.copyOf(
                connectionsByUsername.keySet()
        );
    }


    public int getOnlineUserCount() {

        return connectionsByUsername.size();
    }


    public int getConnectionCount() {

        return usernameByConnection.size();
    }

    public PresenceState getState(
            String username
    ) {

        String normalized =
                normalizeUsername(username);

        if (normalized == null) {
            return PresenceState.IDLE;
        }

        return stateByUsername.getOrDefault(
                normalized,
                PresenceState.IDLE
        );
    }


    public boolean isIdle(
            String username
    ) {

        return getState(username)
                == PresenceState.IDLE;
    }


    public boolean isQueued(
            String username
    ) {

        return getState(username)
                == PresenceState.QUEUED;
    }


    public boolean isInviting(
            String username
    ) {

        return getState(username)
                == PresenceState.INVITING;
    }


    public boolean isInvited(
            String username
    ) {

        return getState(username)
                == PresenceState.INVITED;
    }


    public boolean isInMatch(
            String username
    ) {

        return getState(username)
                == PresenceState.IN_MATCH;
    }


    public void setState(
            String username,
            PresenceState state
    ) {

        String normalized =
                normalizeUsername(username);

        if (normalized == null) {

            throw new IllegalArgumentException(
                    "username cannot be blank"
            );
        }

        if (state == null) {

            throw new IllegalArgumentException(
                    "state cannot be null"
            );
        }

        stateByUsername.put(
                normalized,
                state
        );
    }


    public boolean transitionState(
            String username,
            PresenceState expected,
            PresenceState next
    ) {

        String normalized =
                normalizeUsername(username);

        if (
                normalized == null
                        || expected == null
                        || next == null
        ) {

            return false;
        }


        stateByUsername.putIfAbsent(
                normalized,
                PresenceState.IDLE
        );

        return stateByUsername.replace(
                normalized,
                expected,
                next
        );
    }


    public boolean tryEnterQueue(
            String username
    ) {

        return transitionState(
                username,
                PresenceState.IDLE,
                PresenceState.QUEUED
        );
    }


    public boolean tryStartInviting(
            String username
    ) {

        return transitionState(
                username,
                PresenceState.IDLE,
                PresenceState.INVITING
        );
    }


    public boolean tryMarkInvited(
            String username
    ) {

        return transitionState(
                username,
                PresenceState.IDLE,
                PresenceState.INVITED
        );
    }


    public void markInMatch(
            String username
    ) {

        setState(
                username,
                PresenceState.IN_MATCH
        );
    }


    public void markIdle(
            String username
    ) {

        String normalized =
                normalizeUsername(username);

        if (normalized == null) {
            return;
        }

        if (!isOnline(normalized)) {

            stateByUsername.remove(
                    normalized
            );

            return;
        }

        stateByUsername.put(
                normalized,
                PresenceState.IDLE
        );
    }


    public boolean canReceiveInvite(
            String username
    ) {

        return isOnline(username)
                && isIdle(username);
    }


    public boolean canJoinQueue(
            String username
    ) {

        return isOnline(username)
                && isIdle(username);
    }


    public Map<String, PresenceState>
    getPresenceStateSnapshot() {

        return Map.copyOf(
                stateByUsername
        );
    }


    public void clear() {

        connectionsByUsername.clear();

        usernameByConnection.clear();

        stateByUsername.clear();
    }


    private String normalizeUsername(
            String username
    ) {

        if (username == null) {
            return null;
        }

        String normalized =
                username.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        return normalized;
    }
}