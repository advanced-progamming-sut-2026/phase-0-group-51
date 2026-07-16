package Data.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class QuestDatabaseMigration {
    private QuestDatabaseMigration() {
    }

    public static void migrate() {
        try (Connection connection = DataBaseManager.getConnection()) {
            migrate(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not migrate quest tables.", exception);
        }
    }

    public static void migrate(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "quests", "event_type", "TEXT");
        addColumnIfMissing(connection, "quests", "parameter_options", "TEXT");
        addColumnIfMissing(connection, "quests", "active", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(connection, "user_quests", "claimed", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "user_quests", "parameter", "TEXT");
        addColumnIfMissing(connection, "user_quests", "target_amount", "INTEGER");
        addColumnIfMissing(connection, "user_quests", "reward_amount", "INTEGER");
        addColumnIfMissing(connection, "user_quests", "completion_counted", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "users", "quest_daily_num", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "users", "quest_non_daily_num", "INTEGER NOT NULL DEFAULT 0");

    }

    private static void addColumnIfMissing(
            Connection connection, String table, String column, String definition
    ) throws SQLException {
        if (hasColumn(connection, table, column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static boolean hasColumn(
            Connection connection, String table, String column
    ) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
    private static void migrateOldCompletionCounters(Connection connection)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                UPDATE user_quests
                SET completion_counted = 1
                WHERE claimed = 1
                  AND completion_counted = 0
                """);

            statement.executeUpdate("""
                UPDATE users
                SET quest_daily_num = COALESCE(quest_daily_num, 0) + (
                    SELECT COUNT(*)
                    FROM user_quests uq
                    JOIN quests q ON q.id = uq.quest_id
                    WHERE uq.user_id = users.id
                      AND uq.is_completed = 1
                      AND uq.completion_counted = 0
                      AND q.quest_type = 'DAILY'
                )
                """);

            statement.executeUpdate("""
                UPDATE users
                SET quest_non_daily_num = COALESCE(quest_non_daily_num, 0) + (
                    SELECT COUNT(*)
                    FROM user_quests uq
                    JOIN quests q ON q.id = uq.quest_id
                    WHERE uq.user_id = users.id
                      AND uq.is_completed = 1
                      AND uq.completion_counted = 0
                      AND q.quest_type <> 'DAILY'
                )
                """);

            statement.executeUpdate("""
                UPDATE user_quests
                SET completion_counted = 1
                WHERE is_completed = 1
                  AND completion_counted = 0
                """);
        }
    }
}
