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
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String condition = rs.getString("condition");
            QuestPriority priority = QuestPriority.valueOf(rs.getString("priority"));
            QuestRewardType rewardType = QuestRewardType.valueOf(rs.getString("reward_type"));
            int targetAmount = rs.getInt("target_amount");
            int rewardAmount = rs.getInt("reward_amount");
            QuestType type = QuestType.valueOf(rs.getString("quest_type"));
            String unlockableId = rs.getString("unlockable_id");

            Quest quest =  switch (type) {
                case DAILY -> new DailyQuests(name, condition, priority, unlockableId,
                        targetAmount, rewardAmount, rewardType);
                case MAIN -> new MainQuests(name, condition, priority,unlockableId,
                        targetAmount, rewardAmount, rewardType);
                case EPIC -> new EpicQuests(name, condition, priority,unlockableId,
                        targetAmount, rewardAmount, rewardType);
            };
            quest.setId(id);
            return quest;
        }
    }

