package Data.database;
import models.Quests.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class QuestsRepository {
        public List<Quest> getAllQuests() {
            List<Quest> quests = new ArrayList<>();
            String sql = "SELECT * FROM quests";

            try (Connection conn = DataBaseManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    quests.add(mapRowToQuest(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return quests;
        }

        private Quest mapRowToQuest(ResultSet rs) throws SQLException {
            String name = rs.getString("name");
            String condition = rs.getString("condition");
            QuestPriority priority = QuestPriority.valueOf(rs.getString("priority"));
            QuestRewardType rewardType = QuestRewardType.valueOf(rs.getString("reward_type"));
            int rewardAmount = rs.getInt("reward_amount");
            QuestType type = QuestType.valueOf(rs.getString("quest_type"));

            // نمونه‌سازی بر اساس نوع کوئست
            return switch (type) {
                case DAILY -> new DailyQuests(name, condition, priority, null, rewardAmount, rewardType);
                case MAIN -> new MainQuests(name, condition, priority, rewardAmount, rewardType);
                case EPIC -> new EpicQuests(name, condition, priority, rewardAmount, rewardType);
            };
        }
    }

