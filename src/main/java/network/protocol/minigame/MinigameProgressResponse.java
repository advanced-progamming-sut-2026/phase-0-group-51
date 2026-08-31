package network.protocol.minigame;

import java.util.ArrayList;
import java.util.List;

public class MinigameProgressResponse {
    private boolean success;
    private String message;
    private List<MinigameProgressDto> progress = new ArrayList<>();
    private int miniGamesPlayed;

    public MinigameProgressResponse() {
    }

    public MinigameProgressResponse(
            boolean success,
            String message,
            List<MinigameProgressDto> progress,
            int miniGamesPlayed
    ) {
        this.success = success;
        this.message = message;
        this.progress = progress == null
                ? new ArrayList<>()
                : new ArrayList<>(progress);
        this.miniGamesPlayed = miniGamesPlayed;
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

    public List<MinigameProgressDto> getProgress() {
        return progress;
    }

    public void setProgress(List<MinigameProgressDto> progress) {
        this.progress = progress == null
                ? new ArrayList<>()
                : new ArrayList<>(progress);
    }

    public int getMiniGamesPlayed() {
        return miniGamesPlayed;
    }

    public void setMiniGamesPlayed(int miniGamesPlayed) {
        this.miniGamesPlayed = miniGamesPlayed;
    }
}
