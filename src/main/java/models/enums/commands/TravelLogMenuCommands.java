package models.enums.commands;

public enum TravelLogMenuCommands implements Commands{
    CHANGE_PAGE("(?i)^\\s*travel\\s+log\\s+page\\s+"
            + "(?<page>main|daily|epic|minigame)\\s*$"),
    SHOW_QUESTS("(?i)^\\s*show\\s+quests\\s*$"),
    CLAIM_QUEST("(?i)^\\s*claim\\s+quest\\s+-i\\s+(?<id>\\d+)\\s*$"),
    START_VASEBREAKER("(?i)^\\s*start\\s+vasebreaker\\s+-s\\s+(?<stage>[1-3])\\s*$"),
    START_WALLNUT_BOWLING("(?i)^\\s*start\\s+wallnut\\s+bowling"
            + "\\s+-s\\s+(?<stage>[1-3])\\s*$"),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX);

    private final String regex;
    TravelLogMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
