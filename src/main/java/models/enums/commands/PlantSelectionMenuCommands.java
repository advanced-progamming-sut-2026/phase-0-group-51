package models.enums.commands;

public enum PlantSelectionMenuCommands implements Commands {
    SHOW_AVAILABLE_PLANT_REGEX("^\\s*show\\s+available\\s+plants\\s*$"),
    SHOW_ALL_PLANT_REGEX("^\\s*show\\s+all\\s+plants\\s*$"),
    ADD_PLANT_REGEX("^\\s*add\\s+plant\\s+-t\\s+(?<type>.+?)\\s*$"),
    REMOVE_PLANT_REGEX("^\\s*remove\\s+plant\\s+-t\\s+(?<type>.+?)\\s*$"),
    BOOST_PLANT_REGEX("^\\s*boost\\s+plant\\s+-t\\s+(?<type>.+?)\\s*$"),
    START_GAME_REGEX("^\\s*start\\s+game\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX);

    private final String regex;

    PlantSelectionMenuCommands(String regex) {
        this.regex = regex;
    }

    @Override
    public String getPattern() {
        return regex;
    }
}
