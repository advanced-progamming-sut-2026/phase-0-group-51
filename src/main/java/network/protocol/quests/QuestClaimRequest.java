package network.protocol.quests;

public class QuestClaimRequest {
    private int questId;

    public QuestClaimRequest() {
    }

    public QuestClaimRequest(int questId) {
        this.questId = questId;
    }

    public int getQuestId() { return questId; }
    public void setQuestId(int questId) { this.questId = questId; }
}
