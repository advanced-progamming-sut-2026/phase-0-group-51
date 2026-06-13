package Data.database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProgressRepository {
        public int[] getCurrentProgress(int userId) {
            String sql = "SELECT chapter_index, level_index FROM user_progress WHERE user_id = ?";

            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int chapter = rs.getInt("chapter_index");
                    int level = rs.getInt("level_index");
                    return new int[]{chapter, level};
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new int[]{1, 1};
        }

        public boolean saveProgress(int userId, int newChapter, int newLevel) {
            String sql = "UPDATE user_progress SET chapter_index = ?, level_index = ? WHERE user_id = ?";

            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, newChapter);
                pstmt.setInt(2, newLevel);
                pstmt.setInt(3, userId);

                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        public void saveLevelScore(int userId, int chapter, int level, int score, int stars) {
            String sql = "INSERT OR REPLACE INTO user_scores (user_id, chapter_index, level_index, score, stars) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, userId);
                pstmt.setInt(2, chapter);
                pstmt.setInt(3, level);
                pstmt.setInt(4, score);
                pstmt.setInt(5, stars);

                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

