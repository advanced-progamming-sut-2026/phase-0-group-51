package views.graphical.screens.minigamesScreen.wallnutBowling;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controllers.miniGamesController.WallnutBowlingController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.games.ChapterTheme;
import models.minigames.MinigameType;
import models.minigames.wallnutBowling.RollingWallnut;
import models.minigames.wallnutBowling.WallnutBowling;
import models.minigames.wallnutBowling.WallnutType;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.*;
import views.graphical.ui.conveyorBelt.ConveyorBeltActor;

import java.util.*;

public class WallnutBowlingScreen extends BaseMinigameScreen {
    private static final String BG_LEFT = "IMAGE_BACKGROUNDS_ZCORP_BG_TEXTURE_LEFT";
    private static final String BG_MID = "IMAGE_BACKGROUNDS_ZCORP_BG_TEXTURE";
    private static final String BG_RIGHT = "IMAGE_BACKGROUNDS_ZCORP_BG_TEXTURE_RIGHT";

    private static final int WALLNUT_PLANT_ID = 44;
    private static final int EXPLODE_O_NUT_PLANT_ID = 49;
    private static final int TALLNUT_PLANT_ID = 45;
    private static final float CARD_SCALE = 0.72f;
    private static final float NORMAL_WALLNUT_SCALE = 0.65f;
    private static final float EXPLODE_WALLNUT_SCALE = 0.65f;
    private static final float GIANT_WALLNUT_SCALE = 1.05f;

    private final int stageNumber;
    private final WallnutBowlingController controller;
    private final WallnutBowling wallnutBowling;

    private final BoardArea boardArea;
    private final BoardTransform boardTransform;
    private final ShapeRenderer shapeRenderer;
    private final ZombieAnimationSystem zombieAnimationSystem;

    private BoardView boardView;

    private final Group rollingLayer = new Group();
    private final Map<RollingWallnut, Actor> rollingActors = new IdentityHashMap<>();
    private ConveyorBeltActor conveyorBelt;
    private List<WallnutType> lastConveyorSnapshot = List.of();
    private int selectedConveyorIndex = -1;
    private final ButtonGroup<PlantCard> conveyorButtonGroup = new ButtonGroup<>();
    private float renderDelta;

