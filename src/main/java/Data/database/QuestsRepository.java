package Data.database;
import lombok.Getter;
import models.quests.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class QuestsRepository {
    public List<QuestEntry> getQuestEntries(int userId, QuestType type) {
        prepareUserQuests(userId);
        String sql = entrySelect("AND q.quest_type = ?");
        List<QuestEntry> result = new ArrayList<>();
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, type.name());
            readEntries(statement, result);
        } catch (SQLException exception) {
            throw databaseError("Could not load quests.", exception);
        }
        return result;
    }

    public Optional<QuestEntry> getQuestEntry(int userId, int questId) {
        prepareUserQuests(userId);
        String sql = entrySelect("AND q.id = ?");
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, questId);
            List<QuestEntry> entries = new ArrayList<>();
            readEntries(statement, entries);
            return entries.stream().findFirst();
        } catch (SQLException exception) {
            throw databaseError("Could not load quest.", exception);
        }
    }

    public void addProgress(int userId, QuestEventType eventType, int amount) {
        if (eventType == null || amount <= 0) {
            return;
        }
        prepareUserQuests(userId);
        String sql = """
                UPDATE user_quests
                SET progress = MIN(progress + ?,
                        (SELECT target_amount FROM quests WHERE id = quest_id)),
                    is_completed = CASE
                        WHEN progress + ? >=
                             (SELECT target_amount FROM quests WHERE id = quest_id)
                        THEN 1 ELSE is_completed END
                WHERE user_id = ? AND claimed = 0 AND is_completed = 0
                  AND quest_id IN (SELECT id FROM quests WHERE event_type = ?)
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, amount);
            statement.setInt(2, amount);
            statement.setInt(3, userId);
            statement.setString(4, eventType.name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("Could not update quest progress.", exception);
        }
    }

    public boolean markClaimed(int userId, int questId) {
        String sql = """
                UPDATE user_quests SET claimed = 1
                WHERE user_id = ? AND quest_id = ?
                  AND is_completed = 1 AND claimed = 0
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, questId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw databaseError("Could not claim quest.", exception);
        }
    }

    public void incrementQuestCounter(int userId, QuestType type) {
        String column = type == QuestType.DAILY
                ? "quest_daily_num" : "quest_non_daily_num";
        String sql = "UPDATE users SET " + column + " = " + column + " + 1 WHERE id = ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("Could not update quest statistics.", exception);
        }
    }

    private void prepareUserQuests(int userId) {
        String today = LocalDate.now().toString();
        String assignSql = """
                INSERT OR IGNORE INTO user_quests
                    (user_id, quest_id, progress, is_completed, claimed, reset_date)
                SELECT ?, id, 0, 0, 0,
                       CASE WHEN quest_type = 'DAILY' THEN ? ELSE NULL END
                FROM quests WHERE event_type IS NOT NULL
                """;
        String resetSql = """
                UPDATE user_quests
                SET progress = 0, is_completed = 0, claimed = 0, reset_date = ?
                WHERE user_id = ?
                  AND quest_id IN (SELECT id FROM quests WHERE quest_type = 'DAILY')
                  AND (reset_date IS NULL OR reset_date <> ?)
                """;
        executePreparation(userId, today, assignSql, resetSql);
    }

    private void executePreparation(
            int userId, String today, String assignSql, String resetSql
    ) {
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement assign = connection.prepareStatement(assignSql);
             PreparedStatement reset = connection.prepareStatement(resetSql)) {
            assign.setInt(1, userId);
            assign.setString(2, today);
            assign.executeUpdate();
            reset.setString(1, today);
            reset.setInt(2, userId);
            reset.setString(3, today);
            reset.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("Could not prepare user quests.", exception);
        }
    }

    private String entrySelect(String extraCondition) {
        return """
                SELECT q.*, uq.user_id, uq.progress, uq.is_completed,
                       uq.claimed, uq.reset_date
                FROM quests q
                JOIN user_quests uq ON uq.quest_id = q.id
                WHERE uq.user_id = ? AND q.event_type IS NOT NULL %s
                ORDER BY CASE q.priority
                    WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2
                    WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 ELSE 5 END, q.id
                """.formatted(extraCondition);
    }

    private void readEntries(
            PreparedStatement statement, List<QuestEntry> result
    ) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Quest quest = mapQuest(resultSet);
                Date resetDate = resultSet.getDate("reset_date");
                UserQuest userQuest = new UserQuest(
                        resultSet.getInt("user_id"), quest.getId(),
                        resultSet.getInt("progress"),
                        resultSet.getBoolean("is_completed"),
                        resultSet.getBoolean("claimed"),
                        resetDate == null ? null : resetDate.toLocalDate());
                result.add(new QuestEntry(quest, userQuest));
            }
        }
    }

    private Quest mapQuest(ResultSet resultSet) throws SQLException {
        QuestType type = QuestType.valueOf(resultSet.getString("quest_type"));
        String name = resultSet.getString("name");
        String condition = resultSet.getString("condition");
        QuestPriority priority = QuestPriority.valueOf(resultSet.getString("priority"));
        QuestEventType eventType = QuestEventType.valueOf(resultSet.getString("event_type"));
        int target = resultSet.getInt("target_amount");
        int reward = resultSet.getInt("reward_amount");
        QuestRewardType rewardType = QuestRewardType.valueOf(resultSet.getString("reward_type"));
        String unlockableId = resultSet.getString("unlockable_id");
        Quest quest = createQuest(type, name, condition, priority, eventType,
                target, reward, rewardType, unlockableId);
        quest.setId(resultSet.getInt("id"));
        return quest;
    }

    private Quest createQuest(
            QuestType type, String name, String condition, QuestPriority priority,
            QuestEventType eventType, int target, int reward,
            QuestRewardType rewardType, String unlockableId
    ) {
        return switch (type) {
            case DAILY -> new DailyQuests(name, condition, priority, eventType,
                    target, reward, rewardType, unlockableId);
            case MAIN -> new MainQuests(name, condition, priority, eventType,
                    target, reward, rewardType, unlockableId);
            case EPIC -> new EpicQuests(name, condition, priority, eventType,
                    target, reward, rewardType, unlockableId);
        };
    }

    private IllegalStateException databaseError(String message, SQLException exception) {
        return new IllegalStateException(message, exception);
    }

    public record QuestEntry(Quest quest, UserQuest userQuest) {
    }
    }

