package views.graphical.screens.minigamesScreen.iZombie;

import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import controllers.miniGamesController.IZombieController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.minigames.MinigameType;
import models.minigames.iZombie.IZombie;
import models.minigames.vaseBreaker.Brain;
import models.sun.Sun;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
import views.graphical.gameplay.manager.WorldEffectManager;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.GameOverPopup;
import views.graphical.ui.GameWinPopup;
import views.graphical.ui.StartGameMenuPopup;

import java.util.*;

public class IZombieScreen extends BaseMinigameScreen {
    private static final String BG_LEFT = "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_LEFT";
    private static final String BG_MID = "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE";
    private static final String BG_RIGHT = "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_RIGHT";
    private final List<BrainView> brainViews = new ArrayList<>();
    private final int stageNumber;
    private final IZombieController controller;
    private final IZombie iZombie;
    private final BoardArea boardArea;
    private final BoardTransform boardTransform;
    private BoardView boardView;
    private PlantViewManager plantViewManager;
    private ProjectileViewManager projectileViewManager;
    private WorldEffectManager worldEffectManager;
    private final ShapeRenderer shapeRenderer;
    private IZombieBar zombieBar;
    private String selectedZombieAlias;

    private final Map<Zombie, Image> sunHats = new IdentityHashMap<>();
    private Image rowHighlight;
    private Image columnHighlight;


    private PamAnimationActor zombiePlacementPreview;
    private final Vector2 placementCursorPosition = new Vector2();

    private static final float ZOMBIE_PREVIEW_ALPHA = 0.58f;

    private final ZombieAnimationSystem zombieAnimationSystem;
    private static final float BRAIN_CROSS_REMOVE_DELAY = 2f;
    private final Map<Zombie, Float> brainCrossTimers =
            new IdentityHashMap<>();
    private float renderDelta = 0f;

    public IZombieScreen(PvzGame game, int stageNumber) {
        super(game, BG_LEFT, BG_MID, BG_RIGHT);
        this.stageNumber = stageNumber;
        controller = new IZombieController(game.getNetworkManager());
        Result result = controller.startStage(stageNumber);
        if (!result.success()) {
            throw new IllegalStateException(result.message());
        }

        if (!(App.getInstance().getCurrentGame() instanceof IZombie currentIZombie)) {
            throw new IllegalStateException("I, Zombie game was not created.");
        }

        iZombie = currentIZombie;
        boardArea = new BoardArea(533f, 62f, 737f, 380f);
        boardTransform = new BoardTransform(boardArea);
        zombieAnimationSystem = new ZombieAnimationSystem(game.getPamPlayer(), worldStage, boardTransform, ChapterTheme.MINIGAME);
        shapeRenderer = new ShapeRenderer();
        buildBoard();
        buildZombieBar();
    }

    private SunViewManager sunViewManager;
    private void buildZombieBar() {
        zombieBar = new IZombieBar(
                game,
                iZombie,
                this::handleZombieSelectionChanged
        );
        float gapFromBrain = 12f;
        float x = boardArea.x() - zombieBar.getWidth() - gapFromBrain;
        float y = boardArea.y() + (boardArea.height() - zombieBar.getHeight()) / 2f;
        zombieBar.setPosition(x, y);
        zombieBar.setVisible(false);
        worldStage.addActor(zombieBar);
    }

    private void buildBoard() {
        Board board = iZombie.getGameState().getBoard();
        boardView = new BoardView(board, boardTransform);
        boardView.setOnTileClicked(this::handleTileClicked);
        boardView.setOnTileHovered(this::handleTileHover);


        createPlacementHighlights();
        worldStage.addActor(rowHighlight);
        worldStage.addActor(columnHighlight);
        worldStage.addActor(boardView);
        buildBrains();
        plantViewManager = new PlantViewManager(game, boardTransform);
        worldStage.addActor(plantViewManager);
        projectileViewManager = new ProjectileViewManager(game, boardTransform);
        worldStage.addActor(projectileViewManager);
        worldEffectManager = new WorldEffectManager(game, boardTransform);
        worldStage.addActor(worldEffectManager);
        sunViewManager = new SunViewManager(game, boardTransform);
        sunViewManager.setOnSunClicked(this::handleSunClicked);
        worldStage.addActor(sunViewManager);
        syncViews();
    }

