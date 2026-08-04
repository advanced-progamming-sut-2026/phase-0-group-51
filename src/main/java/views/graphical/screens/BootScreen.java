package views.graphical.screens;

import com.badlogic.gdx.utils.ScreenUtils;
import graphics.PvzGame;

public final class BootScreen extends BaseScreen {

    public BootScreen(PvzGame game) {
        super(game);
    }

    @Override
    public void show() {
        /*
         * The startup Screen does not display the global HUD.
         */
        game.hideHud();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(
                0.05f,
                0.05f,
                0.05f,
                1f
        );

        super.render(delta);
    }
}