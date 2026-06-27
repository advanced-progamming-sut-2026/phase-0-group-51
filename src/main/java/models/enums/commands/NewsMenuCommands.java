package models.enums.commands;

public enum NewsMenuCommands implements Commands{
    SHOW_ALL_NEWS_REGEX("^\\s*menu\\s+news\\s+show-all\\s*$"),
    SHOW_UNREAD_NEWS_REGEX("^\\s*menu\\s+news\\s+show-unread\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX);
    private final String regex;
    NewsMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
