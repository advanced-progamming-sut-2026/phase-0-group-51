package Data.database;

import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;

import java.sql.*;
import java.time.LocalDateTime;

public class GreenHouseRepository {
    private static final String SELECT_COLLECT_POT_SQL = """
            SELECT unlocked, plant_id
            FROM greenhouse_pots
            WHERE user_id = ? AND row = ? AND "column" = ?
            """;
    private static final String ADD_MARIGOLD_COINS_SQL = """
            UPDATE users
            SET coins = COALESCE(coins, 0) + 500
            WHERE id = ?
            """;
    private static final String SELECT_COINS_SQL =
            "SELECT coins FROM users WHERE id = ?";
    private static final String ADD_BOOST_SQL = """
            INSERT OR IGNORE INTO plant_boosts(user_id, plant_id)
            VALUES (?, ?)
            """;
    private static final String CLEAR_POT_SQL = """
            UPDATE greenhouse_pots
            SET plant_id = NULL, planted_at = NULL
            WHERE user_id = ? AND row = ? AND "column" = ?
              AND plant_id = ?
            """;
    private static final String SELECT_GROW_POT_SQL = """
            SELECT u.gems, p.unlocked, p.plant_id
            FROM users u
            JOIN greenhouse_pots p ON p.user_id = u.id
            WHERE u.id = ? AND p.row = ? AND p."column" = ?
            """;
    private static final String UPDATE_GEMS_SQL =
            "UPDATE users SET gems = ? WHERE id = ?";
    private static final String UPDATE_GROW_POT_SQL = """
            UPDATE greenhouse_pots
            SET planted_at = ?
            WHERE user_id = ? AND row = ? AND "column" = ?
              AND plant_id IS NOT NULL
            """;

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

    private record PotState(boolean unlocked, Integer plantId) {
    }

    private record GrowPotState(
            int gems,
            boolean unlocked,
            Integer plantId
    ) {
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
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return collectPotInTransaction(
                        connection,
                        userId,
                        row,
                        column,
                        expectedPlantId
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return collectDatabaseFailure();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return collectDatabaseFailure();
        }
    }

    private static CollectResult collectPotInTransaction(
            Connection connection,
            int userId,
            int row,
            int column,
            int expectedPlantId
    ) throws SQLException {
        PotState pot = readPot(connection, userId, row, column);
        if (pot == null) {
            return rollbackCollect(connection, CollectStatus.POT_NOT_FOUND);
        }
        if (!pot.unlocked()) {
            return rollbackCollect(connection, CollectStatus.POT_LOCKED);
        }
        if (pot.plantId() == null
                || pot.plantId() != expectedPlantId) {
            return rollbackCollect(connection, CollectStatus.POT_EMPTY);
        }

        int newCoins = grantCollectionReward(
                connection,
                userId,
                pot.plantId()
        );
        clearCollectedPot(
                connection,
                userId,
                row,
                column,
                pot.plantId()
        );
        connection.commit();
        return new CollectResult(CollectStatus.SUCCESS, newCoins);
    }

