package Data.loader;
import Data.database.DataBaseManager;
import models.Quests.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
    public class QuestLoader {
        public static void loadQuestsToDatabase() {
            // نام فایل در پوشه resources
            String csvFile = "/Quests.csv";

            String sql = "INSERT OR IGNORE INTO quests (name, condition, priority, reward_amount, reward_type, quest_type) VALUES (?,?,?,?,?,?)";

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(QuestLoader.class.getResourceAsStream(csvFile))));
                 Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                String line;
                br.readLine(); // رد کردن خط اول (هدر)

                while ((line = br.readLine()) != null) {
                    String[] data = line.split(","); // فرض: name, condition, priority, rewardAmount, rewardType, questType

                    pstmt.setString(1, data[0].trim()); // name
                    pstmt.setString(2, data[1].trim()); // condition
                    pstmt.setString(3, data[2].trim()); // priority (Enum string)
                    pstmt.setInt(4, Integer.parseInt(data[3].trim())); // rewardAmount
                    pstmt.setString(5, data[4].trim()); // rewardType (Enum string)
                    pstmt.setString(6, data[5].trim()); // questType (Enum string)

                    pstmt.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

