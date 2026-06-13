package models.Minigames;

public class Brain {
    private int row; //شماره ردیف مغز 1 تا 5
    private boolean isEaten;

    public boolean isEaten() {
        return isEaten;
    }

    public int getRow() {
        return row;
    }

    public void setEaten(boolean eaten) {
        isEaten = eaten;
    }
    public void eat(){} //زامبی بهش رسید خورده بشه
}
