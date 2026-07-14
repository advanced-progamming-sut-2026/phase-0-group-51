package data.database;

import models.shop.DailyOffer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DailyOfferRepository {
    public DailyOffer getOffer(int userId) {

        String sql = "SELECT * FROM daily_offer WHERE user_id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                int plantId = rs.getInt("plant_id");
                LocalDate date = LocalDate.parse(rs.getString("offer_date"));
                boolean purchased = rs.getInt("purchased") == 1;

                return new DailyOffer(
                        plantId,
                        date,
                        purchased
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void saveOffer(int userId, DailyOffer offer) {

        String sql = """
                INSERT INTO daily_offer(user_id, plant_id, offer_date, purchased)
                VALUES(?,?,?,?)
                ON CONFLICT(user_id)
                DO UPDATE SET
                    plant_id = excluded.plant_id,
                    offer_date = excluded.offer_date,
                    purchased = excluded.purchased
                """;

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, offer.getPlantId());
            stmt.setString(3, offer.getDate().toString());
            stmt.setInt(4, offer.isPurchased() ? 1 : 0);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePurchased(int userId, boolean purchased) {

        String sql =
                "UPDATE daily_offer SET purchased = ? WHERE user_id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, purchased ? 1 : 0);
            stmt.setInt(2, userId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteOffer(int userId) {

        String sql = "DELETE FROM daily_offer WHERE user_id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public DailyOffer getOrCreateDailyOffer(int userId) {

        DailyOffer offer = getOffer(userId);

        if (offer != null && !offer.isFinished()) {
            return offer;
        }

        Set<Integer> unlocked =
                PlantRepository.loadUnlockedPlants(userId);

        if (unlocked.isEmpty())
            return null;

        List<Integer> ids = new ArrayList<>(unlocked);

        Random random = new Random();
        int plantId = ids.get(random.nextInt(ids.size()));
        DailyOffer newOffer = new DailyOffer(plantId);
        saveOffer(userId, newOffer);
        return newOffer;
    }
}
