package models.quests;

import java.util.Locale;

public enum QuestType {
    DAILY,
    MAIN,
    EPIC;

    public static QuestType fromPageName(String pageName) {
        if (pageName == null || pageName.isBlank()) {
            throw new IllegalArgumentException("Quest page is required.");
        }
        return valueOf(pageName.trim().toUpperCase(Locale.ROOT));
    }
}
