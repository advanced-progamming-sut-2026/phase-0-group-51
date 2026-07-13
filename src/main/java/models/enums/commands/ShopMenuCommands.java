package models.enums.commands;

public enum ShopMenuCommands implements  Commands{
    SHOP_LIST_REGEX("^\\s*shop\\s+list\\s*$"),
    SHOP_DAILY_REGEX("^\\s*shop\\s+daily\\s*$"),
    SHOP_BUY_REGEX("^\\s*shop\\s+buy\\s+-i\\s+(?<item_id>\\S+)\\s+-n\\s+(?<count>\\S+)\\s*" +
            "(-t\\s+(?<plant_type>\\S+))?\\s*$"),
    SHOP_BUY_DAILY("^\\s*shop\\s+buy\\s+daily\\s*$"),
    EXIT_MENU_REGEX(Commands.EXIT_MENU_REGEX),
    CURRENT_MENU_REGEX(Commands.CURRENT_MENU_REGEX);
    private final String regex;
    ShopMenuCommands(String regex) {this.regex = regex;}
    @Override
    public String getPattern() {
        return regex;
    }
}
