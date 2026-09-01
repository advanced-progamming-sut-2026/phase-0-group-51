package views.graphical.gameplay.manager;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import graphics.PvzGame;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

/** Shared row-and-column placement highlight used by every game screen. */
public final class PlacementHighlightOverlay extends Group {

    private static final Color HIGHLIGHT_COLOR =
        new Color(1f, 1f, 1f, 0.70f);

    private final BoardTransform transform;
    private final Image rowHighlight;
    private final Image columnHighlight;

    public PlacementHighlightOverlay(
        PvzGame game,
        BoardTransform transform
    ) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        if (transform == null) {
            throw new IllegalArgumentException("transform cannot be null");
        }

        this.transform = transform;
        setTouchable(Touchable.disabled);

        Drawable drawable = game.getSkin().newDrawable(
            "white_pixel",
            HIGHLIGHT_COLOR
        );
        rowHighlight = new Image(drawable);
        columnHighlight = new Image(drawable);
        rowHighlight.setTouchable(Touchable.disabled);
        columnHighlight.setTouchable(Touchable.disabled);

        addActor(rowHighlight);
        addActor(columnHighlight);
        hide();
    }

    public void show(int lane, int column) {
        if (lane < 0
            || lane >= BoardTransform.ROWS
            || column < 0
            || column >= BoardTransform.COLUMNS) {
            hide();
            return;
        }

        BoardArea area = transform.getArea();
        rowHighlight.setBounds(
            area.x(),
            transform.tileY(lane),
            area.width(),
            transform.tileHeight()
        );
        columnHighlight.setBounds(
            transform.tileX(column),
            area.y(),
            transform.tileWidth(),
            area.height()
        );
        rowHighlight.setVisible(true);
        columnHighlight.setVisible(true);
    }

    public void hide() {
        rowHighlight.setVisible(false);
        columnHighlight.setVisible(false);
    }
}
