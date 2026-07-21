package models.enums.commands.minigames;

import models.enums.commands.Commands;

public enum VaseBreakerCommands implements Commands {
    BREAK_VASE("^\\s*break\\s+vase\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),
    PICK_SEED_PACKET("^\\s*pick\\s+seed\\s+packet\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),
    PLANT_PACKET("^\\s*plant\\s+packet\\s+-t\\s+" + "(?<plantType>.+?)\\s+-l\\s*" +
        "\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),
    ADVANCE_TIME("^\\s*advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks\\s*$"),
    SHOW_MAP("(?i)^\\s*show\\s+map\\s*$"),
    SHOW_STATUS("^\\s*show\\s+vasebreaker\\s+status\\s*$"),
    CURRENT_MENU(Commands.CURRENT_MENU_REGEX),
    EXIT_MENU(Commands.EXIT_MENU_REGEX);
    private final String regex;
    VaseBreakerCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
