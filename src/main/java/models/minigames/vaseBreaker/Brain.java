package models.minigames.vaseBreaker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Brain {
    private final int row;
    private boolean eaten;
    public Brain(int row) {
        if (row < 1 || row > 5) {throw new IllegalArgumentException("Brain row must be between 1 and 5.");
        }
        this.row = row;
        this.eaten = false;
    }
    public void eat() {
        eaten = true;
    }
}
