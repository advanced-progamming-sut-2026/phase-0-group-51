package Data.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AuthSessionRepository {

    public AuthSessionRepository() {
        initializeTable();
    }

    private void initializeTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS auth_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    token_hash TEXT NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """;

        try (Connection connection =
                     DataBaseManager.getConnection();
             Statement statement =
                     connection.createStatement()) {

            statement.execute(sql);

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public boolean saveToken(
            int userId,
            String tokenHash
    ) {
        String sql = """
                INSERT INTO auth_sessions (
                    user_id,
                    token_hash
                )
                VALUES (?, ?)
                """;

        try (Connection connection =
                     DataBaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, tokenHash);

            return statement.executeUpdate() == 1;

        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public String findUsernameByTokenHash(
            String tokenHash
    ) {
        String sql = """
                SELECT users.username
                FROM auth_sessions
                JOIN users
                  ON users.id = auth_sessions.user_id
                WHERE auth_sessions.token_hash = ?
                """;

        try (Connection connection =
                     DataBaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public void touch(String tokenHash) {
        String sql = """
                UPDATE auth_sessions
                SET last_used_at = CURRENT_TIMESTAMP
                WHERE token_hash = ?
                """;

        try (Connection connection =
                     DataBaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);
            statement.executeUpdate();

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void deleteToken(String tokenHash) {
        String sql =
                "DELETE FROM auth_sessions WHERE token_hash = ?";

        try (Connection connection =
                     DataBaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);
            statement.executeUpdate();

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void deleteAllForUser(int userId) {
        String sql =
                "DELETE FROM auth_sessions WHERE user_id = ?";

        try (Connection connection =
                     DataBaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}