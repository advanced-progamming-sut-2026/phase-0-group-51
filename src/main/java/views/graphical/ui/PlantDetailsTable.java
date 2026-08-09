package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;

public final class PlantDetailsTable extends Table {

    private static final String PLANT_BACKGROUND =
            "IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_MODERN";

    private static final String BACK =
            "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";

    private static final String BACK_PRESSED =
            "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";

    private final PvzGame game;
    private final PlantCard.ViewData data;

    public PlantDetailsTable(
            PvzGame game,
            PlantCard.ViewData data,
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

        if (onBack == null) {
            throw new IllegalArgumentException(
                    "onBack cannot be null"
            );
        }

        this.game = game;
        this.data = data;

        setFillParent(true);
        setTouchable(Touchable.childrenOnly);

        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("183A78")
                )
        );

        ImageButton backButton =
                createBackButton();

        backButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        PlantDetailsTable.this.remove();
                        onBack.run();
                    }
                }
        );

        Table header = new Table();

        Label plantName =
                new Label(
                        data.plant().name(),
                        game.getSkin(),
                        "big"
                );

        header.add(backButton)
                .left()
                .padLeft(20f)
                .padTop(15f);

        header.add(plantName)
                .expandX()
                .center()
                .padTop(15f);

        header.add()
                .width(backButton.getPrefWidth())
                .padRight(20f);

        add(header)
                .growX()
                .row();

        Table body = new Table();

        Table leftSide =
                createLeftSide();

        Table rightSide =
                new Table();

        body.add(leftSide)
                .width(470f)
                .growY()
                .top()
                .padLeft(40f)
                .padTop(25f);

        body.add(rightSide)
                .grow();

        add(body)
                .grow();
    }

    private Table createLeftSide() {
        Table left = new Table();
        left.top();

        Drawable backgroundDrawable =
                drawable(PLANT_BACKGROUND);

        float previewWidth =
                backgroundDrawable.getMinWidth();

        float previewHeight =
                backgroundDrawable.getMinHeight();

        Stack previewStack =
                new Stack();

        Image background =
                new Image(backgroundDrawable);

        background.setScaling(Scaling.none);
        background.setTouchable(
                Touchable.disabled
        );

        Actor idleActor =
                game.createPamActor(
                        data.plant().idlePamPath(),
                        data.plant().idleClip(),
                        0f,
                        0f,
                        true
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

        animationLayer.addActor(idleActor);

        previewStack.add(background);
        previewStack.add(animationLayer);

        ProgressBar progressBar =
                createProgressBar();

        TextButton findMoreButton =
                new TextButton(
                        "FIND MORE",
                        game.getSkin(),
                        "green"
                );

        left.add(previewStack)
                .size(
                        previewWidth,
                        previewHeight
                )
                .row();

        left.add(progressBar)
                .width(previewWidth)
                .height(17f)
                .padTop(8f)
                .row();

        left.add(findMoreButton)
                .width(220f)
                .padTop(15f);

        return left;
    }

    private ProgressBar createProgressBar() {
        float maximum =
                Math.max(
                        1f,
                        data.requiredSeedPackets()
                );

        ProgressBar progressBar =
                new ProgressBar(
                        0f,
                        maximum,
                        1f,
                        false,
                        game.getSkin(),
                        "xp_yellow"
                );

        progressBar.setValue(
                Math.min(
                        data.seedPackets(),
                        maximum
                )
        );

        progressBar.setAnimateDuration(0f);
        progressBar.setTouchable(
                Touchable.disabled
        );

        return progressBar;
    }

    private ImageButton createBackButton() {
        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.imageUp =
                drawable(BACK);

        style.imageDown =
                drawable(BACK_PRESSED);

        style.imageOver =
                drawable(BACK_PRESSED);

        return new ImageButton(style);
    }

    private Drawable drawable(
            String assetId
    ) {
        TextureRegion region =
                game.getTextureBank()
                        .region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        return new TextureRegionDrawable(
                region
        );
    }
}