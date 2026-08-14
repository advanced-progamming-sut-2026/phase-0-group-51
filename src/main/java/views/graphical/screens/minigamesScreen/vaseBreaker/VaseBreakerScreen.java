package views.graphical.screens.minigamesScreen.vaseBreaker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import controllers.miniGamesController.VaseBreakerController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.minigames.MinigameType;
import models.minigames.vaseBreaker.DroppedSeedPacket;
import models.minigames.vaseBreaker.Vase;
import models.minigames.vaseBreaker.VaseBreaker;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.StartGameMenuPopup;

public class VaseBreakerScreen extends BaseMinigameScreen {
    private static final String BG_LEFT = "IMAGE_BACKGROUNDS_DARK_TEXTURE_LEFT";
    private static final String BG_MID = "IMAGE_BACKGROUNDS_DARK_TEXTURE";
    private static final String BG_RIGHT = "IMAGE_BACKGROUNDS_DARK_TEXTURE_RIGHT";
    private final int stageNumber;
    private final VaseBreakerController controller;
    private final VaseBreaker vaseBreaker;
    private final Group droppedPacketLayer = new Group();
    private VasePacketBar packetBar;
    private String selectedPacketName;
    private final BoardTransform boardTransform;
    private BoardView boardView;
    private VaseBoardView vaseBoardView;

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
        boardTransform = new BoardTransform(boardArea);
        buildPacketBar();
        buildBoard();
    }

    private void buildBoard() {
        Board board = vaseBreaker.getGameState().getBoard();

        boardView = new BoardView(board,boardTransform);
        boardView.setOnTileClicked(this::handleTileClicked);
        worldStage.addActor(boardView);


        vaseBoardView = new VaseBoardView(game, vaseBreaker, boardTransform);
        vaseBoardView.setOnVaseClicked(
                this::handleVaseClicked
        );

        worldStage.addActor(vaseBoardView);
        worldStage.addActor(droppedPacketLayer);
        refreshDroppedPackets();
    }
    private void buildPacketBar() {
        packetBar = new VasePacketBar(game, vaseBreaker, plantName -> selectedPacketName = plantName);
        packetBar.setVisible(false);
        uiStage.addActor(packetBar);
    }
    private void refreshDroppedPackets() {
        droppedPacketLayer.clearChildren();
        int currentTick = vaseBreaker.getGameState().getTickCounter();
        int ticksPerSecond = vaseBreaker.getGameState().getTicksPerSecond();
        for (DroppedSeedPacket packet : vaseBreaker.getDroppedSeedPackets()) {
            if (!packet.isActive(
                    currentTick
            )) {
                continue;
            }

            DroppedSeedPacketView packetView = new DroppedSeedPacketView(game, packet, this::handleDroppedPacketClicked);
            placeDroppedPacket(packetView, packet);
            packetView.refreshTimer(currentTick, ticksPerSecond);
            droppedPacketLayer.addActor(packetView);
        }
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

        Result result = controller.pickUpSeedPacket(packet.getX(), packet.getY());
        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        game.notifyInfo(result.message());
        refreshDroppedPackets();
        packetBar.refresh();
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
        game.notifyInfo(result.message());
        packetBar.refresh();
        selectedPacketName = packetBar.getSelectedPlantName();
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
        refreshDroppedPackets();
    }
}
