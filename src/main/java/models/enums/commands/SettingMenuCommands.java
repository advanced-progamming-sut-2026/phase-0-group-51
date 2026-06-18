package models.enums.commands;

public enum SettingMenuCommands implements Commands{
    changeDifficultyLevel("^\\s*menu\\s+settings\\s+change-difficulty\\s+-l\\s+(\\S+)\\s*$"),
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex);
    private final String regex;
    SettingMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
