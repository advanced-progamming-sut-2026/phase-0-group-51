package network.protocol.profile;

public class ProfileDifficultyUpdateRequest {
    private int difficultyLevel;

    public ProfileDifficultyUpdateRequest() {
    }

    public ProfileDifficultyUpdateRequest(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
}
