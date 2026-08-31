package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import graphics.PvzGame;

import lombok.Getter;
import views.graphical.ui.BorderedPanel;

import java.util.Objects;
import java.util.function.Consumer;

@Getter
public final class ChallengePopup
        extends BorderedPanel {

    private static final String TOPPER =
            "IMAGE_UI_PAUSEMENU_WINDOWTOPPER";

    private static final String SUNFLOWER =
            "IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER";


    private final PvzGame game;

    private final String challengerUsername;

    private final Consumer<Boolean>
            decisionHandler;

    private boolean answered;


    public ChallengePopup(
            PvzGame game,
            String challengerUsername,
            Consumer<Boolean> decisionHandler
    ) {

        super(
                game,
                Color.valueOf("8F4909")
        );


        this.game =
                Objects.requireNonNull(
                        game,
                        "game cannot be null"
                );


        if (challengerUsername == null
                || challengerUsername.isBlank()) {

            throw new IllegalArgumentException(
                    "challengerUsername cannot be blank"
            );
        }


        this.challengerUsername =
                challengerUsername.trim();


        this.decisionHandler =
                Objects.requireNonNull(
                        decisionHandler,
                        "decisionHandler cannot be null"
                );


        buildUi();

        pack();
    }


    private void buildUi() {

        Table content =
                getContent();

        content.clearChildren();

        content.pad(
                18f,
                28f,
                28f,
                28f
        );

        Stack topDecoration =
                createTopDecoration();


        content.add(
                        topDecoration
                )
                .width(430f)
                .height(115f)
                .padTop(-55f)
                .padBottom(-10f)
                .row();



        Label title =
                new Label(
                        "ONLINE CHALLENGE",
                        game.getSkin().get(
                                "big_outline",
                                Label.LabelStyle.class
                        )
                );


        title.setColor(
                Color.valueOf("FFE16A")
        );

        title.setAlignment(
                Align.center
        );


        content.add(
                        title
                )
                .width(430f)
                .center()
                .padBottom(15f)
                .row();

        Table messagePanel =
                new Table();


        messagePanel.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("E4D3A7")
                )
        );


        messagePanel.pad(
                18f,
                22f,
                18f,
                22f
        );


        Label username =
                new Label(
                        challengerUsername,
                        game.getSkin().get(
                                "big_outline",
                                Label.LabelStyle.class
                        )
                );


        username.setColor(
                Color.valueOf("7A341D")
        );

        username.setAlignment(
                Align.center
        );


        Label message =
                new Label(
                        "wants to play I, Zombie with you!",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        message.setColor(
                Color.valueOf("5E4A32")
        );

        message.setAlignment(
                Align.center
        );

        message.setWrap(
                true
        );


        messagePanel.add(
                        username
                )
                .growX()
                .padBottom(8f)
                .row();


        messagePanel.add(
                        message
                )
                .width(360f)
                .center();


        content.add(
                        messagePanel
                )
                .width(430f)
                .padBottom(20f)
                .row();

        TextButton acceptButton =
                new TextButton(
                        "ACCEPT",
                        game.getSkin(),
                        "green"
                );


        TextButton rejectButton =
                new TextButton(
                        "REJECT",
                        game.getSkin(),
                        "purple"
                );


        acceptButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        answer(
                                true
                        );
                    }
                }
        );


        rejectButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        answer(
                                false
                        );
                    }
                }
        );


        Table buttons =
                new Table();


        buttons.add(
                        rejectButton
                )
                .width(175f)
                .height(55f)
                .padRight(16f);


        buttons.add(
                        acceptButton
                )
                .width(175f)
                .height(55f);


        content.add(
                        buttons
                )
                .center()
                .padBottom(-25f);
    }


    private Stack createTopDecoration() {

        Stack stack =
                new Stack();


        TextureRegion topperRegion =
                game.getTextureBank()
                        .region(
                                TOPPER
                        );


        if (topperRegion != null) {

            Image topper =
                    new Image(
                            topperRegion
                    );

            topper.setScaling(
                    Scaling.fit
            );

            stack.add(
                    topper
            );
        }


        TextureRegion sunflowerRegion =
                game.getTextureBank()
                        .region(
                                SUNFLOWER
                        );


        if (sunflowerRegion != null) {

            Image sunflower =
                    new Image(
                            sunflowerRegion
                    );

            sunflower.setScaling(
                    Scaling.fit
            );


            Table sunflowerLayer =
                    new Table();

            sunflowerLayer.top();

            sunflowerLayer.add(
                            sunflower
                    )
                    .size(70f)
                    .padTop(-12f);


            stack.add(
                    sunflowerLayer
            );
        }


        return stack;
    }


    private void answer(
            boolean accepted
    ) {

        if (answered) {
            return;
        }


        answered =
                true;


        remove();


        decisionHandler.accept(
                accepted
        );
    }

}