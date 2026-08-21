package views.graphical.dialogue;

import models.games.ChapterTheme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LevelDialogueRegistry {
    private static final String CRAZY_DAVE_PAM =
            "768/INITIAL/CRAZYDAVE/CRAZYDAVE/CRAZYDAVE.PAM";

    private static final Map<LevelKey, NpcDialogueSequence> LEVEL_DIALOGUES =
            createDialogues();

    private LevelDialogueRegistry() {
    }

    public static NpcDialogueSequence find(
            ChapterTheme theme,
            int levelNumber
    ) {
        return LEVEL_DIALOGUES.get(
                new LevelKey(theme, levelNumber)
        );
    }

    private static Map<LevelKey, NpcDialogueSequence> createDialogues() {
        Map<LevelKey, NpcDialogueSequence> dialogues = new HashMap<>();

        dialogues.put(
                new LevelKey(ChapterTheme.ANCIENT_EGYPT, 1),
                crazyDave(
                        "Whoa! Sand, pyramids, and zombies. "
                                + "Yep, definitely Ancient Egypt.",
                        "Plant carefully! Those bandaged brain-munchers "
                                + "are not here for the sightseeing.",
                        "Keep them away from the house, and try not to "
                                + "get sand in the lawn mower!"
                )
        );

        return Map.copyOf(dialogues);
    }

    private static NpcDialogueSequence crazyDave(String... lines) {
        return new NpcDialogueSequence(
                CRAZY_DAVE_PAM,
                "anim_enter",
                "anim_idle",
                List.of(
                        "anim_smalltalk",
                        "anim_crazyblahblah"
                ),
                "anim_leave",
                List.of(lines)
        );
    }

    private record LevelKey(
            ChapterTheme theme,
            int levelNumber
    ) {
    }
}
