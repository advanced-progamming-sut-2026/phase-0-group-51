package data.database;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlantRepository {

    public static Map<Integer, Integer> loadPlantLevels(int userId) {
        String sql = "SELECT plant_id, plant_level FROM user_plants WHERE user_id = ?";
        Map<Integer, Integer> map = new HashMap<>();
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                map.put(rs.getInt("plant_id"), rs.getInt("plant_level"));
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    public static void savePlantLevel(int userId, int plantId, int level) {
        String sql = """
            INSERT INTO user_plants (user_id, plant_id, plant_level)
            VALUES (?, ?, ?)
            ON CONFLICT(user_id, plant_id)
            DO UPDATE SET plant_level = excluded.plant_level
            """;
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, plantId);
            stmt.setInt(3, level);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static Set<Integer> loadUnlockedPlants(int userId) {
        String sql = "SELECT plant_id FROM user_unlocked_plants WHERE user_id = ?";
        Set<Integer> unlocked = new HashSet<>();
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) unlocked.add(rs.getInt("plant_id"));
        } catch (SQLException e) { e.printStackTrace(); }
        return unlocked;
    }

    public static void unlockPlant(int userId, int plantId) {
        String sql = """
            INSERT OR IGNORE INTO user_unlocked_plants (user_id, plant_id)
            VALUES (?, ?)
            """;
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, plantId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    public static int getSeedPackets(int userId,int plantId){

        String sql =
                "SELECT seed_packets FROM user_plants WHERE user_id=? AND plant_id=?";

        try(Connection conn = DataBaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1,userId);
            stmt.setInt(2,plantId);

            ResultSet rs = stmt.executeQuery();

            if(rs.next())
                return rs.getInt("seed_packets");

        }catch(SQLException e){
            e.printStackTrace();
        }

        return 0;
    }
    public static void addSeedPackets(int userId,
                                      int plantId,
                                      int amount){

        String sql = """
        INSERT INTO user_plants(user_id,plant_id,plant_level,seed_packets)
        VALUES(?,?,1,?)
        ON CONFLICT(user_id,plant_id)
        DO UPDATE SET
        seed_packets = seed_packets + excluded.seed_packets
        """;

        try(Connection conn = DataBaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1,userId);
            stmt.setInt(2,plantId);
            stmt.setInt(3,amount);

            stmt.executeUpdate();

        }catch(SQLException e){
            e.printStackTrace();
        }

    }
    public static Map<Integer,Integer> loadSeedPackets(int userId){

        Map<Integer,Integer> map = new HashMap<>();

        String sql =
                "SELECT plant_id,seed_packets FROM user_plants WHERE user_id=?";

        try(Connection conn = DataBaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1,userId);

            ResultSet rs = stmt.executeQuery();

            while(rs.next()){

                map.put(
                        rs.getInt("plant_id"),
                        rs.getInt("seed_packets")
                );

            }

        }catch(SQLException e){
            e.printStackTrace();
        }

        return map;
    }
}
