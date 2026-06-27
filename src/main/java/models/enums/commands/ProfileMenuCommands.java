package models.enums.commands;

public enum ProfileMenuCommands implements Commands{
    SHOW_INFO_REGEX("^\\s*menu\\s+profile\\s+show-info\\s*$"),
    CHANGE_PASSWORD_REGEX("^\\s*menu\\s+profile\\s+change-password\\s+-p\\s+(\\S+)\\s+-o\\s+(\\S+)\\s*$"),
    CHANGE_EMAIL_REGEX("^\\s*menu\\s+profile\\s+change-email\\s+-e\\s+(\\S+)\\s*$"),
    CHANGE_NICKNAME_REGEX("^\\s*menu\\s+profile\\s+change-nickname\\s+-u\\s+(\\S+)\\s*$"),
    CHANGE_USERNAME_REGEX("^\\s*menu\\s+profile\\s+change-username\\s+-u\\s+(\\S+)\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX);
    private final String regex;
    ProfileMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
