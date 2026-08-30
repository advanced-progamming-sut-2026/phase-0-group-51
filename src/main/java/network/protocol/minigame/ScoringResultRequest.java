package network.protocol.minigame;

public class ScoringResultRequest {
    private int score;
    private boolean won;

    public ScoringResultRequest() {
    }

    public ScoringResultRequest(int score, boolean won) {
        this.score = score;
        this.won = won;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }
}
