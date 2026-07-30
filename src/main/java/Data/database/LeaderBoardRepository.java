package Data.database;

import models.leaderBoard.LeaderBoard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LeaderBoardRepository {
    public List<LeaderBoard> getAllEntries() {
        String sql = """
                SELECT
                    u.username,
                    COALESCE((
                        SELECT 'Chapter ' || completed.chapter_index
                               || ' Level ' || completed.level_index
                        FROM user_completed_levels completed
                        WHERE completed.user_id = u.id
                        ORDER BY completed.chapter_index DESC,
                                 completed.level_index DESC
                        LIMIT 1
                    ), 'None') AS last_completed,
                    COALESCE(u.mini_games_played, 0) AS minigames_completed,
                    COALESCE(u.quest_daily_num, 0) AS daily_quests_completed,
                    COALESCE(u.quest_non_daily_num, 0) AS non_daily_quests_completed,
                    MAX(
                        COALESCE(u.most_meow_point, 0),
                        COALESCE(u.max_point, 0),
                        COALESCE((
                            SELECT MAX(s.score)
                            FROM user_scores s
                            WHERE s.user_id = u.id
                              AND s.chapter_index = -1
                        ), 0)
                    ) AS highest_score
                FROM users u
                ORDER BY u.username COLLATE NOCASE
                """;

        List<LeaderBoard> entries = new ArrayList<>();

        try (
                Connection connection = DataBaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                entries.add(LeaderBoard.fromDatabase(
                        resultSet.getString("username"),
                        resultSet.getString("last_completed"),
                        resultSet.getInt("minigames_completed"),
                        resultSet.getInt("daily_quests_completed"),
                        resultSet.getInt("non_daily_quests_completed"),
                        resultSet.getInt("highest_score")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not load the leaderboard.",
                    exception
            );
        }

        return entries;
    }
}
