package network.protocol.gameplay;

public class AdventureProgressResponse {
    private boolean success;
    private String message;
    private int currentChapter;
    private int currentLevel;

    public AdventureProgressResponse() {
    }

    public AdventureProgressResponse(
            boolean success,
            String message,
            int currentChapter,
            int currentLevel
    ) {
        this.success = success;
        this.message = message;
        this.currentChapter = currentChapter;
        this.currentLevel = currentLevel;
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

    public int getCurrentChapter() {
        return currentChapter;
    }

    public void setCurrentChapter(int currentChapter) {
        this.currentChapter = currentChapter;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }
}
