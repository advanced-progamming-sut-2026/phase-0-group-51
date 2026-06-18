package controllers.validation;

public class SettingMenuValidation {
    public int dl;
    public boolean isDifficultyLevelValid(String dlString){
        try {
            dl = Integer.parseInt(dlString);
            return dl >= 1 && dl <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
