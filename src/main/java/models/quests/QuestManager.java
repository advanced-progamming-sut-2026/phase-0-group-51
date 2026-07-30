package models.quests;


public final class QuestManager {
    public QuestService service() {
        return QuestService.getInstance();
    }
}
