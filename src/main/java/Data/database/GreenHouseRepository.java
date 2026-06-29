package Data.database;

import models.GreenHouse.FlowerPot;
import models.GreenHouse.GreenHouse;

import java.sql.*;
import java.time.LocalDateTime;

public class GreenHouseRepository {
    public static void createForUser(int userId) {
        String sql = """
        INSERT INTO greenhouse_pots
        (user_id, row, "column", unlocked, plant_id, planted_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int row = 1; row <= 4; row++) {
                for (int column = 1; column <= 5; column++) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, row);
                    pstmt.setInt(3, column);
                    pstmt.setBoolean(4, row == 1);
                    pstmt.setNull(5, Types.INTEGER);
                    pstmt.setNull(6, Types.VARCHAR);
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static GreenHouse load(int userId) {
        GreenHouse greenHouse = new GreenHouse();
        String sql = """
            SELECT *
            FROM greenhouse_pots
            WHERE user_id = ?
            ORDER BY row, "column"
            """;
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int row = rs.getInt("row");
                int column = rs.getInt("column");
                FlowerPot pot = greenHouse.getPot(row, column);
                pot.setUnlocked(rs.getBoolean("unlocked"));
                int plantId = rs.getInt("plant_id");
                if (rs.wasNull()) {
                    pot.setPlantId(null);
                } else {
                    pot.setPlantId(plantId);
                }
                String plantedAt = rs.getString("planted_at");
                if (plantedAt != null) {
                    pot.setPlantedAt(LocalDateTime.parse(plantedAt));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return greenHouse;
    }

    public static void updatePot(int userId, FlowerPot pot) {
        String sql = """
            UPDATE greenhouse_pots
            SET
                unlocked = ?,
                plant_id = ?,
                planted_at = ?
            WHERE
                user_id = ?
                AND row = ?
                AND "column" = ?
            """;
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, pot.isUnlocked());
            if (pot.getPlantId() == null) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, pot.getPlantId());
            }
            if (pot.getPlantedAt() == null) {
                pstmt.setNull(3, Types.VARCHAR);
            } else {
                pstmt.setString(3, pot.getPlantedAt().toString());
            }
            pstmt.setInt(4, userId);
            pstmt.setInt(5, pot.getRow());
            pstmt.setInt(6, pot.getColumn());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void unlockPot(int userId, int row, int column) {
        String sql = """
            UPDATE greenhouse_pots
            SET unlocked = 1
            WHERE user_id = ?
              AND row = ?
              AND "column" = ?
            """;
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, row);
            pstmt.setInt(3, column);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
