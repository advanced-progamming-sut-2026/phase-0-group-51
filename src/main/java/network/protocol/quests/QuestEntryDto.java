package network.protocol.quests;

import models.quests.QuestPriority;
import models.quests.QuestType;

public class QuestEntryDto {
    private int questId;
    private String name;
    private String description;
    private String rewardText;
    private QuestType type;
    private QuestPriority priority;
    private int progress;
    private int targetAmount;
    private boolean completed;
    private boolean claimed;

    public QuestEntryDto() {
    }

    public QuestEntryDto(
            int questId,
            String name,
            String description,
            String rewardText,
            QuestType type,
            QuestPriority priority,
            int progress,
            int targetAmount,
            boolean completed,
            boolean claimed
    ) {
        this.questId = questId;
        this.name = name;
        this.description = description;
        this.rewardText = rewardText;
        this.type = type;
        this.priority = priority;
        this.progress = progress;
        this.targetAmount = targetAmount;
        this.completed = completed;
        this.claimed = claimed;
    }

    public int getQuestId() { return questId; }
    public void setQuestId(int questId) { this.questId = questId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRewardText() { return rewardText; }
    public void setRewardText(String rewardText) { this.rewardText = rewardText; }
    public QuestType getType() { return type; }
    public void setType(QuestType type) { this.type = type; }
    public QuestPriority getPriority() { return priority; }
    public void setPriority(QuestPriority priority) { this.priority = priority; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public int getTargetAmount() { return targetAmount; }
    public void setTargetAmount(int targetAmount) { this.targetAmount = targetAmount; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }
}
