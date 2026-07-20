package Data.database;

import models.User;
import models.meowPoint.ScoringRules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

public class ScoringRepository {
    private static final int SCORING_CHAPTER_INDEX = -1;

    public int saveDailyBest(User user, LocalDate date, int score, boolean won) {
        if (user == null) {throw new IllegalArgumentException("User is required.");}
        LocalDate safeDate = Objects.requireNonNull(date, "Date is required.");
        int safeScore = Math.max(0, score);
        int dateKey = ScoringRules.dateKey(safeDate);
        String saveScoreSql = """
                INSERT INTO user_scores
                    (user_id, chapter_index, level_index, score, stars)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(user_id, chapter_index, level_index)
                DO UPDATE SET
                    score = MAX(user_scores.score, excluded.score),
                    stars = MAX(user_scores.stars, excluded.stars)
                """;
        String updateUserSql = """
                UPDATE users
                SET most_meow_point = MAX(COALESCE(most_meow_point, 0), ?),
                    max_point = MAX(COALESCE(max_point, 0), ?),
                    games_played = COALESCE(games_played, 0) + 1
                WHERE id = ?
                """;
        try (Connection connection = DataBaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement save = connection.prepareStatement(saveScoreSql);
                 PreparedStatement update = connection.prepareStatement(updateUserSql)) {
                save.setInt(1, user.getId());
                save.setInt(2, SCORING_CHAPTER_INDEX);
                save.setInt(3, dateKey);
                save.setInt(4, safeScore);
                save.setInt(5, won ? 1 : 0);
                save.executeUpdate();
                update.setInt(1, safeScore);
                update.setInt(2, safeScore);
                update.setInt(3, user.getId());
                update.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the scoring-game result.", exception);}
        user.setMostMeowPoint(Math.max(user.getMostMeowPoint(), safeScore));
        user.setMaxPoint(Math.max(user.getMaxPoint(), safeScore));
        user.setGamesPlayed(user.getGamesPlayed() + 1);
        return getDailyBest(user.getId(), safeDate);
    }
    public int getDailyBest(int userId, LocalDate date) {
        LocalDate safeDate = Objects.requireNonNull(date, "Date is required.");
        String sql = """
                SELECT score
                FROM user_scores
                WHERE user_id = ? AND chapter_index = ? AND level_index = ?
                """;
        try (Connection connection = DataBaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, SCORING_CHAPTER_INDEX);
            statement.setInt(3, ScoringRules.dateKey(safeDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("score") : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load today's scoring-game result.", exception);
        }
    }
}
