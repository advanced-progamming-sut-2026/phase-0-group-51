package views.graphical.screens.minigamesScreen.vaseBreaker;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import controllers.miniGamesController.VaseBreakerController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.games.ChapterTheme;
import models.minigames.MinigameType;
import models.minigames.vaseBreaker.DroppedSeedPacket;
import models.minigames.vaseBreaker.Vase;
import models.minigames.vaseBreaker.VaseBreaker;
import models.sun.Sun;
import views.graphical.gameplay.actors.PlantActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.screens.minigamesScreen.iZombie.BrainView;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.StartGameMenuPopup;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class VaseBreakerScreen extends BaseMinigameScreen {
    private static final String BG_LEFT = "IMAGE_BACKGROUNDS_DARK_TEXTURE_LEFT";
    private static final String BG_MID = "IMAGE_BACKGROUNDS_DARK_TEXTURE";
    private static final String BG_RIGHT = "IMAGE_BACKGROUNDS_DARK_TEXTURE_RIGHT";
    private final int stageNumber;
    private final VaseBreakerController controller;
    private final VaseBreaker vaseBreaker;
    private final Group droppedPacketLayer = new Group();
    private final Map<DroppedSeedPacket, DroppedSeedPacketView> droppedPacketViews =
            new IdentityHashMap<>();
    private VasePacketBar packetBar;
    private String selectedPacketName;
    private final BoardTransform boardTransform;
    private BoardView boardView;
    private VaseBoardView vaseBoardView;
    private PlantViewManager plantViewManager;
    private ProjectileViewManager projectileViewManager;
    private SunViewManager sunViewManager;
    private PlantActor placementPreview;
    private Image rowHighlight;
    private Image columnHighlight;
    private final ZombieAnimationSystem zombieAnimationSystem;

    private final List<BrainView> brainViews =
            new ArrayList<>();

    private float renderDelta;
    public VaseBreakerScreen(PvzGame game, int stageNumber) {
        super(game, BG_LEFT, BG_MID, BG_RIGHT);
        this.stageNumber = stageNumber;
        controller = new VaseBreakerController();
        Result result = controller.startStage(stageNumber);

        if (!result.success()) {
            throw new IllegalStateException(
                    result.message()
            );
        }

        if (!(App.getInstance().getCurrentGame() instanceof VaseBreaker currentVaseBreaker)) {
            throw new IllegalStateException(
                    "VaseBreaker game was not created."
            );
        }

        vaseBreaker = currentVaseBreaker;
        BoardArea boardArea = new BoardArea(533f, 62f, 737f, 380f);
        boardTransform =
                new BoardTransform(
                        boardArea
                );

        zombieAnimationSystem =
                new ZombieAnimationSystem(
                        game.getPamPlayer(),
                        worldStage,
                        boardTransform,
                        ChapterTheme.MINIGAME
                );
        buildPacketBar();
        buildBoard();
    }
    private void handleSunClicked(Sun sun) {
        boolean collected =
                vaseBreaker.getGameState().getBoard().collectSun(sun, vaseBreaker.getGameState());
        if (!collected) {
            return;
        }
    }
    private void buildBoard() {
        Board board = vaseBreaker.getGameState().getBoard();
        boardView = new BoardView(
                board,
                boardTransform
        );

        boardView.setOnTileClicked(
                this::handleTileClicked
        );

        boardView.setOnTileHovered(
                this::handleTileHover
        );

        createPlacementHighlights();

        worldStage.addActor(
                rowHighlight
        );

        worldStage.addActor(
                columnHighlight
        );

        worldStage.addActor(
                boardView
        );
    plantViewManager = new PlantViewManager(game, boardTransform);
    worldStage.addActor(plantViewManager);
    projectileViewManager = new ProjectileViewManager(game, boardTransform);
    worldStage.addActor(projectileViewManager);
    sunViewManager = new SunViewManager(game, boardTransform);
    sunViewManager.setOnSunClicked(this::handleSunClicked);

    worldStage.addActor(sunViewManager);


    buildBrains();


        vaseBoardView = new VaseBoardView(game, vaseBreaker, boardTransform);
        vaseBoardView.setOnVaseClicked(
                this::handleVaseClicked
        );

        worldStage.addActor(vaseBoardView);
    worldStage.addActor(
            droppedPacketLayer
    );
        placementPreview =
                new PlantActor(game);

        placementPreview.setPreviewMode(
                true
        );

        worldStage.addActor(
                placementPreview
        );
    syncModelViews();
        refreshDroppedPackets();
    }
    private void buildPacketBar() {
        packetBar = new VasePacketBar(game, vaseBreaker, this::handlePacketSelectionChanged);
        packetBar.setVisible(false);
        uiStage.addActor(packetBar);
    }
    private void handlePacketSelectionChanged(String plantName) {
        selectedPacketName = plantName;
        if (placementPreview == null) {
            return;
        }
        if (plantName == null) {
            placementPreview.clearPlant();
            hidePlacementHighlights();
            return;
        }
        PlantData plant = PlantRegistry.getByName(plantName);

        if (plant == null) {
            placementPreview.clearPlant();
            selectedPacketName = null;
            hidePlacementHighlights();
            return;
        }

        placementPreview.setPreviewMode(true);
        placementPreview.setPlant(plant);
    }
    private void refreshDroppedPackets() {
        int currentTick =
                vaseBreaker.getGameState().getTickCounter();

        int ticksPerSecond =
                vaseBreaker.getGameState().getTicksPerSecond();

        droppedPacketViews.entrySet().removeIf(entry -> {
            DroppedSeedPacket packet = entry.getKey();

            boolean stillInModel =
                    vaseBreaker.getDroppedSeedPackets().contains(packet);

            boolean shouldRemove =
                    !stillInModel || !packet.isActive(currentTick);

            if (shouldRemove) {
                entry.getValue().remove();
            }

            return shouldRemove;
        });

        for (DroppedSeedPacket packet : vaseBreaker.getDroppedSeedPackets()) {
            if (!packet.isActive(
                    currentTick
            )) {
                continue;
            }

            DroppedSeedPacketView packetView =
                    droppedPacketViews.get(packet);

            if (packetView == null) {
                packetView = new DroppedSeedPacketView(
                        game,
                        packet,
                        this::handleDroppedPacketClicked
                );

            placeDroppedPacket(packetView, packet);
            droppedPacketLayer.addActor(packetView);
                droppedPacketViews.put(packet, packetView);
            }

            packetView.refreshTimer(
                    currentTick,
                    ticksPerSecond
            );
        }
    }
    private void buildBrains() {

        brainViews.clear();

        for (models.minigames.vaseBreaker.Brain brain :
                vaseBreaker.getBrains()) {

            BrainView brainView =
                    new BrainView(
                            game,
                            brain,
                            boardTransform
                    );

            brainViews.add(
                    brainView
            );

            worldStage.addActor(
                    brainView
            );
        }
    }
    private void refreshBrains() {

        for (BrainView brainView :
                brainViews) {

            brainView.refresh();
        }
    }
    private void syncModelViews() {

        Board board =
                vaseBreaker
                        .getGameState()
                        .getBoard();

        plantViewManager.sync(
                board
        );

        refreshBrains();
    }
    private void placeDroppedPacket(DroppedSeedPacketView view, DroppedSeedPacket packet) {
        int column = packet.getX() - 1;
        int lane = packet.getY() - 1;
        float tileX = boardTransform.tileX(column);
        float tileY = boardTransform.tileY(lane);
        float tileWidth = boardTransform.tileWidth();
        float tileHeight = boardTransform.tileHeight();
        float packetWidth = tileWidth * 0.82f;
        float packetHeight = packetWidth * (70f / 115f);
        float packetX = tileX + (tileWidth - packetWidth) / 2f;
        float packetY = tileY + (tileHeight - packetHeight) / 2f;

        view.setBounds(
                packetX,
                packetY,
                packetWidth,
                packetHeight
        );
    }
    private void handleVaseClicked(Vase vase) {

        if (!isPlaying()) {
            return;
        }

        if (isPaused()) {
            return;
        }

        Result result = controller.breakVase(vase.getX(), vase.getY());
        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }
        game.notifyInfo(result.message());
        vaseBoardView.refresh();
        refreshDroppedPackets();
    }
    private void handleDroppedPacketClicked(DroppedSeedPacket packet) {
        if (!isPlaying() || isPaused()) {
            return;
        }

        String plantName = packet.getPlantName();

        Result result = controller.pickUpSeedPacket(packet.getX(), packet.getY());
        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        game.notifyInfo(result.message());
        refreshDroppedPackets();
        packetBar.selectPlant(plantName);
    }
    private void handleTileClicked(Tile tile) {

        if (!isPlaying() || isPaused()) {
            return;
        }

        if (selectedPacketName == null) {
            return;
        }
        int x = tile.getColumn() + 1;
        int y = tile.getLane() + 1;

        Result result = controller.plantPacket(selectedPacketName, x, y);

        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        syncModelViews();
        packetBar.clearSelection();
        packetBar.refresh();
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
        boolean hasPlantSelection =
                selectedPacketName != null;

        if (tile == null
                || !hasPlantSelection
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

    @Override
    protected Actor createStartPopup(Runnable onContinue) {
        return new StartGameMenuPopup(
                game,
                onContinue,
                "Vasebreaker",
                stageNumber,
                "Break all vases and defeat every zombie.",
                "Break every vase.",
                "Defeat every zombie.",
                "Do not let a zombie eat a brain."
        );
    }

    @Override
    protected void onGameplayStarted() {
        gameTickAccumulator = 0f;
        packetBar.refresh();
        packetBar.setVisible(true);
    }
    private float getRenderTickAlpha() {

        int ticksPerSecond =
                Math.max(
                        1,
                        vaseBreaker
                                .getGameState()
                                .getTicksPerSecond()
                );

        float tickDuration =
                1f / ticksPerSecond;

        return Math.max(
                0f,
                Math.min(
                        1f,
                        gameTickAccumulator
                                / tickDuration
                )
        );
    }
    @Override
    public void render(float delta) {
        renderDelta = Math.min(delta, 0.25f);
        super.render(delta);
    }
    @Override
    protected void renderWorldUnderlay() {

        if (!isPlaying()
                || isPaused()) {

            return;
        }


        float partialTick =
                getRenderTickAlpha();


        Board board =
                vaseBreaker
                        .getGameState()
                        .getBoard();


        projectileViewManager.sync(
                board.getProjectiles(),
                partialTick
        );


        sunViewManager.sync(
                board.getActiveSuns(),
                partialTick
        );


        zombieAnimationSystem.update(
                renderDelta,
                partialTick,
                vaseBreaker
                        .getGameState()
                        .getTickCounter(),
                vaseBreaker
                        .getGameState()
                        .getZombiesInTheGame()
        );

        droppedPacketLayer.toFront();

        if (placementPreview != null) {
            placementPreview.toFront();
        }
    }
    @Override
    protected void restartMinigame() {
        Gdx.app.postRunnable(
                () -> game.showScreen(new VaseBreakerScreen(game, stageNumber)));
    }

    @Override
    protected void exitMinigame() {
        controller.exitMenu();
        Gdx.app.postRunnable(() -> game.showScreen(new minigames(game, MinigameType.VASEBREAKER)));
    }
    @Override
    protected void onGameTick() {

        syncModelViews();

        refreshDroppedPackets();

    }
}
