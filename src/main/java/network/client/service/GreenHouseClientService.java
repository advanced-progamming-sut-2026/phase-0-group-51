package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.greenhouse.GreenHouseActionRequest;
import network.protocol.greenhouse.GreenHouseResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class GreenHouseClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public GreenHouseClientService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<GreenHouseResponse> getGreenHouse()
            throws IOException {
        return send(
                MessageType.GREENHOUSE_GET_REQUEST,
                MessageType.GREENHOUSE_GET_RESPONSE,
                null
        );
    }

    public CompletableFuture<GreenHouseResponse> plant(
            int row,
            int column
    ) throws IOException {
        return sendAction(
                MessageType.GREENHOUSE_PLANT_REQUEST,
                MessageType.GREENHOUSE_PLANT_RESPONSE,
                row,
                column
        );
    }

    public CompletableFuture<GreenHouseResponse> grow(
            int row,
            int column
    ) throws IOException {
        return sendAction(
                MessageType.GREENHOUSE_GROW_REQUEST,
                MessageType.GREENHOUSE_GROW_RESPONSE,
                row,
                column
        );
    }

    public CompletableFuture<GreenHouseResponse> collect(
            int row,
            int column
    ) throws IOException {
        return sendAction(
                MessageType.GREENHOUSE_COLLECT_REQUEST,
                MessageType.GREENHOUSE_COLLECT_RESPONSE,
                row,
                column
        );
    }

    private CompletableFuture<GreenHouseResponse> sendAction(
            MessageType requestType,
            MessageType responseType,
            int row,
            int column
    ) throws IOException {
        String payload = codec.encodePayload(
                new GreenHouseActionRequest(row, column)
        );
        return send(requestType, responseType, payload);
    }

    private CompletableFuture<GreenHouseResponse> send(
            MessageType requestType,
            MessageType responseType,
            String payload
    ) throws IOException {
        NetworkMessage message = new NetworkMessage(
                requestType,
                UUID.randomUUID().toString(),
                payload
        );

        return networkClient
                .sendRequest(message)
                .thenApply(response -> decode(response, responseType));
    }

    private GreenHouseResponse decode(
            NetworkMessage message,
            MessageType expectedType
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new GreenHouseResponse(
                    false,
                    message.getPayload(),
                    java.util.List.of(),
                    0,
                    0
            );
        }

        if (message.getType() != expectedType) {
            throw new CompletionException(
                    new IllegalStateException(
                            "Unexpected response type: " + message.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    GreenHouseResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}
