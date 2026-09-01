package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import com.badlogic.gdx.scenes.scene2d.ui.*;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

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

import network.protocol.match.ActionResultDto;
import network.protocol.match.GameActionDto;
import network.protocol.match.MatchEndedDto;
import network.protocol.match.MatchSnapshot;

import network.protocol.reaction.ReactionId;
import network.protocol.reaction.ReactionReceivedDto;
import network.protocol.reaction.ReactionSendDto;

import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

import views.graphical.screens.BaseScreen;

import views.graphical.ui.BorderedPanel;

import java.util.Objects;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

import network.protocol.match.GameActionDto;
import network.protocol.match.GameActionType;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public final class OnlineIZombieScreen extends BaseScreen {
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
    private static final float LOCAL_WORLD_HEIGHT = 600f;
    private static final float LOCAL_BOARD_X = 533f;
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

    private ReactionOverlay reactionOverlay;

    private OnlineIZombieControlPanel actionPanel;

    private Actor boardInputActor;


    private Label roleLabel;

    private Label opponentLabel;

    private Label timerLabel;

    private Label sunLabel;


    private boolean active;

    private int lastRenderedTick =
            -1;

    private boolean matchEnded;

    private String selectedCardName;

    private Label seedHintLabel;


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
                        boardTransform
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
                createPlacementInputLayer()
        );

        root.add(
                createSeedBankLayer()
        );

        root.add(
                createReactionControls()
        );


        reactionOverlay =
                new ReactionOverlay(
                        game
                );

        root.add(
                reactionOverlay
        );


        stage.addActor(
                root
        );

        boardInputActor =
                createBoardInputActor();

        stage.addActor(
                boardInputActor
        );


        actionPanel =
                new OnlineIZombieControlPanel(
                        game,
                        clientSession,
                        this::sendGameAction
                );

        actionPanel.setPosition(
                12f,
                80f
        );

        stage.addActor(
                actionPanel
        );

        actionPanel.toFront();
    }

    private Actor createBoardInputActor() {

        Actor actor =
                new Actor();


        actor.setBounds(
                boardArea.x(),
                boardArea.y(),
                boardArea.width(),
                boardArea.height()
        );


        actor.setTouchable(
                Touchable.enabled
        );


        actor.addListener(
                new InputListener() {

                    @Override
                    public boolean touchDown(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            int button
                    ) {

                        if (!active
                                || matchEnded
                                || !clientSession.isMatchRunning()
                                || actionPanel == null) {

                            return false;
                        }


                        int column =
                                (int) Math.floor(
                                        x
                                                / boardTransform.tileWidth()
                                );


                        int visualRowFromBottom =
                                (int) Math.floor(
                                        y
                                                / boardTransform.tileHeight()
                                );


                        if (column < 0
                                || column >= BoardTransform.COLUMNS
                                || visualRowFromBottom < 0
                                || visualRowFromBottom >= BoardTransform.ROWS) {

                            return false;
                        }


                        /*
                         * BoardTransform renders lane 0 at the top, while
                         * Scene2D local Y grows upward from the bottom.
                         */
                        int lane =
                                BoardTransform.ROWS
                                        - 1
                                        - visualRowFromBottom;


                        actionPanel.handleTileClick(
                                lane,
                                column
                        );


                        return true;
                    }
                }
        );


        return actor;
    }


    private void sendGameAction(
            GameActionDto action
    ) {

        if (action == null
                || matchEnded
                || !clientSession.isMatchRunning()) {

            return;
        }


        try {

            matchClientService
                    .sendAction(
                            action
                    )
                    .whenComplete(
                            (result, throwable) ->
                                    Gdx.app.postRunnable(
                                            () -> {

                                                if (!active) {
                                                    return;
                                                }


                                                if (throwable != null) {

                                                    String text =
                                                            "Action failed: "
                                                                    + rootMessage(
                                                                    throwable
                                                            );

                                                    if (actionPanel != null) {
                                                        actionPanel.setStatus(
                                                                text
                                                        );
                                                    }

                                                    game.notifyError(
                                                            text
                                                    );

                                                    return;
                                                }


                                                handleActionResult(
                                                        result
                                                );
                                            }
                                    )
                    );

        } catch (Exception exception) {

            String text =
                    "Action failed: "
                            + rootMessage(
                            exception
                    );


            if (actionPanel != null) {
                actionPanel.setStatus(
                        text
                );
            }


            game.notifyError(
                    text
            );
        }
    }


    private void handleActionResult(
            ActionResultDto result
    ) {

        if (result == null) {

            if (actionPanel != null) {
                actionPanel.setStatus(
                        "Server returned an empty action result."
                );
            }

            return;
        }


        if (!result.isAccepted()) {

            String reason =
                    result.getReason() == null
                            || result.getReason().isBlank()
                            ? "Action rejected by server."
                            : result.getReason();


            if (actionPanel != null) {
                actionPanel.setStatus(
                        reason
                );
            }


            game.notifyError(
                    reason
            );

            return;
        }


        if (actionPanel != null) {

            actionPanel.onActionAccepted();
        }
    }


    private Table createReactionControls() {

        Table layer =
                new Table();

        layer.setFillParent(
                true
        );

        layer.bottom()
                .right();


        Table panel =
                new Table();

        panel.pad(
                8f
        );

        panel.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0.08f,
                                0.08f,
                                0.08f,
                                0.82f
                        )
                )
        );


        addReactionButton(
                panel,
                "GOOD LUCK!",
                ReactionId.GOOD_LUCK
        );

        addReactionButton(
                panel,
                "NICE MOVE!",
                ReactionId.NICE_MOVE
        );

        addReactionButton(
                panel,
                "OH NO!",
                ReactionId.OH_NO
        );


        panel.row();


        addReactionButton(
                panel,
                "\uD83D\uDE42",
                ReactionId.SMILE
        );

        addReactionButton(
                panel,
                "\uD83D\uDE02",
                ReactionId.LAUGH
        );

        addReactionButton(
                panel,
                "\uD83D\uDE31",
                ReactionId.SHOCKED
        );


        layer.add(
                        panel
                )
                .padRight(18f)
                .padBottom(16f);


        return layer;
    }


    private void addReactionButton(
            Table table,
            String caption,
            ReactionId reactionId
    ) {

        TextButton button =
                new TextButton(
                        caption,
                        game.getSkin(),
                        "green"
                );


        button.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        sendReaction(
                                reactionId
                        );
                    }
                }
        );


        table.add(
                        button
                )
                .width(126f)
                .height(42f)
                .pad(3f);
    }


    private void sendReaction(
            ReactionId reactionId
    ) {

        if (reactionId == null
                || matchEnded
                || !clientSession.isMatchRunning()) {

            return;
        }


        ReactionSendDto dto =
                new ReactionSendDto(
                        clientSession.getMatchId(),
                        reactionId
                );


        try {

            matchClientService
                    .sendReaction(
                            dto
                    )
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(
                                            () -> {

                                                if (!active) {
                                                    return;
                                                }


                                                if (throwable != null) {

                                                    game.notifyError(
                                                            "Could not send reaction: "
                                                                    + rootMessage(
                                                                    throwable
                                                            )
                                                    );

                                                    return;
                                                }


                                                if (response == null) {

                                                    game.notifyError(
                                                            "Server returned an empty reaction response."
                                                    );

                                                    return;
                                                }


                                                if (!response.success()) {

                                                    game.notifyError(
                                                            response.message() == null
                                                                    ? "Reaction was rejected by the server."
                                                                    : response.message()
                                                    );
                                                }
                                            }
                                    )
                    );

        } catch (Exception exception) {

            game.notifyError(
                    "Could not send reaction: "
                            + rootMessage(
                            exception
                    )
            );
        }
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

            sunLabel.setText(
                    Integer.toString(
                            snapshot.getPlantSun()
                    )
            );

        } else {

            sunLabel.setText(
                    Integer.toString(
                            snapshot.getZombieSun()
                    )
            );
        }


        if (actionPanel != null) {

            actionPanel.updateFromSnapshot(
                    snapshot
            );
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
                    == MessageType.REACTION_RECEIVED) {

                ReactionReceivedDto reaction =
                        codec.decodePayload(
                                message.getPayload(),
                                ReactionReceivedDto.class
                        );


                if (reaction == null
                        || reaction.matchId() == null
                        || !reaction.matchId().equals(
                        clientSession.getMatchId()
                )) {

                    return;
                }


                /*
                 * The server should only forward the opponent's
                 * reactions. This extra check prevents a malformed
                 * event from being rendered as a remote reaction.
                 */
                String opponent =
                        clientSession.getOpponentUsername();


                if (opponent != null
                        && reaction.senderUsername() != null
                        && !opponent.equalsIgnoreCase(
                        reaction.senderUsername()
                )) {

                    return;
                }


                if (reactionOverlay != null) {

                    reactionOverlay.showReaction(
                            reaction.senderUsername(),
                            reaction.reactionId()
                    );
                }


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


                if (actionPanel != null) {
                    actionPanel.setDisabled(
                            true
                    );
                }


                if (boardInputActor != null) {
                    boardInputActor.setTouchable(
                            Touchable.disabled
                    );
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


        String winner =
                ended.getWinnerRole();


        boolean won =
                winner != null
                        && winner.equals(
                        clientSession.getRole()
                );


        Label title =
                new Label(
                        won
                                ? "YOU WIN!"
                                : "YOU LOSE!",
                        game.getSkin().get(
                                "big_outline",
                                Label.LabelStyle.class
                        )
                );


        title.setColor(
                won
                        ? Color.valueOf("FFE16A")
                        : Color.valueOf("FF8A70")
        );


        Label reason =
                new Label(
                        ended.getReason() == null
                                ? "Match finished."
                                : ended.getReason(),
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

    private Actor createPlacementInputLayer() {
        Actor layer = new Actor() {
            @Override
            public Actor hit(float hx, float hy, boolean touchableCheck) {
                if (touchableCheck && getTouchable() != Touchable.enabled) {
                    return null;
                }
                BoardArea area = boardTransform.getArea();
                if (hx < area.x()
                        || hx > area.x() + area.width()
                        || hy < area.y()
                        || hy > area.y() + area.height()) {
                    return null;
                }
                return this;
            }
        };
        layer.setTouchable(Touchable.enabled);
        layer.addListener(new InputListener() {
            @Override
            public boolean touchDown(
                    InputEvent event,
                    float tx,
                    float ty,
                    int pointer,
                    int buttonIndex
            ) {
                return handleBoardTouch(tx, ty);
            }
        });
        return layer;
    }

    private boolean handleBoardTouch(float px, float py) {
        BoardArea area = boardTransform.getArea();
        if (px < area.x()
                || px > area.x() + area.width()
                || py < area.y()
                || py > area.y() + area.height()) {
            return false;
        }

        float tileW = area.width() / BoardTransform.COLUMNS;
        float tileH = area.height() / BoardTransform.ROWS;

        int column = (int) ((px - area.x()) / tileW);
        int laneFromBottom = (int) ((py - area.y()) / tileH);
        int lane = (BoardTransform.ROWS - 1) - laneFromBottom;

        column = Math.max(0, Math.min(BoardTransform.COLUMNS - 1, column));
        lane = Math.max(0, Math.min(BoardTransform.ROWS - 1, lane));

        if (selectedCardName == null) {
            game.notifyError("Select a card first.");
            return true;
        }

        sendPlaceAction(lane, column);
        return true;
    }

    private void sendPlaceAction(int lane, int column) {
        if (selectedCardName == null) {
            return;
        }

        GameActionType type = isPlantRole()
                ? GameActionType.PLACE_PLANT
                : GameActionType.PLACE_ZOMBIE;

        GameActionDto action = new GameActionDto(
                type,
                selectedCardName,
                lane,
                column,
                UUID.randomUUID().toString()
        );

        try {
            matchClientService.sendAction(action)
                    .whenComplete((result, error) ->
                            Gdx.app.postRunnable(() -> {
                                if (error != null) {
                                    game.notifyError("Could not send action.");
                                    return;
                                }
                                if (result != null && !result.isAccepted()) {
                                    String reason = result.getReason();
                                    game.notifyError(
                                            reason == null
                                                    ? "Action rejected."
                                                    : reason
                                    );
                                }
                            }));
        } catch (IOException exception) {
            game.notifyError("Not connected to the server.");
        }
    }

    private boolean isPlantRole() {
        return "PLANT".equalsIgnoreCase(clientSession.getRole());
    }

    private Table createSeedBankLayer() {
        Table layer = new Table();
        layer.setFillParent(true);
        layer.bottom();
        layer.setTouchable(Touchable.childrenOnly);

        Table bank = new Table();

        seedHintLabel = new Label(
                "Select a card, then click a tile",
                game.getSkin()
        );

        bank.add(seedHintLabel)
                .padBottom(6f)
                .row();

        Table cards = new Table();

        for (CardEntry card : rosterForRole()) {
            TextButton button = new TextButton(
                    card.name() + "   " + card.cost(),
                    game.getSkin()
            );

            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selectedCardName = card.name();
                    seedHintLabel.setText("Selected: " + card.name());
                }
            });

            cards.add(button)
                    .width(140f)
                    .height(52f)
                    .pad(4f);
        }

        bank.add(cards);

        layer.add(bank)
                .padBottom(14f);

        return layer;
    }

    private List<CardEntry> rosterForRole() {
        List<CardEntry> cards = new ArrayList<>();

        if (isPlantRole()) {
            String[] names = {
                    "Sunflower",
                    "Peashooter",
                    "Snow Pea",
                    "Repeater",
                    "Wall-nut"
            };

            for (String name : names) {
                PlantData data = findPlant(name);
                if (data != null) {
                    cards.add(new CardEntry(data.name(), data.cost()));
                }
            }
        } else {
            for (Map.Entry<String, Integer> entry
                    : zombieRoster(currentStage()).entrySet()) {
                cards.add(new CardEntry(entry.getKey(), entry.getValue()));
            }
        }

        return cards;
    }

    private PlantData findPlant(String name) {
        for (PlantData data : PlantRegistry.getAll()) {
            if (data.name().equalsIgnoreCase(name)) {
                return data;
            }
        }
        return null;
    }

    private int currentStage() {
        int stage = clientSession.getStageNumber();
        return (stage >= 1 && stage <= 3) ? stage : 1;
    }

    private Map<String, Integer> zombieRoster(int stage) {
        LinkedHashMap<String, Integer> roster = new LinkedHashMap<>();

        switch (stage) {
            case 2 -> {
                roster.put("ZombieExplorer", 75);
                roster.put("ZombieBeachSnorkel", 75);
                roster.put("ZombieIceAgeHunter", 100);
                roster.put("ZombieProspector", 125);
                roster.put("ZombieModernAllStar", 150);
            }
            case 3 -> {
                roster.put("ZombieDefault", 50);
                roster.put("ZombieBeachOctopus", 125);
                roster.put("ZombieWizard", 150);
                roster.put("ZombiePiano", 150);
                roster.put("ZombieGargantuar", 300);
            }
            default -> {
                roster.put("ZombieImp", 25);
                roster.put("ZombieDefault", 50);
                roster.put("ZombieNewspaper", 75);
                roster.put("ZombieIceAgeDodo", 100);
                roster.put("ZombieDarkJuggler", 125);
            }
        }

        return roster;
    }

    private record CardEntry(String name, int cost) {
    }
}
