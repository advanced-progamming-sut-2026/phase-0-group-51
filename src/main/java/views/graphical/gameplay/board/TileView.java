package views.graphical.gameplay.board;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.Getter;
import lombok.Setter;
import models.Board.Tile;

import java.util.function.Consumer;

public final class TileView extends Actor {

    @Getter
    private final Tile tile;
    @Setter
    private Consumer<Tile> onClicked;

    public TileView(Tile tile) {
        if (tile == null) {
            throw new IllegalArgumentException(
                    "tile cannot be null"
            );
        }

        this.tile = tile;

        addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (onClicked != null) {
                            onClicked.accept(tile);
                        }
                    }
                }
        );
    }

    public int getLane() {
        return tile.getLane();
    }

    public int getColumn() {
        return tile.getColumn();
    }

}