package models.enums.commands.minigames;

import models.enums.commands.Commands;

public enum IZombieCommands implements Commands {
    COLLECT_LOOT("(?i)^\\s*collect\\s+loot\\s+-l\\s*"
            + "\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)\\s*$"),
    // place zombie -t <type> -l (<x>, <y>)   (also accepts: place zombie <type> <x> <y>)
    PLACE_ZOMBIE("(?i)^\\s*place\\s+zombie\\s+(?:-t\\s+)?(?<zombieName>.+?)\\s+"
        + "(?:-l\\s*)?\\(?\\s*(?<x>-?\\d+)\\s*(?:,\\s*|\\s+)(?<y>-?\\d+)\\s*\\)?\\s*$"),
    //advance time -t <count> ticks   (also accepts: advance time <count>)
    ADVANCE_TIME("(?i)^\\s*advance\\s+time\\s+(?:-t\\s+)?(?<count>-?\\d+)(?:\\s+ticks)?\\s*$"),
    SHOW_ROSTER("(?i)^\\s*show\\s+roster\\s*$"),
    // show izombie status   (also accepts: show status)
    SHOW_STATUS("(?i)^\\s*show\\s+(?:izombie\\s+)?status\\s*$"),
    SHOW_MAP("(?i)^\\s*show\\s+map\\s*$"),
    // menu show current   (also accepts: show current menu)
    CURRENT_MENU("(?i)^\\s*(?:menu\\s+show\\s+current|show\\s+current\\s+menu)\\s*$"),
    // menu exit   (also accepts: exit)
    EXIT_MENU("(?i)^\\s*(?:menu\\s+)?exit\\s*$");

    private final String regex;

    IZombieCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
