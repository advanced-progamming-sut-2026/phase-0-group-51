package views.graphical.gameplay.hud;

import Data.database.UserRepository;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.GamingController;
import graphics.PvzGame;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Result;
import models.User;
import models.games.Game;
import models.games.GameState;
import models.games.ZombieWaveManager;
import views.graphical.ui.PauseMenuPopup;
import views.graphical.ui.ProfilePopup;
import views.graphical.ui.SettingsPopup;
@Getter
@Setter
public class GameHud extends Table {
    private static final String STOP = "IMAGE_UI_HUD_INGAME_PAUSE_BUTTON";
    private static final String STOP_CLICKED = "IMAGE_UI_HUD_INGAME_PAUSE_BUTTON_DOWN";
    private static final String SUN_CLICKED = "IMAGE_UI_HUD_INGAME_SUN_DOWN";
    private static final String SUN = "IMAGE_UI_HUD_INGAME_SUN";
    private static final String SUN_BACKGROUND = "IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE";
    private static final String WAVE_PROGRESS = "IMAGE_UI_HUD_INGAME_PROGRESS_METER";
    private static final String SHOVEL ="IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
    private static final String SHOVEL_CLICKED ="IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON_DOWN";
    private static final String CHEAT_ADD = "IMAGE_UI_HUD_INGAME_COIN_BUY";
    private static final String CHEAT_ADD_CLICKED = "IMAGE_UI_HUD_INGAME_COIN_BUY_DOWN";
    private static final String COIN = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL";
    private static final String COIN_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_SELECTED";
    private static final String GEMS = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL";
    private static final String GEMS_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_SELECTED";
    private static final String PLANT_FOOD = "IMAGE_UI_ALMANAC_PLANT_FOOD_STAT_ICON";
    private static final String PLANT_FOOD_RECTANGLE = "IMAGE_UI_HUD_INGAME_DEMO";
    private static final String PLANT_FOOD_DOT_FILL = "IMAGE_UI_GENERIC_NAVDOT_FILL";
    private static final String PLANT_FOOD_DOT = "IMAGE_UI_GENERIC_NAVDOT";
    private static final int MAX_PLANT_FOOD = 3;
    private static final int CHEAT_SUN_AMOUNT = 25;
    private static final float STATE_REFRESH = 0.10f;
    private static final float CURRENCY_REFRESH = 0.50f;

    private final PvzGame game;
    private PauseMenuPopup pausePopup;
    private final Table topLeft;
    private final Table topCenter;
    private final Table topRight;

    private final Table bottomLeft;
    private final Table bottomCenter;
    private final Table bottomRight;

    private final Actor gameplayBar;
    private final Runnable onPauseRequested;
    private Runnable onShovelRequested;
    private Runnable onSpeedRequested;

    private final GamingController gamingController = new GamingController();
    private final UserRepository userRepository = new UserRepository();

    private Label sunLabel;
    private Label coinLabel;
    private Label gemLabel;

    private final Image[] plantFoodDots = new Image[MAX_PLANT_FOOD];


    private ProgressBar waveProgress;
    private Label waveLabel;
    private Stack waveStack;


    private ImageButton shovelButton;

    private float stateRefreshTimer;
    private float currencyRefreshTimer;
    public GameHud(PvzGame game, Actor gameplayBar, Runnable onPauseRequested) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        this.game = game;
        this.gameplayBar = gameplayBar;
        this.onPauseRequested = onPauseRequested;
        setFillParent(true);
        setTouchable(Touchable.childrenOnly);

        topLeft = new Table();
        topCenter = new Table();
        topRight = new Table();

        bottomLeft = new Table();
        bottomCenter = new Table();
        bottomRight = new Table();

