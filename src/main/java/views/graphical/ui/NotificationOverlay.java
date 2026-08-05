package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

public final class NotificationOverlay extends Table {
    private final Label messageLabel;
    private Skin skin;
    public NotificationOverlay(Skin skin) {
        this.skin = skin;
        setFillParent(true);
        top();
        setTouchable(Touchable.disabled);
        setVisible(false);

        messageLabel = new Label("", skin);
        messageLabel.setWrap(true);
        messageLabel.setAlignment(Align.center);
        messageLabel.setFontScale(2f);
        Table notificationBox = new Table();

        TextButton.TextButtonStyle buttonStyle = skin.get(TextButton.TextButtonStyle.class);

        if (buttonStyle.up != null) {
            notificationBox.setBackground(buttonStyle.up);
        }

        notificationBox.pad(18f);
        notificationBox.add(messageLabel).width(500f);

        add(notificationBox).padTop(25f);
    }

    public void showInfo(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        Gdx.app.log("Notification", message);
        showMessage(message, Color.WHITE);
    }

    public void showError(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        Gdx.app.error("Notification", message);
        showMessage(message, Color.RED);
    }
    private void showMessage(String message, Color textColor) {
        clearActions();

        messageLabel.setText(message.trim());
        messageLabel.setColor(textColor);

        setColor(1f, 1f, 1f, 1f);
        setVisible(true);
        toFront();
        addAction(Actions.sequence(Actions.delay(3f), Actions.fadeOut(0.35f), Actions.visible(false)
        ));
    }
}