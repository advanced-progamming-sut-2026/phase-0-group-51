package Data.database;

import models.minigames.MinigameType;

import java.sql.*;

public class MinigameProgressRepository {
    public record Progress(int highestUnlockedStage, int highestCompletedStage) {
    }

    public record Completion(
            boolean saved,
            boolean newlyCompletedStage,
            int newlyUnlockedStage,
            boolean minigameNewlyCompleted
    ) {
    }

    public MinigameProgressRepository() {
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS user_minigame_progress (
                    user_id INTEGER NOT NULL,
                    minigame_type TEXT NOT NULL,
                    highest_unlocked_stage INTEGER NOT NULL DEFAULT 1,
                    highest_completed_stage INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (user_id, minigame_type),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """;

        try (Connection connection = DataBaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public Progress getProgress(int userId, MinigameType type) {
        String sql = """
                SELECT highest_unlocked_stage, highest_completed_stage
                FROM user_minigame_progress
                WHERE user_id = ? AND minigame_type = ?
                """;

        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, type.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Progress(
                            resultSet.getInt("highest_unlocked_stage"),
                            resultSet.getInt("highest_completed_stage")
                    );
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        // No row means a fresh minigame: stage 1 is open and nothing is completed.
        return new Progress(1, 0);
    }

    public boolean isStageUnlocked(int userId, MinigameType type, int stageNumber) {
        if (stageNumber < 1 || stageNumber > 3) {
            return false;
        }
        return stageNumber <= getProgress(userId, type).highestUnlockedStage();
    }

    public Completion completeStage(int userId, MinigameType type, int stageNumber) {
        if (stageNumber < 1 || stageNumber > 3) {
            return new Completion(false, false, 0, false);
        }

        String selectSql = """
                SELECT highest_unlocked_stage, highest_completed_stage
                FROM user_minigame_progress
                WHERE user_id = ? AND minigame_type = ?
                """;
        String upsertSql = """
                INSERT INTO user_minigame_progress (
                    user_id,
                    minigame_type,
                    highest_unlocked_stage,
                    highest_completed_stage
                ) VALUES (?, ?, ?, ?)
                ON CONFLICT(user_id, minigame_type)
                DO UPDATE SET
                    highest_unlocked_stage = excluded.highest_unlocked_stage,
                    highest_completed_stage = excluded.highest_completed_stage
                """;

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int oldUnlocked = 1;
                int oldCompleted = 0;

                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setInt(1, userId);
                    select.setString(2, type.name());
                    try (ResultSet resultSet = select.executeQuery()) {
                        if (resultSet.next()) {
                            oldUnlocked = resultSet.getInt("highest_unlocked_stage");
                            oldCompleted = resultSet.getInt("highest_completed_stage");
                        }
                    }
                }

                if (stageNumber > oldUnlocked) {
                    connection.rollback();
                    return new Completion(false, false, 0, false);
                }

                boolean newlyCompletedStage = stageNumber > oldCompleted;
                int newCompleted = Math.max(oldCompleted, stageNumber);
                int newUnlocked = oldUnlocked;

                if (newlyCompletedStage && stageNumber < 3) {
                    newUnlocked = Math.max(oldUnlocked, stageNumber + 1);
                }

                int newlyUnlockedStage = newUnlocked > oldUnlocked ? newUnlocked : 0;
                boolean minigameNewlyCompleted = stageNumber == 3 && oldCompleted < 3;

                try (PreparedStatement upsert = connection.prepareStatement(upsertSql)) {
                    upsert.setInt(1, userId);
                    upsert.setString(2, type.name());
                    upsert.setInt(3, newUnlocked);
                    upsert.setInt(4, newCompleted);
                    upsert.executeUpdate();
                }

                connection.commit();
                return new Completion(
                        true,
                        newlyCompletedStage,
                        newlyUnlockedStage,
                        minigameNewlyCompleted
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return new Completion(false, false, 0, false);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new Completion(false, false, 0, false);
        }
    }
}
