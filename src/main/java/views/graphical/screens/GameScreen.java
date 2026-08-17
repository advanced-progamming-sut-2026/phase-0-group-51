package views.graphical.screens;

import Data.loader.PlantData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import controllers.GamingController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.Level;


import models.sun.Sun;
import views.graphical.gameplay.actors.PlantActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;

import views.graphical.gameplay.hud.GameHud;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;
import views.graphical.gameplay.zombie.ZombieLevelPreview;
import views.graphical.ui.GameSettings;
import views.graphical.ui.PauseMenuPopup;
import views.graphical.ui.PlantSelectionMenuTable;
import views.graphical.ui.PlantSlotsBar;
import views.graphical.ui.StartGameMenuPopup;

public class GameScreen extends BaseScreen {

    private final ChapterTheme theme;
    private final Level currentLevel;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Stage uiStage;
    private final Stage worldStage;
    private final InputMultiplexer inputMultiplexer;
    private final PlantSlotsBar plantSlotsBar;

    private TextureRegion bgLeft;
    private TextureRegion bgMid;
    private TextureRegion bgRight;

    private final float viewWidth = 1066f;
    private final float worldHeight = 600f;
    private enum IntroState {
        WAIT_AT_MAIN, PAN_TO_ZOMBIES, WAIT_AT_ZOMBIES, PAN_TO_SELECTION, SHOW_PLANT_SELECT, WAITING_FOR_SELECTION, PAN_BACK_TO_MAIN, PLAYING
    }

    private enum OverlayMode {
        NONE, START_OBJECTIVES, PAUSE
    }

    private enum ToolMode {
        NONE, SHOVEL, PLANT_FOOD
    }

    private OverlayMode overlayMode = OverlayMode.NONE;
    private ToolMode toolMode = ToolMode.NONE;

    private Table modalOverlay;
    private final Texture modalDimTexture;

    private IntroState introState = IntroState.WAIT_AT_MAIN;
    private float stateTime = 0f;

    private float cameraMainX;
    private float cameraRightX;
    private float cameraSelectionX;
    private float cameraGameplayX;

    private GameHud gameHud;


    private final ShapeRenderer shapeRenderer;

    private BoardView boardView;
    private BoardArea boardArea;
    private final BoardTransform boardTransform;
    private final ZombieAnimationSystem zombieAnimationSystem;
    private final ZombieLevelPreview zombieLevelPreview;


    private final GamingController gamingController = new GamingController();

    private float gameTickAccumulator;

    private PlantActor placementPreview;
    private PlantViewManager plantViewManager;
    private SunViewManager sunViewManager;
    private ProjectileViewManager projectileViewManager;

    private Image rowHighlight;
    private Image columnHighlight;

    private static final String SHOVEL_CURSOR =
            "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";
    private static final String PLANT_FOOD_CURSOR =
            "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";
    private static final float TOOL_CURSOR_ALPHA = 0.58f;
    private static final float TOOL_CURSOR_SCALE = 0.65f;

    private Image toolCursorPreview;
    private final Vector2 toolCursorPosition = new Vector2();


