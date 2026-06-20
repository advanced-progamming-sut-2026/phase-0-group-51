package models.enums.commands;

public enum CollectionMenuCommands implements Commands{
    exitMenuRegex(Commands.exitMenuRegex),
    currentMenuRegex(Commands.currentMenuRegex),
    enterMenuRegex(Commands.enterMenuRegex),
    SHOW_ALL_PLANTS("^menu\\s+collection\\s+show-all-plants$"),
    SHOW_PLANTS("^menu\\s+collection\\s+show-plants$"),
    SHOW_ZOMBIES("^menu\\s+collection\\s+show-zombies$"),
    SHOW_ALL_ZOMBIES("^menu\\s+collection\\s+show-all-zombies$"),
    SHOW_A_PLANT("^menu\\s+collection\\s+show-plant\\s+-p\\s+(?<plantName>\\S+)$"),
    SHOW_A_ZOMBIE("^menu\\s+collection\\s+show-zombie\\s+-z\\s+(?<zombieName>\\S+)$"),
    UPGRADE_PLANT("^menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(?<plantName>\\S+)$"),
    PURCHASE_PLANT("^menu\\s+collection\\s+purchase-plant\\s+-p\\s+(?<plantName>\\S+)$")
    ;

    private final String regex;
    CollectionMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
