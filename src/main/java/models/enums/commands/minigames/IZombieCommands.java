package models.enums.commands.minigames;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum IZombieCommands {
    PLACE_ZOMBIE("^\\s*place\\s+zombie\\s+(?<zombieName>\\S+)\\s+(?<x>-?\\d+)\\s+(?<y>-?\\d+)\\s*$"),
    SHOW_ROSTER("^\\s*show\\s+roster\\s*$"),
    ADVANCE_TIME("^\\s*advance\\s+time\\s+(?<count>-?\\d+)\\s*$"),
    SHOW_STATUS("^\\s*show\\s+status\\s*$"),
    SHOW_MAP("^\\s*show\\s+map\\s*$"),
    CURRENT_MENU("^\\s*show\\s+current\\s+menu\\s*$"),
    EXIT_MENU("^\\s*exit\\s*$");

    private final Pattern pattern;

    IZombieCommands(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public boolean matches(String input) {
        return input != null && pattern.matcher(input).matches();
    }

    public Matcher getMatcher(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(input);
        return matcher.matches() ? matcher : null;
    }
}
