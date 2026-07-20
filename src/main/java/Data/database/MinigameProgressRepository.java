package Data.database;

import models.minigames.MinigameType;

import java.sql.*;

public class MinigameProgressRepository {
    private static final int FIRST_STAGE = 1;
    private static final int LAST_STAGE = 3;

    private static final String SELECT_PROGRESS_SQL = """
            SELECT highest_unlocked_stage, highest_completed_stage
            FROM user_minigame_progress
            WHERE user_id = ? AND minigame_type = ?
            """;

    private static final String UPSERT_PROGRESS_SQL = """
            INSERT INTO user_minigame_progress (
                user_id,
                minigame_type,
                highest_unlocked_stage,
                highest_completed_stage
            ) VALUES (?, ?, ?, ?)
            ON CONFLICT(user_id, minigame_type)
            DO UPDATE SET
                highest_unlocked_stage =
                    excluded.highest_unlocked_stage,
                highest_completed_stage =
                    excluded.highest_completed_stage
            """;

    private static final String INCREMENT_MINIGAME_COUNTER_SQL = """
            UPDATE users
            SET mini_games_played =
                COALESCE(mini_games_played, 0) + 1
            WHERE id = ?
            """;

    private static final String READ_MINIGAME_COUNTER_SQL = """
            SELECT mini_games_played
            FROM users
            WHERE id = ?
            """;

    public record Progress(
            int highestUnlockedStage,
            int highestCompletedStage
    ) {
    }

    public record Completion(
            boolean saved,
            boolean newlyCompletedStage,
            int newlyUnlockedStage,
            boolean minigameNewlyCompleted,
            int miniGamesPlayed
    ) {
    }

    private record CompletionUpdate(
            boolean newlyCompletedStage,
            int newCompletedStage,
            int newUnlockedStage,
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
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                        ON DELETE CASCADE
                )
                """;

        try (Connection connection = DataBaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public Progress getProgress(
            int userId,
            MinigameType type
    ) {
        try (Connection connection = DataBaseManager.getConnection()) {
            return readProgress(connection, userId, type);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new Progress(FIRST_STAGE, 0);
        }
    }

    public boolean isStageUnlocked(
            int userId,
            MinigameType type,
            int stageNumber
    ) {
        if (!isValidStage(stageNumber)) {
            return false;
        }

        Progress progress = getProgress(userId, type);

        return stageNumber <= progress.highestUnlockedStage();
    }

    public Completion completeStage(
            int userId,
            MinigameType type,
            int stageNumber
    ) {
        if (!isValidStage(stageNumber)) {
            return completionFailure();
        }

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                return completeStageInTransaction(
                        connection,
                        userId,
                        type,
                        stageNumber
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return completionFailure();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return completionFailure();
        }
    }

    private Completion completeStageInTransaction(
            Connection connection,
            int userId,
            MinigameType type,
            int stageNumber
    ) throws SQLException {
        Progress oldProgress =
                readProgress(connection, userId, type);

        if (stageNumber > oldProgress.highestUnlockedStage()) {
            connection.rollback();
            return completionFailure();
        }

        CompletionUpdate update =
                calculateCompletionUpdate(
                        oldProgress,
                        stageNumber
                );

        saveProgress(
                connection,
                userId,
                type,
                update
        );

        int miniGamesPlayed =
                readUpdatedMinigameCount(
                        connection,
                        userId,
                        update
                );

        connection.commit();

        return createSuccessfulCompletion(
                update,
                miniGamesPlayed
        );
    }

    private Progress readProgress(
            Connection connection,
            int userId,
            MinigameType type
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             SELECT_PROGRESS_SQL
                     )) {
            statement.setInt(1, userId);
            statement.setString(2, type.name());

            try (ResultSet resultSet =
                         statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new Progress(FIRST_STAGE, 0);
                }

                return new Progress(
                        resultSet.getInt(
                                "highest_unlocked_stage"
                        ),
                        resultSet.getInt(
                                "highest_completed_stage"
                        )
                );
            }
        }
    }

    private CompletionUpdate calculateCompletionUpdate(
            Progress oldProgress,
            int stageNumber
    ) {
        boolean newlyCompleted =
                stageNumber
                        > oldProgress.highestCompletedStage();

        int newCompleted = Math.max(
                oldProgress.highestCompletedStage(),
                stageNumber
        );

        int newUnlocked = calculateNewUnlockedStage(
                oldProgress,
                stageNumber,
                newlyCompleted
        );

        int newlyUnlocked =
                newUnlocked
                        > oldProgress.highestUnlockedStage()
                        ? newUnlocked
                        : 0;

        boolean minigameNewlyCompleted =
                stageNumber == LAST_STAGE
                        && oldProgress.highestCompletedStage()
                        < LAST_STAGE;

        return new CompletionUpdate(
                newlyCompleted,
                newCompleted,
                newUnlocked,
                newlyUnlocked,
                minigameNewlyCompleted
        );
    }

    private int calculateNewUnlockedStage(
            Progress oldProgress,
            int stageNumber,
            boolean newlyCompleted
    ) {
        int newUnlocked =
                oldProgress.highestUnlockedStage();

        if (newlyCompleted && stageNumber < LAST_STAGE) {
            newUnlocked = Math.max(
                    newUnlocked,
                    stageNumber + 1
            );
        }

        return newUnlocked;
    }

    private void saveProgress(
            Connection connection,
            int userId,
            MinigameType type,
            CompletionUpdate update
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             UPSERT_PROGRESS_SQL
                     )) {
            statement.setInt(1, userId);
            statement.setString(2, type.name());
            statement.setInt(
                    3,
                    update.newUnlockedStage()
            );
            statement.setInt(
                    4,
                    update.newCompletedStage()
            );
            statement.executeUpdate();
        }
    }

    private int readUpdatedMinigameCount(
            Connection connection,
            int userId,
            CompletionUpdate update
    ) throws SQLException {
        if (!update.minigameNewlyCompleted()) {
            return -1;
        }

        return incrementAndReadMinigameCounter(
                connection,
                userId
        );
    }

    private int incrementAndReadMinigameCounter(
            Connection connection,
            int userId
    ) throws SQLException {
        incrementMinigameCounter(connection, userId);

        return readMinigameCounter(connection, userId);
    }

    private void incrementMinigameCounter(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             INCREMENT_MINIGAME_COUNTER_SQL
                     )) {
            statement.setInt(1, userId);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Could not update the "
                                + "completed-minigame counter."
                );
            }
        }
    }

    private int readMinigameCounter(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             READ_MINIGAME_COUNTER_SQL
                     )) {
            statement.setInt(1, userId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Could not read the "
                                    + "completed-minigame counter."
                    );
                }

                return resultSet.getInt(
                        "mini_games_played"
                );
            }
        }
    }

    private Completion createSuccessfulCompletion(
            CompletionUpdate update,
            int miniGamesPlayed
    ) {
        return new Completion(
                true,
                update.newlyCompletedStage(),
                update.newlyUnlockedStage(),
                update.minigameNewlyCompleted(),
                miniGamesPlayed
        );
    }

    private Completion completionFailure() {
        return new Completion(
                false,
                false,
                0,
                false,
                -1
        );
    }

    private boolean isValidStage(int stageNumber) {
        return stageNumber >= FIRST_STAGE
                && stageNumber <= LAST_STAGE;
    }
    }

