package Data.loader;

import Data.database.DataBaseManager;
import Data.database.QuestDatabaseMigration;
import models.games.ChapterTheme;
import models.quests.QuestType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class QuestLoader {
    private static final int EXPANDED_ID = 100;
    private static final int SIZE = 100;
    private static final String UPSERT_SQL = """
            INSERT INTO quests
                (id, name, condition, priority, event_type, target_amount,
                 reward_amount, reward_type, quest_type, unlockable_id,
                 parameter_options, sort_order, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                condition = excluded.condition,
                priority = excluded.priority,
                event_type = excluded.event_type,
                target_amount = excluded.target_amount,
                reward_amount = excluded.reward_amount,
                reward_type = excluded.reward_type,
                quest_type = excluded.quest_type,
                unlockable_id = excluded.unlockable_id,
                parameter_options = excluded.parameter_options,
                sort_order = excluded.sort_order,
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
                    addQuestVariants(statement, record);
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

    private static void addQuestVariants(
            PreparedStatement statement,
            CSVRecord record
    ) throws Exception {
        QuestType questType = QuestType.valueOf(record.get("quest_type"));
        List<String> parameters = parametersFor(record, questType);
        boolean multipleVariants = questType != QuestType.DAILY
                && parameters.size() > 1;

        for (int index = 0; index < parameters.size(); index++) {
            QuestVariant variant = createVariant(
                    record,
                    parameters.get(index),
                    index,
                    multipleVariants
            );
            bind(statement, variant);
            statement.addBatch();
        }
    }

    private static List<String> parametersFor(
            CSVRecord record,
            QuestType questType
    ) {
        String rawOptions = nullableValue(record.get("parameter_options"));


        if (questType == QuestType.DAILY) {
            return Collections.singletonList(rawOptions);
        }

        return expandPermanentParameters(rawOptions);
    }

    private static List<String> expandPermanentParameters(String rawOptions) {
        if (rawOptions == null) {
            return Collections.singletonList(null);
        }

        return switch (rawOptions.trim().toUpperCase()) {
            case "@CHAPTER" -> adventureChapters();
            case "@ROW", "@CROSS" -> numberRange(1, 5);
            case "@COLUMN" -> numberRange(1, 9);
            default -> splitOptions(rawOptions);
        };
    }

    private static List<String> adventureChapters() {
        return List.of(ChapterTheme.values()).stream()
                .filter(chapter -> chapter != ChapterTheme.MINIGAME)
                .map(ChapterTheme::name)
                .toList();
    }

    private static List<String> numberRange(int start, int end) {
        List<String> values = new ArrayList<>();
        for (int value = start; value <= end; value++) {
            values.add(Integer.toString(value));
        }
        return List.copyOf(values);
    }

    private static List<String> splitOptions(String rawOptions) {
        if (!rawOptions.contains("|")) {
            return List.of(rawOptions.trim());
        }

        List<String> values = new ArrayList<>();
        for (String value : rawOptions.split("\\|")) {
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }

        return values.isEmpty()
                ? Collections.singletonList(null)
                : List.copyOf(values);
    }

    private static QuestVariant createVariant(
            CSVRecord record,
            String parameter,
            int variantIndex,
            boolean multipleVariants
    ) {
        int baseId = Integer.parseInt(record.get("id"));
        int id = baseId + variantIndex * EXPANDED_ID;
        int sortOrder = baseId * SIZE + variantIndex;

        String name = record.get("name");
        if (multipleVariants) {
            name += " (" + variantLabel(record.get("event_type"), parameter) + ")";
        }

        return new QuestVariant(
                id,
                name,
                variantCondition(record, parameter, multipleVariants),
                record.get("priority"),
                record.get("event_type"),
                Integer.parseInt(record.get("target_amount")),
                Integer.parseInt(record.get("reward_amount")),
                record.get("reward_type"),
                record.get("quest_type"),
                nullableValue(record.get("unlockable_id")),
                parameter,
                sortOrder
        );
    }

    private static String variantCondition(
            CSVRecord record,
            String parameter,
            boolean multipleVariants
    ) {
        String condition = record.get("condition");
        if (!multipleVariants || parameter == null) {
            return condition;
        }
        if (record.get("event_type").equals("WIN_MAX_PLANTS_LOST")) {
            if (parameter.equals("0")) {
                return "Win a level without losing any plants";
            }
            if (parameter.equals("1")) {
                return "Win a level without losing more than 1 plant";
            }
        }
        return condition.replace("{parameter}", parameter);
    }

    private static String variantLabel(String eventType, String parameter) {
        if (parameter == null || parameter.isBlank()) {
            return "Default";
        }

        return switch (eventType) {
            case "CHAPTER_ZOMBIE_KILLS" -> chapterLabel(parameter);
            case "SUN_COLLECTED" -> parameter + " Sun";
            case "WIN_MAX_PLANTS_LOST" -> parameter.equals("0")
                    ? "No plants lost"
                    : "Lose at most " + parameter
                    + (parameter.equals("1") ? " plant" : " plants");
            case "MOWER_KILLS" -> parameter + " Zombies";
            default -> parameter;
        };
    }

    private static String chapterLabel(String parameter) {
        try {
            return ChapterTheme.valueOf(parameter).getName();
        } catch (IllegalArgumentException exception) {
            return parameter.replace('_', ' ');
        }
    }

    private static String nullableValue(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NONE")) {
            return null;
        }
        return value.trim();
    }

    private static void bind(
            PreparedStatement statement,
            QuestVariant variant
    ) throws Exception {
        statement.setInt(1, variant.id());
        statement.setString(2, variant.name());
        statement.setString(3, variant.condition());
        statement.setString(4, variant.priority());
        statement.setString(5, variant.eventType());
        statement.setInt(6, variant.targetAmount());
        statement.setInt(7, variant.rewardAmount());
        statement.setString(8, variant.rewardType());
        statement.setString(9, variant.questType());
        nullable(statement, 10, variant.unlockableId());
        nullable(statement, 11, variant.parameter());
        statement.setInt(12, variant.sortOrder());
    }

    private static void nullable(
            PreparedStatement statement,
            int index,
            String value
    ) throws Exception {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NONE")) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private record QuestVariant(
            int id,
            String name,
            String condition,
            String priority,
            String eventType,
            int targetAmount,
            int rewardAmount,
            String rewardType,
            String questType,
            String unlockableId,
            String parameter,
            int sortOrder
    ) {
    }
}