    private void handleSunClicked(Sun sun) {
        boolean collected =
                iZombie
                        .getGameState()
                        .getBoard()
                        .collectSun(
                                sun,
                                iZombie.getGameState()
                        );


        if (!collected) {
            game.notifyError(
                    "Sun has expired or was already collected."
            );

            return;
        }


        if (zombieBar != null) {
            zombieBar.refresh();
        }
    }
    @Override
    public void render(float delta) {
        renderDelta = Math.min(delta, 0.25f);

        if (isPlaying() && !isPaused()) {
            updateBrainCrossRemoval(renderDelta);
        }

        super.render(delta);
    }

    private void handleZombieSelectionChanged(String alias) {
        selectedZombieAlias = alias;

        if (alias == null || alias.isBlank()) {
            clearZombiePlacementPreview();
            hidePlacementHighlights();
            return;
        }

        showZombiePlacementPreview(alias);
    }

    private void showZombiePlacementPreview(String alias) {
        clearZombiePlacementPreview();

        String pamPath = ZombieAnimationSystem.resolvePamPath(
                ChapterTheme.MINIGAME,
                alias
        );

        String idleClip = ZombieRegistry.getIdleClip(alias);

        if (pamPath == null || pamPath.isBlank()
                || idleClip == null || idleClip.isBlank()) {
            return;
        }

        try {
            game.getPamPlayer().loadSync(pamPath);

            zombiePlacementPreview = game.createPamActor(
                    pamPath,
                    idleClip,
                    0f,
                    0f,
                    true,
                    ZombieAnimationSystem.resolveVisibleParts(
                            game.getPamPlayer(),
                            pamPath,
                            alias
                    )
            );

            zombiePlacementPreview.setTouchable(
                    Touchable.disabled
            );

            zombiePlacementPreview.setScale(
                    ZombieAnimationSystem.DEFAULT_SCALE,
                    ZombieAnimationSystem.DEFAULT_SCALE
            );


            zombiePlacementPreview.setColor(
                    1f,
                    1f,
                    1f,
                    ZOMBIE_PREVIEW_ALPHA
            );

            worldStage.addActor(
                    zombiePlacementPreview
            );

            updateZombiePlacementPreviewPosition();
            zombiePlacementPreview.toFront();

        } catch (RuntimeException e) {
            clearZombiePlacementPreview();

            Gdx.app.error(
                    "IZombiePlacement",
                    "Failed to create placement preview for " + alias,
                    e
            );
        }
    }

    private void updateZombiePlacementPreviewPosition() {
        if (zombiePlacementPreview == null
                || selectedZombieAlias == null
                || !isPlaying()
                || isPaused()) {
            return;
        }

        placementCursorPosition.set(
                Gdx.input.getX(),
                Gdx.input.getY()
        );

        worldStage.screenToStageCoordinates(
                placementCursorPosition
        );

        zombiePlacementPreview.setPosition(
                placementCursorPosition.x,
                placementCursorPosition.y
        );
    }

