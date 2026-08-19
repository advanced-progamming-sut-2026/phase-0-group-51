package controllers.validation;

import models.greenHouse.GreenHouse;

public class GreenHouseMenuValidation {
    public int x, y;

    public boolean isNumberXValid(String xString) {
        try {
            x = Integer.parseInt(xString);
            return x >= 1 && x <= GreenHouse.COLUMNS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isNumberYValid(String yString) {
        try {
            y = Integer.parseInt(yString);
            return y >= 1 && y <= GreenHouse.ROWS;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
