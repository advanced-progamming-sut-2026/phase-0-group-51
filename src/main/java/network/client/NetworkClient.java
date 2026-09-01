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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkClient implements Closeable {

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    private final Map<String, CompletableFuture<NetworkMessage>>
            pendingRequests =
            new ConcurrentHashMap<>();

    private volatile ServerEventListener primaryEventListener =
            message -> {
            };

    private final Set<ServerEventListener> additionalEventListeners =
            ConcurrentHashMap.newKeySet();

    private volatile boolean running;

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readerThread;


    public synchronized void connect(
            String host,
            int port
    ) throws IOException {

        if (running) {
            throw new IllegalStateException(
                    "Network client is already connected."
            );
        }

        socket =
                new Socket(
                        host,
                        port
                );

        reader =
                new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

        writer =
                new BufferedWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream(),
                                StandardCharsets.UTF_8
                        )
                );

        running = true;

        startReaderThread();
    }


    private void startReaderThread() {

        readerThread =
                new Thread(
                        this::readLoop,
                        "pvz-network-client-reader"
                );

        readerThread.setDaemon(true);

        readerThread.start();
    }


    private void readLoop() {

        try {

            String line;

            while (
                    running
                            && (line = reader.readLine()) != null
            ) {

                handleIncomingLine(line);
            }

        } catch (IOException exception) {

            if (running) {
                failPendingRequests(exception);
            }

        } catch (RuntimeException exception) {

            if (running) {

                failPendingRequests(
                        new IOException(
                                "Unexpected network reader error.",
                                exception
                        )
                );
            }

        } finally {

            close();
        }
    }


    private void handleIncomingLine(
            String line
    ) throws JsonProcessingException {

        NetworkMessage message =
                codec.decode(line);

        if (message == null) {
            return;
        }

        String requestId =
                message.getRequestId();

        CompletableFuture<NetworkMessage> future =
                requestId == null
                        ? null
                        : pendingRequests.remove(requestId);

        if (future != null) {

            future.complete(message);

            return;
        }

        dispatchServerEvent(message);
    }


    private void dispatchServerEvent(
            NetworkMessage message
    ) {

        if (message == null) {
            return;
        }

        ServerEventListener primary =
                primaryEventListener;

        if (primary != null) {

            try {

                primary.onServerEvent(message);

            } catch (RuntimeException exception) {

                System.err.println(
                        "[NetworkClient] Primary event listener failed for "
                                + message.getType()
                                + ": "
                                + exception.getMessage()
                );
            }
        }

        for (
                ServerEventListener listener
                : additionalEventListeners
        ) {

            if (listener == null || listener == primary) {
                continue;
            }

            try {

                listener.onServerEvent(message);

            } catch (RuntimeException exception) {

                System.err.println(
                        "[NetworkClient] Event listener failed for "
                                + message.getType()
                                + ": "
                                + exception.getMessage()
                );
            }
        }
    }


    public CompletableFuture<NetworkMessage> ping(
            String payload
    ) throws IOException {

        return sendRequest(
                NetworkMessage.ping(payload)
        );
    }


    public CompletableFuture<NetworkMessage> sendRequest(
            NetworkMessage message
    ) throws IOException {

        if (message == null) {

            throw new IllegalArgumentException(
                    "message cannot be null"
            );
        }

        if (
                message.getRequestId() == null
                        || message.getRequestId().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Request messages must have a requestId."
            );
        }

        if (!isConnected()) {

            throw new IOException(
                    "Network client is not connected."
            );
        }

        String requestId =
                message.getRequestId();

        CompletableFuture<NetworkMessage> future =
                new CompletableFuture<>();

        CompletableFuture<NetworkMessage> previous =
                pendingRequests.putIfAbsent(
                        requestId,
                        future
                );

        if (previous != null) {

            throw new IllegalArgumentException(
                    "Duplicate requestId: "
                            + requestId
            );
        }

        try {

            send(message);

        } catch (IOException exception) {

            pendingRequests.remove(
                    requestId,
                    future
            );

            future.completeExceptionally(
                    exception
            );

            throw exception;
        }

        return future;
    }


    public synchronized void send(
            NetworkMessage message
    ) throws IOException {

        if (message == null) {

            throw new IllegalArgumentException(
                    "message cannot be null"
            );
        }

        if (
                !running
                        || writer == null
        ) {

            throw new IOException(
                    "Network client is not connected."
            );
        }

        writer.write(
                codec.encode(message)
        );

        writer.newLine();

        writer.flush();
    }

    public void setEventListener(
            ServerEventListener eventListener
    ) {

        this.primaryEventListener =
                eventListener == null
                        ? message -> {
                }
                        : eventListener;
    }

    public void addEventListener(
            ServerEventListener eventListener
    ) {

        if (eventListener == null) {
            return;
        }

        additionalEventListeners.add(
                eventListener
        );
    }

    public void removeEventListener(
            ServerEventListener eventListener
    ) {

        if (eventListener == null) {
            return;
        }

        additionalEventListeners.remove(
                eventListener
        );

        if (
                primaryEventListener
                        == eventListener
        ) {

            primaryEventListener =
                    message -> {
                    };
        }
    }


    public void clearAdditionalEventListeners() {

        additionalEventListeners.clear();
    }


    public boolean isConnected() {

        Socket currentSocket =
                socket;

        return running
                && currentSocket != null
                && currentSocket.isConnected()
                && !currentSocket.isClosed();
    }


    @Override
    public synchronized void close() {

        boolean wasRunning =
                running;

        running = false;

        closeSocket();

        if (
                wasRunning
                        || !pendingRequests.isEmpty()
        ) {

            failPendingRequests(
                    new IOException(
                            "Network connection closed."
                    )
            );
        }
    }


    private void closeSocket() {

        Socket currentSocket =
                socket;

        if (currentSocket == null) {
            return;
        }

        try {

            currentSocket.close();

        } catch (IOException ignored) {

        }
    }


    private void failPendingRequests(
            Exception exception
    ) {

        for (
                CompletableFuture<NetworkMessage> future
                : pendingRequests.values()
        ) {

            future.completeExceptionally(
                    exception
            );
        }

        pendingRequests.clear();
    }
}