package models.enums.commands;

public enum GameMenuCommands implements Commands{
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    ENTER_MENU_REGEX(Commands.ENTER_MENU_REGEX),
    ENTER_CHAPTER_REGEX("^menu\\s+enter\\s+chapter\\s+-c\\s+(?<chapterName>.+?)$"),
    ENTER_LEVEL_REGEX("^menu\\s+enter\\s+level\\s+-l\\s+(?<levelNumber>.+?)$"),
    LOCKED_PLANTS_MODE_REGEX("^choose\\s+locked-plants\\s+mode\\s+-m\\s+(?<mode>family|forced|1|2)$"),
    GREENHOUSE_REGEX("^menu\\s+greenhouse$"),
    TRAVEL_LOG_REGEX("^menu\\s+travel-log$"),
    LEADERBOARD_REGEX("^menu\\s+leaderboard$"),
    COIN_WALLET_REGEX("^menu\\s+coin-wallet$"),
    GEM_WALLET_REGEX("^menu\\s+gem-wallet$"),
    CHEAT_ADD_REGEX("^menu\\s+cheat\\s+add\\s+(?<amount>\\d+)\\s+(?<kind>\\S+)$")
    ;
    private final String regex;
    GameMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
