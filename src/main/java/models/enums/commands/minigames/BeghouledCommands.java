package models.enums.commands.minigames;

import models.enums.commands.Commands;

public enum BeghouledCommands implements Commands {
    SWAP_PLANTS("(?i)^\\s*swap\\s+plants\\s+-f\\s*"
                    + "\\(\\s*(?<x1>-?\\d+)\\s*,\\s*(?<y1>-?\\d+)\\s*\\)\\s+" +
            "-t\\s*\\(\\s*(?<x2>-?\\d+)\\s*,\\s*(?<y2>-?\\d+)\\s*\\)\\s*$"
    ),
    UPGRADE_PLANTS("(?i)^\\s*upgrade\\s+beghouled\\s+-f\\s+" + "(?<fromPlant>.+?)\\s+-t\\s+(?<toPlant>.+?)\\s*$"
    ),
    ADVANCE_TIME("(?i)^\\s*advance\\s+time\\s+(?:-t\\s+)?" + "(?<count>-?\\d+)(?:\\s+ticks)?\\s*$"),
    SHOW_STATUS("(?i)^\\s*show\\s+(?:beghouled\\s+)?status\\s*$"),
    SHOW_MAP("(?i)^\\s*show\\s+(?:beghouled\\s+)?map\\s*$"),
    SHOW_UPGRADES("(?i)^\\s*show\\s+(?:beghouled\\s+)?upgrades\\s*$"),
    CURRENT_MENU("(?i)^\\s*(?:menu\\s+show\\s+current|show\\s+current\\s+menu)\\s*$"),
    EXIT_MENU("(?i)^\\s*(?:menu\\s+)?exit\\s*$");

    private final String regex;

    BeghouledCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
