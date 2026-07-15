package Data.loader;
import Data.database.DataBaseManager;
import Data.database.QuestDatabaseMigration;
import models.quests.QuestPriority;
import models.quests.QuestRewardType;
import models.quests.QuestType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

public class QuestLoader {
    private static final String INSERT_SQL = """
            INSERT INTO quests
                (name, condition, priority, event_type, target_amount,
                 reward_amount, reward_type, quest_type, unlockable_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(name) DO UPDATE SET
                condition = excluded.condition,
                priority = excluded.priority,
                event_type = excluded.event_type,
                target_amount = excluded.target_amount,
                reward_amount = excluded.reward_amount,
                reward_type = excluded.reward_type,
                quest_type = excluded.quest_type,
                unlockable_id = excluded.unlockable_id
            """;

    private QuestLoader() {
    }

    public static void loadQuestsToDatabase() {
        QuestDatabaseMigration.migrate();
        try (Reader reader = openCsv();
             Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            for (CSVRecord record : readRecords(reader)) {
                bind(statement, record);
                statement.addBatch();
            }
            statement.executeBatch();
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
        String target = record.get("unlockable_id");
        if (target == null || target.isBlank() || target.equalsIgnoreCase("NONE")) {
            statement.setNull(9, Types.VARCHAR);
        } else {
            statement.setString(9, target);
        }
    }
    }

