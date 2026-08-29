package network.protocol.auth;

import models.User;

import java.util.List;

public class UserProfileDto {
    private int id;
    private String username;
    private String email;
    private String gender;
    private String nickname;

    private int coins;
    private int gems;
    private int plantFoodNum;

    private int mostMeowPoint;
    private int maxPoint;

    private int gamesPlayed;
    private int miniGamesPlayed;

    private String lastWonGame;
    private int difficultyLevel;

    private int questDailyNum;
    private int questNonDailyNum;

    private List<Integer> unlockedPlantIds;

    public UserProfileDto() {
    }

    public static UserProfileDto fromUser(User user) {
        UserProfileDto dto = new UserProfileDto();

        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.gender = user.getGender();
        dto.nickname = user.getNickname();

        dto.coins = user.getCoins();
        dto.gems = user.getGems();
        dto.plantFoodNum = user.getPlantFoodNum();

        dto.mostMeowPoint = user.getMostMeowPoint();
        dto.maxPoint = user.getMaxPoint();

        dto.gamesPlayed = user.getGamesPlayed();
        dto.miniGamesPlayed = user.getMiniGamesPlayed();

        dto.lastWonGame = user.getLastWonGame();
        dto.difficultyLevel = user.getDifficultyLevel();

        dto.questDailyNum = user.getQuestDailyNum();
        dto.questNonDailyNum = user.getQuestNonDailyNum();

        return dto;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Integer> getUnlockedPlantIds() {
        return unlockedPlantIds;
    }

    public void setUnlockedPlantIds(
            List<Integer> unlockedPlantIds
    ) {
        this.unlockedPlantIds = unlockedPlantIds;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = gems;
    }

    public int getPlantFoodNum() {
        return plantFoodNum;
    }

    public void setPlantFoodNum(int plantFoodNum) {
        this.plantFoodNum = plantFoodNum;
    }

    public int getMostMeowPoint() {
        return mostMeowPoint;
    }

    public void setMostMeowPoint(int mostMeowPoint) {
        this.mostMeowPoint = mostMeowPoint;
    }

    public int getMaxPoint() {
        return maxPoint;
    }

    public void setMaxPoint(int maxPoint) {
        this.maxPoint = maxPoint;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getMiniGamesPlayed() {
        return miniGamesPlayed;
    }

    public void setMiniGamesPlayed(int miniGamesPlayed) {
        this.miniGamesPlayed = miniGamesPlayed;
    }

    public String getLastWonGame() {
        return lastWonGame;
    }

    public void setLastWonGame(String lastWonGame) {
        this.lastWonGame = lastWonGame;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public int getQuestDailyNum() {
        return questDailyNum;
    }

    public void setQuestDailyNum(int questDailyNum) {
        this.questDailyNum = questDailyNum;
    }

    public int getQuestNonDailyNum() {
        return questNonDailyNum;
    }

    public void setQuestNonDailyNum(int questNonDailyNum) {
        this.questNonDailyNum = questNonDailyNum;
    }
}