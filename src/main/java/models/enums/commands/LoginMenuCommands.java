package models.enums.commands;

public enum LoginMenuCommands implements Commands{
    LOGIN_REGEX("^\\s*login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)(?<stay>\\s+-stay-logged-in)?\\s*$"),
    FORGET_PASSWORD_REGEX("^\\s*forget\\s+password\\s+-u\\s+(\\S+)\\s+-e\\s+(\\S+)\\s*$"),
    ANSWER_QUESTION_REGEX("^\\s*answer\\s+-a\\s+(\\S+)\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    ENTER_MENU_REGEX(Commands.ENTER_MENU_REGEX);
    private final String regex;
    LoginMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
