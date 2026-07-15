package models.quests;

import lombok.Getter;

import java.time.LocalDate;
@Getter
public class UserQuest {
    private int userId;
    private int questId;
    private int progress;
    private boolean completed;
    private LocalDate resetDate;
    public UserQuest(int userId, int questId, int progress, boolean completed, LocalDate resetDate) {
        this.userId = userId;
        this.questId = questId;
        this.progress = progress;
        this.completed = completed;
        this.resetDate = resetDate;
    }
}
