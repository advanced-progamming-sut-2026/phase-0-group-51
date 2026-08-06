package views.graphical.screens;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import graphics.PvzGame;

import java.util.Objects;

public abstract class BaseScreen extends ScreenAdapter {


    protected final PvzGame game;
    protected final Stage stage;
    protected final Skin skin;
    protected final FitViewport viewport;

    protected BaseScreen(PvzGame game) {
        this.game = Objects.requireNonNull(
                game,
                "game cannot be null"
        );
        this.skin = game.getSkin();

        viewport = new FitViewport(
                PvzGame.VIRTUAL_WIDTH,
                PvzGame.VIRTUAL_HEIGHT
        );
        stage = new Stage(
                viewport,
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
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}