package Data.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class QuestDatabaseMigration {
    private QuestDatabaseMigration() {
    }

    public static void migrate() {
        try (Connection connection = DataBaseManager.getConnection()) {
            addColumnIfMissing(connection, "quests", "event_type", "TEXT");
            addColumnIfMissing(
                    connection, "user_quests", "claimed",
                    "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(
                    connection, "users", "quest_daily_num",
                    "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(
                    connection, "users", "quest_non_daily_num",
                    "INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not migrate quest tables.", exception);
        }
    }

    private static void addColumnIfMissing(
            Connection connection, String table, String column, String definition
    ) throws SQLException {
        if (hasColumn(connection, table, column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static boolean hasColumn(
            Connection connection, String table, String column
    ) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
