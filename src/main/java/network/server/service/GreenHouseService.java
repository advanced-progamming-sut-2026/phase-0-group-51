package network.server.service;

import Data.database.GreenHouseRepository;
import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;
import models.greenHouse.GreenHousePlantHelper;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.greenhouse.GreenHouseActionRequest;
import network.protocol.greenhouse.GreenHousePotDto;
import network.protocol.greenhouse.GreenHouseResponse;
import network.server.ClientConnection;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GreenHouseService {
    private final NetworkJsonCodec codec = new NetworkJsonCodec();
    private final UserRepository userRepository = new UserRepository();
    private final Random random = new Random();

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        GreenHouseResponse response = getGreenHouse(connection);
        return encode(
                message.getRequestId(),
                MessageType.GREENHOUSE_GET_RESPONSE,
                response
        );
    }

    public NetworkMessage handlePlant(
            ClientConnection connection,
            NetworkMessage message
    ) {
        GreenHouseActionRequest request = decodeAction(message);
        if (request == null) {
            return invalidPayload(message, "Invalid greenhouse plant payload.");
        }

        GreenHouseResponse response = plant(connection, request);
        return encode(
                message.getRequestId(),
                MessageType.GREENHOUSE_PLANT_RESPONSE,
                response
        );
    }

    public NetworkMessage handleGrow(
            ClientConnection connection,
            NetworkMessage message
    ) {
        GreenHouseActionRequest request = decodeAction(message);
        if (request == null) {
            return invalidPayload(message, "Invalid greenhouse grow payload.");
        }

        GreenHouseResponse response = grow(connection, request);
        return encode(
                message.getRequestId(),
                MessageType.GREENHOUSE_GROW_RESPONSE,
                response
        );
    }

    public NetworkMessage handleCollect(
            ClientConnection connection,
            NetworkMessage message
    ) {
        GreenHouseActionRequest request = decodeAction(message);
        if (request == null) {
            return invalidPayload(message, "Invalid greenhouse collect payload.");
        }

        GreenHouseResponse response = collect(connection, request);
        return encode(
                message.getRequestId(),
                MessageType.GREENHOUSE_COLLECT_RESPONSE,
                response
        );
    }

    private GreenHouseResponse getGreenHouse(
            ClientConnection connection
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        try {
            return snapshot(userId, true, "Greenhouse loaded.");
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return failure("The greenhouse could not be loaded.");
        }
    }

    private GreenHouseResponse plant(
            ClientConnection connection,
            GreenHouseActionRequest request
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }
        if (!validCoordinates(request)) {
            return failure("Invalid greenhouse coordinates.");
        }

        try {
            GreenHouse greenHouse = GreenHouseRepository.load(userId);
            FlowerPot pot = greenHouse.getPot(
                    request.getRow(),
                    request.getColumn()
            );

            if (!pot.isUnlocked()) {
                return snapshot(userId, false, "This flower pot is locked.");
            }
            if (!pot.isEmpty()) {
                return snapshot(
                        userId,
                        false,
                        "This flower pot already contains a plant."
                );
            }

            int plantId = chooseRandomPlant(userId);
            LocalDateTime plantedAt = LocalDateTime.now();

            boolean saved = GreenHouseRepository.plantPot(
                    userId,
                    request.getRow(),
                    request.getColumn(),
                    plantId,
                    plantedAt
            );

            if (!saved) {
                return snapshot(userId, false, "The plant could not be saved.");
            }

            return snapshot(userId, true, "Plant planted successfully.");
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return failure("The plant could not be saved.");
        }
    }

    private GreenHouseResponse grow(
            ClientConnection connection,
            GreenHouseActionRequest request
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }
        if (!validCoordinates(request)) {
            return failure("Invalid greenhouse coordinates.");
        }

        try {
            GreenHouse greenHouse = GreenHouseRepository.load(userId);
            FlowerPot pot = greenHouse.getPot(
                    request.getRow(),
                    request.getColumn()
            );

            if (!pot.isUnlocked()) {
                return snapshot(userId, false, "This flower pot is locked.");
            }
            if (pot.isEmpty()) {
                return snapshot(
                        userId,
                        false,
                        "This flower pot does not contain a plant."
                );
            }
            if (pot.isReady()) {
                return snapshot(
                        userId,
                        false,
                        "This plant is already ready to collect."
                );
            }

            int gemCost = Math.toIntExact(pot.getCeilRemainingHours());
            int growthHours = pot.getPlantId() == FlowerPot.MARIGOLD_ID
                    ? 2
                    : 8;
            LocalDateTime readyPlantedAt =
                    LocalDateTime.now().minusHours(growthHours);

            GreenHouseRepository.GrowResult result =
                    GreenHouseRepository.growPotInstantly(
                            userId,
                            request.getRow(),
                            request.getColumn(),
                            gemCost,
                            readyPlantedAt
                    );

            return switch (result.status()) {
                case SUCCESS -> snapshot(
                        userId,
                        true,
                        "The plant grew instantly for "
                                + gemCost
                                + " gems."
                );
                case NOT_ENOUGH_GEMS -> snapshot(
                        userId,
                        false,
                        "You do not have enough gems."
                );
                case POT_LOCKED -> snapshot(
                        userId,
                        false,
                        "This flower pot is locked."
                );
                case POT_EMPTY -> snapshot(
                        userId,
                        false,
                        "This flower pot does not contain a plant."
                );
                case POT_NOT_FOUND, DATABASE_ERROR -> snapshot(
                        userId,
                        false,
                        "The instant growth could not be saved."
                );
            };
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return failure("The instant growth could not be saved.");
        }
    }

    private GreenHouseResponse collect(
            ClientConnection connection,
            GreenHouseActionRequest request
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }
        if (!validCoordinates(request)) {
            return failure("Invalid greenhouse coordinates.");
        }

        try {
            GreenHouse greenHouse = GreenHouseRepository.load(userId);
            FlowerPot pot = greenHouse.getPot(
                    request.getRow(),
                    request.getColumn()
            );

            if (!pot.isUnlocked()) {
                return snapshot(userId, false, "This flower pot is locked.");
            }
            if (pot.isEmpty()) {
                return snapshot(userId, false, "This flower pot is empty.");
            }
            if (!pot.isReady()) {
                return snapshot(
                        userId,
                        false,
                        "This plant is not ready for collection."
                );
            }

            int plantId = pot.getPlantId();
            String plantName = pot.getPlantName();

            GreenHouseRepository.CollectResult result =
                    GreenHouseRepository.collectPot(
                            userId,
                            request.getRow(),
                            request.getColumn(),
                            plantId
                    );

            if (result.status()
                    != GreenHouseRepository.CollectStatus.SUCCESS) {
                return snapshot(
                        userId,
                        false,
                        collectFailureMessage(result.status())
                );
            }

            String message = plantId == FlowerPot.MARIGOLD_ID
                    ? "Marigold harvested! Reward: +500 coins"
                    : plantName
                    + " harvested! Reward: stored "
                    + plantName
                    + " boost";

            return snapshot(userId, true, message);
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return failure("The plant collection could not be saved.");
        }
    }

    private GreenHouseResponse snapshot(
            int userId,
            boolean success,
            String message
    ) {
        GreenHouse greenHouse = GreenHouseRepository.load(userId);
        UserRepository.CurrencyBalance balance =
                userRepository.getCurrencyBalance(userId);

        List<GreenHousePotDto> pots = new ArrayList<>();
        for (int row = 1; row <= GreenHouse.ROWS; row++) {
            for (int column = 1; column <= GreenHouse.COLUMNS; column++) {
                FlowerPot pot = greenHouse.getPot(row, column);
                pots.add(
                        new GreenHousePotDto(
                                row,
                                column,
                                pot.isUnlocked(),
                                pot.getPlantId(),
                                pot.getPlantedAt() == null
                                        ? null
                                        : pot.getPlantedAt().toString()
                        )
                );
            }
        }

        int coins = balance == null ? 0 : balance.coins();
        int gems = balance == null ? 0 : balance.gems();

        return new GreenHouseResponse(
                success,
                message,
                pots,
                coins,
                gems
        );
    }

    private int chooseRandomPlant(int userId) {
        if (random.nextBoolean()) {
            return FlowerPot.MARIGOLD_ID;
        }

        Set<Integer> unlocked =
                PlantRepository.loadUnlockedPlants(userId);
        List<Integer> candidates = new ArrayList<>();

        for (Integer plantId : unlocked) {
            PlantData plant = PlantRegistry.get(plantId);
            if (plant != null
                    && GreenHousePlantHelper.canAppearInGreenHouse(plant)) {
                candidates.add(plantId);
            }
        }

        if (candidates.isEmpty()) {
            return FlowerPot.MARIGOLD_ID;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private GreenHouseActionRequest decodeAction(NetworkMessage message) {
        if (message.getPayload() == null) {
            return null;
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    GreenHouseActionRequest.class
            );
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private boolean validCoordinates(GreenHouseActionRequest request) {
        return request != null
                && request.getRow() >= 1
                && request.getRow() <= GreenHouse.ROWS
                && request.getColumn() >= 1
                && request.getColumn() <= GreenHouse.COLUMNS;
    }

    private String collectFailureMessage(
            GreenHouseRepository.CollectStatus status
    ) {
        return switch (status) {
            case POT_LOCKED -> "This flower pot is locked.";
            case POT_EMPTY -> "This flower pot is empty.";
            case POT_NOT_FOUND, DATABASE_ERROR ->
                    "The plant collection could not be saved.";
            case SUCCESS -> "";
        };
    }

    private Integer authenticatedUserId(ClientConnection connection) {
        if (connection == null
                || !connection.getSession().isAuthenticated()) {
            return null;
        }
        return connection.getSession().getUserId();
    }

    private GreenHouseResponse failure(String message) {
        return new GreenHouseResponse(
                false,
                message,
                List.of(),
                0,
                0
        );
    }

    private NetworkMessage invalidPayload(
            NetworkMessage message,
            String error
    ) {
        return NetworkMessage.error(message.getRequestId(), error);
    }

    private NetworkMessage encode(
            String requestId,
            MessageType responseType,
            GreenHouseResponse response
    ) {
        try {
            return new NetworkMessage(
                    responseType,
                    requestId,
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    requestId,
                    "Could not create greenhouse response."
            );
        }
    }
}
