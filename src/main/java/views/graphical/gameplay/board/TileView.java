package views.graphical.gameplay.board;

import com.badlogic.gdx.scenes.scene2d.Actor;
import models.Board.Tile;

public final class TileView extends Actor {

    private final Tile tile;

    public TileView(Tile tile) {
        if (tile == null) {
            throw new IllegalArgumentException(
                    "tile cannot be null"
            );
        }

        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }

    public int getLane() {
        return tile.getLane();
    }

    public int getColumn() {
        return tile.getColumn();
    }
}