package network.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkClient implements Closeable {
    private final NetworkJsonCodec codec = new NetworkJsonCodec();
    private final Map<String, CompletableFuture<NetworkMessage>> pendingRequests = new ConcurrentHashMap<>();
    private volatile ServerEventListener eventListener = message -> { };
    private volatile boolean running;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readerThread;

    public synchronized void connect(String host, int port) throws IOException {
        if (running) {
            throw new IllegalStateException("Network client is already connected.");
        }
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        running = true;
        startReaderThread();
    }

    private void startReaderThread() {
        readerThread = new Thread(this::readLoop, "pvz-network-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleIncomingLine(line);
            }
        } catch (IOException exception) {
            if (running) {
                failPendingRequests(exception);
            }
        } finally {
            close();
        }
    }

    private void handleIncomingLine(String line) throws JsonProcessingException {
        NetworkMessage message = codec.decode(line);
        String requestId = message.getRequestId();
        CompletableFuture<NetworkMessage> future = requestId == null
                ? null
                : pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(message);
            return;
        }
        eventListener.onServerEvent(message);
    }

    public CompletableFuture<NetworkMessage> ping(String payload) throws IOException {
        return sendRequest(NetworkMessage.ping(payload));
    }

    public CompletableFuture<NetworkMessage> sendRequest(NetworkMessage message) throws IOException {
        if (message.getRequestId() == null || message.getRequestId().isBlank()) {
            throw new IllegalArgumentException("Request messages must have a requestId.");
        }
        CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
        pendingRequests.put(message.getRequestId(), future);
        try {
            send(message);
        } catch (IOException exception) {
            pendingRequests.remove(message.getRequestId());
            future.completeExceptionally(exception);
            throw exception;
        }
        return future;
    }

    public synchronized void send(NetworkMessage message) throws IOException {
        if (!running || writer == null) {
            throw new IOException("Network client is not connected.");
        }
        writer.write(codec.encode(message));
        writer.newLine();
        writer.flush();
    }

    public void setEventListener(ServerEventListener eventListener) {
        this.eventListener = eventListener == null ? message -> { } : eventListener;
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public synchronized void close() {
        if (!running && (socket == null || socket.isClosed())) {
            return;
        }
        running = false;
        closeSocket();
        failPendingRequests(new IOException("Network connection closed."));
    }

    private void closeSocket() {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // The socket may already be closed.
        }
    }

    private void failPendingRequests(Exception exception) {
        for (CompletableFuture<NetworkMessage> future : pendingRequests.values()) {
            future.completeExceptionally(exception);
        }
        pendingRequests.clear();
    }
}
