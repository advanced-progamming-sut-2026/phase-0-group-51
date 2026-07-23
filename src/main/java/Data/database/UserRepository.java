package Data.database;

import Data.loader.PlantRegistry;
import models.User;
import models.enums.LootType;

import java.sql.*;

public class UserRepository {
    private static final String ADD_LOOT_COINS_SQL = """
            UPDATE users SET coins = COALESCE(coins, 0) + 50 WHERE id = ?
            """;
    private static final String ADD_LOOT_GEMS_SQL = """
            UPDATE users SET gems = COALESCE(gems, 0) + 1 WHERE id = ?
            """;
    private static final String READ_LOOT_COINS_SQL =
            "SELECT coins AS total FROM users WHERE id = ?";
    private static final String READ_LOOT_GEMS_SQL =
            "SELECT gems AS total FROM users WHERE id = ?";
    private static final String FIND_LOCKED_POT_SQL = """
            SELECT row, "column"
            FROM greenhouse_pots
            WHERE user_id = ? AND unlocked = 0
            ORDER BY row, "column"
            LIMIT 1
            """;
    private static final String UNLOCK_LOOT_POT_SQL = """
            UPDATE greenhouse_pots
            SET unlocked = 1
            WHERE user_id = ? AND row = ? AND "column" = ? AND unlocked = 0
            """;
    private static final String COUNT_UNLOCKED_POTS_SQL = """
            SELECT COUNT(*) AS total
            FROM greenhouse_pots
            WHERE user_id = ? AND unlocked = 1
            """;

    private record PotCoordinates(int row, int column) {
    }


