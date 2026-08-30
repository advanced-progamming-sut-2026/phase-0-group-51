package network.server.presence;

import network.server.ClientConnection;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionRegistry {
    private final Map<String, Set<ClientConnection>> connectionsByUsername = new ConcurrentHashMap<>();
    private final Map<ClientConnection, String> usernameByConnection = new ConcurrentHashMap<>();

    public void register(String username, ClientConnection connection) {
        Objects.requireNonNull(connection, "connection cannot be null");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username cannot be blank");
        }

        String normalized = username.trim();
        String previousUsername = usernameByConnection.put(connection, normalized);


        if (previousUsername != null && !previousUsername.equals(normalized)) {
            removeConnectionFromUsername(previousUsername, connection);
        }


        connectionsByUsername
                .computeIfAbsent(normalized, ignored -> ConcurrentHashMap.newKeySet()).add(connection);
    }


    public String unregister(ClientConnection connection) {

        if (connection == null) {
            return null;
        }

        String username = usernameByConnection.remove(connection);

        if (username == null) {
            return null;
        }

        removeConnectionFromUsername(username, connection);
        return username;
    }


    public void unregister(String username, ClientConnection connection) {
        if (username == null || connection == null) {
            return;
        }
        usernameByConnection.remove(connection, username);
        removeConnectionFromUsername(username, connection);
    }


    private void removeConnectionFromUsername(
            String username,
            ClientConnection connection
    ) {

        connectionsByUsername.computeIfPresent(
                username,
                (key, connections) -> {
                    connections.remove(connection);
                    if (connections.isEmpty()) {
                        return null;
                    }
                    return connections;
                }
        );
    }


    public boolean isOnline(String username) {

        if (username == null || username.isBlank()) {
            return false;
        }


        Set<ClientConnection> connections = connectionsByUsername.get(username.trim());
        return connections != null && !connections.isEmpty();
    }


    public ClientConnection getConnection(String username) {

        Set<ClientConnection> connections = getConnectionsInternal(username);


        if (connections == null || connections.isEmpty()) {
            return null;
        }


        return connections
                .stream()
                .findFirst()
                .orElse(null);
    }


    public Set<ClientConnection> getConnections(
            String username) {

        Set<ClientConnection> connections = getConnectionsInternal(username);


        if (connections == null || connections.isEmpty()) {
            return Collections.emptySet();
        }

        return Set.copyOf(connections);
    }


    private Set<ClientConnection> getConnectionsInternal(String username) {

        if (username == null || username.isBlank()) {
            return null;
        }


        return connectionsByUsername.get(
                username.trim()
        );
    }


    public String getUsername(ClientConnection connection) {

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


    public void clear() {

        connectionsByUsername.clear();
        usernameByConnection.clear();
    }
}
