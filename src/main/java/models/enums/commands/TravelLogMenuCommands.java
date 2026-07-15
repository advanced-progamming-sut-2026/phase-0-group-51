package models.enums.commands;

public enum TravelLogMenuCommands implements Commands{
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    START_VASEBREAKER("^\\s*start\\s+vasebreaker\\s+-s\\s+(?<stage>[1-3])\\s*$"),
    START_WALLNUT_BOWLING("^\\s*start\\s+wallnut\\s+bowling"
            + "\\s+-s\\s+(?<stage>[1-3])\\s*$"),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    ENTER_MENU_REGEX(Commands.ENTER_MENU_REGEX);
    private final String regex;
    TravelLogMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
