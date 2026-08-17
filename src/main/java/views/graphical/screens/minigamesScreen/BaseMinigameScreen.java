package views.graphical.screens.minigamesScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import graphics.PvzGame;
import models.App;
import models.games.Game;
import views.graphical.gameplay.hud.GameHud;
import views.graphical.screens.BaseScreen;
import views.graphical.ui.PauseMenuPopup;
import views.graphical.ui.PlantSlotsBar;

import java.util.List;

public abstract class BaseMinigameScreen extends BaseScreen {
    protected final GameHud gameHud;
    private final PlantSlotsBar emptyPlantSlotsBar;
    protected static final float VIEW_WIDTH = 1066f;
    protected static final float WORLD_HEIGHT = 600f;

    protected final OrthographicCamera camera;
    protected final Viewport worldViewport;
    protected final Stage worldStage;
    protected final Stage uiStage;
    protected final InputMultiplexer inputMultiplexer;
    protected final TextureRegion bgLeft;
    protected final TextureRegion bgMid;
    protected final TextureRegion bgRight;

    protected float cameraMainX;
    protected float cameraPreviewX;
    protected float cameraGameplayX;

    protected float stateTime = 0f;
    protected float waitDuration = 1.0f;
    protected float panDuration = 1.5f;
    private static final float GAME_END_DELAY = 1.3f;
    private float gameEndTimer = 0f;
    private boolean gameEndHandled = false;
    protected enum OverlayMode {
        NONE,
        START_OBJECTIVES,
        PAUSE,
        RESULT
    }

    protected enum IntroState {
        WAIT_AT_MAIN,
        PAN_TO_PREVIEW,
        WAIT_AT_PREVIEW,
        PAN_BACK_TO_GAMEPLAY,
        PLAYING
    }
    protected IntroState introState = IntroState.WAIT_AT_MAIN;



    protected OverlayMode overlayMode = OverlayMode.NONE;
    protected Table modalOverlay;
    private final Texture modalDimTexture;

    protected float gameTickAccumulator = 0f;

    protected BaseMinigameScreen(PvzGame game, String backgroundLeftId, String backgroundMiddleId, String backgroundRightId) {
        super(game);

        bgLeft = game.getTextureBank().region(backgroundLeftId);
        bgMid = game.getTextureBank().region(backgroundMiddleId);
        bgRight = game.getTextureBank().region(backgroundRightId);
        camera = new OrthographicCamera();

        worldViewport = new ExtendViewport(VIEW_WIDTH, WORLD_HEIGHT, camera);
        worldStage = new Stage(worldViewport, game.getBatch());
        uiStage = stage;
        inputMultiplexer = new InputMultiplexer(uiStage, worldStage);
        emptyPlantSlotsBar = new PlantSlotsBar(game);
        emptyPlantSlotsBar.setMode(PlantSlotsBar.Mode.GAMEPLAY);
        emptyPlantSlotsBar.loadPlants(List.of());
        gameHud = new GameHud(game, emptyPlantSlotsBar, this::showPauseMenu);
        uiStage.addActor(gameHud);
        float totalWorldWidth = bgLeft.getRegionWidth() + bgMid.getRegionWidth() + bgRight.getRegionWidth();
        cameraMainX = VIEW_WIDTH / 2f;
        cameraPreviewX = totalWorldWidth - VIEW_WIDTH / 2f;
        cameraGameplayX = bgLeft.getRegionWidth() + bgMid.getRegionWidth() / 2f;
        camera.position.set(cameraMainX, WORLD_HEIGHT / 2f, 0f);
        camera.update();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        modalDimTexture = new Texture(pixmap);
        pixmap.dispose();
    }
    private void updateGameEnd(float delta) {
        if (gameEndHandled) {
            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || currentGame.getGameState() == null) {
            return;
        }


        if (!currentGame.getGameState().isFinished()) {
            gameEndTimer = 0f;
            return;
        }

        gameEndTimer += delta;
        if (gameEndTimer < GAME_END_DELAY) {
            return;
        }
        gameEndHandled = true;

        overlayMode = OverlayMode.RESULT;
        onGameFinished(currentGame.getGameState().isWon());
    }
    @Override
    public InputMultiplexer getInputProcessor() {
        return inputMultiplexer;
    }

    protected void onGameFinished(boolean won) {

    }

    @Override
    public void show() {
        game.hideHud();
            gameHud.showGameHud();

        Actor startPopup = createStartPopup(this::continueAfterObjectives);
        if (startPopup != null) {
            overlayMode = OverlayMode.START_OBJECTIVES;
            showModal(startPopup);
            animateStartPopup(startPopup);

        } else {
            overlayMode = OverlayMode.NONE;
        }
    }

