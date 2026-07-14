package Data.loader;
import Data.database.DataBaseManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
    public class QuestLoader {
        public static void loadQuestsToDatabase() {
            String csvFile = "/Quests.csv";

            String sql = "INSERT OR IGNORE INTO quests (name, condition, priority,target_amount, reward_amount," +
                    " reward_type, quest_type,unlockable_id) VALUES (?,?,?,?,?,?,?)";

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(QuestLoader.class.getResourceAsStream(csvFile))));
                 Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                String line;
                br.readLine();

                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");

                    pstmt.setString(1, data[0].trim());
                    pstmt.setString(2, data[1].trim());
                    pstmt.setString(3, data[2].trim());
                    pstmt.setInt(4, Integer.parseInt(data[3].trim()));
                    pstmt.setInt(5, Integer.parseInt(data[4].trim()));
                    pstmt.setString(6, data[5].trim());
                    pstmt.setString(7, data[6].trim());
                    String unlockableId = (data.length>7 && !data[7].trim().equals("NONE"))? data[7].trim(): null;
                    pstmt.setString(8,unlockableId);
                    pstmt.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

