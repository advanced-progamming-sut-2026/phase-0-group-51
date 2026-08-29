package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.plants.PlantOwnershipResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PlantOwnershipClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public PlantOwnershipClientService(
            NetworkClient networkClient
    ) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<PlantOwnershipResponse>
    getOwnership() throws IOException {
        NetworkMessage message =
                new NetworkMessage(
                        MessageType.PLANT_OWNERSHIP_GET_REQUEST,
                        UUID.randomUUID().toString(),
                        null
                );

        return networkClient
                .sendRequest(message)
                .thenApply(this::decodeResponse);
    }

    private PlantOwnershipResponse decodeResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new PlantOwnershipResponse(
                    false,
                    message.getPayload(),
                    List.of()
            );
        }

        if (message.getType()
                != MessageType.PLANT_OWNERSHIP_GET_RESPONSE) {
            throw new CompletionException(
                    new IllegalStateException(
                            "Unexpected response type: "
                                    + message.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    PlantOwnershipResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}
