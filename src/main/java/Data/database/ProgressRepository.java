package Data.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProgressRepository {
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
        String progressSql = """
                SELECT chapter_index, level_index
                FROM user_progress
                WHERE user_id = ?
                """;
        String updateUserSql = """
                UPDATE users
                SET games_played = COALESCE(games_played, 0) + 1,
                    last_won_game = ?
                WHERE id = ?
                """;
        String completeLevelSql = """
                INSERT OR IGNORE INTO user_completed_levels(
                    user_id, chapter_index, level_index
                ) VALUES (?, ?, ?)
                """;
        String upsertProgressSql = """
                INSERT INTO user_progress(user_id, chapter_index, level_index)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id)
                DO UPDATE SET
                    chapter_index = excluded.chapter_index,
                    level_index = excluded.level_index
                """;
        String gamesSql = "SELECT games_played FROM users WHERE id = ?";

        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int oldChapter = 1;
                int oldLevel = 1;
                try (PreparedStatement statement = connection.prepareStatement(progressSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            oldChapter = resultSet.getInt("chapter_index");
                            oldLevel = resultSet.getInt("level_index");
                        }
                    }
                }

                String lastWonGame = "Chapter " + completedChapter
                        + " Level " + completedLevel;
                try (PreparedStatement statement = connection.prepareStatement(updateUserSql)) {
                    statement.setString(1, lastWonGame);
                    statement.setInt(2, userId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not record the Adventure win.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        completeLevelSql
                )) {
                    statement.setInt(1, userId);
                    statement.setInt(2, completedChapter);
                    statement.setInt(3, completedLevel);
                    statement.executeUpdate();
                }

                int newChapter = oldChapter;
                int newLevel = oldLevel;
                boolean progressAdvanced = false;
                if (candidateChapter != null && candidateLevel != null
                        && isLater(
                        candidateChapter,
                        candidateLevel,
                        oldChapter,
                        oldLevel
                )) {
                    newChapter = candidateChapter;
                    newLevel = candidateLevel;
                    progressAdvanced = true;
                    try (PreparedStatement statement = connection.prepareStatement(
                            upsertProgressSql
                    )) {
                        statement.setInt(1, userId);
                        statement.setInt(2, newChapter);
                        statement.setInt(3, newLevel);
                        statement.executeUpdate();
                    }
                }

                int gamesPlayed;
                try (PreparedStatement statement = connection.prepareStatement(gamesSql)) {
                    statement.setInt(1, userId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Could not read the games-played counter.");
                        }
                        gamesPlayed = resultSet.getInt("games_played");
                    }
                }

                connection.commit();
                return new AdventureWinResult(
                        true,
                        gamesPlayed,
                        oldChapter,
                        oldLevel,
                        newChapter,
                        newLevel,
                        progressAdvanced
                );
            } catch (SQLException exception) {
                connection.rollback();
                exception.printStackTrace();
                return new AdventureWinResult(
                        false, 0, 1, 1, 1, 1, false
                );
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new AdventureWinResult(
                    false, 0, 1, 1, 1, 1, false
            );
        }
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
