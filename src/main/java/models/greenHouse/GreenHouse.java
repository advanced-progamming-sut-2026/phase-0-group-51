
package models.greenHouse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GreenHouse {
    public static final int ROWS = 4;
    public static final int COLUMNS = 5;
    private final FlowerPot[][] pots;
    public GreenHouse() {
        pots = new FlowerPot[ROWS][COLUMNS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                pots[row][col] = new FlowerPot(row + 1, col + 1);
            }
        }
    }
    public FlowerPot getPot(int row, int column) {
        return pots[row - 1][column - 1];
    }
    public FlowerPot[][] getPots() {
        return pots;
    }
}
