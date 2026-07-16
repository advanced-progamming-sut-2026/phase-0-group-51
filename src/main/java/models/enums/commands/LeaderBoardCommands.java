package models.enums.commands;

public enum LeaderBoardCommands implements Commands {
    SHOW_LEADERBOARD("^\\s*show\\s+leaderboard\\s*$"),
    SORT_LEADERBOARD("^\\s*sort\\s+leaderboard\\s+-b\\s+"
            + "(?<column>username|progress|minigames|daily-quests|non-daily-quests|score)"
            + "\\s+-o\\s+(?<order>asc|desc)\\s*$"
    ),
    CURRENT_MENU(Commands.CURRENT_MENU_REGEX),
    EXIT_MENU(Commands.EXIT_MENU_REGEX);

    private final String regex;

    LeaderBoardCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
