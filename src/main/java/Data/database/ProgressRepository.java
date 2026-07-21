package Data.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProgressRepository {
    private static final String READ_PROGRESS_SQL = """
            SELECT chapter_index, level_index
            FROM user_progress
            WHERE user_id = ?
            """;
    private static final String RECORD_WIN_SQL = """
            UPDATE users
            SET games_played = COALESCE(games_played, 0) + 1,
                last_won_game = ?
            WHERE id = ?
            """;
    private static final String COMPLETE_LEVEL_SQL = """
            INSERT OR IGNORE INTO user_completed_levels(
                user_id, chapter_index, level_index
            ) VALUES (?, ?, ?)
            """;
    private static final String UPSERT_PROGRESS_SQL = """
            INSERT INTO user_progress(user_id, chapter_index, level_index)
            VALUES (?, ?, ?)
            ON CONFLICT(user_id)
            DO UPDATE SET
                chapter_index = excluded.chapter_index,
                level_index = excluded.level_index
            """;
    private static final String READ_GAMES_PLAYED_SQL =
            "SELECT games_played FROM users WHERE id = ?";

    public record AdventureWinResult(
            boolean saved,
            int gamesPlayed,
            int oldChapter,
            int oldLevel,
            int newChapter,
            int newLevel,
            boolean progressAdvanced
    ) {
    }

    private record ProgressPosition(int chapter, int level) {
    }

    private record ProgressChange(
            ProgressPosition position,
            boolean advanced
    ) {
    }

    public int[] getCurrentProgress(int userId) {
        String sql = """
                SELECT chapter_index, level_index
                FROM user_progress
                WHERE user_id = ?
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new int[]{
                            resultSet.getInt("chapter_index"),
                            resultSet.getInt("level_index")
                    };
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return new int[]{1, 1};
    }

    public boolean saveProgress(int userId, int newChapter, int newLevel) {
        String sql = """
                INSERT INTO user_progress(user_id, chapter_index, level_index)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id)
                DO UPDATE SET
                    chapter_index = excluded.chapter_index,
                    level_index = excluded.level_index
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, newChapter);
            statement.setInt(3, newLevel);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public AdventureWinResult recordAdventureWin(
            int userId,
            int completedChapter,
            int completedLevel,
            Integer candidateChapter,
            Integer candidateLevel
    ) {

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return recordAdventureWinInTransaction(
                        connection,
                        userId,
                        completedChapter,
                        completedLevel,
                        candidateChapter,
                        candidateLevel
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return adventureWinFailure();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return adventureWinFailure();
        }
    }

    private AdventureWinResult recordAdventureWinInTransaction(
            Connection connection,
            int userId,
            int completedChapter,
            int completedLevel,
            Integer candidateChapter,
            Integer candidateLevel
    ) throws SQLException {
        ProgressPosition oldPosition = readProgressPosition(
                connection,
                userId
        );
        recordWinOnUser(
                connection,
                userId,
                completedChapter,
                completedLevel
        );
        markLevelCompleted(
                connection,
                userId,
                completedChapter,
                completedLevel
        );
        ProgressChange change = advanceProgressIfLater(
                connection,
                userId,
                oldPosition,
                candidateChapter,
                candidateLevel
        );
        int gamesPlayed = readGamesPlayed(connection, userId);
        connection.commit();
        return buildAdventureWinResult(
                gamesPlayed,
                oldPosition,
                change
        );
    }

    private ProgressPosition readProgressPosition(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                READ_PROGRESS_SQL
        )) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new ProgressPosition(1, 1);
                }
                return new ProgressPosition(
                        resultSet.getInt("chapter_index"),
                        resultSet.getInt("level_index")
                );
                        }
                    }
                }

    private void recordWinOnUser(
            Connection connection,
            int userId,
            int completedChapter,
            int completedLevel
    ) throws SQLException {
                String lastWonGame = "Chapter " + completedChapter
                        + " Level " + completedLevel;
        try (PreparedStatement statement = connection.prepareStatement(
                RECORD_WIN_SQL
        )) {
                    statement.setString(1, lastWonGame);
                    statement.setInt(2, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not record the Adventure win.");
                    }
                }
    }

    private void markLevelCompleted(
            Connection connection,
            int userId,
            int completedChapter,
            int completedLevel
    ) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                COMPLETE_LEVEL_SQL
                )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, completedChapter);
                    statement.setInt(3, completedLevel);
                    statement.executeUpdate();
                }
    }

    private ProgressChange advanceProgressIfLater(
            Connection connection,
            int userId,
            ProgressPosition oldPosition,
            Integer candidateChapter,
            Integer candidateLevel
    ) throws SQLException {
        if (candidateChapter == null || candidateLevel == null
                || !isLater(
                        candidateChapter,
                        candidateLevel,
                oldPosition.chapter(),
                oldPosition.level()
                )) {
            return new ProgressChange(oldPosition, false);
        }
        ProgressPosition newPosition = new ProgressPosition(
                candidateChapter,
                candidateLevel
        );
        saveProgressPosition(connection, userId, newPosition);
        return new ProgressChange(newPosition, true);
    }

    private void saveProgressPosition(
            Connection connection,
            int userId,
            ProgressPosition position
    ) throws SQLException {
                    try (PreparedStatement statement = connection.prepareStatement(
                UPSERT_PROGRESS_SQL
                    )) {
                        statement.setInt(1, userId);
            statement.setInt(2, position.chapter());
            statement.setInt(3, position.level());
                        statement.executeUpdate();
                    }
                }

    private int readGamesPlayed(
            Connection connection,
            int userId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                READ_GAMES_PLAYED_SQL
        )) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Could not read the games-played counter.");
                        }
                return resultSet.getInt("games_played");
            }
                    }
                }

    private AdventureWinResult buildAdventureWinResult(
            int gamesPlayed,
            ProgressPosition oldPosition,
            ProgressChange change
    ) {
                return new AdventureWinResult(
                        true,
                        gamesPlayed,
                oldPosition.chapter(),
                oldPosition.level(),
                change.position().chapter(),
                change.position().level(),
                change.advanced()
                );
    }

    private AdventureWinResult adventureWinFailure() {
                return new AdventureWinResult(
                        false, 0, 1, 1, 1, 1, false
                );

    }

    public void saveLevelScore(
            int userId,
            int chapter,
            int level,
            int score,
            int stars
    ) {
        String sql = """
                INSERT OR REPLACE INTO user_scores(
                    user_id, chapter_index, level_index, score, stars
                ) VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, chapter);
            statement.setInt(3, level);
            statement.setInt(4, score);
            statement.setInt(5, stars);
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private boolean isLater(int candidateChapter, int candidateLevel, int currentChapter, int currentLevel) {
        return candidateChapter > currentChapter || candidateChapter == currentChapter && candidateLevel > currentLevel;
    }
}