    public GameScreen(PvzGame game, ChapterTheme theme, int levelNumber) {
        super(game);
        this.theme = theme;
        this.currentLevel = theme.getLevels().stream()
                .filter(l -> l.levelNumber() == levelNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Level " + levelNumber + " not found!"));

        Game currentGame = new Game();
        int chapterIndex = currentGame.getChapters().indexOf(theme);
        int levelIndex = theme.getLevels().indexOf(currentLevel);
        if (chapterIndex < 0 || levelIndex < 0) {
            throw new IllegalStateException(
                    "Could not resolve selected chapter/level."
            );
        }

        currentGame.setCurrentChapterIndex(chapterIndex);
        currentGame.setCurrentLevelIndex(levelIndex);

        App.getInstance().setCurrentGame(currentGame);

        loadBackgroundAssets();

        float totalWorldWidth = bgLeft.getRegionWidth() + bgMid.getRegionWidth() + bgRight.getRegionWidth();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(viewWidth, worldHeight, camera);
        worldStage = new Stage(viewport, game.getBatch());
        uiStage = new Stage(new ExtendViewport(viewWidth, worldHeight));
        inputMultiplexer = new InputMultiplexer(uiStage, worldStage);

        plantSlotsBar = new PlantSlotsBar(game);
        plantSlotsBar.setMode(PlantSlotsBar.Mode.SELECTION);

        cameraMainX = viewWidth / 2f;
        cameraGameplayX = bgLeft.getRegionWidth() + bgMid.getRegionWidth() / 2f;
        cameraRightX = totalWorldWidth - (viewWidth / 2f);
        cameraSelectionX = cameraRightX - (viewWidth / 2.5f);

        camera.position.set(cameraMainX, worldHeight / 2f, 0);
        camera.update();

        shapeRenderer = new ShapeRenderer();

        Pixmap modalDimPixmap =
                new Pixmap(
                        1,
                        1,
                        Pixmap.Format.RGBA8888
                );

        modalDimPixmap.setColor(Color.WHITE);
        modalDimPixmap.fill();

        modalDimTexture =
                new Texture(modalDimPixmap);

        modalDimPixmap.dispose();

        boardArea = new BoardArea(
                533f,
                62f,
                737f,
                380f);

        boardTransform = new BoardTransform(boardArea);
        zombieAnimationSystem = new ZombieAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                boardTransform,
                theme
        );

        zombieLevelPreview = new ZombieLevelPreview(
                game.getPamPlayer(),
                worldStage,
                theme,
                cameraRightX
        );

        zombieLevelPreview.show(
                currentLevel.resolveAllowedZombies(
                        theme.getAllowedZombies()
                )
        );
    }

    private void loadBackgroundAssets() {
        switch (theme) {
            case ANCIENT_EGYPT:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_RIGHT");
                break;
            case FROSTBITE_CAVES:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_RIGHT");
                break;
            case BIG_WAVE_BEACH:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_BEACH_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_BEACH_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_BEACH_TEXTURE_RIGHT");
                break;
            case DARK_AGES:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_DARK_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_DARK_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_DARK_TEXTURE_RIGHT");
                break;
            default:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_RIGHT");
                break;
        }
    }

    @Override
    public InputMultiplexer getInputProcessor() {
        return inputMultiplexer;
    }

    @Override
    public void show() {
        game.hideHud();
        showStartObjectives();
    }

    @Override
    public void hide() {
        if (gameHud != null) {
            gameHud.hideGameHud();
        }
        removeModal();
    }

    private void updateCutscene(float delta) {
        if (overlayMode != OverlayMode.NONE) {
            return;
        }

        if (introState == IntroState.PLAYING
                || introState == IntroState.WAITING_FOR_SELECTION) {
            return;
        }

        stateTime += delta;

        float waitDuration = 1.0f;
        float panDuration = 1.5f;
        float shortPanDuration = 0.8f;
        switch (introState) {
            case WAIT_AT_MAIN:
                if (stateTime >= waitDuration) {
                    introState = IntroState.PAN_TO_ZOMBIES;
                    stateTime = 0f;
                }
                break;

            case PAN_TO_ZOMBIES:
                float progressRight = Math.min(1f, stateTime / panDuration);
                camera.position.x = Interpolation.smooth.apply(cameraMainX, cameraRightX, progressRight);
                if (progressRight >= 1f) {
                    introState = IntroState.WAIT_AT_ZOMBIES;
                    stateTime = 0f;
                }
                break;

            case WAIT_AT_ZOMBIES:
                if (stateTime >= waitDuration) {
                    introState = IntroState.PAN_TO_SELECTION;
                    stateTime = 0f;
                }
                break;

            case PAN_TO_SELECTION:
                float progressSelection = Math.min(1f, stateTime / shortPanDuration);
                camera.position.x = Interpolation.smooth.apply(cameraRightX, cameraSelectionX, progressSelection);
                if (progressSelection >= 1f) {
                    introState = IntroState.SHOW_PLANT_SELECT;
                    stateTime = 0f;
                }
                break;

            case SHOW_PLANT_SELECT:


                PlantSelectionMenuTable plantSelection = new PlantSelectionMenuTable(game, plantSlotsBar, this::startGameAfterSelection);
                uiStage.addActor(plantSelection);
                introState = IntroState.WAITING_FOR_SELECTION;
                break;

            case PAN_BACK_TO_MAIN:
                float progressLeft = Math.min(1f, stateTime / panDuration);
                camera.position.x = Interpolation.smooth.apply(cameraSelectionX, cameraGameplayX, progressLeft);
                if (progressLeft >= 1f) {
                    camera.position.x = cameraGameplayX;
                    introState = IntroState.PLAYING;
                    if (gameHud != null) {
                        gameHud.showGameHud();
                    }
                }
                break;
        }

        camera.update();
    }

