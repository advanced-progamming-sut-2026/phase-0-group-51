package models.enums.commands;

public enum GreenHouseMenuCommands implements Commands{
    PLANT_REGEX("^\\s*plant\\s+pot\\s+at\\s+\\(\\s*(?<x>\\S+),\\s*(?<y>\\S+)\\)\\s*$"),
    SHOW_GREENHOUSE_REGEX("^\\s*show\\s+greenhouse\\s*$"),
    COLLECT_REGEX("^\\s*collect\\s+\\(\\s*(?<x>\\S+),\\s*(?<y>\\S+)\\)\\s*$"),
    GROW_REGEX("^\\s*grow\\s+\\(\\s*(?<x>\\S+),\\s*(?<y>\\S+)\\)\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
     ENTER_SHOP_REGEX("^\\s*enter\\s+shop\\s*$");
    private final String regex;
    GreenHouseMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
