package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import network.protocol.reaction.ReactionId;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ReactionOverlay extends Table {

    private static final float FADE_IN_SECONDS = 0.12f;
    private static final float HOLD_SECONDS = 1.80f;
    private static final float FADE_OUT_SECONDS = 0.30f;

    private final Label senderLabel;
    private final Label reactionLabel;
    private final Image reactionImage;
    private final Map<ReactionId, Drawable> emojiDrawables;

    public ReactionOverlay(
            PvzGame game,
            Map<ReactionId, Drawable> emojiDrawables
    ) {
        Objects.requireNonNull(game, "game cannot be null");

        EnumMap<ReactionId, Drawable> copy =
                new EnumMap<>(ReactionId.class);

        if (emojiDrawables != null) {
            copy.putAll(emojiDrawables);
        }

        this.emojiDrawables =
                Collections.unmodifiableMap(copy);

        setFillParent(true);
        setTouchable(Touchable.disabled);
        top();
        padTop(78f);

        Table bubble = new Table();
        bubble.pad(10f, 18f, 12f, 18f);
        bubble.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0.05f,
                                0.05f,
                                0.05f,
                                0.88f
                        )
                )
        );

        senderLabel =
                new Label(
                        "",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );

        senderLabel.setColor(
                Color.valueOf("FFE16A")
        );
        senderLabel.setAlignment(
                Align.center
        );

        reactionLabel =
                new Label(
                        "",
                        game.getSkin().get(
                                "big_outline",
                                Label.LabelStyle.class
                        )
                );

        reactionLabel.setColor(
                Color.WHITE
        );
        reactionLabel.setAlignment(
                Align.center
        );

        reactionImage = new Image();
        reactionImage.setScaling(
                Scaling.fit
        );
        reactionImage.setTouchable(
                Touchable.disabled
        );
        reactionImage.setVisible(
                false
        );

        Stack content = new Stack();
        content.setTouchable(
                Touchable.disabled
        );
        content.add(reactionLabel);
        content.add(reactionImage);

        bubble.add(senderLabel)
                .expandX()
                .fillX()
                .center()
                .row();

        bubble.add(content)
                .size(310f, 76f)
                .padTop(4f)
                .center();

        add(bubble)
                .width(360f)
                .height(118f)
                .center();

        setVisible(false);
        getColor().a = 0f;
    }

    public void showReaction(
            String senderUsername,
            ReactionId reactionId
    ) {
        if (reactionId == null) {
            return;
        }

        clearActions();

        String sender =
                senderUsername == null
                        || senderUsername.isBlank()
                        ? "OPPONENT"
                        : senderUsername.trim();

        senderLabel.setText(sender);

        Drawable emoji =
                emojiDrawables.get(
                        reactionId
                );

        if (emoji != null) {
            reactionLabel.setText("");
            reactionLabel.setVisible(false);

            reactionImage.setDrawable(
                    emoji
            );
            reactionImage.setVisible(true);
        } else {
            reactionImage.setVisible(false);
            reactionImage.setDrawable(null);

            reactionLabel.setText(
                    displayText(reactionId)
            );
            reactionLabel.setVisible(true);
        }

        getColor().a = 0f;
        setVisible(true);

        addAction(
                Actions.sequence(
                        Actions.fadeIn(
                                FADE_IN_SECONDS
                        ),
                        Actions.delay(
                                HOLD_SECONDS
                        ),
                        Actions.fadeOut(
                                FADE_OUT_SECONDS
                        ),
                        Actions.run(
                                () -> setVisible(false)
                        )
                )
        );
    }

    private String displayText(
            ReactionId reactionId
    ) {
        return switch (reactionId) {
            case GOOD_LUCK -> "GOOD LUCK!";
            case NICE_MOVE -> "NICE MOVE!";
            case OH_NO -> "OH NO!";
            case SMILE, LAUGH, SHOCKED -> "";
        };
    }
}
