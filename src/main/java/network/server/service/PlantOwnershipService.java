package network.server.service;

import Data.database.PlantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.plants.PlantOwnershipResponse;
import network.server.ClientConnection;

import java.util.ArrayList;
import java.util.List;

public class PlantOwnershipService {
    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        PlantOwnershipResponse response =
                getOwnership(connection);

        try {
            return new NetworkMessage(
                    MessageType.PLANT_OWNERSHIP_GET_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Could not create plant ownership response."
            );
        }
    }

    private PlantOwnershipResponse getOwnership(
            ClientConnection connection
    ) {
        if (connection == null
                || !connection.getSession()
                .isAuthenticated()) {
            return failure(
                    "You must log in first."
            );
        }

        Integer userId =
                connection.getSession()
                        .getUserId();

        if (userId == null) {
            return failure(
                    "Authenticated user id is missing."
            );
        }

        List<Integer> unlockedPlantIds =
                new ArrayList<>(
                        PlantRepository
                                .loadUnlockedPlants(userId)
                );

        unlockedPlantIds.sort(Integer::compareTo);

        return new PlantOwnershipResponse(
                true,
                "Plant ownership loaded.",
                unlockedPlantIds
        );
    }

    private PlantOwnershipResponse failure(
            String message
    ) {
        return new PlantOwnershipResponse(
                false,
                message,
                List.of()
        );
    }
}
