package views.graphical.ui;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import graphics.PvzGame;

import java.util.Objects;

public final class GlobalUiLayer implements Disposable {

    private final Stage stage;

    private final GlobalHud hud;
    private final NotificationOverlay notificationOverlay;

    public GlobalUiLayer(
            PvzGame game,
            Skin skin
    ) {
        Objects.requireNonNull(game, "game cannot be null");
        Objects.requireNonNull(skin, "skin cannot be null");

        stage = new Stage(
                new ExtendViewport(
                        PvzGame.VIRTUAL_WIDTH,
                        PvzGame.VIRTUAL_HEIGHT
                ),
                game.getBatch()
        );

        hud = new GlobalHud(skin);
        notificationOverlay = new NotificationOverlay();

        stage.addActor(hud);
        stage.addActor(notificationOverlay);
    }

    public InputProcessor getInputProcessor() {
        return stage;
    }

    public void render(float delta) {
        float safeDelta = Math.min(delta, 0.1f);

        stage.act(safeDelta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(
                width,
                height,
                true
        );
    }

    public void showHud(
            int coins,
            int gems,
            boolean showBackButton,
            Runnable backAction
    ) {
        hud.configure(
                coins,
                gems,
                showBackButton,
                backAction
        );
    }

    public void hideHud() {
        hud.hideHud();
    }

    public void updateCurrencies(
            int coins,
            int gems
    ) {
        hud.updateCurrencies(coins, gems);
    }

    public void notifyInfo(String message) {
        notificationOverlay.showInfo(message);
    }

    public void notifyError(String message) {
        notificationOverlay.showError(message);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}