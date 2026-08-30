package network.protocol.quests;

import java.util.ArrayList;
import java.util.List;

public class QuestResponse {
    private boolean success;
    private String message;
    private List<QuestEntryDto> entries = new ArrayList<>();
    private int coins;
    private int gems;
    private int questDailyNum;
    private int questNonDailyNum;
    private List<Integer> unlockedPlantIds = new ArrayList<>();

    public QuestResponse() {
    }

    public QuestResponse(
            boolean success,
            String message,
            List<QuestEntryDto> entries,
            int coins,
            int gems,
            int questDailyNum,
            int questNonDailyNum,
            List<Integer> unlockedPlantIds
    ) {
        this.success = success;
        this.message = message;
        this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
        this.coins = coins;
        this.gems = gems;
        this.questDailyNum = questDailyNum;
        this.questNonDailyNum = questNonDailyNum;
        this.unlockedPlantIds = unlockedPlantIds == null
                ? new ArrayList<>()
                : new ArrayList<>(unlockedPlantIds);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<QuestEntryDto> getEntries() { return entries; }
    public void setEntries(List<QuestEntryDto> entries) { this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries); }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }
    public int getQuestDailyNum() { return questDailyNum; }
    public void setQuestDailyNum(int questDailyNum) { this.questDailyNum = questDailyNum; }
    public int getQuestNonDailyNum() { return questNonDailyNum; }
    public void setQuestNonDailyNum(int questNonDailyNum) { this.questNonDailyNum = questNonDailyNum; }
    public List<Integer> getUnlockedPlantIds() { return unlockedPlantIds; }
    public void setUnlockedPlantIds(List<Integer> unlockedPlantIds) { this.unlockedPlantIds = unlockedPlantIds == null ? new ArrayList<>() : new ArrayList<>(unlockedPlantIds); }
}
