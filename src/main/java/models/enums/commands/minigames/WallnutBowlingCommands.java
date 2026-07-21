package models.enums.commands.minigames;

import models.enums.commands.Commands;

public enum WallnutBowlingCommands implements Commands {
    ROLL_WALLNUT("^\\s*roll\\s+wallnut\\s+-l\\s*"
        + "\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),
    ADVANCE_TIME("^\\s*advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks\\s*$"),
    SHOW_CONVEYOR("^\\s*show\\s+conveyor\\s*$"),
    SHOW_MAP("(?i)^\\s*show\\s+map\\s*$"),
    SHOW_STATUS("^\\s*show\\s+wallnut\\s+bowling\\s+status\\s*$"),
    CURRENT_MENU(Commands.CURRENT_MENU_REGEX),
    EXIT_MENU(Commands.EXIT_MENU_REGEX);
    private final String regex;

    WallnutBowlingCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
