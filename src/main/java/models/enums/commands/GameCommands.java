package models.enums.commands;

public enum GameCommands implements Commands {
    CHEAT_SPAWN_ZOMBIE("^\\s*cheat\\s+spawn-zombie\\s+-t\\s+(?<zombieType>.+?)\\s+-l\\s+<\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*>\\s*$"),
    ZOMBIES_INFO_REGEX("^\\s*zombies\\s+info\\s*$"),
    SHOW_TILE_STATUS_REGEX("^\\s*show\\s+tile\\s+status\\s+-l\\s+\\((?<x>\\d+),\\s*(?<y>\\d+)\\)\\s*$"),
    SHOW_PLANT_STATUS_REGEX("^\\s*show\\s+plants?\\s+status\\s*$"),
    SHOW_MAP_REGEX("^\\s*show\\s+map\\s*$"),
    CHEAT_ADD_PLANT_FOOD_REGEX("^\\s*cheat\\s+add-plant-food\\s*$"),
    FEED_PLANT_REGEX("^\\s*feed\\s+plant\\s+-l\\s+\\((?<x>\\d+),\\s*(?<y>\\d+)\\)\\s*$"),
    PLUCK_PLANT_REGEX("^\\s*pluck\\s+plant\\s+-l\\s+\\((?<x>\\d+),\\s*(?<y>\\d+)\\)\\s*$"),
    CHEAT_REMOVE_COOLDOWN_REGEX("^\\s*cheat\\s+remove-cooldown\\s*$"),
    PLANT_PLANT_REGEX("^\\s*plant\\s+plant\\s+-t\\s+(?<type>.+?)\\s+-l\\s+\\((?<x>\\d+),\\s*(?<y>\\d+)\\)\\s*$"),
    RELEASE_NUKE_REGEX("^\\s*release\\s+the\\s+nuke\\s*$"),
    SHOW_SUN_AMOUNT_REGEX("^\\s*show\\s+sun\\s+amount\\s*$"),
    CHEAT_ADD_SUN_REGEX("^\\s*cheat\\s+add\\s+-n\\s+(?<count>\\d+)\\s+suns\\s*$"),
    PLANT_COLLECT_SUN_REGEX("^\\s*collect\\s+sun\\s+-l\\s+\\((?<x>\\d+),\\s*(?<y>\\d+)\\)\\s*$"),
    ADVANCE_TIME_REGEX("^\\s*advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks\\s*$"),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX);

    private final String regex;

    GameCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
