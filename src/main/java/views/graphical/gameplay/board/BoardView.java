package views.graphical.gameplay.board;

import com.badlogic.gdx.scenes.scene2d.Group;
import lombok.Getter;
import models.Board.Board;
import models.Board.Tile;

public final class BoardView extends Group {

    @Getter
    private final Board board;
    private final BoardTransform transform;

    private final TileView[][] tileViews;

    public BoardView(
            Board board,
            BoardTransform transform
    ) {
        if (board == null) {
            throw new IllegalArgumentException(
                    "board cannot be null"
            );
        }

        if (transform == null) {
            throw new IllegalArgumentException(
                    "transform cannot be null"
            );
        }

        this.board = board;
        this.transform = transform;

        tileViews = new TileView[
                board.getLaneCount()
                ][
                board.getColumnCount()
                ];

        buildTiles();
    }

    private void buildTiles() {
        for (
                int lane = 0;
                lane < board.getLaneCount();
                lane++
        ) {
            for (
                    int column = 0;
                    column < board.getColumnCount();
                    column++
            ) {
                Tile modelTile =
                        board.getTile(
                                lane,
                                column
                        );

                TileView tileView =
                        new TileView(modelTile);

                tileView.setBounds(
                        transform.tileX(column),
                        transform.tileY(lane),
                        transform.tileWidth(),
                        transform.tileHeight()
                );

                tileViews[lane][column] =
                        tileView;

                addActor(tileView);
            }
        }
    }

    public TileView getTileView(
            int lane,
            int column
    ) {
        return tileViews[lane][column];
    }

}
