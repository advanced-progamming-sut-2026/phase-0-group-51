package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import com.badlogic.gdx.utils.Align;

import graphics.PvzGame;

import network.protocol.reaction.ReactionId;

import java.util.Objects;


public final class ReactionOverlay extends Table {

    private static final float FADE_IN_SECONDS =
            0.12f;

    private static final float HOLD_SECONDS =
            1.80f;

    private static final float FADE_OUT_SECONDS =
            0.30f;


    private final Label senderLabel;

    private final Label reactionLabel;


    public ReactionOverlay(
            PvzGame game
    ) {

        Objects.requireNonNull(
                game,
                "game cannot be null"
        );


        setFillParent(
                true
        );

        setTouchable(
                Touchable.disabled
        );

        top();

        padTop(
                78f
        );


        Table bubble =
                new Table();

        bubble.pad(
                12f,
                22f,
                14f,
                22f
        );

        bubble.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0.05f,
                                0.05f,
                                0.05f,
                                0.90f
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
                Color.valueOf(
                        "FFE16A"
                )
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


        bubble.add(
                        senderLabel
                )
                .expandX()
                .fillX()
                .center()
                .row();


        bubble.add(
                        reactionLabel
                )
                .expandX()
                .fillX()
                .center()
                .padTop(5f);


        add(
                bubble
        )
                .width(390f)
                .height(126f)
                .center();


        setVisible(
                false
        );

        getColor().a =
                0f;
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


        senderLabel.setText(
                sender
        );


        reactionLabel.setText(
                displayText(
                        reactionId
                )
        );


        getColor().a =
                0f;


        setVisible(
                true
        );


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
                                () -> setVisible(
                                        false
                                )
                        )
                )
        );
    }


    private String displayText(
            ReactionId reactionId
    ) {

        return switch (reactionId) {

            case GOOD_LUCK ->
                    "GOOD LUCK!";

            case NICE_MOVE ->
                    "NICE MOVE!";

            case OH_NO ->
                    "OH NO!";

            case SMILE ->
                    "\uD83D\uDE42";

            case LAUGH ->
                    "\uD83D\uDE02";

            case SHOCKED ->
                    "\uD83D\uDE31";
        };
    }
}
