package views.graphical.screens.minigamesScreen.meowPoint;

import Data.loader.PlantData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import controllers.GamingController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.User;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;
import models.games.ScoringGame;
import models.meowPoint.ScoreBreakdown;
import models.meowPoint.ScoreTracker;
import models.meowPoint.ScoringRules;
import models.meowPoint.ScoringSunSpawner;
import models.sun.Sun;
import views.graphical.gameplay.actors.PlantActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;
import views.graphical.screens.MainMenuScreen;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.ui.CenterGameNotice;
import views.graphical.ui.PlantSelectionMenuTable;
import views.graphical.ui.PlantSlotsBar;
import views.graphical.ui.StartGameMenuPopup;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class MeowPointScreen extends BaseMinigameScreen {
    private static final String BG_LEFT = "IMAGE_BACKGROUNDS_CARNIVAL_TEXTURE_LEFT";
    private static final String BG_MID = "IMAGE_BACKGROUNDS_CARNIVAL_TEXTURE";
    private static final String BG_RIGHT = "IMAGE_BACKGROUNDS_CARNIVAL_TEXTURE_RIGHT";

    private final GamingController gamingController = new GamingController();
    private final ScoringGame scoringGame;
    private final BoardTransform boardTransform;

    private BoardView boardView;
    private ZombieAnimationSystem zombieAnimationSystem;
    private PlantViewManager plantViewManager;
    private ProjectileViewManager projectileViewManager;
    private SunViewManager sunViewManager;

    private PlantActor placementPreview;
    private Image rowHighlight;
    private Image columnHighlight;

    private PlantSelectionMenuTable plantSelectionMenu;
    private PlantSlotsBar selectionSlotsBar;

    private boolean gameplayViewsBuilt;
    private float renderDelta;

    private int lastQuickKillBonus = 0;
    private int lastSimultaneousKillBonus = 0;
    private int lastFastWaveBonus = 0;


    private int lastZombieValuePoints = 0;
    private int lastGardenPreservationBonus = 0;



    private CenterGameNotice centerNotice;
    private final Deque<String> noticeQueue = new ArrayDeque<>();
    public MeowPointScreen(PvzGame game) {
        super(game, BG_LEFT, BG_MID, BG_RIGHT);

        scoringGame = new ScoringGame();
        App.getInstance().setCurrentGame(scoringGame);

        BoardArea boardArea = new BoardArea(533f, 62f, 737f, 380f);
        boardTransform = new BoardTransform(boardArea);
    }

    @Override
    public void show() {
        super.show();
        gameHud.hideGameHud();

    }

    @Override
    protected Actor createStartPopup(Runnable onContinue) {
        return new StartGameMenuPopup(
                game,
                onContinue,
                "MeowPoint Challenge",
                1,
                "Play like a normal level, but maximize your MeowPoint score!",
                new String[]{
                        "Zombie Value: Each kill gives 50 points + that zombie's wave cost.",
                        "Quick Kill: Kill a zombie within 30 seconds for up to 300 bonus points.",
                        "Simultaneous Kill: Kill multiple zombies in the same tick for combo points.",
                        "Fast Wave: Clear a wave within 60 seconds for up to 500 bonus points.",
                        "Garden Preservation: Win with plants, mowers and sun remaining for extra points."
                }
        );
    }

    @Override
    protected void afterPreview() {
        if (plantSelectionMenu != null || gameplayViewsBuilt) {
            return;
        }

        selectionSlotsBar = new PlantSlotsBar(game);
        selectionSlotsBar.setMode(PlantSlotsBar.Mode.SELECTION);

        plantSelectionMenu = new PlantSelectionMenuTable(
                game,
                selectionSlotsBar,
                this::finishPlantSelection
        );
        uiStage.addActor(plantSelectionMenu);
        plantSelectionMenu.toFront();
    }

    private void finishPlantSelection() {
        if (scoringGame.getGameState() == null) {
            game.notifyError("The MeowPoint level could not be started.");
            return;
        }

        if (plantSelectionMenu != null) {
            plantSelectionMenu.remove();
            plantSelectionMenu = null;
        }
        selectionSlotsBar = null;

        PlantSlotsBar gameplayBar = gameHud.getPlantSlotsBar();
        gameplayBar.loadPlants(scoringGame.getSelectedPlantsForThisGame());
        gameplayBar.setMode(PlantSlotsBar.Mode.GAMEPLAY);
        gameplayBar.setOnPlantSelected(this::handlePlantSelectionChanged);

        buildGameplayViews();
        gameTickAccumulator = 0f;
        beginGameplayReturn();
    }

    private void buildGameplayViews() {
        if (gameplayViewsBuilt) {
            return;
        }

        GameState state = scoringGame.getGameState();
        if (state == null) {
            throw new IllegalStateException("Scoring game state was not created.");
        }

        Board board = state.getBoard();

        createPlacementHighlights();
        worldStage.addActor(rowHighlight);
        worldStage.addActor(columnHighlight);

        boardView = new BoardView(board, boardTransform);
        boardView.setOnTileClicked(this::handleTileClick);
        boardView.setOnTileHovered(this::handleTileHover);
        worldStage.addActor(boardView);

        plantViewManager = new PlantViewManager(game, boardTransform);
        projectileViewManager = new ProjectileViewManager(game, boardTransform);
        sunViewManager = new SunViewManager(game, boardTransform);
        sunViewManager.setOnSunClicked(this::handleSunClicked);

        worldStage.addActor(plantViewManager);
        worldStage.addActor(projectileViewManager);
        worldStage.addActor(sunViewManager);

        zombieAnimationSystem = new ZombieAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                boardTransform,
                ChapterTheme.ANCIENT_EGYPT
        );

        placementPreview = new PlantActor(game);
        placementPreview.setPreviewMode(true);
        worldStage.addActor(placementPreview);


        plantViewManager.sync(board);
        gameplayViewsBuilt = true;
    }

    @Override
    protected void onGameplayStarted() {
        gameTickAccumulator = 0f;
        gameHud.showGameHud();
        lastZombieValuePoints = 0;
        lastQuickKillBonus = 0;
        lastSimultaneousKillBonus = 0;
        lastFastWaveBonus = 0;
        lastGardenPreservationBonus = 0;
        noticeQueue.clear();
        if (centerNotice != null) {
            centerNotice.remove();
            centerNotice = null;
        }

        if (plantViewManager != null && scoringGame.getGameState() != null) {
            plantViewManager.sync(scoringGame.getGameState().getBoard());
        }
    }
    private void checkScoreNotices() {
        if (scoringGame == null || scoringGame.getScoreTracker() == null) return;
        ScoreTracker tracker = scoringGame.getScoreTracker();


        int zombieValueDiff = tracker.getZombieValuePoints() - lastZombieValuePoints;
        if (zombieValueDiff > 0) {
            queueNotice("ZOMBIE KILLED!\n(Base Value & Wave Cost)\n+" + zombieValueDiff);
            lastZombieValuePoints = tracker.getZombieValuePoints();
        }


        int quickKillDiff = tracker.getQuickKillBonus() - lastQuickKillBonus;
        if (quickKillDiff > 0) {
            queueNotice("QUICK KILL!\n(Killed under 30s)\n+" + quickKillDiff);
            lastQuickKillBonus = tracker.getQuickKillBonus();
        }


        int comboDiff = tracker.getSimultaneousKillBonus() - lastSimultaneousKillBonus;
        if (comboDiff > 0) {
            queueNotice("SIMULTANEOUS KILL!\n(Multiple in same tick)\n+" + comboDiff);
            lastSimultaneousKillBonus = tracker.getSimultaneousKillBonus();
        }


        int fastWaveDiff = tracker.getFastWaveBonus() - lastFastWaveBonus;
        if (fastWaveDiff > 0) {
            queueNotice("FAST WAVE!\n(Cleared under 60s)\n+" + fastWaveDiff);
            lastFastWaveBonus = tracker.getFastWaveBonus();
        }
    }

    private void queueNotice(String message) {
        noticeQueue.addLast(message);
        showNextNotice();
    }

    private void showNextNotice() {
        if (centerNotice != null || noticeQueue.isEmpty()) {
            return;
        }

        centerNotice = new views.graphical.ui.CenterGameNotice(game.getSkin(), false);
        uiStage.addActor(centerNotice);
        centerNotice.toFront();

        centerNotice.showSequence(
                java.util.List.of(noticeQueue.removeFirst()),
                1.5f,
                () -> {
                    centerNotice = null;
                    showNextNotice();
                }
        );
    }




    @Override
    public void render(float delta) {
        renderDelta = Math.min(delta, 0.25f);
        if (isPlaying() && !isPaused()) {
            checkScoreNotices();
        }
        super.render(delta);
    }

    @Override
    protected void renderWorldUnderlay() {
        if (!gameplayViewsBuilt || !isPlaying() || isPaused()) {
            return;
        }

        GameState state = scoringGame.getGameState();
        if (state == null) {
            return;
        }

        Board board = state.getBoard();
        float partialTick = getRenderTickAlpha();

        plantViewManager.sync(board);
        projectileViewManager.sync(board.getProjectiles(), partialTick);
        sunViewManager.sync(board.getActiveSuns(), partialTick);
        zombieAnimationSystem.update(
                renderDelta,
                partialTick,
                state.getTickCounter(),
                state.getZombiesInTheGame()
        );
    }

    private float getRenderTickAlpha() {
        GameState state = scoringGame.getGameState();
        if (state == null) {
            return 0f;
        }

        int ticksPerSecond = Math.max(1, state.getTicksPerSecond());
        float tickDuration = 1f / ticksPerSecond;
        return Math.max(0f, Math.min(1f, gameTickAccumulator / tickDuration));
    }

    private void handlePlantSelectionChanged(PlantData plant) {
        if (placementPreview == null) {
            return;
        }

        if (plant == null) {
            placementPreview.clearPlant();
            hidePlacementHighlights();
            return;
        }

        placementPreview.setPreviewMode(true);
        placementPreview.setPlant(plant);
    }

    private void handleTileClick(Tile tile) {
        if (!isPlaying() || isPaused() || tile == null) {
            return;
        }

        PlantData selectedPlant = gameHud.getPlantSlotsBar().getSelectedPlant();
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

        if (plantViewManager != null) {
            plantViewManager.sync(scoringGame.getGameState().getBoard());
        }
        gameHud.getPlantSlotsBar().clearPlantSelection();
    }

    private void handleSunClicked(Sun sun) {
        if (!isPlaying() || isPaused() || sun == null) {
            return;
        }

        GameState state = scoringGame.getGameState();
        if (state == null) {
            return;
        }

        boolean collected = state.getBoard().collectSun(sun, state);
        if (!collected) {
            game.notifyError("Sun has expired or was already collected.");
        }
    }

    private void createPlacementHighlights() {
        Drawable highlightDrawable = game.getSkin().newDrawable(
                "white_pixel",
                new Color(1f, 1f, 1f, 0.70f)
        );

        rowHighlight = new Image(highlightDrawable);
        columnHighlight = new Image(highlightDrawable);
        rowHighlight.setTouchable(Touchable.disabled);
        columnHighlight.setTouchable(Touchable.disabled);
        rowHighlight.setVisible(false);
        columnHighlight.setVisible(false);
    }

    private void handleTileHover(Tile tile) {
        if (!isPlaying()
                || isPaused()
                || tile == null
                || gameHud.getPlantSlotsBar().getSelectedPlant() == null) {
            hidePlacementHighlights();
            return;
        }

        BoardArea area = boardTransform.getArea();
        rowHighlight.setBounds(
                area.x(),
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
    protected void onGameFinished(boolean won) {
        GameState state = scoringGame.getGameState();
        if (state == null) {
            return;
        }

        var breakdown = scoringGame.getScoreTracker().finish(state, won);


        if (breakdown.gardenPreservationBonus() > 0) {
            queueNotice("GARDEN PRESERVATION!\n(Plants, Mowers & Sun remaining)\n+" + breakdown.gardenPreservationBonus());
        }

        gameHud.hideGameHud();
        hidePlacementHighlights();

        if (placementPreview != null) {
            placementPreview.clearPlant();
        }

        if (won) {

            views.graphical.ui.GameWinPopup winPopup = new views.graphical.ui.GameWinPopup(
                    game,
                    "MEOWPOINT COMPLETE!",
                    breakdown.format(),
                    "EXIT",
                    this::exitMinigame,
                    "RETRY",
                    this::restartMinigame
            );
            uiStage.addActor(winPopup);
            winPopup.toFront();
        } else {
            views.graphical.ui.GameOverPopup losePopup = new views.graphical.ui.GameOverPopup(
                    game,
                    "THE ZOMBIES\nREACHED YOUR HOUSE!",
                    "EXIT",
                    this::exitMinigame,
                    "RETRY",
                    this::restartMinigame
            );
            uiStage.addActor(losePopup);
            losePopup.toFront();
        }
    }

    @Override
    protected void restartMinigame() {
        removeModal();
        Gdx.app.postRunnable(() -> game.showScreen(new MeowPointScreen(game)));
    }

    @Override
    protected void exitMinigame() {
        removeModal();
        gameHud.hideGameHud();
        game.hideHud();
        App.getInstance().setCurrentGame(null);
        Gdx.app.postRunnable(() -> game.showScreen(new MainMenuScreen(game)));
    }

    @Override
    public void hide() {
        gameHud.hideGameHud();
        game.hideHud();
        super.hide();
    }

    @Override
    public void dispose() {
        if (zombieAnimationSystem != null) {
            zombieAnimationSystem.clear();
        }
        super.dispose();
    }
}
