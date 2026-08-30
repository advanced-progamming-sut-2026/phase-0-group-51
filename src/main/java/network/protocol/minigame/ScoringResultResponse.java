package network.protocol.minigame;

public class ScoringResultResponse {
    private boolean success;
    private String message;
    private int dailyBest;
    private int mostMeowPoint;
    private int maxPoint;
    private int gamesPlayed;

    public ScoringResultResponse() {
    }

    public ScoringResultResponse(
            boolean success,
            String message,
            int dailyBest,
            int mostMeowPoint,
            int maxPoint,
            int gamesPlayed
    ) {
        this.success = success;
        this.message = message;
        this.dailyBest = dailyBest;
        this.mostMeowPoint = mostMeowPoint;
        this.maxPoint = maxPoint;
        this.gamesPlayed = gamesPlayed;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getDailyBest() {
        return dailyBest;
    }

    public void setDailyBest(int dailyBest) {
        this.dailyBest = dailyBest;
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
}
