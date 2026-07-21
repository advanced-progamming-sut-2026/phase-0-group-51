package Data.database;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlantRepository {
    private static final String FIND_USER_COINS_SQL =
            "SELECT coins FROM users WHERE id = ?";
    private static final String FIND_UNLOCKED_PLANT_SQL = """
            SELECT 1
            FROM user_unlocked_plants
            WHERE user_id = ? AND plant_id = ?
            """;
    private static final String UPDATE_PURCHASE_COINS_SQL = """
            UPDATE users
            SET coins = ?
            WHERE id = ?
            """;
    private static final String UNLOCK_PURCHASED_PLANT_SQL = """
            INSERT INTO user_unlocked_plants (user_id, plant_id)
            VALUES (?, ?)
            """;
    private static final String CREATE_PLANT_STATE_SQL = """
            INSERT OR IGNORE INTO user_plants (
                user_id, plant_id, plant_level, seed_packets
            ) VALUES (?, ?, 1, 0)
            """;
    private static final String FIND_PLANT_STATE_SQL = """
            SELECT plant_level, seed_packets
            FROM user_plants
            WHERE user_id = ? AND plant_id = ?
            """;
    private static final String UPDATE_UPGRADE_COINS_SQL =
            "UPDATE users SET coins = ? WHERE id = ?";
    private static final String UPSERT_PLANT_STATE_SQL = """
            INSERT INTO user_plants (
                user_id, plant_id, plant_level, seed_packets
            ) VALUES (?, ?, ?, ?)
            ON CONFLICT(user_id, plant_id)
            DO UPDATE SET
                plant_level = excluded.plant_level,
                seed_packets = excluded.seed_packets
            """;


    public enum PurchaseStatus {
        SUCCESS,
        ALREADY_UNLOCKED,
        NOT_ENOUGH_COINS,
        USER_NOT_FOUND,
        DATABASE_ERROR
    }

    public record PurchaseResult(
            PurchaseStatus status,
            int remainingCoins
    ) {
    }

   public enum UpgradeStatus {
        SUCCESS,
        USER_NOT_FOUND,
        PLANT_NOT_UNLOCKED,
        MAX_LEVEL,
        NOT_ENOUGH_COINS,
        NOT_ENOUGH_SEED_PACKETS,
        DATABASE_ERROR
    }

    public record UpgradeResult(
            UpgradeStatus status,
            int oldLevel,
            int newLevel,
            int remainingCoins,
            int remainingSeedPackets
    ) {
    }

    private record PlantUpgradeState(
            int level,
            int seedPackets
    ) {
    }

    public static Map<Integer, Integer> loadPlantLevels(int userId) {
        String sql = "SELECT plant_id, plant_level FROM user_plants WHERE user_id = ?";
        Map<Integer, Integer> map = new HashMap<>();
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                map.put(rs.getInt("plant_id"), rs.getInt("plant_level"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void savePlantLevel(int userId, int plantId, int level) {
        String sql = """
                INSERT INTO user_plants (user_id, plant_id, plant_level, seed_packets)
                VALUES (?, ?, ?, 0)
            ON CONFLICT(user_id, plant_id)
            DO UPDATE SET plant_level = excluded.plant_level
            """;
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, plantId);
            stmt.setInt(3, level);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static Set<Integer> loadUnlockedPlants(int userId) {
        String sql = "SELECT plant_id FROM user_unlocked_plants WHERE user_id = ?";
        Set<Integer> unlocked = new HashSet<>();
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    unlocked.add(rs.getInt("plant_id"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return unlocked;
    }

    public static void unlockPlant(int userId, int plantId) {
        String unlockSql = """
            INSERT OR IGNORE INTO user_unlocked_plants (user_id, plant_id)
            VALUES (?, ?)
            """;
        String stateSql = """
                INSERT OR IGNORE INTO user_plants (
                    user_id, plant_id, plant_level, seed_packets
                ) VALUES (?, ?, 1, 0)
                """;

        try (Connection conn = DataBaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement unlock = conn.prepareStatement(unlockSql);
                 PreparedStatement state = conn.prepareStatement(stateSql)) {
                unlock.setInt(1, userId);
                unlock.setInt(2, plantId);
                unlock.executeUpdate();

                state.setInt(1, userId);
                state.setInt(2, plantId);
                state.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void unlockPlants(int userId, int... plantIds) {
        for (int plantId : plantIds) {
            unlockPlant(userId, plantId);
        }
    }
    public static int getSeedPackets(int userId,int plantId){

        String sql =
                "SELECT seed_packets FROM user_plants WHERE user_id=? AND plant_id=?";

        try(Connection conn = DataBaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1,userId);
            stmt.setInt(2,plantId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                return rs.getInt("seed_packets");
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }

        return 0;
    }

    public static void addSeedPackets(int userId, int plantId, int amount) {
        String sql = """
        INSERT INTO user_plants(user_id,plant_id,plant_level,seed_packets)
        VALUES(?,?,1,?)
        ON CONFLICT(user_id,plant_id)
        DO UPDATE SET
        seed_packets = seed_packets + excluded.seed_packets
        """;

        try(Connection conn = DataBaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1,userId);
            stmt.setInt(2,plantId);
            stmt.setInt(3,amount);

            stmt.executeUpdate();

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static Map<Integer,Integer> loadSeedPackets(int userId){

        Map<Integer,Integer> map = new HashMap<>();

        String sql =
                "SELECT plant_id,seed_packets FROM user_plants WHERE user_id=?";

        try(Connection conn = DataBaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1,userId);
            try (ResultSet rs = stmt.executeQuery()) {
            while(rs.next()){

                map.put(
                        rs.getInt("plant_id"),
                        rs.getInt("seed_packets")
                );

            }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }

        return map;
    }


    public static PurchaseResult tryPurchasePlant(
            int userId,
            int plantId,
            int purchaseCost
    ) {
        if (purchaseCost < 0) {
            throw new IllegalArgumentException("Purchase cost cannot be negative.");
        }



        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                return purchasePlantInTransaction(
                        connection,
                        userId,
                        plantId,
                        purchaseCost
                );
            } catch (SQLException exception) {
                            connection.rollback();
                exception.printStackTrace();
                return purchaseFailure(PurchaseStatus.DATABASE_ERROR);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return purchaseFailure(PurchaseStatus.DATABASE_ERROR);
        }
    }

    private static PurchaseResult purchasePlantInTransaction(
            Connection connection,
            int userId,
            int plantId,
            int purchaseCost
    ) throws SQLException {
        Integer currentCoins = readUserCoins(connection, userId);
        if (currentCoins == null) {
            return rollbackPurchase(
                    connection,
                                    PurchaseStatus.USER_NOT_FOUND,
                                    0
                            );
                        }
        if (isPlantUnlocked(connection, userId, plantId)) {
            return rollbackPurchase(
                    connection,
                                    PurchaseStatus.ALREADY_UNLOCKED,
                                    currentCoins
                            );
                        }


                if (currentCoins < purchaseCost) {
            return rollbackPurchase(
                    connection,
                            PurchaseStatus.NOT_ENOUGH_COINS,
                            currentCoins
                    );
                }

                int remainingCoins = currentCoins - purchaseCost;
        updatePurchasedPlantCoins(connection, userId, remainingCoins);
        insertUnlockedPlant(connection, userId, plantId);
        insertPlantState(connection, userId, plantId);
        connection.commit();
        return new PurchaseResult(PurchaseStatus.SUCCESS, remainingCoins);
    }

    private static Integer readUserCoins(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_USER_COINS_SQL
        )) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("coins") : null;
            }
        }
    }

    private static boolean isPlantUnlocked(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_UNLOCKED_PLANT_SQL
        )) {
            statement.setInt(1, userId);
            statement.setInt(2, plantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void updatePurchasedPlantCoins(
            Connection connection,
            int userId,
            int remainingCoins
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_PURCHASE_COINS_SQL
        )) {
                    statement.setInt(1, remainingCoins);
                    statement.setInt(2, userId);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Failed to update the user's coin balance."
                        );
                    }
                }
    }

    private static void insertUnlockedPlant(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UNLOCK_PURCHASED_PLANT_SQL
        )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    statement.executeUpdate();
                }
    }

    private static void insertPlantState(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                CREATE_PLANT_STATE_SQL
        )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    statement.executeUpdate();
                }
    }

    private static PurchaseResult rollbackPurchase(
            Connection connection,
            PurchaseStatus status,
            int coins
    ) throws SQLException {
        connection.rollback();
        return new PurchaseResult(status, coins);
    }

    private static PurchaseResult purchaseFailure(PurchaseStatus status) {
        return new PurchaseResult(status, 0);
    }

    public static UpgradeResult tryUpgradePlant(
            int userId,
            int plantId,
            int maximumLevel,
            int coinCost,
            int seedPacketCost
    ) {
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return upgradePlantInTransaction(
                        connection,
                        userId,
                        plantId,
                        maximumLevel,
                        coinCost,
                        seedPacketCost
                );

            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return failure(UpgradeStatus.DATABASE_ERROR);
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
            return failure(UpgradeStatus.DATABASE_ERROR);
        }
    }

    private static UpgradeResult upgradePlantInTransaction(
            Connection connection,
            int userId,
            int plantId,
            int maximumLevel,
            int coinCost,
            int seedPacketCost
    ) throws SQLException {
        Integer coins = readUserCoins(connection, userId);
        if (coins == null) {
            return rollbackUpgrade(
                    connection,
                    failure(UpgradeStatus.USER_NOT_FOUND)
            );
        }
        if (!isPlantUnlocked(connection, userId, plantId)) {
            return rollbackUpgrade(
                    connection,
                    failure(UpgradeStatus.PLANT_NOT_UNLOCKED)
            );
        }

        PlantUpgradeState state = readPlantUpgradeState(
                connection,
                userId,
                plantId
        );
        UpgradeResult validation = validateUpgrade(
                state,
                coins,
                maximumLevel,
                coinCost,
                seedPacketCost
        );
        if (validation != null) {
            return rollbackUpgrade(connection, validation);
                        }
        return savePlantUpgrade(connection, userId, plantId, state, coins,
                coinCost, seedPacketCost);
                }

    private static PlantUpgradeState readPlantUpgradeState(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_PLANT_STATE_SQL
        )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                    return new PlantUpgradeState(1, 0);
                    }
                return new PlantUpgradeState(
                        resultSet.getInt("plant_level"),
                        resultSet.getInt("seed_packets")
                );
                        }
                    }
                }

    private static UpgradeResult validateUpgrade(
            PlantUpgradeState state,
            int coins,
            int maximumLevel,
            int coinCost,
            int seedPacketCost
    ) {
        if (state.level() >= maximumLevel) {
            return upgradeStatusResult(
                            UpgradeStatus.MAX_LEVEL,
                    state,
                    coins
                    );
                }
                if (coins < coinCost) {
            return upgradeStatusResult(
                            UpgradeStatus.NOT_ENOUGH_COINS,
                    state,
                    coins
            );
        }
        if (state.seedPackets() < seedPacketCost) {
            return upgradeStatusResult(
                    UpgradeStatus.NOT_ENOUGH_SEED_PACKETS,
                    state,
                    coins
                    );
                }
        return null;
    }

    private static UpgradeResult upgradeStatusResult(
            UpgradeStatus status,
            PlantUpgradeState state,
            int coins
    ) {
                    return new UpgradeResult(
                status,
                state.level(),
                state.level(),
                            coins,
                state.seedPackets()
                    );
                }

    private static UpgradeResult savePlantUpgrade(
            Connection connection,
            int userId,
            int plantId,
            PlantUpgradeState state,
            int coins,
            int coinCost,
            int seedPacketCost
    ) throws SQLException {
        int newLevel = state.level() + 1;
                int remainingCoins = coins - coinCost;
        int remainingPackets = state.seedPackets() - seedPacketCost;
        updateUpgradeCoins(connection, userId, remainingCoins);
        upsertPlantUpgrade(
                connection,
                userId,
                plantId,
                newLevel,
                remainingPackets
        );
        connection.commit();
        return new UpgradeResult(
                UpgradeStatus.SUCCESS,
                state.level(),
                newLevel,
                remainingCoins,
                remainingPackets
        );
    }

    private static void updateUpgradeCoins(
            Connection connection,
            int userId,
            int remainingCoins
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_UPGRADE_COINS_SQL
        )) {
                    statement.setInt(1, remainingCoins);
                    statement.setInt(2, userId);
                    statement.executeUpdate();
                }
    }

    private static void upsertPlantUpgrade(
            Connection connection,
            int userId,
            int plantId,
            int newLevel,
            int remainingPackets
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPSERT_PLANT_STATE_SQL
        )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    statement.setInt(3, newLevel);
            statement.setInt(4, remainingPackets);
                    statement.executeUpdate();
                }
    }

    private static UpgradeResult rollbackUpgrade(
            Connection connection,
            UpgradeResult result
    ) throws SQLException {
                connection.rollback();
        return result;
    }

    private static UpgradeResult failure(UpgradeStatus status) {
        return new UpgradeResult(status, 0, 0, 0, 0);
    }
}

