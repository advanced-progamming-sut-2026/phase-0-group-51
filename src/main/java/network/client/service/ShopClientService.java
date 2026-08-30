package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.shop.PlantAccountRequest;
import network.protocol.shop.ShopPurchaseRequest;
import network.protocol.shop.ShopResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ShopClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public ShopClientService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<ShopResponse> getShop() throws IOException {
        return send(
                MessageType.SHOP_GET_REQUEST,
                MessageType.SHOP_GET_RESPONSE,
                null
        );
    }

    public CompletableFuture<ShopResponse> purchase(
            int itemId,
            int count,
            Integer selectedPlantId
    ) throws IOException {
        try {
            return send(
                    MessageType.SHOP_PURCHASE_REQUEST,
                    MessageType.SHOP_PURCHASE_RESPONSE,
                    codec.encodePayload(
                            new ShopPurchaseRequest(
                                    itemId,
                                    count,
                                    selectedPlantId
                            )
                    )
            );
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public CompletableFuture<ShopResponse> buyDailyOffer()
            throws IOException {
        return send(
                MessageType.SHOP_DAILY_PURCHASE_REQUEST,
                MessageType.SHOP_DAILY_PURCHASE_RESPONSE,
                null
        );
    }


    public CompletableFuture<ShopResponse> purchaseCollectionPlant(
            int plantId
    ) throws IOException {
        return sendPlantAction(
                MessageType.COLLECTION_PLANT_PURCHASE_REQUEST,
                MessageType.COLLECTION_PLANT_PURCHASE_RESPONSE,
                plantId
        );
    }

    public CompletableFuture<ShopResponse> upgradePlant(
            int plantId
    ) throws IOException {
        return sendPlantAction(
                MessageType.PLANT_UPGRADE_REQUEST,
                MessageType.PLANT_UPGRADE_RESPONSE,
                plantId
        );
    }

    public CompletableFuture<ShopResponse> buyBoost(
            int plantId
    ) throws IOException {
        return sendPlantAction(
                MessageType.PLANT_BOOST_REQUEST,
                MessageType.PLANT_BOOST_RESPONSE,
                plantId
        );
    }

    public CompletableFuture<ShopResponse> consumeBoost(
            int plantId
    ) throws IOException {
        return sendPlantAction(
                MessageType.PLANT_BOOST_CONSUME_REQUEST,
                MessageType.PLANT_BOOST_CONSUME_RESPONSE,
                plantId
        );
    }

    public CompletableFuture<ShopResponse> debugUnlockPlant(
            int plantId
    ) throws IOException {
        return sendPlantAction(
                MessageType.PLANT_DEBUG_UNLOCK_REQUEST,
                MessageType.PLANT_DEBUG_UNLOCK_RESPONSE,
                plantId
        );
    }

    public CompletableFuture<ShopResponse> claimStoredPlantFood()
            throws IOException {
        return send(
                MessageType.PLANT_FOOD_CLAIM_REQUEST,
                MessageType.PLANT_FOOD_CLAIM_RESPONSE,
                null
        );
    }

    private CompletableFuture<ShopResponse> sendPlantAction(
            MessageType requestType,
            MessageType responseType,
            int plantId
    ) throws IOException {
        try {
            return send(
                    requestType,
                    responseType,
                    codec.encodePayload(
                            new PlantAccountRequest(plantId)
                    )
            );
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private CompletableFuture<ShopResponse> send(
            MessageType requestType,
            MessageType responseType,
            String payload
    ) throws IOException {
        String requestId = UUID.randomUUID().toString();
        NetworkMessage request = new NetworkMessage(
                requestType,
                requestId,
                payload
        );

        return networkClient.sendRequest(request)
                .thenApply(response -> {
                    if (response == null) {
                        throw new CompletionException(
                                new IOException("Server returned no response.")
                        );
                    }
                    if (response.getType() == MessageType.ERROR) {
                        throw new CompletionException(
                                new IOException(response.getPayload())
                        );
                    }
                    if (response.getType() != responseType) {
                        throw new CompletionException(
                                new IOException(
                                        "Unexpected response type: "
                                                + response.getType()
                                )
                        );
                    }
                    try {
                        return codec.decodePayload(
                                response.getPayload(),
                                ShopResponse.class
                        );
                    } catch (JsonProcessingException exception) {
                        throw new CompletionException(exception);
                    }
                });
    }
}
