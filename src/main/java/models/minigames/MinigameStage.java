package models.minigames;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MinigameStage {
    private final MinigameType minigameType;
    private final int stageNumber;
    private final int difficulty;

    public MinigameStage(MinigameType minigameType, int stageNumber, int difficulty) {
        if (minigameType == null) {
            throw new IllegalArgumentException(
                    "Minigame type cannot be null.\n"
            );
        }
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException(
                    "Minigame stage must be between 1 and 3.\n"
            );
        }
        if (difficulty <= 0) {
            throw new IllegalArgumentException(
                    "Difficulty must be positive.\n"
            );
        }
        this.minigameType = minigameType;
        this.stageNumber = stageNumber;
        this.difficulty = difficulty;
    }
    public static List<MinigameStage> getStages(MinigameType type) {
        return List.of(
                new MinigameStage(type, 1, 1),
                new MinigameStage(type, 2, 2),
                new MinigameStage(type, 3, 3)
        );
}}
