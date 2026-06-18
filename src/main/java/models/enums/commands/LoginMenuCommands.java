package models.enums.commands;

public enum LoginMenuCommands implements Commands{
    loginRegex("^\\s*login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)(?<stay>\\s+-stay-logged-in)?\\s*$"),
    forgetPasswordRegex("^\\s*forget\\s+password\\s+-u\\s+(\\S+)\\s+-e\\s+(\\S+)\\s*$"),
    answerQuestionRegex("^\\s*answer\\s+-a\\s+(\\S+)\\s*$"),
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex),
    enterMenuRegex(Commands.enterMenuRegex);
    private final String regex;
    LoginMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
