package models.enums.commands;

public enum ProfileMenuCommands implements Commands{
    showInfoRegex("^\\s*menu\\s+profile\\s+show-info\\s*$"),
    changePasswordRegex("^\\s*menu\\s+profile\\s+change-password\\s+-p\\s+(\\S+)\\s+-o\\s+(\\S+)\\s*$"),
    changeEmailRegex("^\\s*menu\\s+profile\\s+change-email\\s+-e\\s+(\\S+)\\s*$"),
    changeNicknameRegex("^\\s*menu\\s+profile\\s+change-nickname\\s+-u\\s+(\\S+)\\s*$"),
    changeUsernameRegex("^\\s*menu\\s+profile\\s+change-username\\s+-u\\s+(\\S+)\\s*$"),
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex);
    private final String regex;
    ProfileMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
