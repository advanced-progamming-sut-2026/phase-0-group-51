package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import com.badlogic.gdx.scenes.scene2d.ui.*;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;

import lombok.Getter;
import network.client.ClientSession;
import network.client.NetworkClient;
import network.client.service.MatchClientService;

import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;

import network.protocol.match.GameActionDto;
import network.protocol.match.GameActionType;
import network.protocol.match.MatchEndedDto;
import network.protocol.match.MatchSnapshot;

import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;

import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

import views.graphical.screens.BaseScreen;

import views.graphical.ui.BorderedPanel;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class OnlineIZombieScreen
        extends BaseScreen {

    private static final String BG_LEFT =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_LEFT";

    private static final String BG_MID =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE";

    private static final String BG_RIGHT =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_RIGHT";
    private static final String SUN =
            "IMAGE_UI_HUD_INGAME_SUN";

    private static final String SUN_BACKGROUND =
            "IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE";
    private static final float LOCAL_WORLD_HEIGHT =
            600f;

    private static final float LOCAL_BOARD_X =
            533f;

    private static final float LOCAL_BOARD_Y =
            62f;

    private static final float LOCAL_BOARD_WIDTH =
            737f;

    private static final float LOCAL_BOARD_HEIGHT =
            380f;


    private final ClientSession clientSession;

    private final NetworkClient networkClient;

    private final MatchClientService matchClientService;

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    private final RemoteMatchMirror mirror;


    private final BoardArea boardArea;

    private final BoardTransform boardTransform;


    private RemoteMatchView remoteMatchView;


    private Label roleLabel;

    private Label opponentLabel;

    private Label timerLabel;

    private Label sunLabel;

    private OnlineUnitCardBar placementBar;


    private boolean active;

    private int lastRenderedTick =
            -1;

    private boolean matchEnded;

    private boolean actionPending;


    public OnlineIZombieScreen(
            PvzGame game,
            ClientSession clientSession,
            NetworkClient networkClient
    ) {

        super(game);


        this.clientSession =
                Objects.requireNonNull(
                        clientSession,
                        "clientSession cannot be null"
                );


        this.networkClient =
                Objects.requireNonNull(
                        networkClient,
                        "networkClient cannot be null"
                );


        if (clientSession.getMatchId() == null
                || clientSession.getMatchId().isBlank()) {

            throw new IllegalStateException(
                    "Online matchId is missing."
            );
        }


        this.matchClientService =
                new MatchClientService(
                        networkClient
                );


        this.mirror =
                new RemoteMatchMirror(
                        clientSession.getMatchId()
                );


        this.boardArea =
                createGameplayBoardArea();


        this.boardTransform =
                new BoardTransform(
                        boardArea
                );


        buildUi();
    }
    private void sendPlaceZombie(
            String zombieName,
            int row,
            int column
    ) {

        GameActionDto action =
                new GameActionDto();


        action.setType(
                GameActionType.PLACE_ZOMBIE
        );


        action.setEntityName(
                zombieName
        );


        action.setRow(
                row
        );


        action.setColumn(
                column
        );


        action.setClientActionId(
                UUID.randomUUID().toString()
        );


        try {

            sendAction(action);

        } catch (IOException e) {

            actionPending = false;

            game.notifyError(
                    e.getMessage()
            );
        }
    }
    private void sendPlacePlant(
            String plantName,
            int row,
            int column
    ) {

        GameActionDto action =
                new GameActionDto();

        action.setType(
                GameActionType.PLACE_PLANT
        );

        action.setEntityName(
                plantName
        );

        action.setRow(
                row
        );

        action.setColumn(
                column
        );

        action.setClientActionId(
                UUID.randomUUID().toString()
        );


        try {

            sendAction(action);

        } catch (IOException e) {

            actionPending = false;

            game.notifyError(
                    e.getMessage()
            );
        }
    }
    private void buildUi() {

        Stack root =
                new Stack();


        root.setFillParent(
                true
        );

        Actor background =
                createBackground();


        background.setBounds(
                0f,
                0f,
                PvzGame.VIRTUAL_WIDTH,
                PvzGame.VIRTUAL_HEIGHT
        );


        background.setTouchable(
                Touchable.disabled
        );


        root.add(
                background
        );

        remoteMatchView =
                new RemoteMatchView(
                        game,
                        boardTransform,
                        stage
                );


        remoteMatchView.setBounds(
                0f,
                0f,
                PvzGame.VIRTUAL_WIDTH,
                PvzGame.VIRTUAL_HEIGHT
        );


        root.add(
                remoteMatchView
        );

        root.add(
                createBoardInputLayer()
        );

        Stack sunBank =
                createSunBank();


        Table sunLayer =
                new Table();

        sunLayer.setFillParent(
                true
        );

        sunLayer.top()
                .left();


        sunLayer.add(
                        sunBank
                )
                .width(120f)
                .height(48f)
                .padTop(10f)
                .padLeft(14f);


        root.add(
                sunLayer
        );


        Table matchInfoLayer =
                new Table();

        matchInfoLayer.setFillParent(
                true
        );

        matchInfoLayer.top()
                .right();


        Table matchInfo =
                new Table();


        roleLabel =
                createHudLabel(
                        clientSession.isPlantPlayer()
                                ? "PLANTS"
                                : "ZOMBIES"
                );


        opponentLabel =
                createHudLabel(
                        "VS "
                                + clientSession.getOpponentUsername()
                );


        timerLabel =
                createHudLabel(
                        "--:--"
                );


        matchInfo.add(
                        roleLabel
                )
                .padRight(16f);


        matchInfo.add(
                        opponentLabel
                )
                .padRight(16f);


        matchInfo.add(
                timerLabel
        );


        matchInfoLayer.add(
                        matchInfo
                )
                .padTop(13f)
                .padRight(20f);


        root.add(
                matchInfoLayer
        );

        root.add(
                createPlacementControls()
        );


        stage.addActor(
                root
        );
    }

    private Group createBoardInputLayer() {

        Group inputLayer = new Group();
        inputLayer.setTouchable(Touchable.childrenOnly);

        for (int lane = 0; lane < BoardTransform.ROWS; lane++) {
            for (int column = 0; column < BoardTransform.COLUMNS; column++) {
                final int clickedLane = lane;
                final int clickedColumn = column;

                Actor tileInput = new Actor();
                tileInput.setBounds(
                        boardTransform.tileX(column),
                        boardTransform.tileY(lane),
                        boardTransform.tileWidth(),
                        boardTransform.tileHeight()
                );
                tileInput.setTouchable(Touchable.enabled);
                tileInput.addListener(new ClickListener() {
                    @Override
                    public void enter(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            Actor fromActor
                    ) {
                        showBoardHighlight(clickedLane, clickedColumn);
                    }

                    @Override
                    public void exit(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            Actor toActor
                    ) {
                        if (remoteMatchView != null) {
                            remoteMatchView.hidePlacementHighlight();
                        }
                    }

                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        handleBoardClick(clickedLane, clickedColumn);
                    }
                });
                inputLayer.addActor(tileInput);
            }
        }

        return inputLayer;
    }

    private Table createPlacementControls() {

        Table layer = new Table();
        layer.setFillParent(true);
        layer.bottom();

        Table panel = new Table(
                game.getSkin()
        );
        panel.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(0f, 0f, 0f, 0.72f)
                )
        );

        Label title = createHudLabel(
                clientSession.isPlantPlayer()
                        ? "SELECT PLANT"
                        : "SELECT ZOMBIE"
        );

        Label placementHintLabel = createHudLabel(
                clientSession.isPlantPlayer()
                        ? "Choose a plant, then click columns 2-6"
                        : "Choose a zombie, then click columns 7-9"
        );
        placementHintLabel.setFontScale(0.75f);

        placementBar = new OnlineUnitCardBar(
                game,
                clientSession.isPlantPlayer(),
                clientSession.getStageNumber(),
                selected -> {
                    if ((selected == null || selected.isBlank())
                            && remoteMatchView != null) {
                        remoteMatchView.hidePlacementHighlight();
                    }
                }
        );

        panel.add(title)
                .left()
                .pad(5f, 10f, 2f, 10f);
        panel.add(placementHintLabel)
                .right()
                .expandX()
                .pad(5f, 10f, 2f, 10f);
        panel.row();
        panel.add(placementBar)
                .colspan(2)
                .width(PvzGame.VIRTUAL_WIDTH - 80f)
                .height(82f)
                .pad(0f, 8f, 5f, 8f);

        layer.add(panel)
                .padBottom(4f);

        return layer;
    }

    private void handleBoardClick(
            int lane,
            int column
    ) {

        if (!active || matchEnded || actionPending || placementBar == null) {
            return;
        }

        String entityName = placementBar.getSelectedEntity();
        if (entityName == null || entityName.isBlank()) {
            game.notifyError("Select a unit first.");
            return;
        }

        if (clientSession.isPlantPlayer()) {
            if (column < MultiplayerIZombieGame.PLANT_START_COLUMN - 1
                    || column > MultiplayerIZombieGame.PLANT_END_COLUMN - 1) {
                game.notifyError("Plants can only be placed in columns 2-6.");
                return;
            }
            sendPlacePlant(entityName, lane, column);
        } else {
            if (column < MultiplayerIZombieGame.RED_LINE_COLUMN) {
                game.notifyError("Zombies can only be placed in columns 7-9.");
                return;
            }
            sendPlaceZombie(entityName, lane, column);
        }
    }

    private void showBoardHighlight(int lane, int column) {
        if (!active
                || matchEnded
                || placementBar == null
                || placementBar.getSelectedEntity() == null
                || placementBar.getSelectedEntity().isBlank()
                || remoteMatchView == null) {
            if (remoteMatchView != null) {
                remoteMatchView.hidePlacementHighlight();
            }
            return;
        }
        remoteMatchView.showPlacementHighlight(lane, column);
    }

    private void sendAction(
            GameActionDto action
    ) throws IOException {

        actionPending = true;

        matchClientService.sendAction(action)
                .whenComplete((result, throwable) ->
                        Gdx.app.postRunnable(() -> {
                            actionPending = false;

                            if (!active) {
                                return;
                            }

                            if (throwable != null) {
                                game.notifyError(rootMessage(throwable));
                                return;
                            }

                            if (result == null) {
                                game.notifyError("Server returned no action result.");
                                return;
                            }

                            if (!result.isAccepted()) {
                                game.notifyError(
                                        result.getReason() == null
                                                ? "Action rejected."
                                                : result.getReason()
                                );
                            }
                        })
                );
    }

    private Stack createSunBank() {

        Stack sunBank =
                new Stack();


        TextureRegion backgroundRegion =
                game.getTextureBank().region(
                        SUN_BACKGROUND
                );

        if (backgroundRegion == null) {

            throw new IllegalStateException(
                    "Missing HUD asset: "
                            + SUN_BACKGROUND
            );
        }


        TextureRegion sunRegion =
                game.getTextureBank().region(
                        SUN
                );

        if (sunRegion == null) {

            throw new IllegalStateException(
                    "Missing HUD asset: "
                            + SUN
            );
        }


        Image sunBackground =
                new Image(
                        new TextureRegionDrawable(
                                backgroundRegion
                        )
                );

        sunBackground.setScaling(
                Scaling.stretch
        );


        Image sunIcon =
                new Image(
                        new TextureRegionDrawable(
                                sunRegion
                        )
                );

        sunIcon.setScaling(
                Scaling.fit
        );


        sunLabel =
                new Label(
                        "0",
                        game.getSkin().get(
                                "big_outline",
                                Label.LabelStyle.class
                        )
                );


        sunLabel.setColor(
                Color.WHITE
        );

        sunLabel.setAlignment(
                Align.center
        );


        Table sunContent =
                new Table();


        sunContent.add(
                        sunIcon
                )
                .size(44f)
                .padLeft(-9f);


        sunContent.add(
                        sunLabel
                )
                .expandX()
                .center()
                .padRight(8f);


        sunBank.add(
                sunBackground
        );

        sunBank.add(
                sunContent
        );


        return sunBank;
    }
    private Label createHudLabel(
            String text
    ) {

        Label label =
                new Label(
                        text,
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        label.setColor(
                Color.WHITE
        );


        return label;
    }

    @Override
    public void show() {

        super.show();


        active =
                true;


        game.hideHud();

        networkClient.setEventListener(
                this::receiveServerEvent
        );
    }


    @Override
    public void hide() {

        active =
                false;

        if (remoteMatchView != null) {
            remoteMatchView.hidePlacementHighlight();
        }


        networkClient.setEventListener(
                null
        );


        super.hide();
    }


    @Override
    public void render(
            float delta
    ) {

        applyLatestSnapshot();


        super.render(
                delta
        );
    }

    private void applyLatestSnapshot() {

        MatchSnapshot snapshot =
                mirror.getLatestSnapshot();


        if (snapshot == null) {
            return;
        }


        if (snapshot.getTick()
                == lastRenderedTick) {

            return;
        }


        lastRenderedTick =
                snapshot.getTick();


        remoteMatchView.sync(
                snapshot
        );


        updateHud(
                snapshot
        );

        finishFromSnapshotIfNeeded(
                snapshot
        );
    }


    private void updateHud(
            MatchSnapshot snapshot
    ) {

        int seconds =
                Math.max(
                        0,
                        snapshot.getRemainingTicks()
                                / 10
                );


        int minutes =
                seconds / 60;


        int remainingSeconds =
                seconds % 60;


        timerLabel.setText(
                String.format(
                        "%d:%02d",
                        minutes,
                        remainingSeconds
                )
        );


        if (clientSession.isPlantPlayer()) {

            int availableSun = snapshot.getPlantSun();

            sunLabel.setText(
                    Integer.toString(
                            availableSun
                    )
            );

            if (placementBar != null) {
                placementBar.setAvailableSun(availableSun);
            }

        } else {

            int availableSun = snapshot.getZombieSun();

            sunLabel.setText(
                    Integer.toString(
                            availableSun
                    )
            );

            if (placementBar != null) {
                placementBar.setAvailableSun(availableSun);
            }
        }
    }

    private void finishFromSnapshotIfNeeded(
            MatchSnapshot snapshot
    ) {
        if (matchEnded || snapshot == null || snapshot.getStatus() == null) {
            return;
        }

        String outcome = snapshot.getStatus().trim().toUpperCase(
                java.util.Locale.ROOT
        );

        String winnerRole = switch (outcome) {
            case "PLANT_WON" -> "PLANT";
            case "ZOMBIE_WON" -> "ZOMBIE";
            default -> null;
        };

        if (winnerRole == null) {
            return;
        }

        String reason = "PLANT".equals(winnerRole)
                ? "The plant player protected at least one brain until time expired."
                : "The zombie player ate every brain.";

        MatchEndedDto ended = new MatchEndedDto(
                snapshot.getMatchId(),
                outcome,
                winnerRole,
                snapshot.getTick(),
                reason
        );

        if (clientSession.applyMatchEnded(ended)) {
            showMatchEndedPopup(ended);
        }
    }

    private void receiveServerEvent(
            NetworkMessage message
    ) {

        if (!active
                || message == null) {

            return;
        }


        Gdx.app.postRunnable(
                () -> {

                    if (active) {

                        handleServerEvent(
                                message
                        );
                    }
                }
        );
    }


    private void handleServerEvent(
            NetworkMessage message
    ) {

        if (message == null
                || message.getType() == null) {

            return;
        }


        try {

            if (message.getType()
                    == MessageType.MATCH_SNAPSHOT) {

                MatchSnapshot snapshot =
                        codec.decodePayload(
                                message.getPayload(),
                                MatchSnapshot.class
                        );


                mirror.apply(
                        snapshot
                );


                return;
            }


            if (message.getType()
                    == MessageType.MATCH_ENDED) {

                MatchEndedDto ended =
                        codec.decodePayload(
                                message.getPayload(),
                                MatchEndedDto.class
                        );


                if (!clientSession.applyMatchEnded(
                        ended
                )) {

                    return;
                }


                showMatchEndedPopup(
                        ended
                );
            }

        } catch (Exception exception) {

            game.notifyError(
                    "Could not process online match event: "
                            + rootMessage(
                            exception
                    )
            );
        }
    }

    private BoardArea createGameplayBoardArea() {

        TextureRegion left =
                game.getTextureBank()
                        .region(
                                BG_LEFT
                        );


        TextureRegion middle =
                game.getTextureBank()
                        .region(
                                BG_MID
                        );


        float scale =
                PvzGame.VIRTUAL_HEIGHT
                        / LOCAL_WORLD_HEIGHT;


        float visibleWorldWidth =
                PvzGame.VIRTUAL_WIDTH
                        / scale;


        float cameraGameplayX =
                left.getRegionWidth()
                        + middle.getRegionWidth()
                        / 2f;


        float cameraLeft =
                cameraGameplayX
                        - visibleWorldWidth
                        / 2f;


        return new BoardArea(
                (
                        LOCAL_BOARD_X
                                - cameraLeft
                )
                        * scale,

                LOCAL_BOARD_Y
                        * scale,

                LOCAL_BOARD_WIDTH
                        * scale,

                LOCAL_BOARD_HEIGHT
                        * scale
        );
    }


    private Actor createBackground() {

        TextureRegion left =
                game.getTextureBank()
                        .region(
                                BG_LEFT
                        );


        TextureRegion middle =
                game.getTextureBank()
                        .region(
                                BG_MID
                        );


        TextureRegion right =
                game.getTextureBank()
                        .region(
                                BG_RIGHT
                        );


        final float scale =
                PvzGame.VIRTUAL_HEIGHT
                        / LOCAL_WORLD_HEIGHT;


        final float visibleWorldWidth =
                PvzGame.VIRTUAL_WIDTH
                        / scale;


        final float cameraGameplayX =
                left.getRegionWidth()
                        + middle.getRegionWidth()
                        / 2f;


        final float cameraLeft =
                cameraGameplayX
                        - visibleWorldWidth
                        / 2f;


        return new Actor() {

            @Override
            public void draw(
                    Batch batch,
                    float parentAlpha
            ) {

                float currentX =
                        -cameraLeft
                                * scale;


                float leftWidth =
                        left.getRegionWidth()
                                * scale;


                float middleWidth =
                        middle.getRegionWidth()
                                * scale;


                float rightWidth =
                        right.getRegionWidth()
                                * scale;


                batch.draw(
                        left,
                        currentX,
                        0f,
                        leftWidth,
                        PvzGame.VIRTUAL_HEIGHT
                );


                currentX +=
                        leftWidth;


                batch.draw(
                        middle,
                        currentX,
                        0f,
                        middleWidth,
                        PvzGame.VIRTUAL_HEIGHT
                );


                currentX +=
                        middleWidth;


                batch.draw(
                        right,
                        currentX,
                        0f,
                        rightWidth,
                        PvzGame.VIRTUAL_HEIGHT
                );
            }
        };
    }

    private void showMatchEndedPopup(
            MatchEndedDto ended
    ) {

        if (matchEnded) {
            return;
        }


        matchEnded =
                true;

        if (remoteMatchView != null) {
            remoteMatchView.hidePlacementHighlight();
        }


        Table darkLayer =
                new Table();


        darkLayer.setFillParent(
                true
        );


        darkLayer.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0f,
                                0f,
                                0f,
                                0.72f
                        )
                )
        );


        BorderedPanel popup =
                new BorderedPanel(
                        game,
                        Color.valueOf("8F4909")
                );


        Table content =
                popup.getContent();


        content.clearChildren();


        String winner = resolveWinnerRole(ended);


        boolean won =
                winner != null
                        && winner.equalsIgnoreCase(
                        clientSession.getRole()
                );

        boolean hasWinner = winner != null;

        String titleText = !hasWinner
                ? "MATCH ENDED"
                : won
                ? "YOU WIN!"
                : "YOU LOSE!";


        Label title =
                new Label(
                        titleText,
                        game.getSkin().get(
                                "big_outline",
                                Label.LabelStyle.class
                        )
                );


        title.setColor(
                !hasWinner
                        ? Color.WHITE
                        : won
                        ? Color.valueOf("FFE16A")
                        : Color.valueOf("FF8A70")
        );


        Label reason =
                new Label(
                        matchEndReason(ended, winner),
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        reason.setWrap(
                true
        );


        reason.setAlignment(
                Align.center
        );


        TextButton continueButton =
                new TextButton(
                        "CONTINUE",
                        game.getSkin(),
                        "green"
                );


        continueButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        int stageNumber =
                                clientSession.getStageNumber();


                        clientSession.clearMatch();


                        game.showScreen(
                                new OnlineIZombieLobbyScreen(
                                        game,
                                        stageNumber
                                )
                        );
                    }
                }
        );


        content.add(
                        title
                )
                .padBottom(18f)
                .row();


        content.add(
                        reason
                )
                .width(380f)
                .padBottom(25f)
                .row();


        content.add(
                        continueButton
                )
                .width(200f)
                .height(55f);


        darkLayer.add(
                        popup
                )
                .width(500f)
                .height(300f)
                .center();


        stage.addActor(
                darkLayer
        );


        darkLayer.toFront();
    }

    private String resolveWinnerRole(MatchEndedDto ended) {
        if (ended == null) {
            return null;
        }

        if (ended.getWinnerRole() != null
                && !ended.getWinnerRole().isBlank()) {
            return ended.getWinnerRole().trim().toUpperCase(
                    java.util.Locale.ROOT
            );
        }

        if (ended.getOutcome() == null) {
            return null;
        }

        return switch (ended.getOutcome().trim().toUpperCase(
                java.util.Locale.ROOT
        )) {
            case "PLANT_WON" -> "PLANT";
            case "ZOMBIE_WON" -> "ZOMBIE";
            default -> null;
        };
    }

    private String matchEndReason(
            MatchEndedDto ended,
            String winner
    ) {
        if (ended != null
                && ended.getReason() != null
                && !ended.getReason().isBlank()) {
            return ended.getReason();
        }

        if ("PLANT".equals(winner)) {
            return "At least one brain survived until time expired.";
        }

        if ("ZOMBIE".equals(winner)) {
            return "All brains were eaten.";
        }

        return "The match ended without a winner.";
    }



    private static String rootMessage(
            Throwable throwable
    ) {

        if (throwable == null) {

            return "Unknown network error.";
        }


        Throwable current =
                throwable;


        while (current.getCause() != null) {

            current =
                    current.getCause();
        }


        String message =
                current.getMessage();


        return message == null
                || message.isBlank()
                ? current
                .getClass()
                .getSimpleName()
                : message;
    }
}
