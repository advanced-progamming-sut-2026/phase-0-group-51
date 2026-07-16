package models.quests;

public abstract class Quest {
    private int id;
    private final String name;
    private final String condition;
    private final QuestPriority priority;
    private final QuestEventType eventType;
    private final int targetAmount;
    private final int rewardAmount;
    private final QuestRewardType rewardType;
    private final QuestType type;
    private final String unlockableId;
    private final String parameterOptions;

    protected Quest(
            String name,
            String condition,
            QuestPriority priority,
            QuestEventType eventType,
            int targetAmount,
            int rewardAmount,
            QuestRewardType rewardType,
            QuestType type,
            String unlockableId,
            String parameterOptions
    ) {
        this.name = name;
        this.condition = condition;
        this.priority = priority;
        this.eventType = eventType;
        this.targetAmount = targetAmount;
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
        this.type = type;
        this.unlockableId = unlockableId;
        this.parameterOptions = parameterOptions;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getCondition() { return condition; }
    public QuestPriority getPriority() { return priority; }
    public QuestEventType getEventType() { return eventType; }
    public int getTargetAmount() { return targetAmount; }
    public int getRewardAmount() { return rewardAmount; }
    public QuestRewardType getRewardType() { return rewardType; }
    public QuestType getType() { return type; }
    public String getUnlockableId() { return unlockableId; }
    public String getParameterOptions() { return parameterOptions; }

    public boolean isComplete(int progress) {
        return progress >= targetAmount;
    }
}
