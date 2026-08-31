package network.protocol.quests;

public class QuestSunProgressRequest {
    private int amount;

    public QuestSunProgressRequest() {
    }

    public QuestSunProgressRequest(int amount) {
        this.amount = amount;
    }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}
