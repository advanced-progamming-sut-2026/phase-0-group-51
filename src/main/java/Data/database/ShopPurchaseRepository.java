package Data.database;

import models.shop.Currency;
import models.shop.ShopItemType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShopPurchaseRepository {
    private static final String USER_SQL = """
            SELECT coins, gems, plant_food_num
            FROM users WHERE id = ?
            """;
    private static final String UPDATE_USER_SQL = """
            UPDATE users
            SET coins = ?, gems = ?, plant_food_num = ?
            WHERE id = ?
            """;
    private static final String SEED_SQL = """
            INSERT INTO user_plants(
                user_id, plant_id, plant_level, seed_packets
            ) VALUES (?, ?, 1, ?)
            ON CONFLICT(user_id, plant_id)
            DO UPDATE SET
                seed_packets = seed_packets + excluded.seed_packets
            """;
    private static final String LOCKED_POTS_SQL = """
            SELECT row, "column"
            FROM greenhouse_pots
            WHERE user_id = ? AND unlocked = 0
            ORDER BY row, "column"
            LIMIT ?
            """;
    private static final String UNLOCK_POT_SQL = """
            UPDATE greenhouse_pots
            SET unlocked = 1
            WHERE user_id = ? AND row = ? AND "column" = ?
              AND unlocked = 0
            """;
    private static final String OFFER_SQL = """
            SELECT plant_id, purchased
            FROM daily_offer
            WHERE user_id = ?
            """;
    private static final String UPDATE_DAILY_COINS_SQL =
            "UPDATE users SET coins = ? WHERE id = ?";
    private static final String DAILY_SEED_SQL = """
            INSERT INTO user_plants(
                user_id, plant_id, plant_level, seed_packets
            ) VALUES (?, ?, 1, 10)
            ON CONFLICT(user_id, plant_id)
            DO UPDATE SET seed_packets = seed_packets + 10
            """;
    private static final String MARK_OFFER_PURCHASED_SQL = """
            UPDATE daily_offer
            SET purchased = 1
            WHERE user_id = ? AND plant_id = ? AND purchased = 0
            """;

    public enum PurchaseStatus {
        SUCCESS,
        USER_NOT_FOUND,
        NOT_ENOUGH_COINS,
        NOT_ENOUGH_GEMS,
        MAXIMUM_POTS_REACHED,
        MAXIMUM_PLANT_FOOD_REACHED,
        NO_UNLOCKED_PLANTS,
        OFFER_NOT_FOUND,
        OFFER_ALREADY_PURCHASED,
        DATABASE_ERROR
    }

    public record PurchaseResult(
            PurchaseStatus status,
            int coins,
            int gems,
            int plantFood
    ) {
    }

    private record Wallet(int coins, int gems, int plantFood) {
    }

    private record DailyOfferState(int plantId, boolean purchased) {
    }

    public PurchaseResult purchasePermanentItem(
            int userId,
            ShopItemType itemType,
            Currency currency,
            int totalPrice,
            int purchaseCount,
            Integer selectedPlantId,
            List<Integer> randomPlantIds
    ) {


        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return purchasePermanentItemInTransaction(
                        connection,
                        userId,
                        itemType,
                        currency,
                        totalPrice,
                        purchaseCount,
                        selectedPlantId,
                        randomPlantIds
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return failure(PurchaseStatus.DATABASE_ERROR);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return failure(PurchaseStatus.DATABASE_ERROR);
        }
    }

    private PurchaseResult purchasePermanentItemInTransaction(
            Connection connection,
            int userId,
            ShopItemType itemType,
            Currency currency,
            int totalPrice,
            int purchaseCount,
            Integer selectedPlantId,
            List<Integer> randomPlantIds
    ) throws SQLException {
        Wallet wallet = readWallet(connection, userId);
        if (wallet == null) {
            return rollbackPurchase(
                    connection,
                    failure(PurchaseStatus.USER_NOT_FOUND)
            );
        }
        PurchaseResult fundsFailure = validateFunds(
                wallet,
                currency,
                totalPrice
        );
        if (fundsFailure != null) {
            return rollbackPurchase(connection, fundsFailure);
        }

        List<int[]> potsToUnlock = loadPotsToUnlock(
                connection,
                userId,
                itemType,
                purchaseCount
        );
        PurchaseResult itemFailure = validatePermanentItem(
                wallet,
                itemType,
                purchaseCount,
                randomPlantIds,
                potsToUnlock
        );
        if (itemFailure != null) {
            return rollbackPurchase(connection, itemFailure);
        }
        return completePermanentPurchase(
                connection, userId, itemType, currency, totalPrice,
                purchaseCount, selectedPlantId, randomPlantIds,
                potsToUnlock, wallet
        );
    }

    private PurchaseResult completePermanentPurchase(
            Connection connection,
            int userId,
            ShopItemType itemType,
            Currency currency,
            int totalPrice,
            int purchaseCount,
            Integer selectedPlantId,
            List<Integer> randomPlantIds,
            List<int[]> potsToUnlock,
            Wallet wallet
    ) throws SQLException {
        Wallet chargedWallet = chargeWallet(wallet, currency, totalPrice);
        Wallet updatedWallet = applyPermanentItem(
                connection,
                userId,
                itemType,
                purchaseCount,
                selectedPlantId,
                randomPlantIds,
                potsToUnlock,
                chargedWallet
        );
        saveWallet(connection, userId, updatedWallet);
        connection.commit();
        return new PurchaseResult(
                PurchaseStatus.SUCCESS,
                updatedWallet.coins(),
                updatedWallet.gems(),
                updatedWallet.plantFood()
        );
    }

    private Wallet readWallet(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                USER_SQL
        )) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                    return null;
                }
                return new Wallet(
                        resultSet.getInt("coins"),
                        resultSet.getInt("gems"),
                        resultSet.getInt("plant_food_num")
                );
                        }

                    }
                }

    private PurchaseResult validateFunds(
            Wallet wallet,
            Currency currency,
            int totalPrice
    ) {
        if (currency == Currency.COIN && wallet.coins() < totalPrice) {
            return resultWithWallet(
                    PurchaseStatus.NOT_ENOUGH_COINS,
                    wallet
                    );
                }
        if (currency == Currency.GEM && wallet.gems() < totalPrice) {
            return resultWithWallet(
                    PurchaseStatus.NOT_ENOUGH_GEMS,
                    wallet
                    );
                }
        return null;
    }

    private List<int[]> loadPotsToUnlock(
            Connection connection,
            int userId,
            ShopItemType itemType,
            int purchaseCount
    ) throws SQLException {
        if (itemType != ShopItemType.POT) {
            return List.of();
        }
        List<int[]> pots = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                LOCKED_POTS_SQL
        )) {
                        statement.setInt(1, userId);
                        statement.setInt(2, purchaseCount);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            while (resultSet.next()) {
                    pots.add(new int[]{
                                        resultSet.getInt("row"),
                                        resultSet.getInt("column")
                                });
                            }
                        }
                    }
        return pots;
    }

    private PurchaseResult validatePermanentItem(
            Wallet wallet,
            ShopItemType itemType,
            int purchaseCount,
            List<Integer> randomPlantIds,
            List<int[]> potsToUnlock
    ) {
        if (itemType == ShopItemType.POT
                && potsToUnlock.size() < purchaseCount) {
            return resultWithWallet(
                                PurchaseStatus.MAXIMUM_POTS_REACHED,
                    wallet
                        );
                    }


                if (itemType == ShopItemType.PLANT_FOOD
                && wallet.plantFood() + purchaseCount > 3) {
            return resultWithWallet(
                            PurchaseStatus.MAXIMUM_PLANT_FOOD_REACHED,
                    wallet
                    );
                }

                if (itemType == ShopItemType.SEED_PACKET_RANDOM
                        && (randomPlantIds == null
                        || randomPlantIds.size() != purchaseCount)) {
            return resultWithWallet(
                            PurchaseStatus.NO_UNLOCKED_PLANTS,
                    wallet
                    );
                }
        return null;
    }

    private Wallet chargeWallet(
            Wallet wallet,
            Currency currency,
            int totalPrice
    ) {
                if (currency == Currency.COIN) {
            return new Wallet(
                    wallet.coins() - totalPrice,
                    wallet.gems(),
                    wallet.plantFood()
            );
        }
        return new Wallet(
                wallet.coins(),
                wallet.gems() - totalPrice,
                wallet.plantFood()
        );
                }

    private Wallet applyPermanentItem(
            Connection connection,
            int userId,
            ShopItemType itemType,
            int purchaseCount,
            Integer selectedPlantId,
            List<Integer> randomPlantIds,
            List<int[]> potsToUnlock,
            Wallet wallet
    ) throws SQLException {
        return switch (itemType) {
                    case POT -> {
                unlockPots(connection, userId, potsToUnlock);
                yield wallet;
            }
            case PLANT_FOOD -> new Wallet(
                    wallet.coins(),
                    wallet.gems(),
                    wallet.plantFood() + purchaseCount
            );
            case SEED_PACKET_RANDOM -> {
                addRandomSeedPackets(
                        connection,
                        userId,
                        randomPlantIds
                );
                yield wallet;
            }
            case SEED_PACKET_SELECTED -> {
                addSelectedSeedPackets(
                        connection,
                        userId,
                        selectedPlantId,
                        purchaseCount
                );
                yield wallet;
            }
            case COIN_CONVERSION -> new Wallet(
                    wallet.coins() + purchaseCount * 500,
                    wallet.gems(),
                    wallet.plantFood()
            );
            default -> throw new SQLException(
                    "Unsupported permanent shop item."
            );
        };
    }

    private void unlockPots(
            Connection connection,
            int userId,
            List<int[]> potsToUnlock
    ) throws SQLException {
                        for (int[] coordinates : potsToUnlock) {
            try (PreparedStatement statement = connection.prepareStatement(
                    UNLOCK_POT_SQL
            )) {
                                statement.setInt(1, userId);
                                statement.setInt(2, coordinates[0]);
                                statement.setInt(3, coordinates[1]);
                                if (statement.executeUpdate() != 1) {
                                    throw new SQLException("A flower pot could not be unlocked.");
                                }
                            }
                        }
                    }

    private void addRandomSeedPackets(
            Connection connection,
            int userId,
            List<Integer> randomPlantIds
    ) throws SQLException {
                        for (Integer plantId : randomPlantIds) {
                            if (plantId == null) {
                                throw new SQLException("Random Seed Packet plant id was missing.");
                            }
            addSeedPackets(connection, userId, plantId, 5);
                        }
                    }

    private void addSelectedSeedPackets(
            Connection connection,
            int userId,
            Integer selectedPlantId,
            int purchaseCount
    ) throws SQLException {
                        if (selectedPlantId == null) {
                            throw new SQLException("Selected Seed Packet plant id was missing.");
                        }
                        addSeedPackets(
                                connection,
                                userId,
                                selectedPlantId,
                                purchaseCount * 10
                        );
                    }

    private void saveWallet(
            Connection connection,
            int userId,
            Wallet wallet
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_USER_SQL
        )) {
            statement.setInt(1, wallet.coins());
            statement.setInt(2, wallet.gems());
            statement.setInt(3, wallet.plantFood());
                    statement.setInt(4, userId);
                    if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "The updated wallet could not be saved."
                );
            }
                    }
                }

    public PurchaseResult purchaseDailyOffer(
            int userId,
            int expectedPlantId,
            int price
    ) {
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return purchaseDailyOfferInTransaction(
                        connection,
                        userId,
                        expectedPlantId,
                        price
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return failure(PurchaseStatus.DATABASE_ERROR);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return failure(PurchaseStatus.DATABASE_ERROR);
        }
    }

    private PurchaseResult purchaseDailyOfferInTransaction(
            Connection connection,
            int userId,
            int expectedPlantId,
            int price
    ) throws SQLException {
        Wallet wallet = readWallet(connection, userId);
        if (wallet == null) {
            return rollbackPurchase(
                    connection,
                    failure(PurchaseStatus.USER_NOT_FOUND)
            );
        }
        DailyOfferState offer = readDailyOffer(connection, userId);
        PurchaseResult offerFailure = validateDailyOffer(
                wallet,
                offer,
                expectedPlantId,
                price
        );
        if (offerFailure != null) {
            return rollbackPurchase(connection, offerFailure);
        }
        if (offer.plantId() != expectedPlantId) {
            throw new SQLException(
                    "The daily offer changed before purchase."
            );
        }

        int newCoins = wallet.coins() - price;
        updateDailyCoins(connection, userId, newCoins);
        addDailySeedPackets(connection, userId, offer.plantId());
        markDailyOfferPurchased(connection, userId, offer.plantId());
        connection.commit();
        return new PurchaseResult(
                PurchaseStatus.SUCCESS,
                newCoins,
                wallet.gems(),
                wallet.plantFood()
        );
                }

    private DailyOfferState readDailyOffer(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                OFFER_SQL
        )) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                    return null;
                }
                return new DailyOfferState(
                        resultSet.getInt("plant_id"),
                        resultSet.getInt("purchased") == 1
                );
            }
        }
    }

    private PurchaseResult validateDailyOffer(
            Wallet wallet,
            DailyOfferState offer,
            int expectedPlantId,
            int price
    ) {
        if (offer == null) {
            return resultWithWallet(
                                    PurchaseStatus.OFFER_NOT_FOUND,
                    wallet
                            );
                        }
        if (offer.purchased()) {
            return resultWithWallet(
                            PurchaseStatus.OFFER_ALREADY_PURCHASED,
                    wallet
                    );
                }
        if (offer.plantId() == expectedPlantId
                && wallet.coins() < price) {
            return resultWithWallet(
                            PurchaseStatus.NOT_ENOUGH_COINS,
                    wallet
                    );
                }
        return null;
    }

    private void updateDailyCoins(
            Connection connection,
            int userId,
            int newCoins
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_DAILY_COINS_SQL
        )) {
                    statement.setInt(1, newCoins);
                    statement.setInt(2, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("The daily-offer balance could not be saved.");
                    }
                }
    }

    private void addDailySeedPackets(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                DAILY_SEED_SQL
        )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    statement.executeUpdate();
                }
    }

    private void markDailyOfferPurchased(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                MARK_OFFER_PURCHASED_SQL
        )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "The daily offer could not be marked as purchased."
                );

            }

        }
    }

    private void addSeedPackets(
            Connection connection,
            int userId,
            int plantId,
            int amount
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SEED_SQL
        )) {
            statement.setInt(1, userId);
            statement.setInt(2, plantId);
            statement.setInt(3, amount);
            statement.executeUpdate();
        }
    }

    private PurchaseResult rollbackPurchase(
            Connection connection,
            PurchaseResult result
    ) throws SQLException {
        connection.rollback();
        return result;
    }

    private PurchaseResult resultWithWallet(
            PurchaseStatus status,
            Wallet wallet
    ) {
        return new PurchaseResult(
                status,
                wallet.coins(),
                wallet.gems(),
                wallet.plantFood()
        );
    }

    private PurchaseResult failure(PurchaseStatus status) {
        return new PurchaseResult(status, 0, 0, 0);
    }
}
