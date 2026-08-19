package views.graphical.screens.minigamesScreen.iZombie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
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
    private final ShapeRenderer shapeRenderer;
    private IZombieBar zombieBar;
    private String selectedZombieAlias;
    private final ZombieAnimationSystem zombieAnimationSystem;
    private static final float BRAIN_CROSS_REMOVE_DELAY = 2f;
    private final Map<Zombie, Float> brainCrossTimers =
            new IdentityHashMap<>();
    private float renderDelta = 0f;

    public IZombieScreen(PvzGame game, int stageNumber) {
        super(game, BG_LEFT, BG_MID, BG_RIGHT);
        this.stageNumber = stageNumber;
        controller = new IZombieController();
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
        zombieBar = new IZombieBar(game, iZombie, alias -> selectedZombieAlias = alias);
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
        worldStage.addActor(boardView);
        buildBrains();
        plantViewManager = new PlantViewManager(game, boardTransform);
        worldStage.addActor(plantViewManager);
        projectileViewManager = new ProjectileViewManager(game, boardTransform);
        worldStage.addActor(projectileViewManager);
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
        zombieBar.refresh();
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
            sunViewManager.sync(
                    iZombie.getGameState()
                            .getBoard()
                            .getActiveSuns(),
                    partialTick
            );
            Set<Zombie> renderableZombies =
                    new HashSet<>(
                            iZombie.getGameState().getZombiesInTheGame()
                    );

            renderableZombies.removeIf(
                    zombie -> IZombie.SUN_PRODUCER_ALIAS.equals(zombie.getAlias())
            );

            zombieAnimationSystem.update(
                    renderDelta,
                    partialTick,
                    iZombie.getGameState().getTickCounter(),
                    renderableZombies
            );
        }

        float redLineX = boardTransform.tileX(IZombie.RED_LINE_COLUMN);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rectLine(
                redLineX,
                boardArea.y(),
                redLineX,
                boardArea.y() + boardArea.height(),
                4f
        );
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
