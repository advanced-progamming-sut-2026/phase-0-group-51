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
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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


import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;

import views.graphical.gameplay.hud.GameHud;
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

    private OverlayMode overlayMode = OverlayMode.NONE;

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


    private final GamingController gamingController = new GamingController();

    private float gameTickAccumulator;


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

    plantSlotsBar.remove();
    plantSlotsBar.setOnRemoveRequested(null);

    uiStage.clear();
    plantSlotsBar.setMode(
            PlantSlotsBar.Mode.GAMEPLAY
    );

    gameHud = new GameHud(
            game,
            plantSlotsBar,
            this::showPauseMenu
    );

    uiStage.addActor(gameHud);
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

    worldStage.addActor(
            boardView
    );


    gameTickAccumulator = 0f;

    introState =
            IntroState.PAN_BACK_TO_MAIN;

    stateTime = 0f;
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

    @Override
    public void render(float delta) {
        handlePauseShortcut();
        updateCutscene(delta);
        updateGameplayTicks(delta);

        uiStage.act(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();

        float currentX = 0;
        game.getBatch().draw(bgLeft, currentX, 0, bgLeft.getRegionWidth(), worldHeight);
        currentX += bgLeft.getRegionWidth();

        game.getBatch().draw(bgMid, currentX, 0, bgMid.getRegionWidth(), worldHeight);
        currentX += bgMid.getRegionWidth();

        game.getBatch().draw(bgRight, currentX, 0, bgRight.getRegionWidth(), worldHeight);

        game.getBatch().end();
        if (introState == IntroState.PLAYING
                && overlayMode == OverlayMode.NONE) {
            worldStage.act(delta);
        }
        worldStage.draw();

        drawDebugGrid();

        uiStage.draw();
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
                || overlayMode != OverlayMode.NONE) {
            return;
        }

        PlantData selectedPlant = plantSlotsBar.getSelectedPlant();
        if (selectedPlant == null) {
            return;
        }
        int x = tile.getColumn() + 1;
        int y = tile.getLane() + 1;
        Result result = gamingController.plantPlant(selectedPlant.name(), x, y);
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
        uiStage.dispose();
        worldStage.dispose();
        shapeRenderer.dispose();
        modalDimTexture.dispose();
    }
}
