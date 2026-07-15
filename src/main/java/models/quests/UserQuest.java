package models.quests;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
public class UserQuest {
    private final int userId;
    private final int questId;
    private final int progress;
    private final boolean completed;
    private final boolean claimed;
    private final LocalDate resetDate;

    public UserQuest(
            int userId,
            int questId,
            int progress,
            boolean completed,
            boolean claimed,
            LocalDate resetDate
    ) {
        this.userId = userId;
        this.questId = questId;
        this.progress = progress;
        this.completed = completed;
        this.claimed = claimed;
        this.resetDate = resetDate;
    }


}