    private void updateIntro(float delta) {
        if (overlayMode != OverlayMode.NONE) {
            return;
        }

        if (introState == IntroState.PLAYING) {
            return;
        }

        stateTime += delta;
        switch (introState) {
            case WAIT_AT_MAIN -> {
                if (stateTime >= waitDuration) {
                    introState = IntroState.PAN_TO_PREVIEW;
                    stateTime = 0f;
                }
            }

            case PAN_TO_PREVIEW -> {
                float progress = Math.min(1f, stateTime / panDuration);
                camera.position.x = Interpolation.smooth.apply(cameraMainX, cameraPreviewX, progress);
                if (progress >= 1f) {
                    camera.position.x = cameraPreviewX;
                    introState = IntroState.WAIT_AT_PREVIEW;
                    stateTime = 0f;
                }
            }


            case WAIT_AT_PREVIEW -> {

                if (stateTime >= waitDuration) {
                    afterPreview();
                }
            }


            case PAN_BACK_TO_GAMEPLAY -> {
                float progress = Math.min(1f, stateTime / panDuration);
                camera.position.x = Interpolation.smooth.apply(cameraPreviewX, cameraGameplayX, progress);

                if (progress >= 1f) {
                    camera.position.x = cameraGameplayX;
                    introState = IntroState.PLAYING;
                    stateTime = 0f;
                    onGameplayStarted();
                }
            }


            case PLAYING -> {

            }
        }


        camera.update();
    }


    protected void afterPreview() {
        beginGameplayReturn();
    }


    protected final void beginGameplayReturn() {
        introState = IntroState.PAN_BACK_TO_GAMEPLAY;
        stateTime = 0f;
    }

    private void updateGameplayTicks(float delta) {
        if (introState != IntroState.PLAYING) {
            return;
        }

        if (overlayMode != OverlayMode.NONE) {

            return;
        }
        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || currentGame.getGameState() == null || currentGame.getGameState().isFinished()) {
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
            onGameTick();
            gameTickAccumulator -= tickDuration;
        }
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

    protected abstract Actor createStartPopup(Runnable onContinue);

    private void animateStartPopup(Actor popup) {
        modalOverlay.validate();
        float targetX = popup.getX();
        float targetY = popup.getY();
        popup.setPosition(targetX, -popup.getHeight());
        popup.addAction(
                Actions.moveTo(
                        targetX,
                        targetY,
                        0.55f,
                        Interpolation.pow3Out
                )
        );
    }

    private void handlePauseShortcut() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return;
        }


        if (overlayMode == OverlayMode.PAUSE) {
            resumeGame();
            return;
        }


        if (introState == IntroState.PLAYING && overlayMode == OverlayMode.NONE) {
            showPauseMenu();
        }
    }


    protected final void showPauseMenu() {
        if (introState != IntroState.PLAYING) {
            return;
        }

        if (overlayMode != OverlayMode.NONE) {
            return;
        }


        overlayMode = OverlayMode.PAUSE;
        gameTickAccumulator = 0f;
        PauseMenuPopup popup = new PauseMenuPopup(game, this::exitMinigame, this::restartMinigame, this::resumeGame);
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


    protected abstract void restartMinigame();

    protected abstract void exitMinigame();

    protected void onGameTick() {

    }
    protected final void showModal(Actor popup) {
        removeModal();
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);

        TextureRegionDrawable background = new TextureRegionDrawable(new TextureRegion(modalDimTexture));

        overlay.setBackground(
                background.tint(
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
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {

                        return true;
                    }
                }
        );
        overlay.add(popup).center();
        modalOverlay = overlay;
        uiStage.addActor(modalOverlay);
        modalOverlay.toFront();
    }


    protected final void removeModal() {
        if (modalOverlay == null) {
            return;
        }
        modalOverlay.remove();
        modalOverlay = null;
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(delta, 0.25f);
        handlePauseShortcut();
        updateIntro(safeDelta);
        updateGameplayTicks(safeDelta);
        updateGameEnd(safeDelta);
        uiStage.act(safeDelta);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(
                GL20.GL_COLOR_BUFFER_BIT
        );


        worldViewport.apply();

        game.getBatch().setProjectionMatrix(
                camera.combined
        );
        game.getBatch().begin();
        float currentX = 0f;

        game.getBatch().draw(
                bgLeft,
                currentX,
                0f,
                bgLeft.getRegionWidth(),
                WORLD_HEIGHT
        );

        currentX += bgLeft.getRegionWidth();
        game.getBatch().draw(bgMid, currentX, 0f, bgMid.getRegionWidth(), WORLD_HEIGHT);
        currentX += bgMid.getRegionWidth();
        game.getBatch().draw(bgRight, currentX, 0f, bgRight.getRegionWidth(), WORLD_HEIGHT);
        game.getBatch().end();
        if (overlayMode == OverlayMode.NONE) {
            worldStage.act(safeDelta);
        }
        renderWorldUnderlay();
        worldViewport.apply();
        worldStage.draw();
        renderWorldOverlay();
        uiStage.getViewport().apply();
        uiStage.draw();
    }

    protected void renderWorldOverlay() {

    }

    protected void onGameplayStarted() {

    }
    protected void renderWorldUnderlay() {

    }

    protected final boolean isPlaying() {
        return introState == IntroState.PLAYING;
    }


    protected final boolean isPaused() {
        return overlayMode == OverlayMode.PAUSE;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        worldViewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        worldStage.dispose();
        modalDimTexture.dispose();
        super.dispose();
    }
}
