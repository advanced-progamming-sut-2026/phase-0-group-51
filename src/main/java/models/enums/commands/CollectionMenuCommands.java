package models.enums.commands;

public enum CollectionMenuCommands implements Commands{
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX),
    ENTER_MENU_REGEX(Commands.ENTER_MENU_REGEX),
    SHOW_ALL_PLANTS_REGEX("^menu\\s+collection\\s+show-all-plants$"),
    SHOW_PLANTS_REGEX("^menu\\s+collection\\s+show-plants$"),
    SHOW_ZOMBIES_REGEX("^menu\\s+collection\\s+show-zombies$"),
    SHOW_ALL_ZOMBIES_REGEX("^menu\\s+collection\\s+show-all-zombies$"),
    SHOW_A_PLANT_REGEX("^menu\\s+collection\\s+show-plant\\s+-p\\s+(?<plantName>.+?)\\s*$"),
    SHOW_A_ZOMBIE_REGEX( "^menu\\s+collection\\s+show-zombie\\s+-z\\s+(?<zombieName>.+?)\\s*$"),
    UPGRADE_PLANT_REGEX("^menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(?<plantName>.+?)\\s*$"),
    PURCHASE_PLANT_REGEX("^menu\\s+collection\\s+purchase-plant\\s+-p\\s+(?<plantName>.+?)\\s*$");

    private final String regex;
    CollectionMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
