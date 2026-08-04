package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public final class NotificationOverlay extends Table {

    public NotificationOverlay() {
        setFillParent(true);

        setTouchable(Touchable.disabled);
        setVisible(false);
    }

    public void showInfo(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        Gdx.app.log(
                "Notification",
                message
        );
    }

    public void showError(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        Gdx.app.error(
                "Notification",
                message
        );
    }
}