package models.quests;

/** Kept as a compatibility facade for older controller code. */
public final class QuestManager {
    public QuestService service() {
        return QuestService.getInstance();
    }
}
