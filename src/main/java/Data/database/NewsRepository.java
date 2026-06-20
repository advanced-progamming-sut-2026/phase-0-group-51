package Data.database;
import models.items.News;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

    public class NewsRepository {
        public List<News> getNewsForUser(int userId) {
            List<News> newsList = new ArrayList<>();
            String sql = "SELECT n.id, n.content, un.is_read " +
                    "FROM news n " +
                    "JOIN user_news un ON n.id = un.news_id " +
                    "WHERE un.user_id = ?";

            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    newsList.add(new News(
                            rs.getInt("id"),
                            userId,
                            rs.getString("content"),
                            rs.getInt("is_read") == 1
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return newsList;
        }
    public void markAsRead(int userId, int newsId) {
            String sql = "UPDATE user_news SET is_read = 1 WHERE user_id = ? AND news_id = ?";
            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, userId);
                pstmt.setInt(2, newsId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void createNewGlobalNews(String content) {
            String sqlNews = "INSERT INTO news (content) VALUES (?)";
            String sqlUserNews = "INSERT INTO user_news (user_id, news_id) SELECT id, ? FROM users";

            try (Connection conn = DataBaseManager.getConnection()) {
                conn.setAutoCommit(false);
                PreparedStatement pstmt = conn.prepareStatement(sqlNews, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, content);
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newsId = rs.getInt(1);
                    PreparedStatement pstmt2 = conn.prepareStatement(sqlUserNews);
                    pstmt2.setInt(1, newsId);
                    pstmt2.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