        buildHud();
        hideGameHud();
    }

    private void buildHud() {

        top();
        buildSunAndPlantFood();
        buildGameplayBar();
        buildRightControls();
        buildWaveProgress();

        Table topRow = new Table();
        topRow.add(topLeft).top().left().width(175f);
        topRow.add(topCenter).expandX().top().center();
        topRow.add(topRight).top().right().width(220f);
        add(topRow).growX().top().padTop(8f).padLeft(8f).padRight(8f).row();
        add().expand().row();
        Table bottomRow = new Table();
        bottomRow.add(bottomLeft).left();
        bottomRow.add(bottomCenter).expandX().center();
        bottomRow.add(bottomRight).right();
        add(bottomRow).growX().bottom().pad(10f);
    }

    private void buildSunAndPlantFood() {

        topLeft.top().left();
        Table sunRow = new Table();
        ImageButton addSunButton = createImageButton(CHEAT_ADD, CHEAT_ADD_CLICKED);
        addSunButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Result result = gamingController.cheatAddSun(CHEAT_SUN_AMOUNT);
                        showResult(result);
                        refreshGameplayState();
                    }
                }
        );


        Stack sunBank = new Stack();
        Image sunBackground = new Image(drawable(SUN_BACKGROUND));
        sunBackground.setScaling(Scaling.stretch);
        Table sunContent = new Table();
        Image sunIcon = new Image(drawable(SUN));
        sunIcon.setScaling(Scaling.fit);

        sunLabel = new Label("0", labelStyle("big_outline"));

        sunLabel.setColor(Color.WHITE);
        sunLabel.setAlignment(Align.center);
        sunContent.add(sunIcon).size(44f).padLeft(-15f);

        sunContent.add(sunLabel).expandX().center().padRight(8f);
        sunBank.add(sunBackground);
        sunBank.add(sunContent);
        sunRow.add(addSunButton).size(42f).padRight(3f);
        sunRow.add(sunBank).width(120f).height(48f);
        topLeft.add(sunRow).left().row();

        Table foodRow = new Table();
        ImageButton addFoodButton = createImageButton(CHEAT_ADD, CHEAT_ADD_CLICKED);

        addFoodButton.addListener(
                new ClickListener() {

                    @Override
                    public void clicked(InputEvent event, float x, float y) {

                        Result result = gamingController.cheatAddPlantFood();
                        showResult(result);
                        refreshGameplayState();
                    }
                }
        );

        Table foodDisplay = new Table();
        Image foodIcon = new Image(drawable(PLANT_FOOD));
        foodIcon.setScaling(Scaling.fit);
        Stack dotsBank = new Stack();

        Image dotsBackground = new Image(drawable(PLANT_FOOD_RECTANGLE));

        dotsBackground.setScaling(Scaling.stretch);

        dotsBank.add(dotsBackground);

        Table dots = new Table();

        dots.center();

        for (int i = 0; i < MAX_PLANT_FOOD; i++) {

            Image dot = new Image(drawable(PLANT_FOOD_DOT_FILL));
            plantFoodDots[i] = dot;
            dots.add(dot).size(14f).pad(2f);
        }

        dotsBank.add(dots);

        foodDisplay.add(foodIcon)
                .size(38f)
                .padRight(-4f);

        foodDisplay.add(dotsBank)
                .width(82f)
                .height(32f);


        foodRow.add(addFoodButton)
                .size(42f)
                .padRight(3f);


        foodRow.add(foodDisplay)
                .width(120f)
                .height(43f);


        topLeft.add(foodRow)
                .left()
                .padTop(3f);
    }
    private void togglePause() {
        if (pausePopup != null && pausePopup.hasParent()) {
            pausePopup.remove();
            return;
        }
        pausePopup = new PauseMenuPopup(game);
        pausePopup.pack();
        pausePopup.setPosition(
                (getStage().getWidth() - pausePopup.getWidth()) / 2f,
                (getStage().getHeight() - pausePopup.getHeight()) / 2f
        );
        getStage().addActor(pausePopup);
    }
    private void buildGameplayBar() {
        topCenter.top().center();
        if (gameplayBar != null) {
            topCenter.add(gameplayBar).top().center();
        }
    }
    private void buildRightControls() {
        topRight.top().right();
        Table buttons = new Table();
        gemLabel = new Label("0", game.getSkin());
        gemLabel.setTouchable(Touchable.disabled);
        gemLabel.setFontScale(1.1f);

        Group gemDisplay = createCurrencyDisplay(GEMS, GEMS_CLICKED, gemLabel);
        coinLabel = new Label("0", game.getSkin());
        coinLabel.setTouchable(Touchable.disabled);
        coinLabel.setFontScale(1.1f);

        Group coinDisplay = createCurrencyDisplay(COIN, COIN_CLICKED, coinLabel);
        shovelButton =createImageButton(SHOVEL, SHOVEL_CLICKED);

        shovelButton.addListener(
                new ClickListener() {

                    @Override
                    public void clicked(InputEvent event, float x, float y) {

                        if (onShovelRequested != null) {
                            onShovelRequested.run();
                        }
                    }
                }
        );

        ImageButton pauseButton = createImageButton(STOP, STOP_CLICKED);
        pauseButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {

                        togglePause();
                    }
                }
        );

        buttons.add(gemDisplay)
                .size(
                        gemDisplay.getWidth(),
                        gemDisplay.getHeight()
                )
                .padRight(8f);


        buttons.add(coinDisplay)
                .size(
                        coinDisplay.getWidth(),
                        coinDisplay.getHeight()
                )
                .padRight(10f);


        buttons.add(shovelButton)
                .size(58f)
                .padRight(5f);


        buttons.add(pauseButton)
                .size(58f);


        topRight.add(buttons).right();
    }
    private Group createCurrencyDisplay(String normalAsset, String pressedAsset, Label label) {
        ImageButton button = createCurrencyButton(normalAsset, pressedAsset);

        float width = button.getPrefWidth();
        float height = button.getPrefHeight();
        Group group = new Group();
        group.setSize(width, height);
        button.setBounds(0f, 0f, width, height);
        label.setPosition(70f, 20f);
        group.addActor(button);
        group.addActor(label);

        return group;
    }


    private ImageButton createCurrencyButton(
            String normalAsset,
            String pressedAsset
    ) {
        TextureRegion normalRegion = game.getTextureBank().region(normalAsset);
        TextureRegion pressedRegion = game.getTextureBank().region(pressedAsset);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();

        style.imageUp = new TextureRegionDrawable(normalRegion);

        style.imageDown = new TextureRegionDrawable(pressedRegion);

        style.imageOver = new TextureRegionDrawable(pressedRegion);


        ImageButton button = new ImageButton(style);
        button.getImageCell().expand().fill();
        button.getImage().setScaling(Scaling.stretch);
        return button;
    }

    private void buildWaveProgress() {
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
        style.background =
                game.getSkin()
                        .newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.55f));


        style.knobBefore = game.getSkin().newDrawable("white_pixel", Color.valueOf("65B83B"));


        waveProgress = new ProgressBar(0f, 1f, 0.01f, false, style);


        waveProgress.setValue(0f);


        waveLabel = new Label("0 / 0", labelStyle("medium_outline"));

        waveLabel.setAlignment(Align.center);
        waveLabel.setColor(Color.WHITE);
        waveStack = new Stack();
        waveStack.add(waveProgress);


        Image frame = new Image(drawable(WAVE_PROGRESS));
        frame.setScaling(Scaling.stretch);
        frame.setTouchable(Touchable.disabled);
        waveStack.add(frame);
        Table textLayer = new Table();
        textLayer.add(waveLabel);
        waveStack.add(textLayer);


        bottomRight.add(waveStack)
                .width(260f).height(38f);
    }
    @Override
    public void act(
            float delta
    ) {

        super.act(delta);


        if (!isVisible()) {
            return;
        }


        stateRefreshTimer += delta;

        if (stateRefreshTimer
                >= STATE_REFRESH) {

            stateRefreshTimer = 0f;

            refreshGameplayState();
        }


        currencyRefreshTimer += delta;

        if (currencyRefreshTimer
                >= CURRENCY_REFRESH) {

            currencyRefreshTimer = 0f;

            refreshCurrencies();
        }
    }


    private void refreshGameplayState() {
        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || currentGame.getGameState() == null) {
            return;
        }

        GameState state = currentGame.getGameState();
        sunLabel.setText(state.getSun());
        int foodCount = MathUtils.clamp(state.getPlantFoodCount(), 0, MAX_PLANT_FOOD);

        for (int i = 0; i < plantFoodDots.length; i++) {
            if (i < foodCount) {
                plantFoodDots[i].setColor(Color.valueOf("65D44B"));

            } else {

                plantFoodDots[i].setColor(
                        new Color(
                                0.20f,
                                0.30f,
                                0.20f,
                                0.30f
                        )
                );
            }
        }

        ZombieWaveManager waveManager = state.getZombieWaveManager();
        if (waveManager == null) {
            waveStack.setVisible(false);
            return;
        }

        waveStack.setVisible(true);
        int current = waveManager.getCurrentWaveNumber();

        if (waveManager.isEndless()) {
            waveProgress.setValue(0f);
            waveLabel.setText("Wave " + current);
            return;
        }
        int total = waveManager.getTotalWaves();


        float progress = total <= 0 ? 0f : current / (float) total;


        waveProgress.setValue(MathUtils.clamp(progress, 0f, 1f));
        waveLabel.setText(current + " / " + total);
    }


    private void refreshCurrencies() {
        User user = App.getInstance().getLoggedInUser();


        if (user == null) {
            gemLabel.setText("0");
            coinLabel.setText("0");
            return;
        }


        UserRepository.CurrencyBalance balance = userRepository.getCurrencyBalance(user.getId());

        if (balance == null) {
            return;
        }

        coinLabel.setText(
                balance.coins()
        );

        gemLabel.setText(
                balance.gems()
        );
    }

    public void setShovelSelected(
            boolean selected
    ) {

        shovelButton.setChecked(
                selected
        );
    }

    public void showGameHud() {

        refreshGameplayState();
        refreshCurrencies();

        setVisible(true);

        setTouchable(
                Touchable.childrenOnly
        );

        toFront();
    }


    public void hideGameHud() {

        setVisible(false);

        setTouchable(
                Touchable.disabled
        );
    }

    private ImageButton createImageButton(String normalId, String pressedId) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = drawable(normalId);
        style.imageDown = drawable(pressedId);
        style.imageOver = drawable(pressedId);
        style.imageChecked = drawable(pressedId);


        return new ImageButton(style);
    }


    private Drawable drawable(String id) {
        try {
            TextureRegion region = game.getTextureBank().region(id);
            if (region != null) {
                return new TextureRegionDrawable(region);
            }
        } catch (Exception ignored) {
        }

        return game.getSkin().newDrawable("white_pixel", Color.DARK_GRAY);
    }


    private Label createValueLabel() {
        Label label = new Label("0", labelStyle("medium_outline"));
        label.setColor(Color.WHITE);
        label.setAlignment(Align.center);
        return label;
    }


    private Label.LabelStyle labelStyle(String name) {

        try {
            return game.getSkin().get(name, Label.LabelStyle.class);

        } catch (Exception exception) {
            return game.getSkin().get("default", Label.LabelStyle.class);
        }
    }


    private void showResult(Result result) {

        if (result == null) {
            return;
        }

        if (result.success()) {
            game.notifyInfo(result.message());
        } else {
            game.notifyError(result.message());
        }
    }



}
