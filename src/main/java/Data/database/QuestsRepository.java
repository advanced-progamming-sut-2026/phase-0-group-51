package Data.database;

import models.quests.DailyQuests;
import models.quests.EpicQuests;
import models.quests.MainQuests;
import models.quests.Quest;
import models.quests.QuestEventType;
import models.quests.QuestPriority;
import models.quests.QuestRewardType;
import models.quests.QuestType;
import models.quests.UserQuest;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestsRepository {
    public List<Quest> getAllQuests() {
        String sql = "SELECT * FROM quests WHERE active = 1 ORDER BY id";
        List<Quest> result = new ArrayList<>();
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(mapQuest(resultSet));
            }
            return result;
        } catch (SQLException exception) {
            throw databaseError("Could not load quests.", exception);
        }
    }

    public Optional<Quest> getQuest(int questId) {
        String sql = "SELECT * FROM quests WHERE id = ? AND active = 1";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, questId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapQuest(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseError("Could not load quest.", exception);
        }
    }

    public Optional<UserQuest> getUserQuest(int userId, int questId) {
        String sql = "SELECT * FROM user_quests WHERE user_id = ? AND quest_id = ?";
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, questId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapStandaloneUserQuest(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseError("Could not load user quest.", exception);
        }
    }

    public void saveAssignment(UserQuest assignment) {
        String sql = """
                INSERT INTO user_quests
                             (user_id, quest_id, progress, target_amount, reward_amount,
                              is_completed, claimed, reset_date, parameter)
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                         ON CONFLICT(user_id, quest_id) DO UPDATE SET
                             progress = excluded.progress,
                             target_amount = excluded.target_amount,
                             reward_amount = excluded.reward_amount,
                             is_completed = excluded.is_completed,
                             claimed = excluded.claimed,
                             reset_date = excluded.reset_date,
                             parameter = excluded.parameter,
                             completion_counted = CASE
                                 WHEN COALESCE(user_quests.reset_date, '')
                                      <> COALESCE(excluded.reset_date, '')
                                 THEN 0
                                 ELSE user_quests.completion_counted
                             END
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignment.getUserId());
            statement.setInt(2, assignment.getQuestId());
            statement.setInt(3, assignment.getProgress());
            statement.setInt(4, assignment.getTargetAmount());
            statement.setInt(5, assignment.getRewardAmount());
            statement.setBoolean(6, assignment.isCompleted());
            statement.setBoolean(7, assignment.isClaimed());
            if (assignment.getResetDate() == null) {
                statement.setNull(8, Types.DATE);
            } else {
                statement.setDate(8, Date.valueOf(assignment.getResetDate()));
            }
            statement.setString(9, assignment.getParameter());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("Could not save quest assignment.", exception);
        }
    }

    public List<QuestEntry> getQuestEntries(int userId, QuestType type) {
        String sql = """
                SELECT q.*, q.id AS quest_id_value, uq.user_id, uq.progress,
                       uq.target_amount AS assigned_target_amount,
                       uq.reward_amount AS assigned_reward_amount,
                       uq.is_completed, uq.claimed, uq.reset_date, uq.parameter
                FROM quests q
                JOIN user_quests uq ON uq.quest_id = q.id
                WHERE uq.user_id = ? AND q.active = 1 AND q.quest_type = ?
                ORDER BY CASE q.priority
                    WHEN 'CRITICAL' THEN 1
                    WHEN 'HIGH' THEN 2
                    WHEN 'AVERAGE' THEN 3
                    WHEN 'MEDIUM' THEN 3
                    WHEN 'LOW' THEN 4 ELSE 5 END, q.id
                """;
        List<QuestEntry> result = new ArrayList<>();
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new QuestEntry(mapQuest(resultSet), mapUserQuest(resultSet)));
                }
            }
            return result;
        } catch (SQLException exception) {
            throw databaseError("Could not load quest page.", exception);
        }
    }

    public boolean addProgress(int userId, int questId, int amount, QuestType questType
    ) {
        if (amount <= 0) {
            return false;
        }

        String sql = """
            UPDATE user_quests
            SET progress = MIN(progress + ?, target_amount),
                is_completed = CASE
                    WHEN progress + ? >= target_amount THEN 1
                    ELSE is_completed
                END
            WHERE user_id = ?
              AND quest_id = ?
              AND claimed = 0
              AND is_completed = 0
            """;

        return updateProgressAndCountCompletion(sql, amount, amount, userId, questId, questType);
    }

    public boolean setProgress(int userId, int questId, int progress, QuestType questType
    ) {
        String sql = """
            UPDATE user_quests
            SET progress = MIN(MAX(?, 0), target_amount),
                is_completed = CASE
                    WHEN ? >= target_amount THEN 1
                    ELSE 0
                END
            WHERE user_id = ?
              AND quest_id = ?
              AND claimed = 0
              AND is_completed = 0
            """;

        return updateProgressAndCountCompletion(sql, progress, progress, userId, questId, questType
        );
    }

    private boolean updateProgressAndCountCompletion(String progressSql, int firstValue,
            int secondValue, int userId, int questId, QuestType questType
    ) {
        try (Connection connection = DataBaseManager.getConnection()) {
            QuestDatabaseMigration.migrate(connection);
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement =
                             connection.prepareStatement(progressSql)) {

                    statement.setInt(1, firstValue);
                    statement.setInt(2, secondValue);
                    statement.setInt(3, userId);
                    statement.setInt(4, questId);
                    statement.executeUpdate();
                }

                boolean newlyCompleted = markCompletionCounted(connection, userId, questId
                );

                if (newlyCompleted) {
                    incrementStoredQuestCounter(connection, userId, questType
                    );
                }

                connection.commit();
                return newlyCompleted;

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }

        } catch (SQLException exception) {
            throw databaseError(
                    "Could not update quest progress.",
                    exception
            );
        }
    }

    private boolean markCompletionCounted(
            Connection connection,
            int userId,
            int questId
    ) throws SQLException {

        String sql = """
            UPDATE user_quests
            SET completion_counted = 1
            WHERE user_id = ?
              AND quest_id = ?
              AND is_completed = 1
              AND completion_counted = 0
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, questId);

            return statement.executeUpdate() == 1;
        }
    }

    private void incrementStoredQuestCounter(
            Connection connection,
            int userId,
            QuestType questType
    ) throws SQLException {

        String column = questType == QuestType.DAILY
                ? "quest_daily_num"
                : "quest_non_daily_num";

        String sql = """
            UPDATE users
            SET %s = COALESCE(%s, 0) + 1
            WHERE id = ?
            """.formatted(column, column);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "The quest owner's user record was not found."
                );
            }
        }
    }
    public boolean markClaimed(Connection connection, int userId, int questId)
            throws SQLException {
        String sql = """
                UPDATE user_quests SET claimed = 1
                WHERE user_id = ? AND quest_id = ?
                  AND is_completed = 1 AND claimed = 0
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, questId);
            return statement.executeUpdate() == 1;
        }
    }

    private Quest mapQuest(ResultSet resultSet) throws SQLException {
        QuestType type = QuestType.valueOf(resultSet.getString("quest_type"));
        Quest quest = switch (type) {
            case DAILY -> new DailyQuests(
                    resultSet.getString("name"), resultSet.getString("condition"),
                    QuestPriority.fromStorage(resultSet.getString("priority")),
                    QuestEventType.fromStorage(resultSet.getString("event_type")),
                    resultSet.getInt("target_amount"), resultSet.getInt("reward_amount"),
                    QuestRewardType.valueOf(resultSet.getString("reward_type")),
                    resultSet.getString("unlockable_id"),
                    resultSet.getString("parameter_options"));
            case MAIN -> new MainQuests(
                    resultSet.getString("name"), resultSet.getString("condition"),
                    QuestPriority.fromStorage(resultSet.getString("priority")),
                    QuestEventType.fromStorage(resultSet.getString("event_type")),
                    resultSet.getInt("target_amount"), resultSet.getInt("reward_amount"),
                    QuestRewardType.valueOf(resultSet.getString("reward_type")),
                    resultSet.getString("unlockable_id"),
                    resultSet.getString("parameter_options"));
            case EPIC -> new EpicQuests(
                    resultSet.getString("name"), resultSet.getString("condition"),
                    QuestPriority.fromStorage(resultSet.getString("priority")),
                    QuestEventType.fromStorage(resultSet.getString("event_type")),
                    resultSet.getInt("target_amount"), resultSet.getInt("reward_amount"),
                    QuestRewardType.valueOf(resultSet.getString("reward_type")),
                    resultSet.getString("unlockable_id"),
                    resultSet.getString("parameter_options"));
        };
        quest.setId(resultSet.getInt("id"));
        return quest;
    }

    private UserQuest mapUserQuest(ResultSet resultSet) throws SQLException {
        Date resetDate = resultSet.getDate("reset_date");
        return new UserQuest(
                resultSet.getInt("user_id"), resultSet.getInt("quest_id_value"),
                resultSet.getInt("progress"),
                resultSet.getInt("assigned_target_amount"),
                resultSet.getInt("assigned_reward_amount"),
                resultSet.getBoolean("is_completed"),
                resultSet.getBoolean("claimed"),
                resetDate == null ? null : resetDate.toLocalDate(),
                resultSet.getString("parameter"));
    }

    private UserQuest mapStandaloneUserQuest(ResultSet resultSet) throws SQLException {
        Date resetDate = resultSet.getDate("reset_date");
        return new UserQuest(
                resultSet.getInt("user_id"), resultSet.getInt("quest_id"),
                resultSet.getInt("progress"),
                nullablePositiveInt(resultSet, "target_amount"),
                nullableNonNegativeInt(resultSet, "reward_amount"),
                resultSet.getBoolean("is_completed"),
                resultSet.getBoolean("claimed"),
                resetDate == null ? null : resetDate.toLocalDate(),
                resultSet.getString("parameter"));
    }

    private int nullablePositiveInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? 0 : value;
    }

    private int nullableNonNegativeInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? -1 : value;
    }

    private IllegalStateException databaseError(String message, SQLException exception) {
        return new IllegalStateException(message, exception);
    }

    public record QuestEntry(Quest quest, UserQuest userQuest) {
    }
}
