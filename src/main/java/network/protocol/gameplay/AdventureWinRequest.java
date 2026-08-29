package network.protocol.gameplay;

public class AdventureWinRequest {
    private int completedChapter;
    private int completedLevel;

    public AdventureWinRequest() {
    }

    public AdventureWinRequest(
            int completedChapter,
            int completedLevel
    ) {
        this.completedChapter = completedChapter;
        this.completedLevel = completedLevel;
    }

    public int getCompletedChapter() {
        return completedChapter;
    }

    public void setCompletedChapter(
            int completedChapter
    ) {
        this.completedChapter = completedChapter;
    }

    public int getCompletedLevel() {
        return completedLevel;
    }

    public void setCompletedLevel(
            int completedLevel
    ) {
        this.completedLevel = completedLevel;
    }
}
