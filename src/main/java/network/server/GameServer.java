package network.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer implements Closeable {
    public static final int DEFAULT_PORT = 5050;

    private final int port;
    private final MessageRouter messageRouter = new MessageRouter();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private final Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public GameServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Game server listening on port " + port + ".");
        acceptClients();
    }

    private void acceptClients() throws IOException {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                registerClient(socket);
            } catch (IOException exception) {
                if (running) {
                    throw exception;
                }
            }
        }
    }

    private void registerClient(Socket socket) {
        ClientConnection[] holder = new ClientConnection[1];
        ClientConnection connection = new ClientConnection(
                socket,
                messageRouter,
                () -> removeConnection(holder[0])
        );
        holder[0] = connection;
        connections.add(connection);
        System.out.println("Client connected: " + connection.getRemoteAddress());
        clientExecutor.submit(connection);
    }

    private void removeConnection(ClientConnection connection) {
        if (connection != null && connections.remove(connection)) {
            System.out.println("Client disconnected: " + connection.getRemoteAddress());
        }
    }

    public int getConnectedClientCount() {
        return connections.size();
    }

    @Override
    public void close() {
        running = false;
        closeServerSocket();
        for (ClientConnection connection : connections) {
            connection.close();
        }
        connections.clear();
        clientExecutor.shutdownNow();
    }

    private void closeServerSocket() {
        if (serverSocket == null) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // The server may already be closed.
        }
    }
}
