package models.enums.commands;

public enum GameMenuCommands implements Commands{
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex),
    enterMenuRegex(Commands.enterMenuRegex),
    ENTER_CHAPTER("^menu\\s+enter\\s+chapter\\s+-c\\s+(?<chapterName>\\S+)$"),
    GREENHOUSE("^menu\\s+greenhouse$"),
    TRAVEL_LOG("^menu\\s+travel-log$"),
    LEADERBOARD("^menu\\s+leaderboard$"),
    COIN_WALLET("^menu\\s+coin-wallet$"),
    GEM_WALLET("^menu\\s+gem-wallet$"),
    CHEAT_ADD("^menu\\s+cheat\\s+add\\s+(?<amount>\\d+)\\s+(?<kind>\\S+)$")
    ;
    private final String regex;
    GameMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
