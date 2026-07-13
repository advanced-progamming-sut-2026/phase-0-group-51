package Data.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlantBoostRepository {
    public static boolean hasBoost(int userId, int plantId) {
        String sql = """
                SELECT 1
                FROM plant_boosts
                WHERE user_id = ?
                  AND plant_id = ?
                """;

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, plantId);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addBoost(int userId, int plantId) {
        String sql = """
                INSERT OR IGNORE INTO plant_boosts(user_id, plant_id)
                VALUES (?, ?)
                """;

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, plantId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void consumeBoost(int userId, int plantId) {
        String sql = """
                DELETE FROM plant_boosts
                WHERE user_id = ?
                  AND plant_id = ?
                """;

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, plantId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
