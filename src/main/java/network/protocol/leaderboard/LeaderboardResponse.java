package network.protocol.leaderboard;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardResponse {
    private boolean success;
    private String message;
    private List<LeaderboardEntryDto> entries = new ArrayList<>();

    public LeaderboardResponse() {
    }

    public LeaderboardResponse(
            boolean success,
            String message,
            List<LeaderboardEntryDto> entries
    ) {
        this.success = success;
        this.message = message;
        this.entries = entries == null
                ? new ArrayList<>()
                : new ArrayList<>(entries);
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

    public List<LeaderboardEntryDto> getEntries() {
        return entries;
    }

    public void setEntries(List<LeaderboardEntryDto> entries) {
        this.entries = entries == null
                ? new ArrayList<>()
                : new ArrayList<>(entries);
    }
}
