package models.enums.commands;

public enum SignUpMenuCommands implements Commands{
    registerRegex("^\\s*register\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)\\s+(?<passwordConfirm>\\S+)\\s+-n\\s+(?<nickname>\\S+)\\s+-e\\s+(?<email>\\S+)\\s+-g\\s+(?<gender>\\S+)\\s*$"),
    usernameRegex("^([a-zA-Z0-9\\-]+)$"),
    nicknameRegex("^(.{3,30})$"),
    passwordRegex( "^[a-zA-Z0-9!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\]+$"),
    strongPasswordRegex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\])[a-zA-Z0-9!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\]{8,}$"),
    specialSymbolsRegex(".*[!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\].*"),
    emailFirstPartRegex("^[A-Za-z0-9](?!.*\\.\\.)(?:[A-Za-z0-9._-]*[A-Za-z0-9])?$"),
    emailSecondPartRegex("^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*(\\.[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)+$"),
    pickSecurityQuestion("^\\s*pick\\s+question\\s+-q\\s+(\\S+)\\s+-a\\s+(\\S+)\\s+-c\\s+(\\S+)\\s*$"),
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex),
    enterMenuRegex(Commands.enterMenuRegex);
    private final String regex;
    SignUpMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }

}
