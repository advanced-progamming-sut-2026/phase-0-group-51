package views.graphical.screens.minigamesScreen.vaseBreaker;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import lombok.Getter;
import models.minigames.vaseBreaker.Vase;

import java.util.function.Consumer;
@Getter
public class VaseView extends Image {
    private static final String SIMPLE_VASE = "IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_115X150";
    private static final String PLANT_VASE = "IMAGE_VASEBREAKER_VASE_GREEN_VASE_GREEN_115X150";
    private static final String GARGANTUAR_VASE = "IMAGE_VASEBREAKER_VASE_GARGANTUAR_VASE_GARGANTUAR_115X150";
    private final Vase vase;

    public VaseView(PvzGame game, Vase vase, Consumer<Vase> onClicked) {
        super(new TextureRegionDrawable(resolveRegion(game, vase)));
        this.vase = vase;
        setScaling(Scaling.fit);
        addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (vase.isBroken()) {
                            return;
                        }

                        if (onClicked != null) {
                            onClicked.accept(vase);
                        }
                    }
                }
        );
    }


    private static TextureRegion resolveRegion(PvzGame game, Vase vase) {
        String assetId = switch (vase.getVaseType()) {
                    case SIMPLE ->
                            SIMPLE_VASE;

                    case PLANT ->
                            PLANT_VASE;

                    case GARGANTUAR ->
                            GARGANTUAR_VASE;
                };
        return game.getTextureBank().region(assetId);
    }

}
