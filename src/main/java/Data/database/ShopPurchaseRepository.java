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

    public PurchaseResult purchasePermanentItem(
            int userId,
            ShopItemType itemType,
            Currency currency,
            int totalPrice,
            int purchaseCount,
            Integer selectedPlantId,
            List<Integer> randomPlantIds
    ) {
        String userSql = """
                SELECT coins, gems, plant_food_num
                FROM users WHERE id = ?
                """;
        String updateUserSql = """
                UPDATE users
                SET coins = ?, gems = ?, plant_food_num = ?
                WHERE id = ?
                """;
        String seedSql = """
                INSERT INTO user_plants(user_id, plant_id, plant_level, seed_packets)
                VALUES (?, ?, 1, ?)
                ON CONFLICT(user_id, plant_id)
                DO UPDATE SET seed_packets = seed_packets + excluded.seed_packets
                """;
        String lockedPotsSql = """
                SELECT row, "column"
                FROM greenhouse_pots
                WHERE user_id = ? AND unlocked = 0
                ORDER BY row, "column"
                LIMIT ?
                """;
        String unlockPotSql = """
                UPDATE greenhouse_pots
                SET unlocked = 1
                WHERE user_id = ? AND row = ? AND "column" = ? AND unlocked = 0
                """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int coins;
                int gems;
                int plantFood;
                try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return failure(PurchaseStatus.USER_NOT_FOUND);
                        }
                        coins = resultSet.getInt("coins");
                        gems = resultSet.getInt("gems");
                        plantFood = resultSet.getInt("plant_food_num");
                    }
                }

                if (currency == Currency.COIN && coins < totalPrice) {
                    connection.rollback();
                    return new PurchaseResult(
                            PurchaseStatus.NOT_ENOUGH_COINS, coins, gems, plantFood
                    );
                }
                if (currency == Currency.GEM && gems < totalPrice) {
                    connection.rollback();
                    return new PurchaseResult(
                            PurchaseStatus.NOT_ENOUGH_GEMS, coins, gems, plantFood
                    );
                }

                List<int[]> potsToUnlock = List.of();
                if (itemType == ShopItemType.POT) {
                    List<int[]> mutablePots = new ArrayList<>();
                    try (PreparedStatement statement = connection.prepareStatement(lockedPotsSql)) {
                        statement.setInt(1, userId);
                        statement.setInt(2, purchaseCount);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            while (resultSet.next()) {
                                mutablePots.add(new int[]{
                                        resultSet.getInt("row"),
                                        resultSet.getInt("column")
                                });
                            }
                        }
                    }
                    if (mutablePots.size() < purchaseCount) {
                        connection.rollback();
                        return new PurchaseResult(
                                PurchaseStatus.MAXIMUM_POTS_REACHED,
                                coins,
                                gems,
                                plantFood
                        );
                    }
                    potsToUnlock = mutablePots;
                }

                if (itemType == ShopItemType.PLANT_FOOD
                        && plantFood + purchaseCount > 3) {
                    connection.rollback();
                    return new PurchaseResult(
                            PurchaseStatus.MAXIMUM_PLANT_FOOD_REACHED,
                            coins,
                            gems,
                            plantFood
                    );
                }

                if (itemType == ShopItemType.SEED_PACKET_RANDOM
                        && (randomPlantIds == null
                        || randomPlantIds.size() != purchaseCount)) {
                    connection.rollback();
                    return new PurchaseResult(
                            PurchaseStatus.NO_UNLOCKED_PLANTS,
                            coins,
                            gems,
                            plantFood
                    );
                }

                int newCoins = coins;
                int newGems = gems;
                int newPlantFood = plantFood;
                if (currency == Currency.COIN) {
                    newCoins -= totalPrice;
                } else {
                    newGems -= totalPrice;
                }

                switch (itemType) {
                    case POT -> {
                        for (int[] coordinates : potsToUnlock) {
                            try (PreparedStatement statement = connection.prepareStatement(unlockPotSql)) {
                                statement.setInt(1, userId);
                                statement.setInt(2, coordinates[0]);
                                statement.setInt(3, coordinates[1]);
                                if (statement.executeUpdate() != 1) {
                                    throw new SQLException("A flower pot could not be unlocked.");
                                }
                            }
                        }
                    }
                    case PLANT_FOOD -> newPlantFood += purchaseCount;
                    case SEED_PACKET_RANDOM -> {
                        for (Integer plantId : randomPlantIds) {
                            if (plantId == null) {
                                throw new SQLException("Random Seed Packet plant id was missing.");
                            }
                            addSeedPackets(connection, seedSql, userId, plantId, 5);
                        }
                    }
                    case SEED_PACKET_SELECTED -> {
                        if (selectedPlantId == null) {
                            throw new SQLException("Selected Seed Packet plant id was missing.");
                        }
                        addSeedPackets(
                                connection,
                                seedSql,
                                userId,
                                selectedPlantId,
                                purchaseCount * 10
                        );
                    }
                    case COIN_CONVERSION -> newCoins += purchaseCount * 500;
                    default -> throw new SQLException("Unsupported permanent shop item.");
                }

                try (PreparedStatement statement = connection.prepareStatement(updateUserSql)) {
                    statement.setInt(1, newCoins);
                    statement.setInt(2, newGems);
                    statement.setInt(3, newPlantFood);
                    statement.setInt(4, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("The updated wallet could not be saved.");
                    }
                }

                connection.commit();
                return new PurchaseResult(
                        PurchaseStatus.SUCCESS,
                        newCoins,
                        newGems,
                        newPlantFood
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

    public PurchaseResult purchaseDailyOffer(
            int userId,
            int expectedPlantId,
            int price
    ) {
        String userSql = """
                SELECT coins, gems, plant_food_num
                FROM users WHERE id = ?
                """;
        String offerSql = """
                SELECT plant_id, purchased
                FROM daily_offer
                WHERE user_id = ?
                """;
        String updateCoinsSql = "UPDATE users SET coins = ? WHERE id = ?";
        String seedSql = """
                INSERT INTO user_plants(user_id, plant_id, plant_level, seed_packets)
                VALUES (?, ?, 1, 10)
                ON CONFLICT(user_id, plant_id)
                DO UPDATE SET seed_packets = seed_packets + 10
                """;
        String purchasedSql = """
                UPDATE daily_offer
                SET purchased = 1
                WHERE user_id = ? AND plant_id = ? AND purchased = 0
                """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int coins;
                int gems;
                int plantFood;
                try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return failure(PurchaseStatus.USER_NOT_FOUND);
                        }
                        coins = resultSet.getInt("coins");
                        gems = resultSet.getInt("gems");
                        plantFood = resultSet.getInt("plant_food_num");
                    }
                }

                int plantId;
                boolean purchased;
                try (PreparedStatement statement = connection.prepareStatement(offerSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return new PurchaseResult(
                                    PurchaseStatus.OFFER_NOT_FOUND,
                                    coins,
                                    gems,
                                    plantFood
                            );
                        }
                        plantId = resultSet.getInt("plant_id");
                        purchased = resultSet.getInt("purchased") == 1;
                    }
                }

                if (purchased) {
                    connection.rollback();
                    return new PurchaseResult(
                            PurchaseStatus.OFFER_ALREADY_PURCHASED,
                            coins,
                            gems,
                            plantFood
                    );
                }
                if (plantId != expectedPlantId) {
                    throw new SQLException("The daily offer changed before purchase.");
                }
                if (coins < price) {
                    connection.rollback();
                    return new PurchaseResult(
                            PurchaseStatus.NOT_ENOUGH_COINS,
                            coins,
                            gems,
                            plantFood
                    );
                }

                int newCoins = coins - price;
                try (PreparedStatement statement = connection.prepareStatement(updateCoinsSql)) {
                    statement.setInt(1, newCoins);
                    statement.setInt(2, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("The daily-offer balance could not be saved.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(seedSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(purchasedSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("The daily offer could not be marked as purchased.");
                    }
                }

                connection.commit();
                return new PurchaseResult(
                        PurchaseStatus.SUCCESS,
                        newCoins,
                        gems,
                        plantFood
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

    private void addSeedPackets(
            Connection connection,
            String sql,
            int userId,
            int plantId,
            int amount
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, plantId);
            statement.setInt(3, amount);
            statement.executeUpdate();
        }
    }

    private PurchaseResult failure(PurchaseStatus status) {
        return new PurchaseResult(status, 0, 0, 0);
    }
}
