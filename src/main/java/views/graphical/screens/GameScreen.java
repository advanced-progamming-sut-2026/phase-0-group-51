package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.Level;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.hud.GameHud;
import views.graphical.ui.PlantSelectionMenuTable;
import views.graphical.ui.PlantSlotsBar;

public class GameScreen extends BaseScreen {

    private final ChapterTheme theme;
    private final Level currentLevel;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Stage uiStage;
    private final Stage worldStage;
    private final PlantSlotsBar plantSlotsBar;

    private GameHud gameHud;

    private TextureRegion bgLeft;
    private TextureRegion bgMid;
    private TextureRegion bgRight;

    private final float viewWidth = 1066f;
    private final float worldHeight = 600f;
    private enum IntroState {
        WAIT_AT_MAIN, PAN_TO_ZOMBIES, WAIT_AT_ZOMBIES, PAN_TO_SELECTION, SHOW_PLANT_SELECT, WAITING_FOR_SELECTION, PAN_BACK_TO_MAIN, PLAYING
    }

    private IntroState introState = IntroState.WAIT_AT_MAIN;
    private float stateTime = 0f;

    private float cameraMainX;
    private float cameraRightX;
    private float cameraSelectionX;
    private float cameraGameplayX;

    private final ShapeRenderer shapeRenderer;

    private BoardView boardView;
    private BoardArea boardArea;
    private final BoardTransform boardTransform;

    private boolean showGrid = true;

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

        plantSlotsBar = new PlantSlotsBar(game);
        plantSlotsBar.setMode(PlantSlotsBar.Mode.SELECTION);

        cameraMainX = viewWidth / 2f;
        cameraGameplayX = bgLeft.getRegionWidth() + bgMid.getRegionWidth() / 2f;
        cameraRightX = totalWorldWidth - (viewWidth / 2f);
        cameraSelectionX = cameraRightX - (viewWidth / 2.5f);

        camera.position.set(cameraMainX, worldHeight / 2f, 0);
        camera.update();

        shapeRenderer = new ShapeRenderer();

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
    public void show() {
        game.hideHud();
        Gdx.input.setInputProcessor(uiStage);
    }

    private void updateCutscene(float delta) {
        if (introState == IntroState.PLAYING || introState == IntroState.WAITING_FOR_SELECTION) return;

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
                PlantSelectionMenuTable plantSelection =
                        new PlantSelectionMenuTable(
                                game,
                                plantSlotsBar,
                                this::startGameAfterSelection
                        );
                uiStage.addActor(plantSelection);
                introState = IntroState.WAITING_FOR_SELECTION;
                break;

            case PAN_BACK_TO_MAIN:
                float progressLeft = Math.min(1f, stateTime / panDuration);
                camera.position.x = Interpolation.smooth.apply(cameraSelectionX, cameraGameplayX, progressLeft);
                if (progressLeft >= 1f) {
                    camera.position.x = cameraGameplayX;
                    introState = IntroState.PLAYING;
                }
                break;
        }

        camera.update();
    }

    public void startGameAfterSelection() {
        plantSlotsBar.remove();
        plantSlotsBar.setOnRemoveRequested(null);

        uiStage.clear();

        plantSlotsBar.setMode(PlantSlotsBar.Mode.GAMEPLAY);

        gameHud = new GameHud(game, plantSlotsBar);
        uiStage.addActor(gameHud);
        Game currentGame = App.getInstance().getCurrentGame();

        if (currentGame == null || currentGame.getGameState() == null) {
            throw new IllegalStateException(
                    "Game state was not created."
            );
        }

        Board board = currentGame.getGameState().getBoard();
        boardView = new BoardView(board, boardTransform);
        worldStage.addActor(boardView);

        introState = IntroState.PAN_BACK_TO_MAIN;
        stateTime = 0f;
    }

    @Override
    public void render(float delta) {
        updateCutscene(delta);
        uiStage.act(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();

        float currentX = 0;
        game.getBatch().draw(bgLeft, currentX, 0, bgLeft.getRegionWidth(), worldHeight);
        currentX += bgLeft.getRegionWidth();

        game.getBatch().draw(bgMid, currentX, 0, bgMid.getRegionWidth(), worldHeight);
        currentX += bgMid.getRegionWidth();

        game.getBatch().draw(bgRight, currentX, 0, bgRight.getRegionWidth(), worldHeight);

        game.getBatch().end();
        worldStage.act(delta);
        worldStage.draw();

        drawDebugGrid();

        uiStage.draw();
    }

    private void drawDebugGrid() {
        if (!showGrid) {
            return;
        }

        BoardArea area =
                boardTransform.getArea();

        float tileWidth =
                boardTransform.tileWidth();

        float tileHeight =
                boardTransform.tileHeight();

        shapeRenderer.setProjectionMatrix(
                camera.combined
        );

        shapeRenderer.begin(
                ShapeRenderer.ShapeType.Line
        );

        shapeRenderer.setColor(Color.RED);

        for (int column = 0;
             column <= BoardTransform.COLUMNS;
             column++) {

            float x =
                    area.x()
                            + column * tileWidth;

            shapeRenderer.line(
                    x,
                    area.y(),
                    x,
                    area.y() + area.height()
            );
        }

        for (int row = 0;
             row <= BoardTransform.ROWS;
             row++) {

            float y =
                    area.y()
                            + row * tileHeight;

            shapeRenderer.line(
                    area.x(),
                    y,
                    area.x() + area.width(),
                    y
            );
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
        uiStage.dispose();
    }
}
