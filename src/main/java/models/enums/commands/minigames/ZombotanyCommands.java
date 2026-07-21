package models.enums.commands.minigames;

import models.enums.commands.Commands;

public enum ZombotanyCommands implements Commands {
    COLLECT_LOOT("(?i)^\\s*collect\\s+loot\\s+-l\\s*"
            + "\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)\\s*$"),
    //plant plant -t <type> -l (<x>, <y>)   (also accepts: plant plant <type> <x> <y>)
    PLACE_PLANT("(?i)^\\s*(?:plant|place)\\s+plant\\s+(?:-t\\s+)?(?<plantName>.+?)\\s+"
        + "(?:-l\\s*)?\\(?\\s*(?<x>-?\\d+)\\s*(?:,\\s*|\\s+)(?<y>-?\\d+)\\s*\\)?\\s*$"),
    //advance time -t <count> ticks   (also accepts: advance time <count>)
    ADVANCE_TIME("(?i)^\\s*advance\\s+time\\s+(?:-t\\s+)?(?<count>-?\\d+)(?:\\s+ticks)?\\s*$"),
    // show available plants   (also accepts: show plants)
    SHOW_PLANTS("(?i)^\\s*show\\s+(?:available\\s+)?plants\\s*$"),
    // show zombotany status   (also accepts: show status)
    SHOW_STATUS("(?i)^\\s*show\\s+(?:zombotany\\s+)?status\\s*$"),
    SHOW_MAP("(?i)^\\s*show\\s+map\\s*$"),
    // menu show current   (also accepts: show current menu)
    CURRENT_MENU("(?i)^\\s*(?:menu\\s+show\\s+current|show\\s+current\\s+menu)\\s*$"),
    //  menu exit   (also accepts: exit)
    EXIT_MENU("(?i)^\\s*(?:menu\\s+)?exit\\s*$");

    private final String regex;

    ZombotanyCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
