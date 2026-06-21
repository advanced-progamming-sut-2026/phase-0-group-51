package models.enums.commands;

public enum SignUpMenuCommands implements Commands{
    REGISTER_REGEX(
            """
    ^\\s*register\\s+-u\\s+(?<username>\\S+)\\s+
    -p\\s+(?<password>\\S+)\\s+(?<passwordConfirm>\\S+)\\s+
    -n\\s+(?<nickname>\\S+)\\s+-e\\s+(?<email>\\S+)\\s+
    -g\\s+(?<gender>\\S+)\\s*$"),
    """),
    USERNAME_REGEX("^([a-zA-Z0-9\\-]+)$"),
    NICKNAME_REGEX("^(.{3,30})$"),
    PASSWORD_REGEX( "^[a-zA-Z0-9!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\]+$"),
    STRONG_PASSWORD_REGEX("^(?=.*[a-z])" +
            "(?=.*[A-Z])" +
            "(?=.*\\d)" +
            "(?=.*[!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\])" +
            "[a-zA-Z0-9!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\]{8,}$"),
    SPECIAL_SYMBOLS_REGEX(".*[!#$%^&*()=+{}\\[\\]|/:;'\",<>?\\\\].*"),
    EMAIL_FIRST_PART_REGEX("^[A-Za-z0-9](?!.*\\.\\.)(?:[A-Za-z0-9._-]*[A-Za-z0-9])?$"),
    EMAIL_SECOND_PART_REGEX("^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*(\\.[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)+$"),
    PICK_SECURITY_QUESTION_REGEX("^\\s*pick\\s+question\\s+-q\\s+(\\S+)\\s+-a\\s+(\\S+)\\s+-c\\s+(\\S+)\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    ENTER_MENU_REGEX(Commands.ENTER_MENU_REGEX);
    private final String regex;
    SignUpMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }

}
