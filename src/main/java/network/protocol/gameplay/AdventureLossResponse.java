package network.protocol.gameplay;

public class AdventureLossResponse {
    private boolean success;
    private String message;
    private int gamesPlayed;

    public AdventureLossResponse() {
    }

    public AdventureLossResponse(
            boolean success,
            String message,
            int gamesPlayed
    ) {
        this.success = success;
        this.message = message;
        this.gamesPlayed = gamesPlayed;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(
            boolean success
    ) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(
            String message
    ) {
        this.message = message;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(
            int gamesPlayed
    ) {
        this.gamesPlayed = gamesPlayed;
    }
}
