package network.protocol.leaderboard;

import models.leaderBoard.LeaderBoard;

public class LeaderboardEntryDto {
    private String username;
    private String lastCompleted;
    private int completedChapter;
    private int completedLevel;
    private int minigamesCompleted;
    private int dailyQuestsCompleted;
    private int nonDailyQuestsCompleted;
    private int highestScore;

    public LeaderboardEntryDto() {
    }

    public LeaderboardEntryDto(
            String username,
            String lastCompleted,
            int completedChapter,
            int completedLevel,
            int minigamesCompleted,
            int dailyQuestsCompleted,
            int nonDailyQuestsCompleted,
            int highestScore
    ) {
        this.username = username;
        this.lastCompleted = lastCompleted;
        this.completedChapter = completedChapter;
        this.completedLevel = completedLevel;
        this.minigamesCompleted = minigamesCompleted;
        this.dailyQuestsCompleted = dailyQuestsCompleted;
        this.nonDailyQuestsCompleted = nonDailyQuestsCompleted;
        this.highestScore = highestScore;
    }

    public static LeaderboardEntryDto fromModel(LeaderBoard entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Leaderboard entry cannot be null.");
        }

        return new LeaderboardEntryDto(
                entry.username(),
                entry.lastCompleted(),
                entry.completedChapter(),
                entry.completedLevel(),
                entry.minigamesCompleted(),
                entry.dailyQuestsCompleted(),
                entry.nonDailyQuestsCompleted(),
                entry.highestScore()
        );
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(String lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public int getCompletedChapter() {
        return completedChapter;
    }

    public void setCompletedChapter(int completedChapter) {
        this.completedChapter = completedChapter;
    }

    public int getCompletedLevel() {
        return completedLevel;
    }

    public void setCompletedLevel(int completedLevel) {
        this.completedLevel = completedLevel;
    }

    public int getMinigamesCompleted() {
        return minigamesCompleted;
    }

    public void setMinigamesCompleted(int minigamesCompleted) {
        this.minigamesCompleted = minigamesCompleted;
    }

    public int getDailyQuestsCompleted() {
        return dailyQuestsCompleted;
    }

    public void setDailyQuestsCompleted(int dailyQuestsCompleted) {
        this.dailyQuestsCompleted = dailyQuestsCompleted;
    }

    public int getNonDailyQuestsCompleted() {
        return nonDailyQuestsCompleted;
    }

    public void setNonDailyQuestsCompleted(int nonDailyQuestsCompleted) {
        this.nonDailyQuestsCompleted = nonDailyQuestsCompleted;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }
}
