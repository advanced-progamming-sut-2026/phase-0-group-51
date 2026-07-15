package models.quests;

import lombok.Getter;

import java.util.Locale;

@Getter
public enum QuestEventType {
    SUN_COLLECTED,
    ZOMBIE_KILLED,
    PLANT_PLANTED,
    ADVENTURE_WON,
    MINIGAME_WON
}
