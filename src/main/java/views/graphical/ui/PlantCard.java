package views.graphical.ui;

import Data.loader.PlantData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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
    private final Table lockLayer;

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

        stateBackground = createImage(READY_BACKGROUND,Scaling.none);
        cardStack.add(stateBackground);

        Image plantImage = createImage(data.plant().cardAssetId(), Scaling.none);
        cardStack.add(plantImage);

        Table informationLayer = createInformationLayer();

        cardStack.add(informationLayer);

        lockLayer = createLockLayer();
        cardStack.add(lockLayer);

        selectedBorder = createImage(SELECTED_BORDER, Scaling.none);

        cardStack.add(selectedBorder);

        add(cardStack).grow();

        refreshVisualState();
    }

    private Table createInformationLayer() {
        Table overlay = new Table();
        overlay.top();
        overlay.setTouchable(Touchable.disabled);

        String familyAsset = familyFor(data.plant().category());

        Image familyLogo = createImage(familyAsset, Scaling.none);

        overlay.add(familyLogo).size(31f).top().left().padTop(5f).padLeft(5f);
        overlay.add().expandX();
        overlay.row();
        overlay.add().colspan(2).expand().fill();
        overlay.row();
        ProgressBar progressBar = createPacketProgressBar();
        overlay.add(progressBar).colspan(2).growX().height(10f).padLeft(10f).padRight(10f).padBottom(8f);
        return overlay;
    }

    private ProgressBar createPacketProgressBar() {
        float maximum = Math.max(1, data.requiredSeedPackets());

        ProgressBar progressBar = new ProgressBar(0f, maximum, 1f, false, game.getSkin(), "xp_green");

        progressBar.setValue(Math.min(data.seedPackets(), maximum));

        progressBar.setAnimateDuration(0f);
        progressBar.setTouchable(Touchable.disabled);
        progressBar.setVisible(data.unlocked());

        return progressBar;
    }

    private Table createLockLayer() {
        Table layer = new Table();
        layer.setTouchable(Touchable.disabled);

        Image lockImage = createImage(LOCK, Scaling.none);

        layer.add(lockImage).size(52f);

        return layer;
    }

    private Image createImage(String assetId, Scaling scaling) {
        TextureRegion region = game.getTextureBank().region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        Image image = new Image(new TextureRegionDrawable(region));

        image.setScaling(scaling);
        image.setTouchable(Touchable.disabled);

        return image;
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        if (stateBackground != null) {
            refreshVisualState();
        }
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

        TextureRegion backgroundRegion = game.getTextureBank().region(backgroundAsset);

        if (backgroundRegion == null) {
            throw new IllegalStateException("Card background was not found: " + backgroundAsset);
        }

        stateBackground.setDrawable(new TextureRegionDrawable(backgroundRegion));

        selectedBorder.setVisible(isChecked());
        lockLayer.setVisible(locked);
    }
}