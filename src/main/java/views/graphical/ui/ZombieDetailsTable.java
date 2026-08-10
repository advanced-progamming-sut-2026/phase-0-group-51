package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import graphics.PvzGame;

public final class ZombieDetailsTable extends Table {

    private final PvzGame game;
    private final ZombieCard.ViewData data;

    public ZombieDetailsTable(
            PvzGame game,
            ZombieCard.ViewData data,
            Runnable onBack
    ) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        if (data == null) {
            throw new IllegalArgumentException(
                    "data cannot be null"
            );
        }

        this.game = game;
        this.data = data;

        setFillParent(true);
        setTouchable(Touchable.enabled);

        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("183A78")
                )
        );

        TextButton backButton =
                new TextButton(
                        "BACK",
                        game.getSkin()
                );

        backButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        remove();
                        onBack.run();
                    }
                }
        );

        Label nameLabel =
                new Label(
                        data.alias(),
                        game.getSkin()
                );

        Table header =
                new Table();

        header.add(backButton)
                .left()
                .pad(15f);

        header.add(nameLabel)
                .expandX()
                .center();

        add(header)
                .growX()
                .row();


        Stack preview =
                new Stack();

        Actor idleActor =
                game.createPamActor(
                        data.idlePamPath(),
                        data.idleClip(),
                        0f,
                        0f,
                        true,
                        data.idleVisibleParts()
                );

        WidgetGroup animationLayer =
                new WidgetGroup() {
                    @Override
                    public void layout() {
                        idleActor.setPosition(
                                getWidth() / 2f,
                                getHeight() / 2f
                        );
                    }
                };

        animationLayer.setTouchable(
                Touchable.disabled
        );

        animationLayer.addActor(
                idleActor
        );

        preview.add(
                animationLayer
        );

        add(preview)
                .grow()
                .minWidth(0f)
                .minHeight(0f);
    }
}