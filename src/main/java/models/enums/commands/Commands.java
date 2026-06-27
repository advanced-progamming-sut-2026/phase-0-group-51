package models.enums.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Commands {
    String EXIT_MENU_REGEX = "^\\s*menu\\s+exit\\s*$";
    String CURRENT_MENU_REGEX = "^\\s*menu\\s+show\\s+current\\s*$";
    String ENTER_MENU_REGEX="^\\s*menu\\s+enter\\s+(?<menuName>\\S+)\\s*$";
    String MENU_NAME_REGEX=("^(?i)(Main|Game|Login|SignUp|Setting|Network|News|Profile|Collection)$");
    String getPattern();
    default Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile(getPattern()).matcher(input);
        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
    default boolean matches(String input) {
        return Pattern.compile(getPattern()).matcher(input).matches();
    }
    default String getGroup(String input, String group) {
        return getMatcher(input).group(group);
    }
}