        public boolean register(User user) {
        String userSql = """
                INSERT INTO users ( username, email, password_hash, gender, nickname, security_question, answer
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        String progressSql = """
                INSERT INTO user_progress(user_id, chapter_index, level_index)
                VALUES (?, 1, 1)
                """;
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        userSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, user.getUsername());
                    statement.setString(2, user.getEmail());
                    statement.setString(3, user.getPasswordHash());
                    statement.setString(4, user.getGender());
                    statement.setString(5, user.getNickname());
                    statement.setInt(6, user.getSecurityQuestion());
                    statement.setString(7, user.getAnswer());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("The user row was not created.");}
                    try (ResultSet resultSet = statement.getGeneratedKeys()) {
                        if (!resultSet.next()) {
                            throw new SQLException("The generated user id was not returned.");}
                        user.setId(resultSet.getInt(1));}}
                try (PreparedStatement statement = connection.prepareStatement(progressSql)) {
                    statement.setInt(1, user.getId());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("The initial progress row was not created.");}}
                GreenHouseRepository.insertInitialPots(connection, user.getId());
                PlantRepository.unlockPlants(
                        connection,
                        user.getId(),
                        PlantRegistry.getStarterPlantIds()
                );
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                user.setId(0);
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            user.setId(0);
            return false;
            }
        }
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();}
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;}
    }
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();}
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;}
    }

    public boolean emailExistsForAnotherUser(String email, int userId) {
        String sql = "SELECT 1 FROM users WHERE email = ? AND id <> ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return readUser(resultSet);
            }
        }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public boolean setStayLoggedIn(int userId, boolean stayLoggedIn) {
        String clearSql = "UPDATE users SET stay_logged_in = 0";
        String rememberSql = "UPDATE users SET stay_logged_in = 1 WHERE id = ?";
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement clearStatement =
                         connection.prepareStatement(clearSql);
                 PreparedStatement rememberStatement =
                         connection.prepareStatement(rememberSql)) {
                clearStatement.executeUpdate();

                if (stayLoggedIn) {
                    rememberStatement.setInt(1, userId);
                    if (rememberStatement.executeUpdate() != 1) {
                        throw new SQLException(
                                "User was not found while saving the login session."
                        );
                    }
                }
                connection.commit();
                return true;

            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }
    public User getRememberedUser() {
        String sql = """
        SELECT *
        FROM users
        WHERE stay_logged_in = 1
        ORDER BY id DESC
        LIMIT 1
        """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return readUser(resultSet);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }
        public boolean updateStats(User user) {
        String sql = """
                UPDATE users
                SET coins = ?, gems = ?, plant_food_num = ?,
                    most_meow_point = ?, max_point = ?, games_played = ?,
                    mini_games_played = ?, last_won_game = ?
                WHERE id = ?
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, user.getCoins());
            statement.setInt(2, user.getGems());
            statement.setInt(3, user.getPlantFoodNum());
            statement.setInt(4, user.getMostMeowPoint());
            statement.setInt(5, user.getMaxPoint());
            statement.setInt(6, user.getGamesPlayed());
            statement.setInt(7, user.getMiniGamesPlayed());
            statement.setString(8, user.getLastWonGame());
            statement.setInt(9, user.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public boolean updateUsername(int userId, String newUsername) {
        return updateSingleStringField(
                "UPDATE users SET username = ? WHERE id = ?",
                userId,
                newUsername
        );
    }

    public boolean updateNickname(int userId, String newNickname) {
        return updateSingleStringField(
                "UPDATE users SET nickname = ? WHERE id = ?",
                userId,
                newNickname
        );
    }

    public boolean updateEmail(int userId, String newEmail) {
        return updateSingleStringField(
                "UPDATE users SET email = ? WHERE id = ?",
                userId,
                newEmail
        );
    }

    public boolean updatePassword(String username, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, username);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
                return false;
            }
        }

    public boolean updateDifficulty(String username, int difficultyLevel) {
        String sql = "UPDATE users SET difficulty_level = ? WHERE username = ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, difficultyLevel);
            statement.setString(2, username);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }



    public int getPassedLevels(int userId) {
        String sql = """
                SELECT COUNT(*) AS completed_count
                FROM user_completed_levels
                WHERE user_id = ?
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("completed_count");
            }

        }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    public int claimStoredPlantFood(int userId) {
        String selectSql = "SELECT plant_food_num FROM users WHERE id = ?";
        String clearSql = "UPDATE users SET plant_food_num = 0 WHERE id = ?";
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int amount;
                try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return -1;
                        }
                        amount = Math.max(0, Math.min(3, resultSet.getInt("plant_food_num")));
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(clearSql)) {
                    statement.setInt(1, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not consume stored Plant Food.");
                    }
                }
                connection.commit();
                return amount;
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return -1;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return -1;
        }
    }

    public int recordAdventureLoss(int userId) {
        String updateSql = """
                UPDATE users
                SET games_played = COALESCE(games_played, 0) + 1
                WHERE id = ?
                """;
        String selectSql = "SELECT games_played FROM users WHERE id = ?";
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setInt(1, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not record the played Adventure game.");
                    }
                }
                int gamesPlayed;
                try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Could not read the games-played counter.");
                        }
                        gamesPlayed = resultSet.getInt("games_played");
                    }
                }
                connection.commit();
                return gamesPlayed;
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return -1;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return -1;
        }
    }

    public LootResult applyZombieLoot(int userId, LootType lootType) {

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return applyZombieLootInTransaction(
                        connection,
                        userId,
                        lootType
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return lootFailure();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return lootFailure();
        }
    }

    private LootResult applyZombieLootInTransaction(
            Connection connection,
            int userId,
            LootType lootType
    ) throws SQLException {
                if (lootType == LootType.POT) {
            return applyPotLoot(connection, userId);
        }
        return applyCurrencyLoot(connection, userId, lootType);
    }

    private LootResult applyPotLoot(
            Connection connection,
            int userId
    ) throws SQLException {
        PotCoordinates coordinates = findNextLockedPot(connection, userId);
        if (coordinates == null) {
            connection.rollback();
            return lootFailure();
        }
        unlockLootPot(connection, userId, coordinates);
        int total = countUnlockedPots(connection, userId);
        connection.commit();
        return new LootResult(
                true,
                total,
                coordinates.row(),
                coordinates.column()
        );
    }

    private PotCoordinates findNextLockedPot(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                FIND_LOCKED_POT_SQL
        )) {
                        statement.setInt(1, userId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                    return null;
                            }
                return new PotCoordinates(
                        resultSet.getInt("row"),
                        resultSet.getInt("column")
                );
                        }
                    }
    }

    private void unlockLootPot(
            Connection connection,
            int userId,
            PotCoordinates coordinates
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UNLOCK_LOOT_POT_SQL
        )) {
                        statement.setInt(1, userId);
            statement.setInt(2, coordinates.row());
            statement.setInt(3, coordinates.column());
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Could not unlock the dropped flower pot.");
                        }
                    }
    }

    private int countUnlockedPots(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                COUNT_UNLOCKED_POTS_SQL
        )) {
                        statement.setInt(1, userId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                throw new SQLException("Could not count the unlocked flower pots.");
                            }
                return resultSet.getInt("total");
                        }
                    }
    }

    private LootResult applyCurrencyLoot(
            Connection connection,
            int userId,
            LootType lootType
    ) throws SQLException {
        incrementLootBalance(connection, userId, lootType);
        int total = readLootBalance(connection, userId, lootType);
                    connection.commit();
        return new LootResult(true, total, 0, 0);
                }

    private void incrementLootBalance(
            Connection connection,
            int userId,
            LootType lootType
    ) throws SQLException {
        String sql = lootType == LootType.COIN
                ? ADD_LOOT_COINS_SQL
                : ADD_LOOT_GEMS_SQL;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not save the zombie loot.");
                    }
                }
    }

    private int readLootBalance(
            Connection connection,
            int userId,
            LootType lootType
    ) throws SQLException {
        String sql = lootType == LootType.COIN
                ? READ_LOOT_COINS_SQL
                : READ_LOOT_GEMS_SQL;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Could not read the updated loot balance.");
                        }
                return resultSet.getInt("total");
                    }
                }
    }

    private LootResult lootFailure() {
            return new LootResult(false, 0, 0, 0);
        }

    public record LootResult(
            boolean saved,
            int total,
            int unlockedRow,
            int unlockedColumn
    ) {
    }
    public boolean clearStayLoggedIn() {
        String sql = "UPDATE users SET stay_logged_in = 0";

        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }
    private boolean updateSingleStringField(
            String sql,
            int userId,
            String value
    ) {
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private User readUser(ResultSet resultSet) throws SQLException {
        User user = new User(
                resultSet.getInt("id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                resultSet.getString("gender"),
                resultSet.getString("nickname"),
                resultSet.getInt("security_question"),
                resultSet.getString("answer"),
                resultSet.getInt("coins"),
                resultSet.getInt("gems"),
                resultSet.getInt("plant_food_num"),
                resultSet.getInt("most_meow_point"),
                resultSet.getInt("max_point"),
                resultSet.getInt("games_played"),
                resultSet.getInt("mini_games_played"),
                resultSet.getString("last_won_game"),
                resultSet.getInt("difficulty_level")
        );
        user.setQuestDailyNum(resultSet.getInt("quest_daily_num"));
        user.setQuestNonDailyNum(resultSet.getInt("quest_non_daily_num"));
        return user;
    }}

