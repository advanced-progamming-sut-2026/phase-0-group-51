package Data.database;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlantRepository {
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

    public static UpgradeResult tryUpgradePlant(
            int userId,
            int plantId,
            int maximumLevel,
            int coinCost,
            int seedPacketCost
    ) {
        String userSql = "SELECT coins FROM users WHERE id = ?";
        String unlockedSql = """
                SELECT 1 FROM user_unlocked_plants
                WHERE user_id = ? AND plant_id = ?
                """;
        String plantSql = """
                SELECT plant_level, seed_packets
                FROM user_plants
                WHERE user_id = ? AND plant_id = ?
                """;
        String updateCoinsSql = "UPDATE users SET coins = ? WHERE id = ?";
        String upsertPlantSql = """
                INSERT INTO user_plants (
                    user_id, plant_id, plant_level, seed_packets
                ) VALUES (?, ?, ?, ?)
                ON CONFLICT(user_id, plant_id)
                DO UPDATE SET
                    plant_level = excluded.plant_level,
                    seed_packets = excluded.seed_packets
                """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int coins;
                try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return failure(UpgradeStatus.USER_NOT_FOUND);
                        }
                        coins = resultSet.getInt("coins");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(unlockedSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return failure(UpgradeStatus.PLANT_NOT_UNLOCKED);
                        }
                    }
                }

                int currentLevel = 1;
                int seedPackets = 0;
                try (PreparedStatement statement = connection.prepareStatement(plantSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            currentLevel = resultSet.getInt("plant_level");
                            seedPackets = resultSet.getInt("seed_packets");
                        }
                    }
                }

                if (currentLevel >= maximumLevel) {
                    connection.rollback();
                    return new UpgradeResult(
                            UpgradeStatus.MAX_LEVEL,
                            currentLevel,
                            currentLevel,
                            coins,
                            seedPackets
                    );
                }
                if (coins < coinCost) {
                    connection.rollback();
                    return new UpgradeResult(
                            UpgradeStatus.NOT_ENOUGH_COINS,
                            currentLevel,
                            currentLevel,
                            coins,
                            seedPackets
                    );
                }
                if (seedPackets < seedPacketCost) {
                    connection.rollback();
                    return new UpgradeResult(
                            UpgradeStatus.NOT_ENOUGH_SEED_PACKETS,
                            currentLevel,
                            currentLevel,
                            coins,
                            seedPackets
                    );
                }

                int newLevel = currentLevel + 1;
                int remainingCoins = coins - coinCost;
                int remainingSeedPackets = seedPackets - seedPacketCost;

                try (PreparedStatement statement = connection.prepareStatement(updateCoinsSql)) {
                    statement.setInt(1, remainingCoins);
                    statement.setInt(2, userId);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(upsertPlantSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    statement.setInt(3, newLevel);
                    statement.setInt(4, remainingSeedPackets);
                    statement.executeUpdate();
                }

                connection.commit();
                return new UpgradeResult(
                        UpgradeStatus.SUCCESS,
                        currentLevel,
                        newLevel,
                        remainingCoins,
                        remainingSeedPackets
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

    private static UpgradeResult failure(UpgradeStatus status) {
        return new UpgradeResult(status, 0, 0, 0, 0);
    }
}

