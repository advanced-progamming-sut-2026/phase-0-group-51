package Data.loader;

import Data.database.DataBaseManager;
import Data.database.QuestDatabaseMigration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.Objects;

public final class QuestLoader {
    private static final String UPSERT_SQL = """
            INSERT INTO quests
                (name, condition, priority, event_type, target_amount,
                 reward_amount, reward_type, quest_type, unlockable_id,
                 parameter_options, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            ON CONFLICT(name) DO UPDATE SET
                condition = excluded.condition,
                priority = excluded.priority,
                event_type = excluded.event_type,
                target_amount = excluded.target_amount,
                reward_amount = excluded.reward_amount,
                reward_type = excluded.reward_type,
                quest_type = excluded.quest_type,
                unlockable_id = excluded.unlockable_id,
                parameter_options = excluded.parameter_options,
                active = 1
            """;

    private QuestLoader() {
    }

    public static void loadQuestsToDatabase() {
        try (Connection connection = DataBaseManager.getConnection()) {
            QuestDatabaseMigration.migrate(connection);
            connection.setAutoCommit(false);
            try (Statement deactivate = connection.createStatement();
                 Reader reader = openCsv();
                 PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
                deactivate.executeUpdate("UPDATE quests SET active = 0");
                for (CSVRecord record : readRecords(reader)) {
                    bind(statement, record);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load quests.", exception);
        }
    }

    private static Reader openCsv() {
        return new InputStreamReader(
                Objects.requireNonNull(
                        QuestLoader.class.getResourceAsStream("/Quests.csv"),
                        "Quests.csv was not found."),
                StandardCharsets.UTF_8);
    }

    private static Iterable<CSVRecord> readRecords(Reader reader) throws Exception {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader);
    }

    private static void bind(PreparedStatement statement, CSVRecord record) throws Exception {
        statement.setString(1, record.get("name"));
        statement.setString(2, record.get("condition"));
        statement.setString(3, record.get("priority"));
        statement.setString(4, record.get("event_type"));
        statement.setInt(5, Integer.parseInt(record.get("target_amount")));
        statement.setInt(6, Integer.parseInt(record.get("reward_amount")));
        statement.setString(7, record.get("reward_type"));
        statement.setString(8, record.get("quest_type"));
        nullable(statement, 9, record.get("unlockable_id"));
        nullable(statement, 10, record.get("parameter_options"));
    }

    private static void nullable(PreparedStatement statement, int index, String value)
            throws Exception {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NONE")) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
