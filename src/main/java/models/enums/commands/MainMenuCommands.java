package models.enums.commands;

public enum MainMenuCommands implements Commands{
    LOGOUT_REGEX("^\\s*menu\\s+logout\\s*"),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    ENTER_MENU_REGEX(Commands.ENTER_MENU_REGEX);
    private final String regex;
    MainMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
