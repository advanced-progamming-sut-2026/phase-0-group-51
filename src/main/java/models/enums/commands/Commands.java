package models.enums.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Commands {
    String exitMenuRegex = "^\\s*menu\\s+exit\\s*$";
    String currentMenuRegex = "^\\s*menu\\s+show\\s+current\\s*$";
    String enterMenuRegex="^\\s*menu\\s+enter\\s+(\\S+)\\s*$";
    String menuNamesRegex=("^(?i)(Main|Game|Login|SignUp|Setting|Network|News|Profile|Collection)$");
    String getPattern();
    default Matcher MatchRegex(String input) {
        Matcher matcher = Pattern.compile(getPattern()).matcher(input);
        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
    default boolean matches(String input) {
        return Pattern.compile(getPattern()).matcher(input).matches();
    }
}