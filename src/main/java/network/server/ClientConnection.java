package network.server;

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

public class ClientConnection implements Runnable, Closeable {
    private final Socket socket;
    private final MessageRouter messageRouter;
    private final Runnable onClosed;
    private final NetworkJsonCodec codec = new NetworkJsonCodec();
    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean running = true;

    public ClientConnection(Socket socket, MessageRouter messageRouter, Runnable onClosed) {
        this.socket = socket;
        this.messageRouter = messageRouter;
        this.onClosed = onClosed;
    }

    @Override
    public void run() {
        try {
            openStreams();
            readMessages();
        } catch (IOException exception) {
            if (running) {
                System.err.println("Client connection error: " + exception.getMessage());
            }
        } finally {
            close();
            onClosed.run();
        }
    }

    private void openStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    private void readMessages() throws IOException {
        String line;
        while (running && (line = reader.readLine()) != null) {
            handleLine(line);
        }
    }

    private void handleLine(String line) throws IOException {
        try {
            NetworkMessage request = codec.decode(line);
            System.out.println(
                    "Received " + request.getType() + " from " + getRemoteAddress()
                            + " requestId=" + request.getRequestId()
            );
            NetworkMessage response = messageRouter.route(request);
            send(response);
        } catch (JsonProcessingException exception) {
            send(NetworkMessage.error(null, "Invalid JSON message."));
        }
    }

    public synchronized void send(NetworkMessage message) throws IOException {
        if (!running || writer == null) {
            throw new IOException("Connection is not ready for writing.");
        }
        writer.write(codec.encode(message));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing an already closed socket is harmless.
        }
    }

    public String getRemoteAddress() {
        return String.valueOf(socket.getRemoteSocketAddress());
    }
}
