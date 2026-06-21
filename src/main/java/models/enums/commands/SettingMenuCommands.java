package models.enums.commands;

public enum SettingMenuCommands implements Commands{
    CHANGE_DIFFICULTY_LEVEL_REGEX("^\\s*menu\\s+settings\\s+change-difficulty\\s+-l\\s+(\\S+)\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX);
    private final String regex;
    SettingMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
