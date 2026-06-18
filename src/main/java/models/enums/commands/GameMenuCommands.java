package models.enums.commands;

public enum GameMenuCommands implements Commands{
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex),
    enterMenuRegex(Commands.enterMenuRegex);
    private final String regex;
    GameMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
