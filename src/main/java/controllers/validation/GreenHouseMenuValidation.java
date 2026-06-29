package controllers.validation;

public class GreenHouseMenuValidation {
    public int x,y;
    public boolean isNumberXValid(String xString){
        try {
            x = Integer.parseInt(xString);
            return x >= 1 && x <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public boolean isNumberYValid(String yString){
        try {
            y = Integer.parseInt(yString);
            return y >= 1 && y <= 4;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
