package models.enums.commands;

public enum NewsMenuCommands implements Commands{
    showAllNewsRegex("^\\s*menu\\s+news\\s+show-all\\s*$"),
    showUnreadNewsRegex("^\\s*menu\\s+news\\s+show-unread\\s*$"),
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex);
    private final String regex;
    NewsMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
