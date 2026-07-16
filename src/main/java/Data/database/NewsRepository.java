package Data.database;
import models.items.News;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NewsRepository {
        public List<News> getNewsForUser(int userId) {
            List<News> newsList = new ArrayList<>();

            String sql = """
                SELECT n.id, n.content, un.is_read
                FROM news n
                JOIN user_news un ON n.id = un.news_id
                WHERE un.user_id = ?
                ORDER BY n.created_at DESC, n.id DESC
                """;

            try (Connection connection = DataBaseManager.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        newsList.add(new News(
                                resultSet.getInt("id"),
                                userId,
                                resultSet.getString("content"),
                                resultSet.getInt("is_read") == 1
                        ));
                    }
                }

            } catch (SQLException exception) {
                exception.printStackTrace();
            }

            return newsList;
        }

        public int countUnreadNews(int userId) {
            String sql = """
                SELECT COUNT(*)
                FROM user_news
                WHERE user_id = ?
                  AND is_read = 0
                """;

            try (Connection connection = DataBaseManager.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
                    }
            } catch (SQLException exception) {
                exception.printStackTrace();
            return 0;
        }
    }

        public boolean markAsRead(int userId, int newsId) {
            String sql = """
                UPDATE user_news
                SET is_read = 1
                WHERE user_id = ?
                  AND news_id = ?
                """;

            try (Connection connection = DataBaseManager.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, userId);
                statement.setInt(2, newsId);

                return statement.executeUpdate() == 1;

            } catch (SQLException exception) {
                exception.printStackTrace();
                return false;
            }
        }

        public boolean createNewsForUser(int userId, String content) {
            if (content == null || content.isBlank()) {
                return false;
            }

            try (Connection connection = DataBaseManager.getConnection()) {
                connection.setAutoCommit(false);

                try {
                    int newsId = insertNews(connection, content);
                linkNewsToUser(connection, userId, newsId);
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

    public boolean discoverZombie(int userId, String zombieAlias) {
        if (zombieAlias == null || zombieAlias.isBlank()) {
                        return false;
                    }

        String discoverySql = """
                INSERT OR IGNORE INTO user_unlocked_zombies (
                    user_id, zombie_alias
                ) VALUES (?, ?)
                        """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(discoverySql)) {
                        statement.setInt(1, userId);
                statement.setString(2, zombieAlias);
                int inserted = statement.executeUpdate();

                if (inserted == 0) {
                    connection.rollback();
                    return false;
                    }

                int newsId = insertNews(
                        connection,
                        "New zombie discovered: " + zombieAlias + "."
                );
                linkNewsToUser(connection, userId, newsId);
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

    public Set<String> getDiscoveredZombieAliases(int userId) {
        Set<String> aliases = new LinkedHashSet<>();
        String sql = """
                SELECT zombie_alias
                FROM user_unlocked_zombies
                WHERE user_id = ?
                ORDER BY zombie_alias COLLATE NOCASE
                """;

        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    aliases.add(resultSet.getString("zombie_alias"));
                    }
                }
        } catch (SQLException exception) {
            exception.printStackTrace();
            }

        return aliases;
        }

    public boolean hasDiscoveredZombie(int userId, String zombieAlias) {
            String sql = """
                SELECT 1
                FROM user_unlocked_zombies
                WHERE user_id = ? AND zombie_alias = ?
            """;
            try (Connection connection = DataBaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                statement.setString(2, zombieAlias);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
                return false;
            }
        }

    private int insertNews(Connection connection, String content) throws SQLException {
        String sql = "INSERT INTO news (content) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, content);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        throw new SQLException("The generated news ID was not returned.");
    }

    private void linkNewsToUser(
            Connection connection,
            int userId,
            int newsId
    ) throws SQLException {
        String sql = """
                INSERT INTO user_news (user_id, news_id, is_read)
                VALUES (?, ?, 0)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, newsId);
            statement.executeUpdate();
        }
    }
}
