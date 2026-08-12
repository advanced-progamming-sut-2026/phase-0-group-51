package views.graphical.ui;

import Data.loader.ZombieRegistry;
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

public final class ZombieDetailsTable extends Table {

    private static final String ZOMBIE_BACKGROUND = "IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_MODERN";
    private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String TOUGHNESS_ICON = "IMAGE_UI_ALMANAC_ZOMBIES_ZOMBIETOUGHNESS_ICON";
    private static final String SPEED_ICON = "IMAGE_UI_ALMANAC_ZOMBIES_ZOMBIESPEED_ICON";

    private static final float PREVIEW_WIDTH = 320f;
    private static final float PREVIEW_HEIGHT = 440f;

    private static final float STAT_WIDTH = 250f;
    private static final float DESCRIPTION_WIDTH = 530f;


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

        if (onBack == null) {
            throw new IllegalArgumentException(
                    "onBack cannot be null"
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


        ImageButton backButton =
                createBackButton();

        backButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        ZombieDetailsTable.this.remove();
                        onBack.run();
                    }
                }
        );


        Label zombieName =
                new Label(
                        data.alias(),
                        game.getSkin(),
                        "big"
                );


        Table header =
                new Table();

        header.add(backButton)
                .left()
                .padLeft(20f)
                .padTop(15f);

        header.add(zombieName)
                .expandX()
                .center()
                .padTop(15f);

        header.add()
                .width(backButton.getPrefWidth())
                .padRight(20f);


        add(header)
                .growX()
                .row();


        Table body =
                new Table();

        Table leftSide =
                createLeftSide();

        Table rightSide =
                createRightSide();


        ScrollPane rightScroll =
                new ScrollPane(
                        rightSide,
                        game.getSkin()
                );

        rightScroll.setFadeScrollBars(false);
        rightScroll.setOverscroll(false, false);
        rightScroll.setScrollingDisabled(true, false);


        body.add(leftSide)
                .width(430f)
                .growY()
                .top()
                .padLeft(45f)
                .padTop(25f);

        body.add(rightScroll)
                .grow()
                .top()
                .padTop(25f)
                .padRight(35f);


        add(body)
                .grow();
    }


    private Table createLeftSide() {
        Table left =
                new Table();

        left.top();


        Stack preview =
                new Stack();


        Actor background =
                createZombieBackground();

        preview.add(background);


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


        left.add(preview)
                .size(
                        PREVIEW_WIDTH,
                        PREVIEW_HEIGHT
                );


        return left;
    }


    private Table createRightSide() {
        Table right =
                new Table();

        right.top().left();


        String alias =
                data.alias();


        /*
         * First row:
         *
         * TOUGHNESS          SPEED
         * Protected          Basic
         */

        Table stats =
                new Table();

        stats.top().left();


        stats.add(
                        createStat(
                                TOUGHNESS_ICON,
                                "TOUGHNESS",
                                ZombieRegistry
                                        .getToughness(alias)
                        )
                )
                .width(STAT_WIDTH)
                .left();


        stats.add(
                        createStat(
                                SPEED_ICON,
                                "SPEED",
                                ZombieRegistry
                                        .getSpeed(alias)
                        )
                )
                .width(STAT_WIDTH)
                .left()
                .row();
        right.add(stats)
                .growX()
                .left()
                .row();
        String damage =
                ZombieRegistry.getDamage(alias);

        String weakness =
                ZombieRegistry.getWeakness(alias);

        String special =
                ZombieRegistry.getSpecial(alias);


        if (!damage.isBlank()) {
            right.add(
                            createTextStat(
                                    "Damage",
                                    damage
                            )
                    )
                    .width(DESCRIPTION_WIDTH)
                    .left()
                    .padTop(18f)
                    .row();
        }


        if (!weakness.isBlank()) {
            right.add(
                            createTextStat(
                                    "Weakness",
                                    weakness
                            )
                    )
                    .width(DESCRIPTION_WIDTH)
                    .left()
                    .padTop(10f)
                    .row();
        }


        if (!special.isBlank()) {
            right.add(
                            createTextStat(
                                    "Special",
                                    special
                            )
                    )
                    .width(DESCRIPTION_WIDTH)
                    .left()
                    .padTop(10f)
                    .row();
        }

        /*
         * Short white description.
         */

        String overallDescription =
                ZombieRegistry
                        .getOverallDisc(alias);


        if (!overallDescription.isBlank()) {

            Label overallLabel =
                    createWrappedLabel(
                            overallDescription,
                            Color.WHITE
                    );

            right.add(overallLabel)
                    .width(DESCRIPTION_WIDTH)
                    .left()
                    .padTop(30f)
                    .row();
        }


        /*
         * Yellow Almanac personality/fun description.
         */

        String funDescription =
                ZombieRegistry
                        .getFunDisc(alias);


        if (!funDescription.isBlank()) {

            Label funLabel =
                    createWrappedLabel(
                            funDescription,
                            Color.valueOf("FFD75A")
                    );

            right.add(funLabel)
                    .width(DESCRIPTION_WIDTH)
                    .left()
                    .padTop(20f)
                    .padBottom(20f)
                    .row();
        }


        return right;
    }


    private Table createStat(
            String iconAsset,
            String title,
            String value
    ) {
        Table stat =
                new Table();

        stat.left();


        Actor icon =
                createIconOrPlaceholder(
                        iconAsset
                );


        Table text =
                new Table();

        text.left();


        Label titleLabel =
                new Label(
                        title,
                        game.getSkin()
                );

        titleLabel.setColor(
                Color.LIGHT_GRAY
        );


        Label valueLabel =
                new Label(
                        value == null
                                || value.isBlank()
                                ? "—"
                                : value,
                        game.getSkin(),
                        "big"
                );

        valueLabel.setColor(
                Color.WHITE
        );

        valueLabel.setWrap(true);


        text.add(titleLabel)
                .left()
                .row();

        text.add(valueLabel)
                .width(175f)
                .left();


        stat.add(icon)
                .size(48f)
                .top()
                .padRight(8f);

        stat.add(text)
                .left()
                .top();


        return stat;
    }


    private Label createWrappedLabel(
            String text,
            Color color
    ) {
        Label label =
                new Label(
                        text,
                        game.getSkin()
                                .get(
                                        "big",
                                        Label.LabelStyle.class
                                )
                );

        label.setWrap(true);
        label.setColor(color);

        return label;
    }


    private Actor createZombieBackground() {

        if (ZOMBIE_BACKGROUND != null) {

            Image image =
                    new Image(
                            drawable(
                                    ZOMBIE_BACKGROUND
                            )
                    );

            image.setScaling(
                    Scaling.stretch
            );

            image.setTouchable(
                    Touchable.disabled
            );

            return image;
        }


        /*
         * Temporary placeholder until you add
         * the real Almanac lawn/background asset.
         */
        Table placeholder =
                new Table();

        placeholder.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("537D36")
                )
        );

        placeholder.setTouchable(
                Touchable.disabled
        );

        return placeholder;
    }


    private Actor createIconOrPlaceholder(
            String assetId
    ) {

        if (assetId != null
                && !assetId.isBlank()) {

            Image image =
                    new Image(
                            drawable(assetId)
                    );

            image.setScaling(
                    Scaling.fit
            );

            image.setTouchable(
                    Touchable.disabled
            );

            return image;
        }


        /*
         * Temporary purple square similar to
         * the stat boxes in the real Almanac.
         */
        Table placeholder =
                new Table();

        placeholder.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("7960B5")
                )
        );

        placeholder.setTouchable(
                Touchable.disabled
        );

        return placeholder;
    }
    private Table createTextStat(
            String title,
            String value
    ) {
        Table row = new Table();
        row.left();

        Label titleLabel =
                new Label(
                        title + ":",
                        game.getSkin(),
                        "big"
                );

        titleLabel.setColor(
                Color.valueOf("FFD75A")
        );

        Label valueLabel =
                new Label(
                        value,
                        game.getSkin(),
                        "big"
                );

        valueLabel.setWrap(true);
        valueLabel.setColor(Color.WHITE);

        row.add(titleLabel)
                .top()
                .left()
                .padRight(6f);

        row.add(valueLabel)
                .width(DESCRIPTION_WIDTH - 100f)
                .top()
                .left();

        return row;
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