package models.enums.commands;

public enum NetworkMenuCommands implements Commands{
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX);

    private final String regex;

    NetworkMenuCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
