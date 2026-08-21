package views.graphical.gameplay.hud;


import Data.database.UserRepository;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.GamingController;
import controllers.GameMenuController;
import graphics.PvzGame;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Result;
import models.User;
import models.Zombie.Zombie;
import models.games.Game;
import models.games.GameState;
import models.games.ZombieWaveManager;
import models.games.specialLevelConfig.TimedBattleConfig;
import views.graphical.ui.BorderedPanel;
import views.graphical.ui.GameSettings;
import views.graphical.ui.PlantSlotsBar;
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
    private static final String FLAG ="IMAGE_ZOMBIE_ZOMBIE_BIGHEAD_FLAG_ZOMBIE_BIGHEAD_FLAG_123X95";
    private static final String FLAG_BAR ="IMAGE_ZOMBIE_ZOMBIE_BIGHEAD_FLAG_ZOMBIE_BIGHEAD_FLAG_23X194";
    private static final int MAX_PLANT_FOOD = 3;
    private static final int CHEAT_SUN_AMOUNT = 25;
    private static final int CHEAT_COIN_AMOUNT = 1000;
    private static final int CHEAT_GEM_AMOUNT = 10;
    private static final float STATE_REFRESH = 0.10f;
    private static final float CURRENCY_REFRESH = 0.50f;

    private static final float TIMED_BATTLE_PANEL_WIDTH = 270f;
    private static final double TIMED_BATTLE_DANGER_SECONDS = 10.0;
    private static final String TIMED_BATTLE_ZOMBIE_ICON =
        "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL";
    private static final String TIMED_BATTLE_SUN_ICON =
        "IMAGE_DANGERROOM_CARD_SUN";
    private static final float TIMED_BATTLE_ZOMBIE_ICON_SIZE = 20f;
    private static final float TIMED_BATTLE_SUN_ICON_SIZE = 18f;

    private static final String LOVE_PLANTS_ICON =
        "IMAGE_PLANT_LILYPAD_LILYPAD_60X58";

    private static final String PLANT_WHAT_YOU_GET_BUTTON =
        "IMAGE_UI_GENERIC_GREENBUTTON";
    private static final String PLANT_WHAT_YOU_GET_BUTTON_DOWN =
        "IMAGE_UI_GENERIC_GREENBUTTON_DOWN";
    private static final float PLANT_WHAT_YOU_GET_PANEL_WIDTH = 245f;
    private static final float LOVE_PLANTS_PANEL_WIDTH = 235f;
    private static final float LOVE_PLANTS_CONTENT_WIDTH = 190f;
    private static final float LOVE_PLANTS_ICON_ROW_WIDTH = 185f;
    private static final float LOVE_PLANTS_ICON_MAX_SIZE = 24f;
    private static final float LOVE_PLANTS_ICON_MIN_SIZE = 15f;

    private final PvzGame game;
    private final Table topLeft;
    private final Table topCenter;
    private final Table topRight;

    private final Table bottomLeft;
    private final Table bottomCenter;
    private final Table bottomRight;

    private final Runnable onPauseRequested;
    private Runnable onShovelRequested;
    private Runnable onPlantFoodRequested;
    private Runnable onSpeedRequested;

    private final GamingController gamingController = new GamingController();
    private final GameMenuController gameMenuController =
        new GameMenuController();
    private final UserRepository userRepository = new UserRepository();

    private Label sunLabel;
    private Label coinLabel;
    private Label gemLabel;

    private BorderedPanel timedBattlePanel;
    private Table timedBattleKillRow;
    private Table timedBattleSunRow;
    private Label timedBattleTimerLabel;
    private Label timedBattleKillLabel;
    private Label timedBattleSunLabel;

    private BorderedPanel lovePlantsPanel;
    private Table lovePlantsIconRow;
    private Label lovePlantsTitleLabel;
    private Label lovePlantsCountLabel;
    private Image[] lovePlantIcons = new Image[0];
    private int lastLovePlantsLimit = -1;
    private int lastLovePlantsLost = -1;

    private BorderedPanel plantWhatYouGetPanel;
    private TextButton plantWhatYouGetStartButton;

    private final Image[] plantFoodDots = new Image[MAX_PLANT_FOOD];

    private static final float PLANT_SLOTS_TOP_GAP = 0f;
    private static final float SIDE_PADDING = 16f;

    private final PlantSlotsBar plantSlotsBar;
    private ImageButton shovelButton;
    private ImageButton addSunButton;
    private ImageButton addFoodButton;

    private float stateRefreshTimer;
    private float currencyRefreshTimer;
    private Group waveGroup;

    private Image waveFill;
    private Image waveZombieHead;
    private Texture waveZombieTexture;
    private Group[] waveMarkers;
    private Image[] waveFlagImages;

    private int lastCompletedWaves = -1;

    private static final float WAVE_BAR_WIDTH = 260f;
    private static final float WAVE_BAR_HEIGHT = 36f;

    private static final float TRACK_LEFT = 20f;
    private static final float TRACK_RIGHT = 250f;

    private static final float WAVE_FILL_Y = 8f;
    private static final float WAVE_FILL_HEIGHT = 20f;


    private static final float FLAG_WIDTH = 27f;
    private static final float FLAG_HEIGHT = 21f;


    private static final float FLAG_POLE_WIDTH = 5f;
    private static final float FLAG_POLE_HEIGHT = 46f;

    private static final float FLAG_DOWN_Y = 2f;
    private static final float FLAG_UP_Y = 29f;
    public GameHud(PvzGame game, PlantSlotsBar plantSlotsBar, Runnable onPauseRequested) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        if (plantSlotsBar == null) {
            throw new IllegalArgumentException(
                "plantSlotsBar cannot be null"
            );
        }

        this.plantSlotsBar = plantSlotsBar;
        this.game = game;
        this.onPauseRequested = onPauseRequested;
        setFillParent(true);
        setTouchable(Touchable.childrenOnly);

        topLeft = new Table();
        topCenter = new Table();
        topRight = new Table();

        bottomLeft = new Table();
        bottomCenter = new Table();
        bottomRight = new Table();

        setTouchable(Touchable.childrenOnly);
        buildHud();
        refreshDebugControls();
        hideGameHud();
    }

    private void buildHud() {

        top();

        buildSunAndPlantFood();
        buildRightControls();
        buildWaveProgress();
        buildTimedBattlePanel();
        buildLoveYourPlantsPanel();
        buildPlantWhatYouGetPanel();

        Table timedBattleWrapper = new Table();
        timedBattleWrapper.top().center();
        timedBattleWrapper.add(timedBattlePanel)
            .width(TIMED_BATTLE_PANEL_WIDTH)
            .top()
            .center();

        Table lovePlantsWrapper = new Table();
        lovePlantsWrapper.top().center();
        lovePlantsWrapper.add(lovePlantsPanel)
            .width(LOVE_PLANTS_PANEL_WIDTH)
            .top()
            .center();

        Table plantWhatYouGetWrapper = new Table();
        plantWhatYouGetWrapper.top().center();
        plantWhatYouGetWrapper.add(plantWhatYouGetPanel)
            .width(PLANT_WHAT_YOU_GET_PANEL_WIDTH)
            .top()
            .center();

        topCenter.stack(
                timedBattleWrapper,
                lovePlantsWrapper,
                plantWhatYouGetWrapper
            )
            .width(Math.max(
                TIMED_BATTLE_PANEL_WIDTH,
                Math.max(
                    LOVE_PLANTS_PANEL_WIDTH,
                    PLANT_WHAT_YOU_GET_PANEL_WIDTH
                )
            ))
            .top()
            .center();

        Table topRow = new Table();
        topRow.top();

        topRow.add(topLeft)
            .top()
            .left()
            .width(175f);

        topRow.add(topCenter)
            .expandX()
            .top()
            .center();

        topRow.add(topRight)
            .top()
            .right();

        add(topRow)
            .growX()
            .top()
            .padTop(4f)
            .padLeft(8f)
            .padRight(8f)
            .row();

        add(plantSlotsBar)
            .left()
            .padTop(PLANT_SLOTS_TOP_GAP)
            .padLeft(SIDE_PADDING)
            .padRight(SIDE_PADDING)
            .row();

        add().expand().row();

        Table bottomRow = new Table();
        bottomRow.add(bottomLeft).left();
        bottomRow.add(bottomCenter).expandX().center();
        bottomRow.add(bottomRight).right();

        add(bottomRow)
            .growX()
            .bottom()
            .pad(10f);
    }

    private void buildSunAndPlantFood() {

        topLeft.top().left();
        Table sunRow = new Table();
        addSunButton = createImageButton(CHEAT_ADD, CHEAT_ADD_CLICKED);
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
        sunContent.add(sunIcon).size(44f).padLeft(-9f);

        sunContent.add(sunLabel).expandX().center().padRight(8f);
        sunBank.add(sunBackground);
        sunBank.add(sunContent);
        sunRow.add(addSunButton).size(42f).padRight(3f);
        sunRow.add(sunBank).width(120f).height(48f);
        topLeft.add(sunRow).left().row();

        Table foodRow = new Table();
        addFoodButton = createImageButton(CHEAT_ADD, CHEAT_ADD_CLICKED);

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

        Stack foodDisplay = new Stack();
        foodDisplay.setTouchable(Touchable.enabled);
        foodDisplay.addListener(
            new ClickListener() {
                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {
                    if (onPlantFoodRequested != null) {
                        onPlantFoodRequested.run();
                    }
                }
            }
        );

        Stack dotsBank = new Stack();

        Image dotsBackground =
            new Image(
                drawable(
                    PLANT_FOOD_RECTANGLE
                )
            );

        dotsBackground.setScaling(
            Scaling.stretch
        );

        dotsBank.add(
            dotsBackground
        );


        Table dots =
            new Table();

        dots.center();

        for (int i = 0; i < MAX_PLANT_FOOD; i++) {

            Image dot =
                new Image(
                    drawable(
                        PLANT_FOOD_DOT_FILL
                    )
                );

            plantFoodDots[i] =
                dot;

            dots.add(dot)
                .size(14f)
                .pad(2f);
        }

        dotsBank.add(
            dots
        );


        Table rectangleLayer =
            new Table();

        rectangleLayer.right();

        rectangleLayer.add(dotsBank)
            .width(82f)
            .height(32f);

        foodDisplay.add(
            rectangleLayer
        );

        Image foodIcon =
            new Image(
                drawable(
                    PLANT_FOOD
                )
            );

        foodIcon.setScaling(
            Scaling.fit
        );


        Table iconLayer =
            new Table();

        iconLayer.left();

        iconLayer.add(foodIcon)
            .size(60f);

        foodDisplay.add(
            iconLayer
        );

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

    private void buildRightControls() {
        topRight.top().right();
        Table buttons = new Table();
        gemLabel = new Label("0", game.getSkin());
        gemLabel.setTouchable(Touchable.disabled);
        gemLabel.setFontScale(1.1f);

        Group gemDisplay = createCurrencyDisplay(
            GEMS,
            GEMS_CLICKED,
            gemLabel,
            () -> runCurrencyCheat(
                CHEAT_GEM_AMOUNT,
                "diamond"
            )
        );

        coinLabel = new Label("0", game.getSkin());
        coinLabel.setTouchable(Touchable.disabled);
        coinLabel.setFontScale(1.1f);

        Group coinDisplay = createCurrencyDisplay(
            COIN,
            COIN_CLICKED,
            coinLabel,
            () -> runCurrencyCheat(
                CHEAT_COIN_AMOUNT,
                "coin"
            )
        );

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
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {
                    if (onPauseRequested != null) {
                        onPauseRequested.run();
                    }
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

    private void runCurrencyCheat(
        int amount,
        String kind
    ) {
        if (!GameSettings.debugMode) {
            return;
        }

        if (App.getInstance().getLoggedInUser() == null) {
            game.notifyError(
                "You must be logged in to use debug currency controls."
            );
            return;
        }

        Result result =
            gameMenuController.cheatAdd(
                amount,
                kind
            );

        showResult(result);
        refreshCurrencies();
    }

    private void refreshDebugControls() {
        boolean visible =
            GameSettings.debugMode;

        setDebugButtonVisible(
            addSunButton,
            visible
        );
        setDebugButtonVisible(
            addFoodButton,
            visible
        );
    }

    private void setDebugButtonVisible(
        ImageButton button,
        boolean visible
    ) {
        if (button == null) {
            return;
        }

        button.setVisible(visible);
        button.setTouchable(
            visible
                ? Touchable.enabled
                : Touchable.disabled
        );
    }

    private Group createCurrencyDisplay(
        String normalAsset,
        String pressedAsset,
        Label label,
        Runnable onDebugClick
    ) {
        ImageButton button = createCurrencyButton(normalAsset, pressedAsset);

        button.addListener(
            new ClickListener() {
                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {
                    if (onDebugClick != null) {
                        onDebugClick.run();
                    }
                }
            }
        );

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
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();

        style.imageUp = drawable(normalAsset);
        style.imageDown = drawable(pressedAsset);
        style.imageOver = drawable(pressedAsset);


        ImageButton button = new ImageButton(style);
        button.getImageCell().expand().fill();
        button.getImage().setScaling(Scaling.stretch);
        return button;
    }

    private void buildTimedBattlePanel() {
        timedBattlePanel = new BorderedPanel(
            game,
            Color.valueOf("38566E")
        );
        timedBattlePanel.setTouchable(Touchable.disabled);

        Table content = timedBattlePanel.getContent();
        content.clearChildren();
        content.pad(14f, 18f, 20f, 18f);
        content.top();

        Label title = new Label(
            "TIMED BATTLE",
            labelStyle("medium_outline")
        );
        title.setColor(Color.valueOf("FFE06A"));
        title.setAlignment(Align.center);
        title.setFontScale(0.72f);

        timedBattleTimerLabel = new Label(
            "00:00",
            labelStyle("big_outline")
        );
        timedBattleTimerLabel.setColor(Color.WHITE);
        timedBattleTimerLabel.setAlignment(Align.center);
        timedBattleTimerLabel.setFontScale(0.78f);

        timedBattleKillLabel = new Label(
            "ZOMBIES 0 / 0",
            labelStyle("medium_outline")
        );
        timedBattleKillLabel.setColor(Color.WHITE);
        timedBattleKillLabel.setAlignment(Align.left);
        timedBattleKillLabel.setFontScale(0.54f);

        timedBattleSunLabel = new Label(
            "SUN 0 / 0",
            labelStyle("medium_outline")
        );
        timedBattleSunLabel.setColor(Color.WHITE);
        timedBattleSunLabel.setAlignment(Align.left);
        timedBattleSunLabel.setFontScale(0.54f);

        Image zombieIcon = new Image(
            drawable(TIMED_BATTLE_ZOMBIE_ICON)
        );
        zombieIcon.setScaling(Scaling.fit);
        zombieIcon.setTouchable(Touchable.disabled);

        Image sunGoalIcon = new Image(
            drawable(TIMED_BATTLE_SUN_ICON)
        );
        sunGoalIcon.setScaling(Scaling.fit);
        sunGoalIcon.setTouchable(Touchable.disabled);

        timedBattleKillRow = new Table();
        timedBattleKillRow.add(zombieIcon)
            .size(TIMED_BATTLE_ZOMBIE_ICON_SIZE)
            .padRight(6f)
            .center();
        timedBattleKillRow.add(timedBattleKillLabel)
            .left()
            .center();

        timedBattleSunRow = new Table();
        timedBattleSunRow.add(sunGoalIcon)
            .size(TIMED_BATTLE_SUN_ICON_SIZE)
            .padRight(6f)
            .center();
        timedBattleSunRow.add(timedBattleSunLabel)
            .left()
            .center();

        content.add(title)
            .width(220f)
            .center()
            .padBottom(3f)
            .row();

        content.add(timedBattleTimerLabel)
            .width(220f)
            .center()
            .padBottom(9f)
            .row();

        content.add(timedBattleKillRow)
            .width(220f)
            .center()
            .padBottom(4f)
            .row();

        content.add(timedBattleSunRow)
            .width(220f)
            .center()
            .row();

        timedBattlePanel.setVisible(false);
    }

    private void refreshTimedBattle(
        GameState state
    ) {
        if (timedBattlePanel == null) {
            return;
        }

        if (state == null
            || !state.isTimedBattleActive()) {
            timedBattlePanel.setVisible(false);
            return;
        }

        TimedBattleConfig config =
            state.getTimedBattleConfig();

        if (config == null
            || !config.isEnabled()) {
            timedBattlePanel.setVisible(false);
            return;
        }

        timedBattlePanel.setVisible(true);

        double remainingSeconds =
            Math.max(
                0.0,
                state.getTimedBattleRemainingSeconds()
            );

        int wholeSeconds =
            (int) Math.ceil(remainingSeconds);

        int minutes =
            wholeSeconds / 60;

        int seconds =
            wholeSeconds % 60;

        timedBattleTimerLabel.setText(
            String.format(
                "%02d:%02d",
                minutes,
                seconds
            )
        );

        if (remainingSeconds <= TIMED_BATTLE_DANGER_SECONDS
            && !state.isTimedBattleComplete()) {
            timedBattleTimerLabel.setColor(
                Color.valueOf("FF5448")
            );
        } else {
            timedBattleTimerLabel.setColor(
                Color.WHITE
            );
        }

        boolean requiresKills =
            config.requiresZombieKills();

        timedBattleKillRow.setVisible(
            requiresKills
        );

        if (requiresKills) {
            int target =
                config.zombieKillTarget();

            int current =
                MathUtils.clamp(
                    state.getTimedBattleZombieKills(),
                    0,
                    target
                );

            timedBattleKillLabel.setText(
                "ZOMBIES "
                    + current
                    + " / "
                    + target
            );

            timedBattleKillLabel.setColor(
                state.isTimedBattleKillObjectiveComplete()
                    ? Color.valueOf("75E06E")
                    : Color.WHITE
            );
        }

        boolean requiresSun =
            config.requiresSunProduction();

        timedBattleSunRow.setVisible(
            requiresSun
        );

        if (requiresSun) {
            int target =
                config.sunProductionTarget();

            int current =
                MathUtils.clamp(
                    state.getTimedBattleSunProduced(),
                    0,
                    target
                );

            timedBattleSunLabel.setText(
                "SUN "
                    + current
                    + " / "
                    + target
            );

            timedBattleSunLabel.setColor(
                state.isTimedBattleSunObjectiveComplete()
                    ? Color.valueOf("75E06E")
                    : Color.WHITE
            );
        }
    }

    private void buildLoveYourPlantsPanel() {
        lovePlantsPanel = new BorderedPanel(
            game,
            Color.valueOf("38566E")
        );
        lovePlantsPanel.setTouchable(Touchable.disabled);

        Table content = lovePlantsPanel.getContent();
        content.clearChildren();
        content.pad(12f, 12f, 9f, 12f);
        content.top();

        lovePlantsTitleLabel = new Label(
            "LOVE YOUR PLANTS",
            labelStyle("medium_outline")
        );
        lovePlantsTitleLabel.setColor(
            Color.valueOf("B9F36A")
        );
        lovePlantsTitleLabel.setAlignment(
            Align.center
        );
        lovePlantsTitleLabel.setFontScale(
            0.64f
        );

        lovePlantsIconRow = new Table();
        lovePlantsIconRow.setTouchable(
            Touchable.disabled
        );

        lovePlantsCountLabel = new Label(
            "PLANTS LOST 0 / 0",
            labelStyle("medium_outline")
        );
        lovePlantsCountLabel.setColor(
            Color.WHITE
        );
        lovePlantsCountLabel.setAlignment(
            Align.center
        );
        lovePlantsCountLabel.setFontScale(
            0.50f
        );

        content.add(lovePlantsTitleLabel)
            .width(LOVE_PLANTS_CONTENT_WIDTH)
            .center()
            .padTop(5f)
            .padBottom(3f)
            .row();

        content.add(lovePlantsIconRow)
            .width(LOVE_PLANTS_ICON_ROW_WIDTH)
            .height(27f)
            .center()
            .padBottom(1f)
            .row();

        content.add(lovePlantsCountLabel)
            .width(LOVE_PLANTS_CONTENT_WIDTH)
            .center()
            .padTop(4f)
            .row();

        lovePlantsPanel.setVisible(false);
    }

    private void refreshLoveYourPlants(
        GameState state
    ) {
        if (lovePlantsPanel == null) {
            return;
        }

        if (state == null
            || !state.hasPlantLossLimit()) {
            lovePlantsPanel.setVisible(false);
            lastLovePlantsLost = -1;
            return;
        }

        int limit = Math.max(
            1,
            state.getPlantLossLimit()
        );

        int lost = MathUtils.clamp(
            state.getQuestTracker()
                .getPlantsLost(),
            0,
            limit
        );

        ensureLovePlantIcons(limit);

        lovePlantsPanel.setVisible(true);

        lovePlantsCountLabel.setText(
            "PLANTS LOST "
                + lost
                + " / "
                + limit
        );

        for (int i = 0; i < lovePlantIcons.length; i++) {
            Image icon = lovePlantIcons[i];

            if (i < lost) {
                icon.setColor(
                    new Color(
                        0.55f,
                        0.18f,
                        0.18f,
                        0.42f
                    )
                );
            } else {
                icon.setColor(
                    Color.WHITE
                );
            }
        }

        int remaining =
            Math.max(
                0,
                limit - lost
            );

        if (remaining <= 1) {
            lovePlantsCountLabel.setColor(
                Color.valueOf("FF5448")
            );
            lovePlantsTitleLabel.setColor(
                Color.valueOf("FFD05A")
            );
        } else {
            lovePlantsCountLabel.setColor(
                Color.WHITE
            );
            lovePlantsTitleLabel.setColor(
                Color.valueOf("B9F36A")
            );
        }

        if (lastLovePlantsLost >= 0
            && lost > lastLovePlantsLost) {
            animatePlantLost(
                lost,
                limit
            );
        }

        lastLovePlantsLost = lost;
    }

    private void ensureLovePlantIcons(
        int limit
    ) {
        if (limit == lastLovePlantsLimit
            && lovePlantIcons.length == limit) {
            return;
        }

        lovePlantsIconRow.clearChildren();
        lovePlantIcons = new Image[limit];

        float availableWidth =
            LOVE_PLANTS_ICON_ROW_WIDTH - 8f;

        float iconSize = MathUtils.clamp(
            availableWidth
                / Math.max(1, limit)
                - 4f,
            LOVE_PLANTS_ICON_MIN_SIZE,
            LOVE_PLANTS_ICON_MAX_SIZE
        );

        float iconHeight =
            iconSize * 58f / 60f;

        for (int i = 0; i < limit; i++) {
            Image icon = new Image(
                drawable(
                    LOVE_PLANTS_ICON
                )
            );

            icon.setScaling(
                Scaling.fit
            );
            icon.setTouchable(
                Touchable.disabled
            );
            icon.setOrigin(
                Align.center
            );

            lovePlantIcons[i] = icon;

            lovePlantsIconRow.add(icon)
                .size(
                    iconSize,
                    iconHeight
                )
                .padLeft(2f)
                .padRight(2f);
        }

        lastLovePlantsLimit = limit;
    }

    private void animatePlantLost(
        int lost,
        int limit
    ) {
        int iconIndex = MathUtils.clamp(
            lost - 1,
            0,
            Math.max(
                0,
                limit - 1
            )
        );

        if (iconIndex < lovePlantIcons.length) {
            Image lostIcon =
                lovePlantIcons[iconIndex];

            lostIcon.clearActions();
            lostIcon.setOrigin(
                Align.center
            );
            lostIcon.addAction(
                Actions.sequence(
                    Actions.scaleTo(
                        1.28f,
                        1.28f,
                        0.08f
                    ),
                    Actions.scaleTo(
                        0.86f,
                        0.86f,
                        0.10f
                    ),
                    Actions.scaleTo(
                        1f,
                        1f,
                        0.10f
                    )
                )
            );
        }

        lovePlantsIconRow.clearActions();
        lovePlantsIconRow.addAction(
            Actions.sequence(
                Actions.moveBy(
                    -4f,
                    0f,
                    0.04f
                ),
                Actions.moveBy(
                    8f,
                    0f,
                    0.07f
                ),
                Actions.moveBy(
                    -8f,
                    0f,
                    0.07f
                ),
                Actions.moveBy(
                    8f,
                    0f,
                    0.07f
                ),
                Actions.moveBy(
                    -4f,
                    0f,
                    0.04f
                )
            )
        );

        lovePlantsCountLabel.clearActions();
        lovePlantsCountLabel.setColor(
            Color.valueOf("FF3B30")
        );
        lovePlantsCountLabel.addAction(
            Actions.sequence(
                Actions.delay(
                    0.28f
                ),
                Actions.color(
                    limit - lost <= 1
                        ? Color.valueOf("FF5448")
                        : Color.WHITE,
                    0.18f
                )
            )
        );
    }

    private void buildPlantWhatYouGetPanel() {
        plantWhatYouGetPanel = new BorderedPanel(
            game,
            Color.valueOf("38566E")
        );
        plantWhatYouGetPanel.setTouchable(
            Touchable.childrenOnly
        );

        Table content =
            plantWhatYouGetPanel.getContent();

        content.clearChildren();
        content.pad(
            12f,
            14f,
            12f,
            14f
        );
        content.top();

        Label title = new Label(
            "PLANT WHAT YOU GET",
            labelStyle("medium_outline")
        );
        title.setColor(
            Color.valueOf("FFE06A")
        );
        title.setAlignment(
            Align.center
        );
        title.setFontScale(
            0.62f
        );

        Label subtitle = new Label(
            "PREPARE YOUR LAWN",
            labelStyle("medium_outline")
        );
        subtitle.setColor(
            Color.WHITE
        );
        subtitle.setAlignment(
            Align.center
        );
        subtitle.setFontScale(
            0.48f
        );

        plantWhatYouGetStartButton =
            createPlantWhatYouGetButton(
                "START WAVES"
            );

        plantWhatYouGetStartButton.addListener(
            new ClickListener() {
                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {
                    Result result =
                        gamingController
                            .startZombieWaves();

                    showResult(result);
                    refreshGameplayState();
                }
            }
        );

        content.add(title)
            .width(205f)
            .center()
            .padBottom(2f)
            .row();

        content.add(subtitle)
            .width(205f)
            .center()
            .padBottom(7f)
            .row();

        content.add(
                plantWhatYouGetStartButton
            )
            .width(170f)
            .height(44f)
            .center()
            .row();

        plantWhatYouGetPanel.setVisible(
            false
        );
    }

    private TextButton createPlantWhatYouGetButton(
        String text
    ) {
        TextButton.TextButtonStyle style =
            new TextButton.TextButtonStyle();

        style.font =
            labelStyle(
                "medium_outline"
            ).font;

        style.fontColor =
            Color.WHITE;

        style.up = drawable(
            PLANT_WHAT_YOU_GET_BUTTON
        );

        style.down = drawable(
            PLANT_WHAT_YOU_GET_BUTTON_DOWN
        );

        style.over = drawable(
            PLANT_WHAT_YOU_GET_BUTTON_DOWN
        );

        TextButton button =
            new TextButton(
                text,
                style
            );

        button.getLabel().setFontScale(
            0.70f
        );

        return button;
    }

    private void refreshPlantWhatYouGet(
        Game currentGame
    ) {
        if (plantWhatYouGetPanel == null) {
            return;
        }

        boolean preparing =
            currentGame != null
                && currentGame
                .isPlantWhatYouGetLevel()
                && currentGame
                .isPreparingPlantWhatYouGet();

        plantWhatYouGetPanel.setVisible(
            preparing
        );

        plantWhatYouGetPanel.setTouchable(
            preparing
                ? Touchable.childrenOnly
                : Touchable.disabled
        );
    }

    private void buildWaveProgress() {
        waveGroup = new Group();
        waveGroup.setSize(
            WAVE_BAR_WIDTH + 70f,
            85f
        );

        waveFill = new Image(game.getSkin().newDrawable("white_pixel", Color.valueOf("65B83B")));
        waveFill.setBounds(
            WAVE_BAR_WIDTH,
            WAVE_FILL_Y,
            0f,
            WAVE_FILL_HEIGHT
        );

        waveGroup.addActor(waveFill);
        Image progressFrame = new Image(drawable(WAVE_PROGRESS));
        progressFrame.setScaling(Scaling.stretch);
        progressFrame.setBounds(
            0f,
            0f,
            WAVE_BAR_WIDTH,
            WAVE_BAR_HEIGHT
        );

        waveGroup.addActor(progressFrame);
        waveZombieTexture =
            new Texture(
                Gdx.files.internal(
                    "assets/UIs/zombie.png"
                )
            );

        waveZombieHead =
            new Image(waveZombieTexture);

        waveZombieHead.setScaling(
            Scaling.fit
        );
        waveZombieHead.setBounds(
            TRACK_RIGHT - 12f,
            -7f,
            52f,
            52f
        );
        waveGroup.addActor(
            waveZombieHead
        );
        bottomRight.add(waveGroup)
            .width(WAVE_BAR_WIDTH + 70f)
            .height(85f);

        waveGroup.setVisible(false);
    }

    private void rebuildWaveFlags(int totalWaves) {

        if (waveMarkers != null) {
            for (Group marker : waveMarkers) {
                if (marker != null) {
                    marker.remove();
                }
            }
        }

        waveMarkers = new Group[totalWaves];
        waveFlagImages = new Image[totalWaves];
        float trackWidth = TRACK_RIGHT - TRACK_LEFT;
        for (int i = 0; i < totalWaves; i++) {

            float fraction = (i + 1f) / totalWaves;
            float poleX = TRACK_RIGHT - trackWidth * fraction;
            Group marker = new Group();
            marker.setBounds(
                poleX - FLAG_WIDTH,
                0f,
                FLAG_WIDTH + FLAG_POLE_WIDTH,
                58f
            );

            Image pole =
                new Image(
                    drawable(FLAG_BAR)
                );

            pole.setScaling(
                Scaling.stretch
            );

            pole.setBounds(
                FLAG_WIDTH - FLAG_POLE_WIDTH / 2f,
                0f,
                FLAG_POLE_WIDTH,
                FLAG_POLE_HEIGHT
            );

            marker.addActor(pole);
            Image flag =
                new Image(
                    drawable(FLAG)
                );

            flag.setScaling(
                Scaling.fit
            );


            flag.setBounds(
                23f,
                FLAG_DOWN_Y,
                FLAG_WIDTH,
                FLAG_HEIGHT
            );

            marker.addActor(flag);


            waveGroup.addActor(marker);

            waveMarkers[i] = marker;
            waveFlagImages[i] = flag;
        }

        lastCompletedWaves = -1;
    }

    private void updateWaveFlags(int completedWaves, int totalWaves) {
        if (waveFlagImages == null || waveFlagImages.length != totalWaves) {
            rebuildWaveFlags(totalWaves);
        }

        completedWaves = MathUtils.clamp(
            completedWaves,
            0,
            totalWaves
        );

        if (completedWaves == lastCompletedWaves) {
            return;
        }

        for (int i = 0; i < waveFlagImages.length; i++) {
            Image flag = waveFlagImages[i];
            float targetY;
            if (i < completedWaves) {
                targetY = FLAG_UP_Y;
            } else {
                targetY = FLAG_DOWN_Y;
            }
            flag.clearActions();
            flag.addAction(
                Actions.moveTo(
                    flag.getX(),
                    targetY,
                    0.30f
                )
            );
        }
        lastCompletedWaves = completedWaves;
    }

    private void updateWaveFill(float progress) {

        progress = MathUtils.clamp(progress, 0f, 1f);
        float trackWidth = TRACK_RIGHT - TRACK_LEFT;
        float fillWidth = trackWidth * progress;

        float progressX = TRACK_RIGHT - fillWidth;
        waveFill.setBounds(progressX, WAVE_FILL_Y, fillWidth, WAVE_FILL_HEIGHT);
        waveZombieHead.setPosition(
            progressX - 12f,
            waveZombieHead.getY()
        );
    }

    @Override
    public void act(
        float delta
    ) {

        super.act(delta);


        if (!isVisible()) {
            return;
        }

        refreshDebugControls();

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
        if (currentGame == null
            || currentGame.getGameState() == null) {
            return;
        }

        GameState state = currentGame.getGameState();
        refreshTimedBattle(state);
        refreshLoveYourPlants(state);
        refreshPlantWhatYouGet(currentGame);
        sunLabel.setText(state.getSun());
        int foodCount = MathUtils.clamp(state.getPlantFoodCount(), 0, MAX_PLANT_FOOD);
        for (int i = 0; i < plantFoodDots.length; i++) {

            if (i < foodCount) {plantFoodDots[i].setColor(Color.valueOf("65D44B"));
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
        if (waveManager == null
            || currentGame.isPreparingPlantWhatYouGet()) {
            waveGroup.setVisible(false);
            return;
        }
        waveGroup.setVisible(true);
        int current = waveManager.getCurrentWaveNumber();
        if (waveManager.isEndless()) {

            // Endless بعداً
            return;
        }


        int total = waveManager.getTotalWaves();
        if (total <= 0) {
            return;
        }
        int completed = waveManager.isLevelCleared() ? total : MathUtils.clamp(current - 1, 0, total);
        updateWaveFlags(
                completed,
                total
        );
        float progress;

        if (waveManager.isLevelCleared()) {
            progress = 1f;
        } else if (current <= 0) {
            progress = 0f;
        } else {
            float currentWaveProgress =
                    waveManager.getCurrentWaveProgress();

            progress = (completed + currentWaveProgress) / (float) total;
        }

        updateWaveFill(progress);
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
        refreshDebugControls();

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
            TextureRegion region =
                game.getTextureBank().region(id);

            if (region == null) {
                throw new IllegalStateException(
                    "Missing HUD asset: " + id
                );
            }

            return new TextureRegionDrawable(region);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Could not load HUD asset: " + id,
                exception
            );
        }
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

    public void dispose() {
        if (waveZombieTexture != null) {
            waveZombieTexture.dispose();
            waveZombieTexture = null;
        }
    }

    public PlantSlotsBar getPlantSlotsBar() {
        return plantSlotsBar;
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
