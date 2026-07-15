package Data.database;
import models.User;

import java.sql.*;

public class UserRepository {

        public boolean register(User user) {
            String sql = "INSERT INTO users (username, email, password_hash, gender, nickname, security_question," +
                    " answer) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DataBaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                         sql,
                         Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, user.getPasswordHash());
                pstmt.setString(4, user.getGender());
                pstmt.setString(5, user.getNickname());
                pstmt.setInt(6, user.getSecurityQuestion());
                pstmt.setString(7, user.getAnswer());

                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
                initializeProgress(user.getUsername());
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("gender"),
                        rs.getString("nickname"),
                        rs.getInt("security_question"),
                        rs.getString("answer"),
                        rs.getInt("coins"),
                        rs.getInt("gems"),
                        rs.getInt("plant_food_num"),
                        rs.getInt("most_meow_point"),
                        rs.getInt("max_point"),
                        rs.getInt("games_played"),
                        rs.getInt("mini_games_played"),
                        rs.getString("last_won_game"),
                        rs.getInt("difficulty_level")
                );
                user.setQuestDailyNum(rs.getInt("quest_daily_num"));
                user.setQuestNonDailyNum(rs.getInt("quest_non_daily_num"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void setStayLoggedIn(int userId, boolean value) {
        String sql =
                "UPDATE users SET stay_logged_in = ? WHERE id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, value ? 1 : 0);
            pstmt.setInt(2, userId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public User getRememberedUser() {
        String sql = "SELECT * FROM users WHERE stay_logged_in = 1 LIMIT 1";
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("gender"),
                        rs.getString("nickname"),
                        rs.getInt("security_question"),
                        rs.getString("answer"),
                        rs.getInt("coins"),
                        rs.getInt("gems"),
                        rs.getInt("plant_food_num"),
                        rs.getInt("most_meow_point"),
                        rs.getInt("max_point"),
                        rs.getInt("games_played"),
                        rs.getInt("mini_games_played"),
                        rs.getString("last_won_game"),
                        rs.getInt("difficulty_level")
                );
                user.setQuestDailyNum(rs.getInt("quest_daily_num"));
                user.setQuestNonDailyNum(rs.getInt("quest_non_daily_num"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
        public boolean updateStats(User user) {
            String sql = "UPDATE users SET coins = ?, gems = ?, plant_food_num = ?, " +
                    "most_meow_point = ?, max_point = ?, games_played = ?, mini_games_played = ? WHERE id = ?";
            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, user.getCoins());
                pstmt.setInt(2, user.getGems());
                pstmt.setInt(3, user.getPlantFoodNum());
                pstmt.setInt(4, user.getMostMeowPoint());
                pstmt.setInt(5, user.getMaxPoint());
                pstmt.setInt(6, user.getGamesPlayed());
                pstmt.setInt(7, user.getMiniGamesPlayed());
                pstmt.setInt(8, user.getId());

                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        @SuppressWarnings("CallToPrintStackTrace")
        private void initializeProgress(String username) {
            String sql = "INSERT INTO user_progress (user_id, chapter_index, level_index) " +
                    "SELECT id, 1, 1 FROM users WHERE username = ?";
            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    public void updateUsername(int userId, String newUsername) {
        String sql = "UPDATE users SET username = ? WHERE id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newUsername);
            pstmt.setInt(2, userId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void updateNickname(int userId, String newNickname) {
        String sql = "UPDATE users SET nickname = ? WHERE id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newNickname);
            pstmt.setInt(2, userId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void updateEmail(int userId, String newEmail) {
        String sql = "UPDATE users SET email = ? WHERE id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newEmail);
            pstmt.setInt(2, userId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePassword(String username,
                               String passwordHash) {

        String sql =
                "UPDATE users SET password_hash = ? WHERE username = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, passwordHash);
            pstmt.setString(2, username);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void updateDifficulty(String username, int difficultyLevel) {

        String sql =
                "UPDATE users SET difficulty_level = ? WHERE username = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, difficultyLevel);
            pstmt.setString(2, username);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static final int LEVELS_PER_CHAPTER = 4;

    public int getPassedLevels(int userId) {

        String sql =
                "SELECT chapter_index, level_index " +
                        "FROM user_progress WHERE user_id = ?";

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                int chapterIndex =
                        rs.getInt("chapter_index");

                int levelIndex =
                        rs.getInt("level_index");

                return (chapterIndex - 1) * LEVELS_PER_CHAPTER
                        + (levelIndex - 1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
    }