    public void startGameAfterSelection() {
        zombieLevelPreview.clear();

        plantSlotsBar.remove();
        plantSlotsBar.setOnRemoveRequested(null);

        uiStage.clear();
        plantSlotsBar.setMode(
                PlantSlotsBar.Mode.GAMEPLAY
        );
        plantSlotsBar.setOnPlantSelected(
                this::handlePlantSelectionChanged
        );

        gameHud = new GameHud(
                game,
                plantSlotsBar,
                this::showPauseMenu
        );
        uiStage.addActor(gameHud);
        gameHud.setOnShovelRequested(
                this::toggleShovelMode
        );
        gameHud.setOnPlantFoodRequested(
                this::togglePlantFoodMode
        );

        Game currentGame =
                App.getInstance()
                        .getCurrentGame();

        if (currentGame == null
                || currentGame.getGameState() == null) {
            throw new IllegalStateException(
                    "Game state was not created."
            );
        }

        Board board =
                currentGame
                        .getGameState()
                        .getBoard();

        boardView =
                new BoardView(
                        board,
                        boardTransform
                );

        boardView.setOnTileClicked(
                this::handleTileClick
        );
        boardView.setOnTileHovered(
                this::handleTileHover
        );

        createPlacementHighlights();
        worldStage.addActor(rowHighlight);
        worldStage.addActor(columnHighlight);
        worldStage.addActor(boardView);

        plantViewManager = new PlantViewManager(game, boardTransform);
        worldStage.addActor(plantViewManager);
        projectileViewManager = new ProjectileViewManager(game, boardTransform);
        worldStage.addActor(projectileViewManager);
        sunViewManager = new SunViewManager(game, boardTransform);
        sunViewManager.setOnSunClicked(this::handleSunClicked);
        worldStage.addActor(sunViewManager);

        placementPreview = new PlantActor(game);
        placementPreview.setPreviewMode(true);
        worldStage.addActor(placementPreview);

        gameTickAccumulator = 0f;
        introState = IntroState.PAN_BACK_TO_MAIN;
        stateTime = 0f;
    }

    private void handlePlantSelectionChanged(
            PlantData plant
    ) {
        if (placementPreview == null) {
            return;
        }

        if (plant == null) {
            placementPreview.clearPlant();
            if (toolMode == ToolMode.NONE) {
                hidePlacementHighlights();
            }
            return;
        }

        setToolMode(ToolMode.NONE);
        placementPreview.setPreviewMode(true);
        placementPreview.setPlant(plant);
    }

    private void toggleShovelMode() {
        if (toolMode == ToolMode.SHOVEL) {
            setToolMode(ToolMode.NONE);
            return;
        }

        plantSlotsBar.clearPlantSelection();
        setToolMode(ToolMode.SHOVEL);
    }

    private void togglePlantFoodMode() {
        if (toolMode == ToolMode.PLANT_FOOD) {
            setToolMode(ToolMode.NONE);
            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null
                || currentGame.getGameState() == null) {
            return;
        }

        if (currentGame.getGameState().getPlantFoodCount() <= 0) {
            game.notifyError("You do not have any Plant Food.");
            return;
        }

        plantSlotsBar.clearPlantSelection();
        setToolMode(ToolMode.PLANT_FOOD);
    }

    private void setToolMode(ToolMode mode) {
        toolMode = mode;

        if (gameHud != null) {
            gameHud.setShovelSelected(
                    mode == ToolMode.SHOVEL
            );
        }

        switch (mode) {
            case SHOVEL -> showToolCursor(SHOVEL_CURSOR);
            case PLANT_FOOD -> showToolCursor(PLANT_FOOD_CURSOR);
            case NONE -> hideToolCursor();
        }

        if (mode != ToolMode.NONE && placementPreview != null) {
            placementPreview.clearPlant();
        }

        if (mode == ToolMode.NONE
                && plantSlotsBar.getSelectedPlant() == null) {
            hidePlacementHighlights();
        }
    }

