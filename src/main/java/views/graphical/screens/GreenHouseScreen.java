package views.graphical.screens;

import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.GreenHouseMenuController;
import graphics.PvzGame;
import models.Result;
import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;
import views.graphical.gameplay.manager.AudioManager;
import views.graphical.ui.BorderedPanel;
import views.graphical.ui.ShopPopup;

import java.time.Duration;

public class GreenHouseScreen extends BaseScreen {
    // Project-local image path. Change this only if GreenHouseBG.png is stored elsewhere.
    private static final String GREENHOUSE_BACKGROUND = "assets/backgrounds/GreenHouseBG.png";

    private static final String POT_COUNT = "IMAGE_UI_HUD_INGAME_SPROUT_ICON";
    private static final String POT_COUNT_CLICKED = "IMAGE_UI_HUD_INGAME_SPROUT_ICON_DOWN";
    private static final String SALE = "IMAGE_UI_HUD_WORLDMAP_HUD_STORE_SALE_BANNER";
    private static final String POT =
            "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161";
    private static final String POT_GROWING =
            "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2";
    private static final String GREEN_BUTTON = "IMAGE_UI_GENERIC_GREENBUTTON";
    private static final String GREEN_BUTTON_DOWN = "IMAGE_UI_GENERIC_GREENBUTTON_DOWN";
    private static final String LOCK =
            "768/INITIAL/UI/CHOOSER/SLOT_LOCK_SMALL/SLOT_LOCK_SMALL.PAM";

    // PAM address needed only for Marigold because it is not registered as a normal PlantData entry.
    // If your pvz-assets version uses a different Marigold path, replace this one constant.
    private static final String MARIGOLD_PAM =
            "768/INITIAL/PLANT/MARIGOLD/MARIGOLD.PAM";
    private static final String MARIGOLD_CLIP = "idle";

    private static final float[] POT_X = {495f, 650f, 800f, 950f};
    private static final float[] POT_Y = {380f, 205f, 50f};
    private static final float LOCK_OFFSET_X = 68f;
    private static final float LOCK_OFFSET_Y = 75f;
    private static final float TIMER_REFRESH_SECONDS = 1f;

    private static final float PLANT_OFFSET_X = 60f;
    private static final float PLANT_OFFSET_Y = 65f;
    private static final float PLANT_SCALE = 0.45f;

    private final Group[][] potSlots = new Group[GreenHouse.ROWS][GreenHouse.COLUMNS];
    private final Label[][] timerLabels = new Label[GreenHouse.ROWS][GreenHouse.COLUMNS];
    private final GreenHouseMenuController controller = new GreenHouseMenuController();

    private Texture backgroundTexture;
    private Label potCountLabel;
    private float timerRefreshAccumulator;

    public GreenHouseScreen(PvzGame game) {
        super(game);
        buildUi();
    }
    private void buildUi() {
        Stack root = new Stack();
        root.setFillParent(true);
        backgroundTexture = new Texture(Gdx.files.internal(GREENHOUSE_BACKGROUND));
        backgroundTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
        );
        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setScaleX(1.2f);
        backgroundImage.setFillParent(true);
        backgroundImage.setTouchable(Touchable.disabled);

        Group uiLayer = new Group();
        addTopControls(uiLayer);
        createGreenHouseSlots(uiLayer);

        root.add(backgroundImage);
        root.add(uiLayer);
        stage.addActor(root);

