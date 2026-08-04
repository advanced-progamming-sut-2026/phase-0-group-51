package views.graphical.screens;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import graphics.PvzGame;

import java.util.Objects;

public abstract class BaseScreen extends ScreenAdapter {

    protected final PvzGame game;
    protected final Stage stage;
    protected final Skin skin;

    protected BaseScreen(PvzGame game) {
        this.game = Objects.requireNonNull(
                game,
                "game cannot be null"
        );
        this.skin = game.getSkin();

        stage = new Stage(
                new ExtendViewport(
                        PvzGame.VIRTUAL_WIDTH,
                        PvzGame.VIRTUAL_HEIGHT
                ),
                game.getBatch()
        );
    }

    public InputProcessor getInputProcessor() {
        return stage;
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(delta, 0.1f);

        stage.act(safeDelta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(
                width,
                height,
                true
        );
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}