    public WallnutBowlingScreen(PvzGame game, int stageNumber) {
        super(game, BG_LEFT, BG_MID, BG_RIGHT);

        this.stageNumber = stageNumber;
        this.controller = new WallnutBowlingController();

        Result result = controller.startStage(stageNumber);
        if (!result.success()) {
            throw new IllegalStateException(result.message());
        }

        if (!(App.getInstance().getCurrentGame() instanceof WallnutBowling currentGame)) {
            throw new IllegalStateException("Wall-nut Bowling game was not created.");
        }

        this.wallnutBowling = currentGame;

        boardArea = new BoardArea(533f, 62f, 737f, 380f);
        boardTransform = new BoardTransform(boardArea);

        shapeRenderer = new ShapeRenderer();
        zombieAnimationSystem = new ZombieAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                boardTransform,
                ChapterTheme.MINIGAME
        );
        conveyorButtonGroup.setMinCheckCount(0);
        conveyorButtonGroup.setMaxCheckCount(1);
        conveyorButtonGroup.setUncheckLast(true);
        buildBoard();
        buildConveyorBar();
    }

    private void buildBoard() {
        Board board = wallnutBowling.getGameState().getBoard();

        boardView = new BoardView(board, boardTransform);
        boardView.setOnTileClicked(this::handleTileClicked);
        worldStage.addActor(boardView);

        rollingLayer.setTouchable(Touchable.disabled);
        worldStage.addActor(rollingLayer);
    }

    private void buildConveyorBar() {
        conveyorBelt = new ConveyorBeltActor("assets/UIs/Belt.png");
        conveyorBelt.setPosition(
                0f,
                82f
        );
        uiStage.addActor(
                conveyorBelt
        );
    }

    private void refreshConveyorBar(boolean force) {
        List<WallnutType> current = wallnutBowling.getConveyorBelt();
        if (!force && current.equals(lastConveyorSnapshot)) return;

        int diff = current.size() - lastConveyorSnapshot.size();

        if (force || current.isEmpty()) {
            conveyorBelt.clearPlants();
            conveyorButtonGroup.clear();
            for (WallnutType type : current) {
                addCardToBelt(type);
            }
        } else if (diff > 0) {
            for (int i = lastConveyorSnapshot.size(); i < current.size(); i++) {
                addCardToBelt(current.get(i));
            }
        }

        lastConveyorSnapshot = List.copyOf(current);
    }
    private void addCardToBelt(WallnutType type) {
        PlantData plantData = plantDataFor(type);
        if (plantData == null) return;

        PlantCard card = new PlantCard(game, new PlantCard.ViewData(plantData, true, false, 1, 0, 1, false, false), CARD_SCALE);
        Stack stack = new Stack();
        stack.add(card);
        conveyorButtonGroup.add(card);

        card.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (card.isChecked()) {
                    selectedConveyorIndex = conveyorBelt.getItems().indexOf(stack, true);
                } else {
                    int currentIndex = conveyorBelt.getItems().indexOf(stack, true);
                    if (selectedConveyorIndex == currentIndex) selectedConveyorIndex = -1;
                }
            }
        });

        conveyorBelt.addPlant(stack);
    }
    private PlantData plantDataFor(WallnutType type) {
        return switch (type) {
            case BOWLING -> PlantRegistry.getById(WALLNUT_PLANT_ID);
            case EXPLODE -> PlantRegistry.getById(EXPLODE_O_NUT_PLANT_ID);
            case BIG_WALLNUT -> PlantRegistry.getById(TALLNUT_PLANT_ID);
        };
    }

    private float worldScaleFor(WallnutType type) {
        return switch (type) {
            case BOWLING -> NORMAL_WALLNUT_SCALE;
            case EXPLODE -> EXPLODE_WALLNUT_SCALE;
            case BIG_WALLNUT -> GIANT_WALLNUT_SCALE;
        };
    }

    private void handleTileClicked(Tile tile) {
        if (!isPlaying() || isPaused() || tile == null) return;
        if (selectedConveyorIndex < 0) {
            game.notifyError("Select a walnut from the conveyor first.");
            return;
        }

        Result result = controller.rollWallnutAt(selectedConveyorIndex, tile.getColumn() + 1, tile.getLane() + 1);
        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        conveyorBelt.removePlant(selectedConveyorIndex);
        conveyorButtonGroup.uncheckAll();
        selectedConveyorIndex = -1;
        lastConveyorSnapshot = List.copyOf(wallnutBowling.getConveyorBelt());
        rebuildButtonGroup();
    }

    private void rebuildButtonGroup() {
        conveyorButtonGroup.clear();
        for (Actor actor : conveyorBelt.getItems()) {
            if (actor instanceof Stack stack && stack.getChildren().size > 0 && stack.getChild(0) instanceof PlantCard card) {
                conveyorButtonGroup.add(card);
            }
        }
    }
    @Override
    public void render(float delta) {
        renderDelta = Math.min(delta, 0.25f);
        if (conveyorBelt != null) {
            conveyorBelt.update(delta);
        }
        super.render(delta);
    }

    @Override
    protected void onGameTick() {
        refreshConveyorBar(false);
    }

    private float getRenderTickAlpha() {
        int ticksPerSecond = Math.max(
                1,
                wallnutBowling.getGameState().getTicksPerSecond()
        );

        float tickDuration = 1f / ticksPerSecond;

        return Math.max(
                0f,
                Math.min(1f, gameTickAccumulator / tickDuration)
        );
    }

    @Override
    protected void renderWorldUnderlay() {
        float partialTick = getRenderTickAlpha();

        if (isPlaying() && !isPaused()) {
            zombieAnimationSystem.update(
                    renderDelta,
                    partialTick,
                    wallnutBowling.getGameState().getTickCounter(),
                    wallnutBowling.getGameState().getZombiesInTheGame()
            );

            syncRollingWallnuts(partialTick);
        }

        drawRedLine();
    }

    private void syncRollingWallnuts(float partialTick) {
        Set<RollingWallnut> active = Collections.newSetFromMap(new IdentityHashMap<>());
        active.addAll(wallnutBowling.getRollingWallnuts());

        for (RollingWallnut wallnut : active) {
            Actor actor = rollingActors.get(wallnut);

            if (actor == null) {
                actor = createRollingActor(wallnut);
                rollingActors.put(wallnut, actor);
                rollingLayer.addActor(actor);
            }

            positionRollingActor(wallnut, actor, partialTick);
        }

        Iterator<Map.Entry<RollingWallnut, Actor>> iterator = rollingActors.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<RollingWallnut, Actor> entry = iterator.next();

            if (active.contains(entry.getKey())) {
                continue;
            }

            Actor actor = entry.getValue();

            if (entry.getKey().getWallnutType() == WallnutType.EXPLODE) {
                playExplosionEffect(actor.getX(), actor.getY());
            }

            actor.remove();
            iterator.remove();
        }
    }

    private Actor createRollingActor(RollingWallnut wallnut) {
        PlantData data = plantDataFor(wallnut.getWallnutType());

        if (data == null
                || data.idlePamPath() == null
                || data.idlePamPath().isBlank()
                || data.idleClip() == null
                || data.idleClip().isBlank()) {
            throw new IllegalStateException(
                    "Missing visual data for " + wallnut.getWallnutType().getName()
            );
        }

        game.getPamPlayer().loadSync(data.idlePamPath());

        Actor actor = game.createPamActor(
                data.idlePamPath(),
                data.idleClip(),
                0f,
                0f,
                true
        );

        actor.setScale(worldScaleFor(wallnut.getWallnutType()));
        actor.setTouchable(Touchable.disabled);
        return actor;
    }

    private void positionRollingActor(
            RollingWallnut wallnut,
            Actor actor,
            float partialTick
    ) {
        double renderX = wallnut.getPreviousX()
                + (wallnut.getX() - wallnut.getPreviousX()) * partialTick;

        double renderY = wallnut.getPreviousY()
                + (wallnut.getY() - wallnut.getPreviousY()) * partialTick;

        float centerX = boardArea.x()
                + ((float) renderX + 0.5f) * boardTransform.tileWidth();

        float centerY = boardArea.y()
                + (BoardTransform.ROWS - 1f - (float) renderY) * boardTransform.tileHeight()
                + boardTransform.tileHeight() * 0.5f;

        actor.setPosition(centerX, centerY);
    }

    private void playExplosionEffect(float x, float y) {
        // Put the explosion PAM path/clip here when you add that effect asset.
    }

    private void drawRedLine() {
        float redLineX = boardTransform.tileX(wallnutBowling.getRedLineColumn());

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
    protected void onGameplayStarted() {
        gameTickAccumulator = 0f;
        conveyorBelt.setVisible(true);
        refreshConveyorBar(true);
    }

    @Override
    protected Actor createStartPopup(Runnable onContinue) {
        return new StartGameMenuPopup(
                game,
                onContinue,
                "Wall-nut Bowling",
                stageNumber,
                "Use the conveyor walnuts to stop every zombie wave.",
                "No sun falls from the sky.",
                "Plant walnuts only before the red line.",
                "Bowling Wallnut deflects after zombie hits.",
                "Explode-O-Nut explodes in a 3x3 area.",
                "Giant Wallnut crushes zombies and keeps rolling."
        );
    }

    @Override
    protected void onGameFinished(boolean won) {
        controller.recordGraphicalResult();
        conveyorBelt.setVisible(false);

        if (won) {
            showWinPopup();
        } else {
            showLosePopup();
        }
    }

    private void showWinPopup() {
        boolean hasNextStage = stageNumber < 3;

        Runnable nextAction = hasNextStage
                ? () -> Gdx.app.postRunnable(
                () -> game.showScreen(
                        new WallnutBowlingScreen(game, stageNumber + 1)
                )
        )
                : this::exitMinigame;

        GameWinPopup popup = new GameWinPopup(
                game,
                "WALL-NUT BOWLING COMPLETE!",
                "All zombie waves were cleared.",
                "EXIT",
                this::exitMinigame,
                hasNextStage ? "NEXT STAGE" : "MINIGAMES",
                nextAction
        );

        uiStage.addActor(popup);
        popup.toFront();
    }

    private void showLosePopup() {
        GameOverPopup popup = new GameOverPopup(
                game,
                "THE ZOMBIES\nREACHED YOUR HOUSE!",
                "EXIT",
                this::exitMinigame,
                "RETRY",
                this::restartMinigame
        );

        uiStage.addActor(popup);
        popup.toFront();
    }

    @Override
    protected void restartMinigame() {
        Gdx.app.postRunnable(
                () -> game.showScreen(
                        new WallnutBowlingScreen(game, stageNumber)
                )
        );
    }

    @Override
    protected void exitMinigame() {
        controller.exitMenu();
        Gdx.app.postRunnable(
                () -> game.showScreen(
                        new minigames(game, MinigameType.WALLNUT_BOWLING)
                )
        );
    }

    @Override
    public void dispose() {
        zombieAnimationSystem.clear();
        shapeRenderer.dispose();
        super.dispose();
    }
}
