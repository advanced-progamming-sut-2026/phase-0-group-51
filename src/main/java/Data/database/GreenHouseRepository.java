package Data.database;

import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;

import java.sql.*;
import java.time.LocalDateTime;

public class GreenHouseRepository {
    public enum CollectStatus {
        SUCCESS,
        POT_NOT_FOUND,
        POT_LOCKED,
        POT_EMPTY,
        DATABASE_ERROR
    }

    public record CollectResult(CollectStatus status, int newCoinBalance) {
    }

    public enum GrowStatus {
        SUCCESS,
        POT_NOT_FOUND,
        POT_LOCKED,
        POT_EMPTY,
        NOT_ENOUGH_GEMS,
        DATABASE_ERROR
    }

    public record GrowResult(GrowStatus status, int newGemBalance) {
    }

    public static void createForUser(int userId) {
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertInitialPots(connection, userId);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw new IllegalStateException(
                        "Could not initialize the greenhouse.", exception
                );
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not initialize the greenhouse.", exception
            );
        }
    }

    public static void insertInitialPots(Connection connection, int userId)
            throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO greenhouse_pots
        (user_id, row, "column", unlocked, plant_id, planted_at)
                VALUES (?, ?, ?, ?, NULL, NULL)
        """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int row = 1; row <= GreenHouse.ROWS; row++) {
                for (int column = 1; column <= GreenHouse.COLUMNS; column++) {
                    statement.setInt(1, userId);
                    statement.setInt(2, row);
                    statement.setInt(3, column);
                    statement.setBoolean(4, row == 1);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    public static GreenHouse load(int userId) {
        GreenHouse greenHouse = new GreenHouse();
        String sql = """
            SELECT *
            FROM greenhouse_pots
            WHERE user_id = ?
            ORDER BY row, "column"
            """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int row = resultSet.getInt("row");
                    int column = resultSet.getInt("column");
                FlowerPot pot = greenHouse.getPot(row, column);
                    pot.setUnlocked(resultSet.getBoolean("unlocked"));
                    int plantId = resultSet.getInt("plant_id");
                    pot.setPlantId(resultSet.wasNull() ? null : plantId);
                    String plantedAt = resultSet.getString("planted_at");
                if (plantedAt != null) {
                    pot.setPlantedAt(LocalDateTime.parse(plantedAt));
                }
            }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not load the greenhouse.", exception
            );
        }
        return greenHouse;
    }

    public static boolean updatePot(int userId, FlowerPot pot) {
        String sql = """
            UPDATE greenhouse_pots
            SET
                unlocked = ?,
                plant_id = ?,
                planted_at = ?
            WHERE
                user_id = ?
                AND row = ?
                AND "column" = ?
            """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, pot.isUnlocked());
            if (pot.getPlantId() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, pot.getPlantId());
            }
            if (pot.getPlantedAt() == null) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, pot.getPlantedAt().toString());
            }
            statement.setInt(4, userId);
            statement.setInt(5, pot.getRow());
            statement.setInt(6, pot.getColumn());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean plantPot(
            int userId,
            int row,
            int column,
            int plantId,
            LocalDateTime plantedAt
    ) {
        String sql = """
            UPDATE greenhouse_pots
                SET plant_id = ?, planted_at = ?
            WHERE user_id = ?
              AND row = ?
              AND "column" = ?
                  AND unlocked = 1
                  AND plant_id IS NULL
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, plantId);
            statement.setString(2, plantedAt.toString());
            statement.setInt(3, userId);
            statement.setInt(4, row);
            statement.setInt(5, column);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static CollectResult collectPot(
            int userId,
            int row,
            int column,
            int expectedPlantId
    ) {
        String selectPotSql = """
                SELECT unlocked, plant_id
                FROM greenhouse_pots
                WHERE user_id = ? AND row = ? AND "column" = ?
                """;
        String addCoinsSql = """
                UPDATE users
                SET coins = COALESCE(coins, 0) + 500
                WHERE id = ?
                """;
        String selectCoinsSql = "SELECT coins FROM users WHERE id = ?";
        String addBoostSql = """
                INSERT OR IGNORE INTO plant_boosts(user_id, plant_id)
                VALUES (?, ?)
                """;
        String clearPotSql = """
                UPDATE greenhouse_pots
                SET plant_id = NULL, planted_at = NULL
                WHERE user_id = ? AND row = ? AND "column" = ?
                  AND plant_id = ?
                """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer storedPlantId;
                boolean unlocked;
                try (PreparedStatement statement = connection.prepareStatement(selectPotSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, row);
                    statement.setInt(3, column);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return new CollectResult(CollectStatus.POT_NOT_FOUND, 0);
                        }
                        unlocked = resultSet.getBoolean("unlocked");
                        int value = resultSet.getInt("plant_id");
                        storedPlantId = resultSet.wasNull() ? null : value;
                    }
                }

                if (!unlocked) {
                    connection.rollback();
                    return new CollectResult(CollectStatus.POT_LOCKED, 0);
                }
                if (storedPlantId == null || storedPlantId != expectedPlantId) {
                    connection.rollback();
                    return new CollectResult(CollectStatus.POT_EMPTY, 0);
                }

                int newCoins = -1;
                if (storedPlantId == FlowerPot.MARIGOLD_ID) {
                    try (PreparedStatement statement = connection.prepareStatement(addCoinsSql)) {
                        statement.setInt(1, userId);
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("User was not found while collecting Marigold.");
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement(selectCoinsSql)) {
                        statement.setInt(1, userId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                throw new SQLException("Could not read the updated coin balance.");
                            }
                            newCoins = resultSet.getInt("coins");
                        }
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement(addBoostSql)) {
                        statement.setInt(1, userId);
                        statement.setInt(2, storedPlantId);
                        statement.executeUpdate();
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(clearPotSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, row);
                    statement.setInt(3, column);
                    statement.setInt(4, storedPlantId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("The greenhouse pot changed before collection.");
                    }
                }

                connection.commit();
                return new CollectResult(CollectStatus.SUCCESS, newCoins);
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return new CollectResult(CollectStatus.DATABASE_ERROR, 0);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new CollectResult(CollectStatus.DATABASE_ERROR, 0);
        }
    }

    public static GrowResult growPotInstantly(
            int userId,
            int row,
            int column,
            int gemCost,
            LocalDateTime readyPlantedAt
    ) {
        String selectSql = """
                SELECT u.gems, p.unlocked, p.plant_id
                FROM users u
                JOIN greenhouse_pots p ON p.user_id = u.id
                WHERE u.id = ? AND p.row = ? AND p."column" = ?
                """;
        String updateUserSql = "UPDATE users SET gems = ? WHERE id = ?";
        String updatePotSql = """
                UPDATE greenhouse_pots
                SET planted_at = ?
                WHERE user_id = ? AND row = ? AND "column" = ?
                  AND plant_id IS NOT NULL
                """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int gems;
                boolean unlocked;
                Integer plantId;
                try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, row);
                    statement.setInt(3, column);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return new GrowResult(GrowStatus.POT_NOT_FOUND, 0);
                        }
                        gems = resultSet.getInt("gems");
                        unlocked = resultSet.getBoolean("unlocked");
                        int value = resultSet.getInt("plant_id");
                        plantId = resultSet.wasNull() ? null : value;
                    }
                }

                if (!unlocked) {
                    connection.rollback();
                    return new GrowResult(GrowStatus.POT_LOCKED, gems);
                }
                if (plantId == null) {
                    connection.rollback();
                    return new GrowResult(GrowStatus.POT_EMPTY, gems);
                }
                if (gems < gemCost) {
                    connection.rollback();
                    return new GrowResult(GrowStatus.NOT_ENOUGH_GEMS, gems);
                }

                int newGems = gems - gemCost;
                try (PreparedStatement statement = connection.prepareStatement(updateUserSql)) {
                    statement.setInt(1, newGems);
                    statement.setInt(2, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not update the gem balance.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(updatePotSql)) {
                    statement.setString(1, readyPlantedAt.toString());
                    statement.setInt(2, userId);
                    statement.setInt(3, row);
                    statement.setInt(4, column);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not update the greenhouse pot.");
                    }
                }

                connection.commit();
                return new GrowResult(GrowStatus.SUCCESS, newGems);
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return new GrowResult(GrowStatus.DATABASE_ERROR, 0);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new GrowResult(GrowStatus.DATABASE_ERROR, 0);
        }
    }

    public static boolean unlockPot(int userId, int row, int column) {
        String sql = """
                UPDATE greenhouse_pots
                SET unlocked = 1
                WHERE user_id = ? AND row = ? AND "column" = ?
            """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, row);
            statement.setInt(3, column);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
