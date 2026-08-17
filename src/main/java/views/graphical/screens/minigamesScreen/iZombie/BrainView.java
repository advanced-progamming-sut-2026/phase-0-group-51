package views.graphical.screens.minigamesScreen.iZombie;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import models.minigames.vaseBreaker.Brain;
import views.graphical.gameplay.board.BoardTransform;

public class BrainView extends Image {
    private static final String BRAIN_ASSET = "IMAGE_UI_CURRENCY_VALENBRAINZ_STACK_0";
    private final Brain brain;

    public BrainView(PvzGame game, Brain brain, BoardTransform transform) {
        super(
                new TextureRegionDrawable(requireRegion(game, BRAIN_ASSET)));
        this.brain = brain;
        setScaling(Scaling.fit);
        setTouchable(Touchable.disabled);
        placeOnBoard(transform);
        refresh();
    }


    private void placeOnBoard(BoardTransform transform) {

        int column = 0;
        int lane = brain.getRow() - 1;
        float tileX = transform.tileX(column);
        float tileY = transform.tileY(lane);
        float width = transform.tileWidth() * 0.75f;
        float height = transform.tileHeight() * 0.75f;
        float x = tileX + (transform.tileWidth() - width) / 2f;
        float y = tileY + (transform.tileHeight() - height) / 2f;
        setBounds(
                x,
                y,
                width,
                height
        );
    }


    public void refresh() {
        setVisible(!brain.isEaten());
    }


    private static TextureRegion requireRegion(PvzGame game, String assetId) {
        TextureRegion region = game.getTextureBank().region(assetId);
        if (region == null) {
            throw new IllegalStateException(
                    "Brain not found: "
                            + assetId
            );
        }

        return region;
    }
}