    private void clearZombiePlacementPreview() {
        if (zombiePlacementPreview == null) {
            return;
        }

        zombiePlacementPreview.remove();
        zombiePlacementPreview = null;
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
        boolean hasZombieSelection =
                selectedZombieAlias != null;

        if (tile == null
                || !hasZombieSelection
                || !isPlaying()
                || isPaused()) {
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

    private void handleTileClicked(Tile tile) {
        if (!isPlaying() || isPaused()) {
            return;
        }
        if (selectedZombieAlias == null) {
            return;
        }
        int x = tile.getColumn() + 1;
        int y = tile.getLane() + 1;

        Result result = controller.placeZombie(selectedZombieAlias, x, y);

        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }
        game.notifyInfo(result.message());
        zombieBar.clearSelection();
        zombieBar.refresh();
        hidePlacementHighlights();
    }

    private void syncViews() {
        Board board = iZombie.getGameState().getBoard();
        plantViewManager.sync(board);
        refreshBrains();
    }


    @Override
    protected void onGameTick() {
        syncViews();
        trackZombiesPastBrain();
        if (zombieBar != null) {
            zombieBar.refresh();
        }
    }

    private float getRenderTickAlpha() {
        int ticksPerSecond = Math.max(
                1,
                iZombie.getGameState().getTicksPerSecond()
        );

        float tickDuration = 1f / ticksPerSecond;

        return Math.max(
                0f,
                Math.min(
                        1f,
                        gameTickAccumulator / tickDuration
                )
        );
    }

    @Override
    protected void renderWorldUnderlay() {
        if (isPlaying() && !isPaused()) {
            float partialTick = getRenderTickAlpha();

            projectileViewManager.sync(
                    iZombie.getGameState().getBoard().getProjectiles(),
                    partialTick
            );

            iZombie.getGameState()
                    .consumeVisualEffects()
                    .forEach(worldEffectManager::play);

            worldEffectManager.toFront();

            Set<Zombie> renderableZombies = new HashSet<>(iZombie.getGameState().getZombiesInTheGame());

            zombieAnimationSystem.update(
                    renderDelta, partialTick, iZombie.getGameState().getTickCounter(), renderableZombies
            );


            for (Zombie zombie : renderableZombies) {
                if (zombie.getMaxHitpoints() == IZombie.SUN_PRODUCER_HP && !zombie.isDead()) {
                    Image hat = sunHats.get(zombie);
                    if (hat == null) {
                        com.badlogic.gdx.graphics.g2d.TextureRegion sunRegion = game.getTextureBank().region("IMAGE_UI_HUD_INGAME_SUN");
                        hat = new Image(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(sunRegion));
                        hat.setSize(40f, 40f);
                        hat.setTouchable(Touchable.disabled);
                        worldStage.addActor(hat);
                        sunHats.put(zombie, hat);
                    }

                    float hatX = boardArea.x() + zombie.getX() + 20f;
                    float hatY = boardTransform.tileY(zombie.getLane()) + 95f;

                    hat.setPosition(hatX, hatY);
                    hat.toFront();
                }
            }


            sunHats.entrySet().removeIf(entry -> {
                if (!renderableZombies.contains(entry.getKey()) || entry.getKey().isDead()) {
                    entry.getValue().remove();
                    return true;
                }
                return false;
            });


            sunViewManager.sync(iZombie.getGameState().getBoard().getActiveSuns(), partialTick);
            sunViewManager.toFront();

            updateZombiePlacementPreviewPosition();
            if (zombiePlacementPreview != null) {
                zombiePlacementPreview.toFront();
            }
        }

        float redLineX = boardTransform.tileX(IZombie.RED_LINE_COLUMN);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rectLine(redLineX, boardArea.y(), redLineX, boardArea.y() + boardArea.height(), 4f);
        shapeRenderer.end();
    }

    @Override
    protected Actor createStartPopup(Runnable onContinue) {

        return new StartGameMenuPopup(
                game,
                onContinue,
                "I, Zombie",
                stageNumber,
                "Play as the zombies and eat all five brains.",
                "Place zombies to the right of the red line.",
                "Spend sun to deploy zombies.",
                "Destroy the plants.",
                "Eat all five brains to win."
        );
    }

    private void trackZombiesPastBrain() {

        for (Zombie zombie :
                iZombie
                        .getGameState()
                        .getZombiesInTheGame()) {

            if (zombie.isDead()) {
                continue;
            }

            if (zombie.getX() > 0) {
                continue;
            }

            brainCrossTimers.putIfAbsent(
                    zombie,
                    0f
            );
        }
    }
    private void updateBrainCrossRemoval(
            float delta
    ) {

        Iterator<Map.Entry<Zombie, Float>> iterator =
                brainCrossTimers
                        .entrySet()
                        .iterator();


        while (iterator.hasNext()) {

            Map.Entry<Zombie, Float> entry =
                    iterator.next();

            Zombie zombie =
                    entry.getKey();


            if (!iZombie
                    .getGameState()
                    .getZombiesInTheGame()
                    .contains(zombie)) {

                iterator.remove();
                continue;
            }


            float elapsed =
                    entry.getValue()
                            + delta;


            if (elapsed >= BRAIN_CROSS_REMOVE_DELAY) {

                iZombie
                        .getGameState()
                        .removeZombie(zombie);

                iterator.remove();

            } else {

                entry.setValue(
                        elapsed
                );
            }
        }
    }
    @Override
    protected void onGameplayStarted() {
        gameTickAccumulator = 0f;
        syncViews();
        zombieBar.refresh();
        zombieBar.setVisible(true);
    }

    @Override
    protected void restartMinigame() {
        Gdx.app.postRunnable(() -> game.showScreen(new IZombieScreen(game, stageNumber)));
    }

    @Override
    protected void exitMinigame() {
        controller.exitMenu();
        Gdx.app.postRunnable(() -> game.showScreen(new minigames(game, MinigameType.IZOMBIE))
        );
    }


    @Override
    public void dispose() {
        clearZombiePlacementPreview();
        shapeRenderer.dispose();
        super.dispose();
    }
    private void buildBrains() {
        brainViews.clear();
        for (Brain brain : iZombie.getBrains()) {
            BrainView brainView = new BrainView(game, brain, boardTransform);
            brainViews.add(brainView);
            worldStage.addActor(brainView);
        }
    }
    private void refreshBrains() {
        for (BrainView brainView : brainViews) {
            brainView.refresh();
        }
    }


    private void showIZombieWin() {
        boolean hasNextStage = stageNumber < 3;

        Runnable nextAction =
                hasNextStage

                        ? () ->
                        Gdx.app.postRunnable(
                                () ->
                                        game.showScreen(
                                                new IZombieScreen(
                                                        game,
                                                        stageNumber + 1
                                                )
                                        )
                        )

                        : this::exitMinigame;


        GameWinPopup popup =
                new GameWinPopup(
                        game,
                        "I, ZOMBIE COMPLETE!",
                        "All five brains were eaten!",
                        "EXIT",
                        this::exitMinigame,
                        hasNextStage ? "NEXT STAGE" : "MINIGAMES",
                        nextAction
                );


        uiStage.addActor(popup);
        popup.toFront();
    }
    private void showIZombieGameOver() {

        GameOverPopup popup =
                new GameOverPopup(
                        game,
                        ChapterTheme.MINIGAME,
                        stageNumber
                );


        rewireGameOverButtons(
                popup
        );


        uiStage.addActor(
                popup
        );

        popup.toFront();
    }
    private void rewireGameOverButtons(
            Group root
    ) {

        for (Actor actor :
                root.getChildren()) {


            if (actor instanceof TextButton button) {

                String text =
                        button.getText()
                                .toString();


                if ("RETRY".equals(text)) {

                    button.clearListeners();

                    button.addListener(
                            new ClickListener() {

                                @Override
                                public void clicked(
                                        InputEvent event,
                                        float x,
                                        float y
                                ) {

                                    restartMinigame();
                                }
                            }
                    );

                } else if (
                        "EXIT TO MAP".equals(text)
                ) {

                    button.setText(
                            "EXIT"
                    );

                    button.clearListeners();

                    button.addListener(
                            new ClickListener() {

                                @Override
                                public void clicked(
                                        InputEvent event,
                                        float x,
                                        float y
                                ) {

                                    exitMinigame();
                                }
                            }
                    );
                }

            } else if (
                    actor instanceof Group group
            ) {

                rewireGameOverButtons(
                        group
                );
            }
        }
    }
    @Override
    protected void onGameFinished(boolean won) {
        controller.recordGraphicalResult();

        if (won) {
            showIZombieWin();
        } else {
            showIZombieGameOver();
        }
    }
}
