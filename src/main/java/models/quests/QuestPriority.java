package models.quests;

import java.util.Locale;

public enum QuestPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW;

    public static QuestPriority fromStorage(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("AVERAGE")) {
            normalized = "MEDIUM";
        }
        return valueOf(normalized);
    }
}
