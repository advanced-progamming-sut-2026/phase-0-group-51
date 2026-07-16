package models.quests;

import java.util.Locale;

public enum QuestEventType {
    SUN_COLLECTED,
    CHAPTER_ZOMBIE_KILLS,
    PLANT_ONLY_KILLS,
    SPECIFIC_PLANT_KILLS,
    WIN_MAX_PLANTS_LOST,
    WIN_EXACT_SUN,
    FAST_KILLS,
    EXPLOSIVE_PLANTS_USED,
    FINISH_SYMMETRIC,
    ONLY_FAMILY_KILLS,
    WIN_WITHOUT_FAMILY,
    WIN_DAY_WITH_NIGHT_PLANTS,
    MAX_DIFFICULTY_WIN_STREAK,
    FIRST_COLUMN_KILLS_NO_MOWER,
    WIN_ASYMMETRIC_EXCEPT_MIDDLE,
    WIN_ONLY_SUN_PRODUCERS_EXACT_COUNT,
    WIN_EMPTY_COLUMN,
    WIN_EMPTY_ROW,
    WIN_EMPTY_CROSS,
    MOWER_KILLS;

    public static QuestEventType fromStorage(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Quest event type is required.");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
    }
}