    private static PotState readPot(
            Connection connection,
            int userId,
            int row,
            int column
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT_COLLECT_POT_SQL
        )) {
            statement.setInt(1, userId);
            statement.setInt(2, row);
            statement.setInt(3, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                boolean unlocked = resultSet.getBoolean("unlocked");
                int value = resultSet.getInt("plant_id");
                Integer plantId = resultSet.wasNull() ? null : value;
                return new PotState(unlocked, plantId);
            }
        }
    }

    private static int grantCollectionReward(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        if (plantId == FlowerPot.MARIGOLD_ID) {
            addMarigoldCoins(connection, userId);
            return readCoinBalance(connection, userId);
        }
        addStoredBoost(connection, userId, plantId);
        return -1;
    }

    private static void addMarigoldCoins(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                ADD_MARIGOLD_COINS_SQL
        )) {
            statement.setInt(1, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "User was not found while collecting Marigold."
                );
            }
        }
    }

    private static int readCoinBalance(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT_COINS_SQL
        )) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Could not read the updated coin balance."
                    );
                }
                return resultSet.getInt("coins");
            }
        }
    }

    private static void addStoredBoost(
            Connection connection,
            int userId,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                ADD_BOOST_SQL
        )) {
            statement.setInt(1, userId);
            statement.setInt(2, plantId);
            statement.executeUpdate();
        }
    }

    private static void clearCollectedPot(
            Connection connection,
            int userId,
            int row,
            int column,
            int plantId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                CLEAR_POT_SQL
        )) {
            statement.setInt(1, userId);
            statement.setInt(2, row);
            statement.setInt(3, column);
            statement.setInt(4, plantId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "The greenhouse pot changed before collection."
                );
            }
        }
    }

    private static CollectResult rollbackCollect(
            Connection connection,
            CollectStatus status
    ) throws SQLException {
        connection.rollback();
        return new CollectResult(status, 0);
    }

    private static CollectResult collectDatabaseFailure() {
        return new CollectResult(CollectStatus.DATABASE_ERROR, 0);
    }

    public static GrowResult growPotInstantly(
            int userId,
            int row,
            int column,
            int gemCost,
            LocalDateTime readyPlantedAt
    ) {
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return growPotInTransaction(
                        connection,
                        userId,
                        row,
                        column,
                        gemCost,
                        readyPlantedAt
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return growDatabaseFailure();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return growDatabaseFailure();
        }
    }

    private static GrowResult growPotInTransaction(
            Connection connection,
            int userId,
            int row,
            int column,
            int gemCost,
            LocalDateTime readyPlantedAt
    ) throws SQLException {
        GrowPotState pot = readGrowPot(connection, userId, row, column);
        if (pot == null) {
            return rollbackGrow(connection, GrowStatus.POT_NOT_FOUND, 0);
        }
        if (!pot.unlocked()) {
            return rollbackGrow(connection, GrowStatus.POT_LOCKED, pot.gems());
        }
        if (pot.plantId() == null) {
            return rollbackGrow(connection, GrowStatus.POT_EMPTY, pot.gems());
        }
        if (pot.gems() < gemCost) {
            return rollbackGrow(
                    connection,
                    GrowStatus.NOT_ENOUGH_GEMS,
                    pot.gems()
            );
        }

        int newGems = pot.gems() - gemCost;
        updateGemBalance(connection, userId, newGems);
        markPotReady(connection, userId, row, column, readyPlantedAt);
        connection.commit();
        return new GrowResult(GrowStatus.SUCCESS, newGems);
    }

    private static GrowPotState readGrowPot(
            Connection connection,
            int userId,
            int row,
            int column
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT_GROW_POT_SQL
        )) {
            statement.setInt(1, userId);
            statement.setInt(2, row);
            statement.setInt(3, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                int value = resultSet.getInt("plant_id");
                Integer plantId = resultSet.wasNull() ? null : value;
                return new GrowPotState(
                        resultSet.getInt("gems"),
                        resultSet.getBoolean("unlocked"),
                        plantId
                );
            }
        }
    }

    private static void updateGemBalance(
            Connection connection,
            int userId,
            int newGems
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_GEMS_SQL
        )) {
            statement.setInt(1, newGems);
            statement.setInt(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not update the gem balance.");
            }
        }
    }

    private static void markPotReady(
            Connection connection,
            int userId,
            int row,
            int column,
            LocalDateTime readyPlantedAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_GROW_POT_SQL
        )) {
            statement.setString(1, readyPlantedAt.toString());
            statement.setInt(2, userId);
            statement.setInt(3, row);
            statement.setInt(4, column);
            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Could not update the greenhouse pot."
                );
            }
        }
    }

    private static GrowResult rollbackGrow(
            Connection connection,
            GrowStatus status,
            int gems
    ) throws SQLException {
        connection.rollback();
        return new GrowResult(status, gems);
    }

    private static GrowResult growDatabaseFailure() {
        return new GrowResult(GrowStatus.DATABASE_ERROR, 0);
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
