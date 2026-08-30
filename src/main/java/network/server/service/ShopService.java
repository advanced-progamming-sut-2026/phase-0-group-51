package network.server.service;

import Data.database.DailyOfferRepository;
import Data.database.NewsRepository;
import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.database.ShopPurchaseRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.User;
import models.shop.DailyOffer;
import models.shop.Shop;
import models.shop.ShopItem;
import models.shop.ShopItemType;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.shop.PlantAccountRequest;
import network.protocol.shop.ShopDailyOfferDto;
import network.protocol.shop.ShopItemDto;
import network.protocol.shop.ShopPlantStateDto;
import network.protocol.shop.ShopPurchaseRequest;
import network.protocol.shop.ShopResponse;
import network.server.ClientConnection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ShopService {
    private final NetworkJsonCodec codec = new NetworkJsonCodec();
    private final Shop shop = new Shop();
    private final DailyOfferRepository dailyOfferRepository =
            new DailyOfferRepository();
    private final ShopPurchaseRepository purchaseRepository =
            new ShopPurchaseRepository();
    private final UserRepository userRepository = new UserRepository();
    private final NewsRepository newsRepository = new NewsRepository();
    private final Random random = new Random();
    private static final int BOOST_GEM_COST = 2;

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        ShopResponse response = getShop(connection);
        return encode(
                message.getRequestId(),
                MessageType.SHOP_GET_RESPONSE,
                response
        );
    }

    public NetworkMessage handlePurchase(
            ClientConnection connection,
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Shop purchase payload is required."
            );
        }

        try {
            ShopPurchaseRequest request = codec.decodePayload(
                    message.getPayload(),
                    ShopPurchaseRequest.class
            );
            ShopResponse response = purchase(connection, request);
            return encode(
                    message.getRequestId(),
                    MessageType.SHOP_PURCHASE_RESPONSE,
                    response
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid shop purchase payload."
            );
        }
    }

    public NetworkMessage handleDailyPurchase(
            ClientConnection connection,
            NetworkMessage message
    ) {
        ShopResponse response = buyDailyOffer(connection);
        return encode(
                message.getRequestId(),
                MessageType.SHOP_DAILY_PURCHASE_RESPONSE,
                response
        );
    }


    public NetworkMessage handleCollectionPlantPurchase(
            ClientConnection connection,
            NetworkMessage message
    ) {
        return handlePlantAction(
                connection,
                message,
                MessageType.COLLECTION_PLANT_PURCHASE_RESPONSE,
                this::purchaseCollectionPlant
        );
    }

    public NetworkMessage handlePlantUpgrade(
            ClientConnection connection,
            NetworkMessage message
    ) {
        return handlePlantAction(
                connection,
                message,
                MessageType.PLANT_UPGRADE_RESPONSE,
                this::upgradePlant
        );
    }

    public NetworkMessage handlePlantBoost(
            ClientConnection connection,
            NetworkMessage message
    ) {
        return handlePlantAction(
                connection,
                message,
                MessageType.PLANT_BOOST_RESPONSE,
                this::boostPlant
        );
    }

    public NetworkMessage handlePlantBoostConsume(
            ClientConnection connection,
            NetworkMessage message
    ) {
        return handlePlantAction(
                connection,
                message,
                MessageType.PLANT_BOOST_CONSUME_RESPONSE,
                this::consumePlantBoost
        );
    }

    public NetworkMessage handleDebugPlantUnlock(
            ClientConnection connection,
            NetworkMessage message
    ) {
        return handlePlantAction(
                connection,
                message,
                MessageType.PLANT_DEBUG_UNLOCK_RESPONSE,
                this::debugUnlockPlant
        );
    }

    public NetworkMessage handlePlantFoodClaim(
            ClientConnection connection,
            NetworkMessage message
    ) {
        ShopResponse response = claimStoredPlantFood(connection);
        return encode(
                message.getRequestId(),
                MessageType.PLANT_FOOD_CLAIM_RESPONSE,
                response
        );
    }

    private NetworkMessage handlePlantAction(
            ClientConnection connection,
            NetworkMessage message,
            MessageType responseType,
            PlantAction action
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Plant action payload is required."
            );
        }

        try {
            PlantAccountRequest request = codec.decodePayload(
                    message.getPayload(),
                    PlantAccountRequest.class
            );
            ShopResponse response = action.apply(
                    connection,
                    request == null ? 0 : request.getPlantId()
            );
            return encode(
                    message.getRequestId(),
                    responseType,
                    response
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid plant action payload."
            );
        }
    }

    private ShopResponse getShop(ClientConnection connection) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }
        return snapshot(userId, true, "Shop loaded.");
    }

    private ShopResponse purchase(
            ClientConnection connection,
            ShopPurchaseRequest request
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }
        if (request == null || request.getCount() < 1) {
            return snapshot(userId, false, "Purchase count must be positive.");
        }

        ShopItem item = shop.getItemById(request.getItemId());
        if (item == null) {
            return snapshot(userId, false, "Item not found.");
        }

        int totalPrice;
        try {
            totalPrice = Math.multiplyExact(
                    item.getBasePrice(),
                    request.getCount()
            );
            Math.multiplyExact(
                    Math.max(1, item.getAmountPerPurchase()),
                    request.getCount()
            );
        } catch (ArithmeticException exception) {
            return snapshot(userId, false, "Purchase count is too large.");
        }

        Integer selectedPlantId = null;
        List<Integer> randomPlantIds = null;

        if (item.getType() == ShopItemType.SEED_PACKET_SELECTED) {
            selectedPlantId = request.getSelectedPlantId();
            if (!validSelectedPlant(userId, selectedPlantId)) {
                return snapshot(
                        userId,
                        false,
                        "The selected plant is not unlocked."
                );
            }
        } else if (item.getType() == ShopItemType.SEED_PACKET_RANDOM) {
            randomPlantIds = chooseRandomPlants(
                    userId,
                    request.getCount()
            );
            if (randomPlantIds == null) {
                return snapshot(
                        userId,
                        false,
                        "No unlocked plant is available for this purchase."
                );
            }
        }

        ShopPurchaseRepository.PurchaseResult result =
                purchaseRepository.purchasePermanentItem(
                        userId,
                        item.getType(),
                        item.getCurrency(),
                        totalPrice,
                        request.getCount(),
                        item.getAmountPerPurchase(),
                        selectedPlantId,
                        randomPlantIds
                );

        if (result.status() != ShopPurchaseRepository.PurchaseStatus.SUCCESS) {
            return snapshot(
                    userId,
                    false,
                    purchaseFailureMessage(result.status())
            );
        }

        return snapshot(
                userId,
                true,
                purchaseSuccessMessage(
                        item,
                        request.getCount(),
                        selectedPlantId,
                        randomPlantIds,
                        result
                )
        );
    }


    private ShopResponse debugUnlockPlant(
            ClientConnection connection,
            int plantId
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        PlantData plant = PlantRegistry.getById(plantId);
        if (plant == null) {
            return snapshot(userId, false, "Plant not found.");
        }

        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(userId);
        if (unlocked.contains(plantId)) {
            return snapshot(
                    userId,
                    true,
                    plant.name() + " is already unlocked for testing."
            );
        }

        PlantRepository.unlockPlant(userId, plantId);

        if (!PlantRepository.loadUnlockedPlants(userId).contains(plantId)) {
            return snapshot(
                    userId,
                    false,
                    "Testing unlock could not be saved."
            );
        }

        newsRepository.createNewsForUser(
                userId,
                "Testing unlock: " + plant.name() + "."
        );

        return snapshot(
                userId,
                true,
                "CHEAT: " + plant.name() + " was unlocked for testing."
        );
    }

    private ShopResponse purchaseCollectionPlant(
            ClientConnection connection,
            int plantId
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        PlantData plant = PlantRegistry.getById(plantId);
        if (plant == null) {
            return snapshot(userId, false, "Plant not found.");
        }

        PlantRegistry.UnlockRule rule =
                PlantRegistry.getUnlockRule(plantId);
        if (!rule.isPurchasable()) {
            return snapshot(
                    userId,
                    false,
                    plant.name() + " is unlocked through Adventure."
            );
        }

        PlantRepository.PurchaseResult result =
                PlantRepository.tryPurchasePlant(
                        userId,
                        plantId,
                        rule.purchaseCost()
                );

        String message = switch (result.status()) {
            case SUCCESS -> plant.name()
                    + " was added to your collection.";
            case ALREADY_UNLOCKED -> "You already own "
                    + plant.name() + ".";
            case NOT_ENOUGH_COINS -> "You need "
                    + rule.purchaseCost()
                    + " coins to purchase "
                    + plant.name()
                    + ", but you only have "
                    + result.remainingCoins() + ".";
            case USER_NOT_FOUND -> "The logged-in user no longer exists.";
            case DATABASE_ERROR -> "The purchase could not be saved.";
        };

        if (result.status() == PlantRepository.PurchaseStatus.SUCCESS) {
            newsRepository.createNewsForUser(
                    userId,
                    "New plant unlocked: " + plant.name() + "."
            );
            return snapshot(userId, true, message);
        }

        return snapshot(userId, false, message);
    }

    private ShopResponse upgradePlant(
            ClientConnection connection,
            int plantId
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        PlantData plant = PlantRegistry.getById(plantId);
        if (plant == null) {
            return snapshot(userId, false, "Plant not found.");
        }

        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(userId);
        if (!unlocked.contains(plantId)) {
            return snapshot(
                    userId,
                    false,
                    "You have not unlocked " + plant.name() + " yet."
            );
        }

        int currentLevel = PlantRepository.loadPlantLevels(userId)
                .getOrDefault(plantId, 1);
        int maximumLevel = plant.upgrades() == null
                ? 1
                : plant.upgrades().size() + 1;

        if (currentLevel >= maximumLevel) {
            return snapshot(
                    userId,
                    false,
                    plant.name() + " is already at maximum level "
                            + maximumLevel + "."
            );
        }

        int targetLevel = currentLevel + 1;
        int coinCost = coinCostForLevel(targetLevel);
        int packetCost = seedPacketCostForLevel(targetLevel);

        PlantRepository.UpgradeResult result =
                PlantRepository.tryUpgradePlant(
                        userId,
                        plantId,
                        maximumLevel,
                        coinCost,
                        packetCost
                );

        String message = switch (result.status()) {
            case SUCCESS -> plant.name()
                    + " upgraded from level "
                    + result.oldLevel()
                    + " to level "
                    + result.newLevel()
                    + ". Cost: "
                    + coinCost
                    + " coins and "
                    + packetCost
                    + " seed packets.";
            case NOT_ENOUGH_COINS -> "Not enough coins to upgrade "
                    + plant.name() + ".";
            case NOT_ENOUGH_SEED_PACKETS -> "Not enough seed packets to upgrade "
                    + plant.name() + ".";
            case MAX_LEVEL -> plant.name() + " is already at maximum level.";
            case PLANT_NOT_UNLOCKED -> "You have not unlocked "
                    + plant.name() + " yet.";
            case USER_NOT_FOUND -> "The logged-in user no longer exists.";
            case DATABASE_ERROR -> "The plant upgrade could not be saved.";
        };

        return snapshot(
                userId,
                result.status() == PlantRepository.UpgradeStatus.SUCCESS,
                message
        );
    }

    private ShopResponse boostPlant(
            ClientConnection connection,
            int plantId
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        PlantData plant = PlantRegistry.getById(plantId);
        if (plant == null) {
            return snapshot(userId, false, "Plant not found.");
        }

        PlantBoostRepository.BoostPurchaseResult result =
                PlantBoostRepository.purchaseBoost(
                        userId,
                        plantId,
                        BOOST_GEM_COST
                );

        String message = switch (result.status()) {
            case SUCCESS -> plant.name()
                    + " received a stored boost for "
                    + BOOST_GEM_COST + " gems.";
            case USER_NOT_FOUND -> "The logged-in user no longer exists.";
            case PLANT_NOT_UNLOCKED -> "You have not unlocked "
                    + plant.name() + " yet.";
            case ALREADY_BOOSTED -> "This plant already has a stored boost.";
            case NOT_ENOUGH_GEMS -> "Not enough gems.";
            case DATABASE_ERROR -> "The plant boost could not be saved.";
        };

        return snapshot(
                userId,
                result.status()
                        == PlantBoostRepository.BoostPurchaseStatus.SUCCESS,
                message
        );
    }

    private ShopResponse consumePlantBoost(
            ClientConnection connection,
            int plantId
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        PlantData plant = PlantRegistry.getById(plantId);
        if (plant == null) {
            return snapshot(userId, false, "Plant not found.");
        }

        boolean consumed;
        try {
            consumed = PlantBoostRepository.consumeBoostIfPresent(
                    userId,
                    plantId
            );
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return snapshot(
                    userId,
                    false,
                    "The stored boost could not be consumed."
            );
        }

        if (!consumed) {
            return snapshot(
                    userId,
                    false,
                    "No stored boost is available for "
                            + plant.name() + "."
            );
        }

        return snapshot(
                userId,
                true,
                "Stored boost consumed for " + plant.name() + "."
        );
    }

    private ShopResponse claimStoredPlantFood(
            ClientConnection connection
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        int claimed = userRepository.claimStoredPlantFood(userId);
        if (claimed < 0) {
            return snapshot(
                    userId,
                    false,
                    "Stored Plant Food could not be loaded."
            );
        }

        return snapshot(
                userId,
                true,
                claimed == 0
                        ? "No purchased Plant Food was stored."
                        : "Loaded " + claimed
                        + " purchased Plant Food into this game.",
                claimed
        );
    }

    private ShopResponse buyDailyOffer(ClientConnection connection) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return failure("You must log in first.");
        }

        DailyOffer offer = dailyOfferRepository.getOrCreateDailyOffer(userId);
        if (offer == null || PlantRegistry.getById(offer.getPlantId()) == null) {
            return snapshot(userId, false, "No daily offer available.");
        }

        ShopPurchaseRepository.PurchaseResult result =
                purchaseRepository.purchaseDailyOffer(
                        userId,
                        offer.getPlantId(),
                        offer.getFinalPrice()
                );

        if (result.status() != ShopPurchaseRepository.PurchaseStatus.SUCCESS) {
            return snapshot(
                    userId,
                    false,
                    purchaseFailureMessage(result.status())
            );
        }

        PlantData plant = PlantRegistry.getById(offer.getPlantId());
        String name = plant == null ? "the offered plant" : plant.name();
        return snapshot(
                userId,
                true,
                "Purchased 10 Seed Packets for " + name + "."
        );
    }

    private ShopResponse snapshot(
            int userId,
            boolean success,
            String message
    ) {
        return snapshot(userId, success, message, 0);
    }

    private ShopResponse snapshot(
            int userId,
            boolean success,
            String message,
            int claimedPlantFood
    ) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            return failure("The logged-in user no longer exists.");
        }

        List<ShopItemDto> catalogue = new ArrayList<>();
        List<ShopItem> items = shop.getCatalogue();
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            catalogue.add(
                    new ShopItemDto(
                            i + 1,
                            item.getType(),
                            item.getName(),
                            item.getBasePrice(),
                            item.getCurrency(),
                            item.getAmountPerPurchase(),
                            item.getMaxStack(),
                            item.isRequiresPlantType()
                    )
            );
        }

        DailyOffer offer = dailyOfferRepository.getOrCreateDailyOffer(userId);
        ShopDailyOfferDto dailyOffer = offer == null
                ? null
                : new ShopDailyOfferDto(
                        offer.getPlantId(),
                        offer.getDate().toString(),
                        offer.isPurchased(),
                        offer.getBasePrice(),
                        offer.getFinalPrice()
                );

        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(userId);
        Map<Integer, Integer> levels = PlantRepository.loadPlantLevels(userId);
        Map<Integer, Integer> packets = PlantRepository.loadSeedPackets(userId);
        Set<Integer> boosted = PlantBoostRepository.loadBoostedPlantIds(userId);

        List<ShopPlantStateDto> plants = unlocked.stream()
                .filter(id -> PlantRegistry.getById(id) != null)
                .sorted(Comparator.naturalOrder())
                .map(id -> new ShopPlantStateDto(
                        id,
                        levels.getOrDefault(id, 1),
                        packets.getOrDefault(id, 0),
                        boosted.contains(id)
                ))
                .toList();

        return new ShopResponse(
                success,
                message,
                catalogue,
                dailyOffer,
                plants,
                user.getCoins(),
                user.getGems(),
                user.getPlantFoodNum(),
                claimedPlantFood
        );
    }

    private boolean validSelectedPlant(
            int userId,
            Integer plantId
    ) {
        return plantId != null
                && PlantRegistry.getById(plantId) != null
                && PlantRepository.loadUnlockedPlants(userId)
                .contains(plantId);
    }

    private List<Integer> chooseRandomPlants(
            int userId,
            int count
    ) {
        List<Integer> pool = PlantRepository.loadUnlockedPlants(userId)
                .stream()
                .filter(id -> PlantRegistry.getById(id) != null)
                .sorted()
                .toList();

        if (pool.isEmpty()) {
            return null;
        }

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(pool.get(random.nextInt(pool.size())));
        }
        return result;
    }

    private String purchaseSuccessMessage(
            ShopItem item,
            int purchaseCount,
            Integer selectedPlantId,
            List<Integer> randomPlantIds,
            ShopPurchaseRepository.PurchaseResult result
    ) {
        int units = Math.multiplyExact(
                purchaseCount,
                Math.max(1, item.getAmountPerPurchase())
        );

        return switch (item.getType()) {
            case POT -> "Purchased " + units + " Flower Pot"
                    + (units == 1 ? "" : "s")
                    + ". The new greenhouse slot is unlocked.";
            case PLANT_FOOD -> "Purchased " + units
                    + " Plant Food. It will be available when your next Adventure game starts.";
            case SEED_PACKET_SELECTED -> {
                PlantData plant = PlantRegistry.getById(selectedPlantId);
                String name = plant == null ? "Selected Plant" : plant.name();
                yield "Purchased " + units + " Seed Packets for " + name + ".";
            }
            case SEED_PACKET_RANDOM -> randomSeedMessage(
                    randomPlantIds,
                    Math.max(1, item.getAmountPerPurchase())
            );
            case COIN_CONVERSION -> "Purchased " + units
                    + " Coins. Coin balance: " + result.coins() + ".";
            default -> "Purchase completed successfully.";
        };
    }

    private String randomSeedMessage(
            List<Integer> randomPlantIds,
            int packetsPerPurchase
    ) {
        if (randomPlantIds == null || randomPlantIds.isEmpty()) {
            return "Mystery Seeds purchased successfully.";
        }
        return "Mystery Seeds purchased successfully: "
                + randomPlantIds.size()
                + " bundle(s), "
                + packetsPerPurchase
                + " packets each.";
    }

    private String purchaseFailureMessage(
            ShopPurchaseRepository.PurchaseStatus status
    ) {
        return switch (status) {
            case USER_NOT_FOUND -> "The logged-in user no longer exists.";
            case NOT_ENOUGH_COINS -> "Not enough coins.";
            case NOT_ENOUGH_GEMS -> "Not enough gems.";
            case MAXIMUM_POTS_REACHED -> "Maximum greenhouse slots reached.";
            case MAXIMUM_PLANT_FOOD_REACHED -> "Maximum Plant Food reached.";
            case NO_UNLOCKED_PLANTS -> "No unlocked plant is available for this purchase.";
            case OFFER_NOT_FOUND -> "No daily offer is currently available.";
            case OFFER_ALREADY_PURCHASED -> "Today's offer has already been purchased.";
            case DATABASE_ERROR -> "The purchase could not be saved.";
            case SUCCESS -> "";
        };
    }


    private int seedPacketCostForLevel(int targetLevel) {
        return switch (targetLevel) {
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default -> 20 * Math.max(1, targetLevel - 3);
        };
    }

    private int coinCostForLevel(int targetLevel) {
        return switch (targetLevel) {
            case 2 -> 1000;
            case 3 -> 2000;
            case 4 -> 4000;
            default -> 4000 * Math.max(1, targetLevel - 3);
        };
    }

    @FunctionalInterface
    private interface PlantAction {
        ShopResponse apply(
                ClientConnection connection,
                int plantId
        );
    }

    private Integer authenticatedUserId(ClientConnection connection) {
        if (connection == null
                || !connection.getSession().isAuthenticated()) {
            return null;
        }
        return connection.getSession().getUserId();
    }

    private ShopResponse failure(String message) {
        return new ShopResponse(
                false,
                message,
                List.of(),
                null,
                List.of(),
                0,
                0,
                0
        );
    }

    private NetworkMessage encode(
            String requestId,
            MessageType responseType,
            ShopResponse response
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
                    "Could not create shop response."
            );
        }
    }
}
