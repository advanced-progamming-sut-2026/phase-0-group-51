package network.protocol.gameplay;

import java.util.ArrayList;
import java.util.List;

public class AdventureWinResponse {
    private boolean success;
    private String message;
    private int gamesPlayed;
    private String lastWonGame;
    private int currentChapter;
    private int currentLevel;
    private boolean progressAdvanced;
    private List<Integer> unlockedPlantIds =
            new ArrayList<>();
    private List<Integer> newlyUnlockedPlantIds =
            new ArrayList<>();

    public AdventureWinResponse() {
    }

    public AdventureWinResponse(
            boolean success,
            String message,
            int gamesPlayed,
            String lastWonGame,
            int currentChapter,
            int currentLevel,
            boolean progressAdvanced,
            List<Integer> unlockedPlantIds,
            List<Integer> newlyUnlockedPlantIds
    ) {
        this.success = success;
        this.message = message;
        this.gamesPlayed = gamesPlayed;
        this.lastWonGame = lastWonGame;
        this.currentChapter = currentChapter;
        this.currentLevel = currentLevel;
        this.progressAdvanced = progressAdvanced;
        setUnlockedPlantIds(unlockedPlantIds);
        setNewlyUnlockedPlantIds(
                newlyUnlockedPlantIds
        );
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

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public String getLastWonGame() {
        return lastWonGame;
    }

    public void setLastWonGame(
            String lastWonGame
    ) {
        this.lastWonGame = lastWonGame;
    }

    public int getCurrentChapter() {
        return currentChapter;
    }

    public void setCurrentChapter(
            int currentChapter
    ) {
        this.currentChapter = currentChapter;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(
            int currentLevel
    ) {
        this.currentLevel = currentLevel;
    }

    public boolean isProgressAdvanced() {
        return progressAdvanced;
    }

    public void setProgressAdvanced(
            boolean progressAdvanced
    ) {
        this.progressAdvanced = progressAdvanced;
    }

    public List<Integer> getUnlockedPlantIds() {
        return unlockedPlantIds;
    }

    public void setUnlockedPlantIds(
            List<Integer> unlockedPlantIds
    ) {
        this.unlockedPlantIds =
                unlockedPlantIds == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                                unlockedPlantIds
                        );
    }

    public List<Integer>
    getNewlyUnlockedPlantIds() {
        return newlyUnlockedPlantIds;
    }

    public void setNewlyUnlockedPlantIds(
            List<Integer> newlyUnlockedPlantIds
    ) {
        this.newlyUnlockedPlantIds =
                newlyUnlockedPlantIds == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                                newlyUnlockedPlantIds
                        );
    }
}