    private void showToolCursor(String assetId) {
        TextureRegion region =
                game.getTextureBank().region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: " + assetId
            );
        }

        if (toolCursorPreview == null) {
            toolCursorPreview = new Image();
            toolCursorPreview.setTouchable(Touchable.disabled);
            uiStage.addActor(toolCursorPreview);
        }

        toolCursorPreview.setDrawable(
                new TextureRegionDrawable(region)
        );
        toolCursorPreview.setSize(
                region.getRegionWidth() * TOOL_CURSOR_SCALE,
                region.getRegionHeight() * TOOL_CURSOR_SCALE
        );
        toolCursorPreview.setColor(
                1f,
                1f,
                1f,
                TOOL_CURSOR_ALPHA
        );
        toolCursorPreview.setVisible(true);
        toolCursorPreview.toFront();
    }

    private void hideToolCursor() {
        if (toolCursorPreview != null) {
            toolCursorPreview.setVisible(false);
        }
    }

    private void updateToolCursorPreview() {
        if (toolCursorPreview == null) {
            return;
        }

        boolean shouldShow =
                toolMode != ToolMode.NONE
                        && introState == IntroState.PLAYING
                        && overlayMode == OverlayMode.NONE;

        toolCursorPreview.setVisible(shouldShow);
        if (!shouldShow) {
            return;
        }

        toolCursorPosition.set(
                Gdx.input.getX(),
                Gdx.input.getY()
        );
        uiStage.screenToStageCoordinates(toolCursorPosition);

        toolCursorPreview.setPosition(
                toolCursorPosition.x
                        - toolCursorPreview.getWidth() / 2f,
                toolCursorPosition.y
                        - toolCursorPreview.getHeight() / 2f
        );
        toolCursorPreview.toFront();
    }
    private void handleSunClicked(Sun sun) {
        if (sun == null || introState != IntroState.PLAYING || overlayMode != OverlayMode.NONE) {

            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();

        if (currentGame == null || currentGame.getGameState() == null) {

            return;
        }

        boolean collected = currentGame.getGameState().getBoard().collectSun(sun, currentGame.getGameState());

        if (!collected) {
            game.notifyError("Sun has expired or was already collected.");
        }
    }

    private void showStartObjectives() {
        if (overlayMode != OverlayMode.NONE) {
            return;
        }

        overlayMode = OverlayMode.START_OBJECTIVES;

        gameTickAccumulator = 0f;

        ChapterTheme levelTheme =
                currentLevel.chapterTheme();

        String chapterName =
                levelTheme == null
                        ? theme.getName()
                        : levelTheme.getName();

        StartGameMenuPopup popup = new StartGameMenuPopup(
                game,
                this::continueAfterObjectives,
                chapterName,
                currentLevel.levelNumber(),
                currentLevel.description(),
                currentLevel.objectives().toArray(String[]::new));

        showModal(popup);
        animateStartPopup(popup);
    }
    private void animateStartPopup(Actor popup) {
        modalOverlay.validate();
        float targetX = popup.getX();
        float targetY = popup.getY();
        popup.setPosition(targetX, -popup.getHeight());
        popup.addAction(Actions.moveTo(targetX, targetY, 0.55f, Interpolation.pow3Out));
    }
    private void continueAfterObjectives() {
        if (overlayMode != OverlayMode.START_OBJECTIVES) {
            return;
        }

        removeModal();

        overlayMode = OverlayMode.NONE;
        gameTickAccumulator = 0f;
        stateTime = 0f;
    }
    private void showModal(Actor popup) {
        removeModal();

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);

        TextureRegionDrawable dimBackground =
                new TextureRegionDrawable(
                        new TextureRegion(
                                modalDimTexture
                        )
                );

        overlay.setBackground(
                dimBackground.tint(
                        new Color(
                                0f,
                                0f,
                                0f,
                                0.62f
                        )
                )
        );

        overlay.addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            int button
                    ) {
                        return true;
                    }
                }
        );

        overlay.add(popup).center();

        modalOverlay = overlay;
        uiStage.addActor(modalOverlay);
        modalOverlay.toFront();
    }
    private void removeModal() {
        if (modalOverlay == null) {
            return;
        }
        modalOverlay.remove();
        modalOverlay = null;
    }

    private void handlePauseShortcut() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return;
        }

        if (overlayMode == OverlayMode.PAUSE) {
            resumeGame();
            return;
        }

        if (introState == IntroState.PLAYING
                && overlayMode == OverlayMode.NONE) {
            showPauseMenu();
        }
    }

    private void showPauseMenu() {
        if (introState != IntroState.PLAYING
                || overlayMode != OverlayMode.NONE) {
            return;
        }

        overlayMode = OverlayMode.PAUSE;
        gameTickAccumulator = 0f;

        PauseMenuPopup popup =
                new PauseMenuPopup(
                        game,
                        this::saveAndExit,
                        this::restartLevel,
                        this::resumeGame
                );

        showModal(popup);
    }

    private void resumeGame() {
        if (overlayMode != OverlayMode.PAUSE) {
            return;
        }

        removeModal();
        overlayMode = OverlayMode.NONE;
        gameTickAccumulator = 0f;
    }

    private void restartLevel() {
        removeModal();

        Gdx.app.postRunnable(
                () -> game.showScreen(
                        new GameScreen(
                                game,
                                theme,
                                currentLevel.levelNumber()
                        )
                )
        );
    }

    private void saveAndExit() {
        removeModal();
        if (gameHud != null) {
            gameHud.hideGameHud();
            gameHud.remove();
        }

        Gdx.app.postRunnable(
                () -> game.showScreen(
                        new MainMenuScreen(game)
                )
        );
    }
    private void createPlacementHighlights() {
        Drawable highlightDrawable = game.getSkin().newDrawable("white_pixel", new Color(1f, 1f, 1f, 0.70f));
        rowHighlight = new Image(highlightDrawable);
        columnHighlight = new Image(highlightDrawable);
        rowHighlight.setTouchable(Touchable.disabled);
        columnHighlight.setTouchable(Touchable.disabled);
        rowHighlight.setVisible(false);
        columnHighlight.setVisible(false);
    }
    private void handleTileHover(Tile tile) {
        boolean hasPlantSelection =
                plantSlotsBar.getSelectedPlant() != null;
        boolean hasToolSelection =
                toolMode != ToolMode.NONE;

        if (tile == null
                || (!hasPlantSelection && !hasToolSelection)) {
            hidePlacementHighlights();
            return;
        }
        BoardArea area = boardTransform.getArea();
        rowHighlight.setBounds(area.x(),
                boardTransform.tileY(tile.getLane()),
                area.width(),
                boardTransform.tileHeight()
        );

        columnHighlight.setBounds(
                boardTransform.tileX(tile.getColumn()),
                area.y(),
                boardTransform.tileWidth(),
                area.height()
        );

        rowHighlight.setVisible(true);
        columnHighlight.setVisible(true);
    }
    private void hidePlacementHighlights() {
        if (rowHighlight != null) {
            rowHighlight.setVisible(false);
        }

        if (columnHighlight != null) {
            columnHighlight.setVisible(false);
        }
    }
    @Override
    public void render(float delta) {
        handlePauseShortcut();
        updateCutscene(delta);
        updateGameplayTicks(delta);

        if (introState == IntroState.PLAYING
                && overlayMode == OverlayMode.NONE) {
            Game currentGame = App.getInstance().getCurrentGame();
            if (currentGame != null && currentGame.getGameState() != null) {
                zombieAnimationSystem.update(
                        delta,
                        currentGame.getGameState().getZombiesInTheGame()
                );
            }
        }

        updateToolCursorPreview();
        uiStage.act(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        game.getBatch().setColor(Color.WHITE);

        float currentX = 0;
        game.getBatch().draw(bgLeft, currentX, 0, bgLeft.getRegionWidth(), worldHeight);
        currentX += bgLeft.getRegionWidth();

        game.getBatch().draw(bgMid, currentX, 0, bgMid.getRegionWidth(), worldHeight);
        currentX += bgMid.getRegionWidth();

        game.getBatch().draw(bgRight, currentX, 0, bgRight.getRegionWidth(), worldHeight);

        game.getBatch().end();
        boolean animateZombiePreview =
                introState == IntroState.PAN_TO_ZOMBIES
                        || introState == IntroState.WAIT_AT_ZOMBIES
                        || introState == IntroState.PAN_TO_SELECTION
                        || introState == IntroState.WAITING_FOR_SELECTION;

        if (introState == IntroState.PLAYING
                && overlayMode == OverlayMode.NONE) {
            Game currentGame = App.getInstance().getCurrentGame();
            if (currentGame != null
                    && currentGame.getGameState() != null) {
                Board board =
                        currentGame.getGameState().getBoard();

                if (plantViewManager != null) {
                    plantViewManager.sync(
                            board
                    );
                }
                if (projectileViewManager != null) {
                    projectileViewManager.sync(
                            board.getProjectiles(),
                            getRenderTickAlpha()
                    );
                }
                if (sunViewManager != null) {
                    sunViewManager.sync(
                            board.getActiveSuns(),
                            getRenderTickAlpha()
                    );
                }
            }
        }

        if (overlayMode == OverlayMode.NONE
                && (introState == IntroState.PLAYING
                || animateZombiePreview)) {
            worldStage.act(delta);
        }

        worldStage.draw();

        drawDebugGrid();
        game.getBatch().setColor(Color.WHITE);
        uiStage.draw();
    }
    private float getRenderTickAlpha() {
        Game currentGame =
                App.getInstance()
                        .getCurrentGame();

        if (currentGame == null
                || currentGame.getGameState() == null) {
            return 0f;
        }

        int ticksPerSecond =
                Math.max(
                        1,
                        currentGame
                                .getGameState()
                                .getTicksPerSecond()
                );

        float tickDuration =
                1f / ticksPerSecond;

        return Math.min(
                1f,
                gameTickAccumulator
                        / tickDuration
        );
    }

    private void updateGameplayTicks(float delta) {
        if (introState != IntroState.PLAYING || overlayMode != OverlayMode.NONE) {
            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();

        if (currentGame == null
                || currentGame.getGameState() == null
                || currentGame.getGameState().isFinished()) {
            return;
        }

        int ticksPerSecond = Math.max(1, currentGame.getGameState().getTicksPerSecond());

        float tickDuration = 1f / ticksPerSecond;

        gameTickAccumulator += Math.min(delta, 0.25f);

        while (gameTickAccumulator >= tickDuration) {
            if (currentGame.getGameState().isFinished()) {
                gameTickAccumulator = 0f;
                break;
            }
            currentGame.forward(1);
            gameTickAccumulator -= tickDuration;
        }
    }

    private void handleTileClick(
            Tile tile
    ) {
        if (introState != IntroState.PLAYING
                || overlayMode != OverlayMode.NONE
                || tile == null) {
            return;
        }

        int x = tile.getColumn() + 1;
        int y = tile.getLane() + 1;

        if (toolMode == ToolMode.SHOVEL) {
            Result result = gamingController.pluckPlant(x, y);
            if (!result.success()) {
                game.notifyError(result.message());
                return;
            }

            setToolMode(ToolMode.NONE);
            return;
        }

        if (toolMode == ToolMode.PLANT_FOOD) {
            Result result = gamingController.feedPlant(x, y);
            if (!result.success()) {
                game.notifyError(result.message());
                return;
            }

            setToolMode(ToolMode.NONE);
            return;
        }

        PlantData selectedPlant = plantSlotsBar.getSelectedPlant();
        if (selectedPlant == null) {
            return;
        }

        Result result =
                gamingController.plantPlant(
                        selectedPlant.name(),
                        x,
                        y
                );

        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        plantSlotsBar.clearPlantSelection();
    }

    private void drawDebugGrid() {
        if (!GameSettings.showGrid) {
            return;
        }

        BoardArea area = boardTransform.getArea();

        float tileWidth = boardTransform.tileWidth();

        float tileHeight = boardTransform.tileHeight();

        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        shapeRenderer.setColor(Color.RED);

        for (int column = 0; column <= BoardTransform.COLUMNS; column++) {
            float x = area.x() + column * tileWidth;
            shapeRenderer.line(x, area.y(), x, area.y() + area.height());
        }

        for (int row = 0; row <= BoardTransform.ROWS; row++) {
            float y = area.y() + row * tileHeight;
            shapeRenderer.line(area.x(), y, area.x() + area.width(), y);
        }
        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (gameHud != null) {
            gameHud.dispose();
        }
        zombieLevelPreview.clear();
        zombieAnimationSystem.clear();
        uiStage.dispose();
        worldStage.dispose();
        shapeRenderer.dispose();
        modalDimTexture.dispose();
    }
}
