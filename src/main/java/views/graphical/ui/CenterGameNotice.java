package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

public final class CenterGameNotice extends Table {

    private static final float NOTICE_WIDTH = 1000f;
    private static final float NOTICE_HEIGHT = 150f;
    private static final float FONT_SCALE = 1.65f;
    private static final float POP_START_SCALE = 0.28f;
    private static final float POP_OVERSHOOT_SCALE = 1.10f;
    private static final float POP_IN_DURATION = 0.16f;
    private static final float POP_SETTLE_DURATION = 0.10f;

    private final Label messageLabel;
    private final Container<Label> messageContainer;

    public CenterGameNotice(Skin skin, boolean consumeInput) {
        setFillParent(true);
        center();

        Label.LabelStyle style = createNoticeStyle(skin);
        messageLabel = new Label("", style);
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(false);
        messageLabel.setFontScale(FONT_SCALE);

        messageContainer = new Container<>(messageLabel);
        messageContainer.setTransform(true);
        messageContainer.setSize(NOTICE_WIDTH, NOTICE_HEIGHT);
        messageContainer.setOrigin(NOTICE_WIDTH / 2f, NOTICE_HEIGHT / 2f);

        add(messageContainer)
            .width(NOTICE_WIDTH)
            .height(NOTICE_HEIGHT)
            .center();

        configureInput(consumeInput);
    }

    public void showSequence(
        List<String> messages,
        float secondsPerMessage,
        Runnable onFinished
    ) {
        if (messages == null || messages.isEmpty()) {
            finish(onFinished);
            return;
        }

        float duration = Math.max(0.01f, secondsPerMessage);
        List<Action> sequence = new ArrayList<>();

        for (String message : messages) {
            if (message == null || message.isBlank()) {
                continue;
            }

            String displayedMessage = message.trim();
            sequence.add(
                Actions.run(
                    () -> popMessage(displayedMessage)
                )
            );
            sequence.add(Actions.delay(duration));
        }

        if (sequence.isEmpty()) {
            finish(onFinished);
            return;
        }

        sequence.add(
            Actions.run(
                () -> finish(onFinished)
            )
        );

        clearActions();
        setVisible(true);
        toFront();
        addAction(
            Actions.sequence(
                sequence.toArray(new Action[0])
            )
        );
    }

    private void popMessage(String message) {
        messageLabel.setText(message);
        messageContainer.clearActions();
        messageContainer.setScale(POP_START_SCALE);
        messageContainer.setColor(Color.WHITE);
        messageContainer.addAction(
            Actions.sequence(
                Actions.scaleTo(
                    POP_OVERSHOOT_SCALE,
                    POP_OVERSHOOT_SCALE,
                    POP_IN_DURATION,
                    Interpolation.pow3Out
                ),
                Actions.scaleTo(
                    1f,
                    1f,
                    POP_SETTLE_DURATION,
                    Interpolation.pow2Out
                )
            )
        );
    }

    private void finish(Runnable onFinished) {
        remove();
        if (onFinished != null) {
            onFinished.run();
        }
    }

    private void configureInput(boolean consumeInput) {
        if (!consumeInput) {
            setTouchable(Touchable.disabled);
            return;
        }

        setTouchable(Touchable.enabled);
        addListener(
            new InputListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    return true;
                }
            }
        );
    }

    private Label.LabelStyle createNoticeStyle(Skin skin) {
        Label.LabelStyle source;
        if (skin.has("big_outline", Label.LabelStyle.class)) {
            source = skin.get("big_outline", Label.LabelStyle.class);
        } else {
            source = skin.get("default", Label.LabelStyle.class);
        }

        Label.LabelStyle style = new Label.LabelStyle();
        style.font = skin.getFont("HOUSE_OF_TERROR");
        style.background = source.background;
        style.fontColor = Color.RED;
        return style;
    }
}
