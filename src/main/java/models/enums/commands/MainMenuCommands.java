package models.enums.commands;

public enum MainMenuCommands implements Commands{
    logoutRegex("^\\s*menu\\s+logout\\s*"),
    currentMenuRegex(Commands.currentMenuRegex),
    enterMenuRegex(Commands.enterMenuRegex);
    private final String regex;
    MainMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
