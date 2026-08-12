package views.graphical.gameplay.board;

public final class BoardTransform {

    public static final int ROWS = 5;
    public static final int COLUMNS = 9;

    private BoardArea area;

    public BoardTransform(BoardArea area) {
        this.area = area;
    }

    public void setArea(BoardArea area) {
        this.area = area;
    }

    public BoardArea getArea() {
        return area;
    }

    public float tileWidth() {
        return area.width() / COLUMNS;
    }

    public float tileHeight() {
        return area.height() / ROWS;
    }

    public float tileX(int column) {
        return area.x()
                + column * tileWidth();
    }

    public float tileY(int lane) {
        return area.y()
                + (ROWS - 1 - lane)
                * tileHeight();
    }
}