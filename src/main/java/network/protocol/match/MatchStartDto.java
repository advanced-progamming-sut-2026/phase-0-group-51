package network.protocol.match;

public class MatchStartDto {

    private String matchId;
    private String role;
    private long seed;
    private int stageNumber;
    private int matchDurationTicks;

    public MatchStartDto() {
    }

    public MatchStartDto(String matchId, String role, long seed,
                         int stageNumber, int matchDurationTicks) {
        this.matchId = matchId;
        this.role = role;
        this.seed = seed;
        this.stageNumber = stageNumber;
        this.matchDurationTicks = matchDurationTicks;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public int getStageNumber() { return stageNumber; }
    public void setStageNumber(int stageNumber) { this.stageNumber = stageNumber; }

    public int getMatchDurationTicks() { return matchDurationTicks; }
    public void setMatchDurationTicks(int matchDurationTicks) { this.matchDurationTicks = matchDurationTicks; }
}
