package Data.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

public class PlantBoostRepository {
    public static boolean hasBoost(int userId, int plantId) {
        String sql = """
                SELECT 1
                FROM plant_boosts
                WHERE user_id = ?
                  AND plant_id = ?
                """;

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, plantId);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static Set<Integer> loadBoostedPlantIds(int userId) {
        String sql = """
                SELECT plant_id
                FROM plant_boosts
                WHERE user_id = ?
                ORDER BY plant_id
                """;

        Set<Integer> result = new LinkedHashSet<>();
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("plant_id"));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public enum BoostPurchaseStatus {
        SUCCESS,
        USER_NOT_FOUND,
        PLANT_NOT_UNLOCKED,
        ALREADY_BOOSTED,
        NOT_ENOUGH_GEMS,
        DATABASE_ERROR
    }

    public record BoostPurchaseResult(
            BoostPurchaseStatus status,
            int remainingGems
    ) {
    }

    public static BoostPurchaseResult purchaseBoost(
            int userId,
            int plantId,
            int gemCost
    ) {
        if (gemCost < 0) {
            throw new IllegalArgumentException(
                    "Boost gem cost cannot be negative."
            );
        }

        String userSql = "SELECT gems FROM users WHERE id = ?";
        String unlockedSql = """
                SELECT 1
                FROM user_unlocked_plants
                WHERE user_id = ? AND plant_id = ?
                """;
        String boostSql = """
                SELECT 1
                FROM plant_boosts
                WHERE user_id = ? AND plant_id = ?
                """;
        String updateGemsSql = "UPDATE users SET gems = ? WHERE id = ?";
        String insertBoostSql = """
                INSERT INTO plant_boosts(user_id, plant_id)
                VALUES (?, ?)
                """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer gems;
                try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return new BoostPurchaseResult(
                                    BoostPurchaseStatus.USER_NOT_FOUND,
                                    0
                            );
                        }
                        gems = resultSet.getInt("gems");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(unlockedSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return new BoostPurchaseResult(
                                    BoostPurchaseStatus.PLANT_NOT_UNLOCKED,
                                    gems
                            );
                        }
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(boostSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            connection.rollback();
                            return new BoostPurchaseResult(
                                    BoostPurchaseStatus.ALREADY_BOOSTED,
                                    gems
                            );
                        }
                    }
                }

                if (gems < gemCost) {
                    connection.rollback();
                    return new BoostPurchaseResult(
                            BoostPurchaseStatus.NOT_ENOUGH_GEMS,
                            gems
                    );
                }

                int remainingGems = gems - gemCost;
                try (PreparedStatement statement = connection.prepareStatement(updateGemsSql)) {
                    statement.setInt(1, remainingGems);
                    statement.setInt(2, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not update boost gem balance.");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(insertBoostSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, plantId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not store plant boost.");
                    }
                }

                connection.commit();
                return new BoostPurchaseResult(
                        BoostPurchaseStatus.SUCCESS,
                        remainingGems
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return new BoostPurchaseResult(
                        BoostPurchaseStatus.DATABASE_ERROR,
                        0
                );
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new BoostPurchaseResult(
                    BoostPurchaseStatus.DATABASE_ERROR,
                    0
            );
        }
    }

    public static boolean consumeBoostIfPresent(
            int userId,
            int plantId
    ) {
        String sql = """
                DELETE FROM plant_boosts
                WHERE user_id = ?
                  AND plant_id = ?
                """;

        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, plantId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void addBoost(int userId, int plantId) {
        String sql = """
                INSERT OR IGNORE INTO plant_boosts(user_id, plant_id)
                VALUES (?, ?)
                """;

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, plantId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void consumeBoost(int userId, int plantId) {
        String sql = """
                DELETE FROM plant_boosts
                WHERE user_id = ?
                  AND plant_id = ?
                """;

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, plantId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
