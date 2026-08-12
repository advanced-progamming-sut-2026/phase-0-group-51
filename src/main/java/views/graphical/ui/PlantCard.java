package views.graphical.ui;

import Data.loader.PlantData;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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
    private static final String LOCK = "IMAGE_UI_LOCK_SMALL_GOLD";

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
                           int level, int seedPackets, int requiredSeedPackets, boolean showSunCost, boolean showProgress) {
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
        public ViewData(
                PlantData plant,
                boolean unlocked,
                boolean boosted,
                int level,
                int seedPackets,
                int requiredSeedPackets,
                boolean showSunCost
        ) {
            this(
                    plant,
                    unlocked,
                    boosted,
                    level,
                    seedPackets,
                    requiredSeedPackets,
                    showSunCost,
                    true
            );
        }
    }
    private final PvzGame game;
    @Getter
    private final ViewData data;

    private final float cardScale;
    private final float cardWidth;
    private final float cardHeight;

    private final Image stateBackground;
    private final Image selectedBorder;
    private final Image plantImage;
    private final Container<Image> lockLayer;
    private final ProgressBar progressBar;
    private final CooldownOverlay cooldownOverlay;

    private Label sunCostLabel;

    private boolean boosted;
    private boolean locked;
    private boolean hovered;
    private boolean forceHideProgress = false;

    public PlantCard(PvzGame game, ViewData data) {
        this(game, data, 1f);
    }

    public PlantCard(
            PvzGame game,
            ViewData data,
            float cardScale
    ) {
        super(new ButtonStyle());

        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }

        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }

        if (cardScale <= 0f) {
            throw new IllegalArgumentException(
                    "cardScale must be positive"
            );
        }

        this.game = game;
        this.data = data;
        this.cardScale = cardScale;
        this.cardWidth = CARD_WIDTH * cardScale;
        this.cardHeight = CARD_HEIGHT * cardScale;
        this.boosted = data.boosted();
        this.locked = !data.unlocked();

        setProgrammaticChangeEvents(true);
        pad(0f);

        Stack cardStack = new Stack();
        cardStack.setTouchable(Touchable.disabled);

        Scaling cardImageScaling = cardScale == 1f ? Scaling.none : Scaling.fit;

        stateBackground = createImage(READY_BACKGROUND, cardImageScaling);

        plantImage = createImage(data.plant().cardAssetId(), cardImageScaling);

        progressBar = createPacketProgressBar();

        selectedBorder = createImage(SELECTED_BORDER, cardImageScaling);

        lockLayer = createLockLayer(cardImageScaling);

        TextureRegion cooldownRegion = game.getTextureBank().region(READY_BACKGROUND);

        if (cooldownRegion == null) {
            throw new IllegalStateException(
                    "Cooldown overlay region was not found: "
                            + READY_BACKGROUND
            );
        }

        cooldownOverlay = new CooldownOverlay(cooldownRegion);

        cardStack.add(stateBackground);
        cardStack.add(plantImage);
        cardStack.add(selectedBorder);
        cardStack.add(createInformationLayer());
        cardStack.add(cooldownOverlay);
        cardStack.add(createCostLayer());
        cardStack.add(lockLayer);

        add(cardStack).size(
                cardWidth,
                cardHeight
        );

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
        addListener(new InputListener() {
            @Override
            public void enter(
                InputEvent event,
                float x,
                float y,
                int pointer,
                Actor fromActor
            ) {
                hovered = true;
                refreshVisualState();
            }

            @Override
            public void exit(
                InputEvent event,
                float x,
                float y,
                int pointer,
                Actor toActor
            ) {
                hovered = false;
                refreshVisualState();
            }
        });
    }

    private Stack createInformationLayer() {
        Stack overlay = new Stack();
        overlay.setTouchable(Touchable.disabled);

        Image familyLogo = createImage(familyFor(data.plant().category()), Scaling.fit);
        float familyWidth = familyLogo.getDrawable().getMinWidth() * 0.7f * cardScale;

        float familyHeight = familyLogo.getDrawable().getMinHeight() * 0.7f * cardScale;
        Container<Image> familyLayer = new Container<>(familyLogo);

        familyLayer.top().left();
        familyLayer.size(familyWidth, familyHeight);
        familyLayer.padTop(-10);
        familyLayer.padLeft(-10);
        familyLayer.setTouchable(Touchable.disabled);

        Table progressLayer = new Table();
        progressLayer.bottom();
        progressLayer.setTouchable(Touchable.disabled);

        if (data.showProgress()) {
            progressLayer.add(progressBar).width(PROGRESS_WIDTH).height(PROGRESS_HEIGHT).padBottom(-10f);
        }

        overlay.add(familyLayer);
        overlay.add(progressLayer);

        return overlay;
    }

    private Table createCostLayer() {
        Table costLayer = new Table();
        costLayer.bottom().right();
        costLayer.setTouchable(Touchable.disabled);

        if (data.showSunCost()) {
            sunCostLabel = new Label(
                    Integer.toString(data.plant().cost()),
                    game.getSkin().get("medium", Label.LabelStyle.class)
            );

            costLayer.add(sunCostLabel).padRight(8f).padBottom(6f);
        }

        return costLayer;
    }

    private ProgressBar createPacketProgressBar() {
        float maximum = Math.max(1f, data.requiredSeedPackets());

        ProgressBar bar = new ProgressBar(
            0f,
            maximum,
            1f,
            false,
            game.getSkin(),
            "xp_green"
        );

        bar.setValue(Math.min(data.seedPackets(), maximum));

        bar.setAnimateDuration(0f);
        bar.setTouchable(Touchable.disabled);

        return bar;
    }

    private Container<Image> createLockLayer(Scaling scaling) {
        Image lockImage = createImage(LOCK, scaling);

        Container<Image> layer = new Container<>(lockImage);

        layer.top().right();
        layer.setTouchable(Touchable.disabled);

        return layer;
    }

    private Image createImage(String assetId, Scaling scaling) {
        TextureRegion region = game.getTextureBank().region(assetId);

        if (region == null) {
            throw new IllegalStateException("TextureBank region was not found: " + assetId);
        }

        Image image = new Image(new TextureRegionDrawable(region));

        image.setScaling(scaling);
        image.setTouchable(Touchable.disabled);

        return image;
    }

    public void setCooldownFraction(float cooldownFraction) {
        float clamped = Math.max(0f, Math.min(1f, cooldownFraction));

        boolean coolingDown = clamped > 0f;

        cooldownOverlay.setFraction(clamped);

        if (coolingDown && isChecked()) {
            setChecked(false);
        }

        setDisabled(coolingDown);
        refreshVisualState();
    }

    public void setEnoughSun(boolean enoughSun) {
        if (sunCostLabel == null) {
            return;
        }

        sunCostLabel.setColor(enoughSun ? Color.WHITE : Color.RED);
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

    public void hideProgressBar() {
        this.forceHideProgress = true;
        refreshVisualState();
    }

    private void refreshVisualState() {
        String backgroundAsset;

        if (isChecked()  || hovered) {
            backgroundAsset = SELECTED_BACKGROUND;
        } else if (boosted) {
            backgroundAsset = BOOST_BACKGROUND;
        } else {
            backgroundAsset = READY_BACKGROUND;
        }

        TextureRegion backgroundRegion = game.getTextureBank().region(backgroundAsset);

        if (backgroundRegion == null) {
            throw new IllegalStateException("Card background was not found: " + backgroundAsset);
        }

        stateBackground.setDrawable(new TextureRegionDrawable(backgroundRegion));

        selectedBorder.setVisible(isChecked() && !isDisabled());
        lockLayer.setVisible(locked);
        progressBar.setVisible(data.showProgress() && !locked);

        if (locked) {
            plantImage.setColor(0.5f, 0.5f, 0.5f, 1f);
            stateBackground.setColor(0.65f, 0.65f, 0.65f, 1f);
        } else {
            plantImage.setColor(1f, 1f, 1f, 1f);
            stateBackground.setColor(1f, 1f, 1f, 1f);
        }
    }

    private static final class CooldownOverlay extends Actor {

        private static final float BASE_DARK_ALPHA = 0.25f;

        private static final float TIMER_DARK_ALPHA = 0.55f;

        private final TextureRegion region;

        private float fraction;

        private CooldownOverlay(TextureRegion region) {
            this.region = region;
            setTouchable(Touchable.disabled);
        }

        private void setFraction(float fraction) {
            this.fraction = fraction;
            setVisible(fraction > 0f);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (fraction <= 0f) {
                return;
            }

            Color previousColor = new Color(batch.getColor());
            batch.setColor(0f, 0f, 0f, BASE_DARK_ALPHA * parentAlpha);

            batch.draw(region, getX(), getY(), getWidth(), getHeight());
            float timerHeight = getHeight() * fraction;

            batch.setColor(0f, 0f, 0f, TIMER_DARK_ALPHA * parentAlpha);
            batch.draw(region, getX(), getY(), getWidth(), timerHeight);
            batch.setColor(previousColor);
        }
    }

    @Override
    public float getPrefWidth() {
        return cardWidth;
    }

    @Override
    public float getPrefHeight() {
        return cardHeight;
    }
}
