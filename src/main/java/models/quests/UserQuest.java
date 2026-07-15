package models.quests;

import java.time.LocalDate;

public class UserQuest {
    private final int userId;
    private final int questId;
    private final int progress;
    private final int targetAmount;
    private final int rewardAmount;
    private final boolean completed;
    private final boolean claimed;
    private final LocalDate resetDate;
    private final String parameter;

    public int getUserId() { return userId; }
    public int getQuestId() { return questId; }
    public int getProgress() { return progress; }
    public int getTargetAmount() { return targetAmount; }
    public int getRewardAmount() { return rewardAmount; }
    public boolean isCompleted() { return completed; }
    public boolean isClaimed() { return claimed; }
    public LocalDate getResetDate() { return resetDate; }
    public String getParameter() { return parameter; }

    public UserQuest(
            int userId,
            int questId,
            int progress,
            int targetAmount,
            int rewardAmount,
            boolean completed,
            boolean claimed,
            LocalDate resetDate,
            String parameter
    ) {
        this.userId = userId;
        this.questId = questId;
        this.progress = progress;
        this.targetAmount = targetAmount;
        this.rewardAmount = rewardAmount;
        this.completed = completed;
        this.claimed = claimed;
        this.resetDate = resetDate;
        this.parameter = parameter;
    }
}
