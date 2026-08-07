package views.graphical.ui;

import Data.loader.PlantData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import lombok.Getter;

public final class PlantCard extends Button {
    private static final String READY_BACKGROUND = "IMAGE_UI_PACKETS_READY";
    private static final String BOOST_BACKGROUND = "IMAGE_UI_PACKETS_BOOST";
    private static final String SELECTED_BACKGROUND = "IMAGE_UI_PACKETS_SELECTED";
    private static final String SELECTED_BORDER = "IMAGE_UI_PACKETS_SELECT";
    private static final String LOCK = "IMAGE_UI_CARDS_LOCK_MEDIUM_GOLD";

    private static final float CARD_WIDTH = 115f;
    private static final float CARD_HEIGHT = 70f;

    private static final float FAMILY_TOP_PADDING = 0f;
    private static final float FAMILY_LEFT_PADDING = 0f;

    private static final float PROGRESS_WIDTH = 100f;
    private static final float PROGRESS_HEIGHT = 3f;
    private static final float PROGRESS_BOTTOM_PADDING = 0f;

    private static String familyFor(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Plant category cannot be null or blank.");
        }

        return switch (category) {
            case "Explosive" -> "IMAGE_UI_PACKETS_MINTFAM_EXPLOSIVE";
            case "Lobber" -> "IMAGE_UI_PACKETS_MINTFAM_LOBBER";
            case "Melee" -> "IMAGE_UI_PACKETS_MINTFAM_MELEE";
            case "Shooter" -> "IMAGE_UI_PACKETS_MINTFAM_PEASHOOTER";
            case "SunProducer" -> "IMAGE_UI_PACKETS_MINTFAM_SUN";
            case "Wall-nut" -> "IMAGE_UI_PACKETS_MINTFAM_DEFENSE";
            case "Homing" -> "IMAGE_UI_PACKETS_MINTFAM_SHADOW";
            case "Strike-through" -> "IMAGE_UI_PACKETS_MINTFAM_ELECTRICITY";
            case "Modifier" -> "IMAGE_UI_PACKETS_MINTFAM_MAGIC";

            default -> throw new IllegalStateException("No family asset is defined for category: " + category);
        };
    }

    public record ViewData(PlantData plant, boolean unlocked, boolean boosted,
            int level, int seedPackets, int requiredSeedPackets) {
        public ViewData {
            if (plant == null) {
                throw new IllegalArgumentException("plant cannot be null");
            }
            if (level < 1) {
                throw new IllegalArgumentException("level must be at least 1");
            }
            if (seedPackets < 0) {
                throw new IllegalArgumentException("seedPackets cannot be negative");
            }
            if (requiredSeedPackets < 1) {
                throw new IllegalArgumentException("requiredSeedPackets must be positive");
            }
        }
    }
    private final PvzGame game;
    @Getter
    private final ViewData data;

    private final Image stateBackground;
    private final Image selectedBorder;
    private final Container<Image> lockLayer;
    private final ProgressBar progressBar;

    private boolean boosted;
    private boolean locked;

    public PlantCard(PvzGame game, ViewData data) {
        super(new ButtonStyle());

        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }

        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }

        this.game = game;
        this.data = data;
        this.boosted = data.boosted();
        this.locked = !data.unlocked();

        setProgrammaticChangeEvents(true);
        pad(0f);

        Stack cardStack = new Stack();
        cardStack.setTouchable(Touchable.disabled);

        stateBackground = createImage(READY_BACKGROUND, Scaling.none);

        Image plantImage = createImage(data.plant().cardAssetId(), Scaling.none);

        progressBar = createPacketProgressBar();

        selectedBorder = createImage(SELECTED_BORDER, Scaling.none);

        lockLayer = createLockLayer();

        cardStack.add(stateBackground);
        cardStack.add(plantImage);
        cardStack.add(selectedBorder);
        cardStack.add(createInformationLayer());
        cardStack.add(lockLayer);

        add(cardStack).size(CARD_WIDTH, CARD_HEIGHT);

        refreshVisualState();

        addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                refreshVisualState();
            }
        });
    }

    private Stack createInformationLayer() {
        Stack overlay = new Stack();
        overlay.setTouchable(Touchable.disabled);

        Image familyLogo = createImage(familyFor(data.plant().category()), Scaling.fit);
        float familyWidth =
                familyLogo.getDrawable().getMinWidth() * 0.7f;

        float familyHeight =
                familyLogo.getDrawable().getMinHeight() * 0.7f;
        Container<Image> familyLayer = new Container<>(familyLogo);

        familyLayer.top().left();
        familyLayer.size(familyWidth, familyHeight);
        familyLayer.padTop(-10);
        familyLayer.padLeft(-10);
        familyLayer.setTouchable(Touchable.disabled);

        Container<ProgressBar> progressLayer =
                new Container<>(progressBar);

        progressLayer.bottom();
        progressLayer.width(PROGRESS_WIDTH);
        progressLayer.height(PROGRESS_HEIGHT);
        progressLayer.padBottom(-10);
        progressLayer.setTouchable(Touchable.disabled);

        overlay.add(familyLayer);
        overlay.add(progressLayer);

        return overlay;
    }

    private ProgressBar createPacketProgressBar() {
        float maximum = Math.max(
                1f,
                data.requiredSeedPackets()
        );

        ProgressBar bar = new ProgressBar(
                0f,
                maximum,
                1f,
                false,
                game.getSkin(),
                "xp_green"
        );

        bar.setValue(
                Math.min(
                        data.seedPackets(),
                        maximum
                )
        );

        bar.setAnimateDuration(0f);
        bar.setTouchable(Touchable.disabled);

        return bar;
    }

    private Container<Image> createLockLayer() {
        Image lockImage = createImage(
                LOCK,
                Scaling.none
        );

        Container<Image> layer =
                new Container<>(lockImage);

        layer.center();
        layer.setTouchable(Touchable.disabled);

        return layer;
    }

    private Image createImage(
            String assetId,
            Scaling scaling
    ) {
        TextureRegion region =
                game.getTextureBank().region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        Image image = new Image(
                new TextureRegionDrawable(region)
        );

        image.setScaling(scaling);
        image.setTouchable(Touchable.disabled);

        return image;
    }

    public void setBoosted(boolean boosted) {
        if (this.boosted == boosted) {
            return;
        }

        this.boosted = boosted;
        refreshVisualState();
    }

    public void setLocked(boolean locked) {
        if (this.locked == locked) {
            return;
        }

        this.locked = locked;
        refreshVisualState();
    }

    private void refreshVisualState() {
        String backgroundAsset;

        if (isChecked()) {
            backgroundAsset = SELECTED_BACKGROUND;
        } else if (boosted) {
            backgroundAsset = BOOST_BACKGROUND;
        } else {
            backgroundAsset = READY_BACKGROUND;
        }

        TextureRegion backgroundRegion =
                game.getTextureBank().region(
                        backgroundAsset
                );

        if (backgroundRegion == null) {
            throw new IllegalStateException(
                    "Card background was not found: "
                            + backgroundAsset
            );
        }

        stateBackground.setDrawable(
                new TextureRegionDrawable(
                        backgroundRegion
                )
        );

        selectedBorder.setVisible(isChecked());
        lockLayer.setVisible(locked);
        progressBar.setVisible(!locked);
    }

    @Override
    public float getPrefWidth() {
        return CARD_WIDTH;
    }

    @Override
    public float getPrefHeight() {
        return CARD_HEIGHT;
    }
}