        refreshGreenHouse();
    }
    private void addTopControls(Group uiLayer) {
        Actor potCountButton = createImageButton(POT_COUNT, POT_COUNT_CLICKED);
        potCountButton.setTouchable(Touchable.disabled);
        place(uiLayer, potCountButton, 490f, 650f);

        potCountLabel = borderedLabel("0/12");
        potCountLabel.setTouchable(Touchable.disabled);
        place(uiLayer, potCountLabel, 555f, 666f);

        Image saleBanner = createImage(SALE);
        saleBanner.setTouchable(Touchable.enabled);
        saleBanner.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openShop();
            }
        });
        place(uiLayer, saleBanner, 1200f, 620f);

        Label saleLabel = borderedLabel("SALE");
        saleLabel.setFontScale(1.25f);
        saleLabel.setTouchable(Touchable.disabled);
        place(uiLayer, saleLabel, 1220f, 630f);
    }

    private void createGreenHouseSlots(Group uiLayer) {
        for (int row = 1; row <= GreenHouse.ROWS; row++) {
            for (int column = 1; column <= GreenHouse.COLUMNS; column++) {
                Group slot = new Group();
                slot.setSize(184f, 161f);
                slot.setPosition(POT_X[column - 1], POT_Y[row - 1]);
                potSlots[row - 1][column - 1] = slot;
                uiLayer.addActor(slot);
            }
        }
    }
    private void refreshGreenHouse() {
        for (int row = 1; row <= GreenHouse.ROWS; row++) {
            for (int column = 1; column <= GreenHouse.COLUMNS; column++) {
                refreshSlot(row, column);
            }
        }
        refreshPotCount();
        refreshHudCurrencies();
    }
    private void refreshSlot(int row, int column) {
        FlowerPot pot = controller.getPot(row, column);
        Group slot = potSlots[row - 1][column - 1];
        timerLabels[row - 1][column - 1] = null;
        slot.clearChildren();
        if (pot == null) {
            return;
        }
        if (!pot.isUnlocked()) {
            buildLockedSlot(slot);
        } else if (pot.isEmpty()) {
            buildEmptySlot(slot, row, column);
        } else if (pot.isReady()) {
            buildReadySlot(slot, pot, row, column);
        } else {
            buildGrowingSlot(slot, pot, row, column);
        }
    }
    private void buildLockedSlot(Group slot) {
        addLockAnimation(slot);
        TextButton buyButton = createActionButton("BUY POT");
        buyButton.setBounds(20f, -6f, 120f, 38f);
        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openShop();
            }
        });
        slot.addActor(buyButton);
    }

    private void buildEmptySlot(Group slot, int row, int column) {
        addPotVisual(slot, false);
        TextButton plantButton = createActionButton("PLANT");
        plantButton.setBounds(20f, -6f, 120f, 38f);
        plantButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handlePlant(row, column);
            }
        });
        slot.addActor(plantButton);
    }
    private void buildGrowingSlot(Group slot, FlowerPot pot, int row, int column) {
        addPotVisual(slot, true);
        addPlantAnimation(slot, pot);
        Label timer = borderedLabel(formatRemainingTime(pot.getRemainingTime()));
        timer.setAlignment(Align.center);
        timer.setBounds(10f, -12f, 140f, 28f);
        timerLabels[row - 1][column - 1] = timer;
        slot.addActor(timer);
        int gemsNeeded = Math.toIntExact(pot.getCeilRemainingHours());
        TextButton growButton = createActionButton("GROW  " + gemsNeeded + " GEMS");
        growButton.setBounds(5f, -48f, 150f, 34f);
        growButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleGrow(row, column);
            }
        });
        slot.addActor(growButton);
    }
    private void buildReadySlot(Group slot, FlowerPot pot, int row, int column) {
        addPotVisual(slot, true);
        addPlantAnimation(slot, pot);
        Actor collectHitArea = new Actor();
        collectHitArea.setBounds(20f, 25f, 120f, 125f);
        collectHitArea.setTouchable(Touchable.enabled);
        collectHitArea.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleCollect(row, column);
            }
        });
        slot.addActor(collectHitArea);

        Label readyLabel = borderedLabel("READY!");
        readyLabel.setColor(Color.GREEN);
        readyLabel.setAlignment(Align.center);
        readyLabel.setBounds(20f, -10f, 120f, 28f);
        slot.addActor(readyLabel);

        TextButton collectButton = createActionButton("COLLECT");
        collectButton.setBounds(20f, -46f, 120f, 34f);
        collectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleCollect(row, column);
            }
        });
        slot.addActor(collectButton);
    }
    private void handlePlant(int row, int column) {
        Result result = controller.plantPot(String.valueOf(column), String.valueOf(row));
        showResult(result);
        if (result.success()) {
            refreshSlot(row, column);
        }
    }
    private void handleGrow(int row, int column) {
        Result result = controller.growPlant(String.valueOf(column), String.valueOf(row));
        showResult(result);
        if (result.success()) {
            refreshSlot(row, column);
            refreshHudCurrencies();
        }
    }

    private void handleCollect(int row, int column) {
        Result result = controller.collectPlant(String.valueOf(column), String.valueOf(row));
        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }
        showHarvestPopup(result.message());
        refreshSlot(row, column);
        refreshHudCurrencies();
    }
    private void showResult(Result result) {
        if (result.success()) {
            game.notifyInfo(result.message());
        } else {
            game.notifyError(result.message());
        }
    }
    private void addPotVisual(Group slot, boolean growing) {
        Image potImage = createImage(growing ? POT_GROWING : POT);
        potImage.setPosition(0f, 0f);
        slot.addActor(potImage);
    }
    private void addLockAnimation(Group slot) {
        game.getPamPlayer().loadSync(LOCK);
        Actor lockActor = game.createPamActor(LOCK, "idle", 0f, 0f, true);
        Group scaleGroup = new Group();
        scaleGroup.setTransform(true);
        scaleGroup.setScale(0.5f);
        scaleGroup.setPosition(LOCK_OFFSET_X, LOCK_OFFSET_Y);
        scaleGroup.addActor(lockActor);
        slot.addActor(scaleGroup);
    }

    private void addPlantAnimation(Group slot, FlowerPot pot) {
        String pamPath;
        String idleClip;

        if (pot.getPlantId() == FlowerPot.MARIGOLD_ID) {
            pamPath = MARIGOLD_PAM;
            idleClip = MARIGOLD_CLIP;
        } else {
            PlantData data = PlantRegistry.getById(pot.getPlantId());
            if (!hasIdleAnimation(data)) {
                addMissingAnimationLabel(slot, pot.getPlantName());
                return;
            }
            // No new PAM address is needed: normal plants use idlePamPath/idleClip from PlantData.
            pamPath = data.idlePamPath();
            idleClip = data.idleClip();
        }

        game.getPamPlayer().loadSync(pamPath);
        Actor plantActor = game.createPamActor(pamPath, idleClip, 0f, 30f, true);
        Group scaleGroup = new Group();
        scaleGroup.setTransform(true);
        scaleGroup.setScale(0.45f);
        scaleGroup.setPosition(
                PLANT_OFFSET_X,
                PLANT_OFFSET_Y
        );
        scaleGroup.addActor(plantActor);
        slot.addActor(scaleGroup);
    }

    private boolean hasIdleAnimation(PlantData data) {
        return data != null
                && data.idlePamPath() != null
                && !data.idlePamPath().isBlank()
                && data.idleClip() != null
                && !data.idleClip().isBlank();
    }

    private void addMissingAnimationLabel(Group slot, String plantName) {
        Label fallback = borderedLabel(plantName);
        fallback.setAlignment(Align.center);
        fallback.setBounds(10f, 75f, 140f, 30f);
        slot.addActor(fallback);
    }
    private void updateGrowingTimers(float delta) {
        timerRefreshAccumulator += delta;
        if (timerRefreshAccumulator < TIMER_REFRESH_SECONDS) {
            return;
        }
        timerRefreshAccumulator = 0f;

        for (int row = 1; row <= GreenHouse.ROWS; row++) {
            for (int column = 1; column <= GreenHouse.COLUMNS; column++) {
                updateTimer(row, column);
            }
        }
    }
    private void updateTimer(int row, int column) {
        Label timer = timerLabels[row - 1][column - 1];
        if (timer == null) {
            return;
        }
        FlowerPot pot = controller.getPot(row, column);
        if (pot == null || pot.isEmpty()) {
            return;
        }
        if (pot.isReady()) {
            refreshSlot(row, column);
            return;
        }
        timer.setText(formatRemainingTime(pot.getRemainingTime()));
    }
    private String formatRemainingTime(Duration duration) {
        long seconds = Math.max(0L, duration.getSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
    private void refreshPotCount() {
        int unlocked = 0;
        for (int row = 1; row <= GreenHouse.ROWS; row++) {
            for (int column = 1; column <= GreenHouse.COLUMNS; column++) {
                FlowerPot pot = controller.getPot(row, column);
                if (pot != null && pot.isUnlocked()) {
                    unlocked++;
                }
            }
        }
        potCountLabel.setText(unlocked + "/" + (GreenHouse.ROWS * GreenHouse.COLUMNS));
    }

    private void refreshHudCurrencies() {
        UserRepository.CurrencyBalance balance = controller.getCurrencyBalance();
        if (balance != null) {
            game.updateCurrencies(balance.coins(), balance.gems());
        }
    }
    private void openShop() {
        ShopPopup shopPopup = new ShopPopup(game, this::refreshGreenHouse);
        shopPopup.pack();
        shopPopup.setPosition(
                (stage.getWidth() - shopPopup.getWidth()) / 2f,
                (stage.getHeight() - shopPopup.getHeight()) / 2f
        );
        stage.addActor(shopPopup);
    }
    private void showHarvestPopup(String message) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(skin.newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.7f)));
        BorderedPanel panel = new BorderedPanel(game, Color.valueOf("A0522D"));
        Label title = borderedLabel("HARVEST COMPLETE");
        title.setColor(Color.GOLD);
        title.setFontScale(1.1f);

        Label reward = borderedLabel(message.trim());
        reward.setWrap(true);
        reward.setAlignment(Align.center);

        TextButton okButton = createActionButton("OK");
        okButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();
            }
        });

        panel.getContent().add(title).pad(10f).row();
        panel.getContent().add(reward).width(320f).pad(15f).row();
        panel.getContent().add(okButton).size(110f, 40f).pad(10f);
        panel.pack();
        overlay.add(panel).center();
        stage.addActor(overlay);
    }

    private TextButton createActionButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        if (skin.has("medium_outline", Label.LabelStyle.class)) {
            style.font = skin.get("medium_outline", Label.LabelStyle.class).font;
        } else {
            style.font = skin.getFont("default-font");
        }
        style.fontColor = Color.WHITE;
        style.up = new TextureRegionDrawable(game.getTextureBank().region(GREEN_BUTTON));
        style.down = new TextureRegionDrawable(game.getTextureBank().region(GREEN_BUTTON_DOWN));
        style.over = style.down;

        TextButton button = new TextButton(text, style);
        button.getLabel().setFontScale(0.68f);
        return button;
    }

    private Actor createImageButton(String normalAsset, String pressedAsset) {
        TextureRegion normalRegion = game.getTextureBank().region(normalAsset);
        TextureRegion pressedRegion = game.getTextureBank().region(pressedAsset);
        com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle style =
                new com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);

        com.badlogic.gdx.scenes.scene2d.ui.ImageButton button =
                new com.badlogic.gdx.scenes.scene2d.ui.ImageButton(style);
        button.getImageCell().expand().fill();
        button.getImage().setScaling(Scaling.stretch);
        return button;
    }

    private Image createImage(String assetId) {
        TextureRegion region = game.getTextureBank().region(assetId);
        Image image = new Image(new TextureRegionDrawable(region));
        image.setScaling(Scaling.stretch);
        return image;
    }

    private void place(Group layer, Actor actor, float x, float y) {
        actor.setPosition(x, y);
        layer.addActor(actor);
    }

    private Label borderedLabel(String text) {
        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        labelStyle.font = skin.getFont("FBUSV8C5EI_2_outline");
        labelStyle.fontColor = Color.WHITE;
        Label label = new Label(text, labelStyle);
        label.setFontScale(0.8f);
        return label;
    }

    @Override
    public void show() {
        super.show();
        game.showHud(0, 0, true, () -> game.showScreen(new MainMenuScreen(game)));
        refreshHudCurrencies();
        AudioManager.getInstance().playMusic("assets/sounds/GreenHouse.mp3");
    }

    @Override
    public void render(float delta) {
        updateGrowingTimers(delta);
        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
    }
}
