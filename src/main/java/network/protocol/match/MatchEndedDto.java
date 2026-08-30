package network.protocol.match;

public class MatchEndedDto {

    private String matchId;
    private String outcome;
    private String winnerRole;
    private int finalTick;
    private String reason;

    public MatchEndedDto() {
    }

    public MatchEndedDto(String matchId, String outcome, String winnerRole,
                         int finalTick, String reason) {
        this.matchId = matchId;
        this.outcome = outcome;
        this.winnerRole = winnerRole;
        this.finalTick = finalTick;
        this.reason = reason;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getWinnerRole() { return winnerRole; }
    public void setWinnerRole(String winnerRole) { this.winnerRole = winnerRole; }

    public int getFinalTick() { return finalTick; }
    public void setFinalTick(int finalTick) { this.finalTick = finalTick; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
