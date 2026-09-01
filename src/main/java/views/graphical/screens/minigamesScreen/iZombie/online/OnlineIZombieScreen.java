package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import com.badlogic.gdx.scenes.scene2d.ui.*;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

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

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;


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
    private static final String SHOVEL =
            "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
    private static final String SHOVEL_CLICKED =
            "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON_DOWN";
    private static final String SHOVEL_CURSOR =
            "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";
    private static final String PLANT_FOOD_BUTTON =
            "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";
    private static final String PLANT_FOOD_BUTTON_DOWN =
            "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON_DOWN";

    private static final String REACTION_SMILE_PATH =
            "assets/UIs/reactions/reaction_smile.png";
    private static final String REACTION_LAUGH_PATH =
            "assets/UIs/reactions/reaction_laugh.png";
    private static final String REACTION_SHOCKED_PATH =
            "assets/UIs/reactions/reaction_shocked.png";
    private static final float TOOL_CURSOR_ALPHA = 0.58f;
    private static final float TOOL_CURSOR_SCALE = 0.65f;
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

    private ImageButton shovelButton;
    private ImageButton plantFoodButton;
    private ButtonGroup<ImageButton> plantToolGroup;
    private Image toolCursorPreview;
    private final Vector2 toolCursorPosition = new Vector2();

    private final Map<ReactionId, Texture> reactionTextures =
            new EnumMap<>(ReactionId.class);

    private final Map<ReactionId, Drawable> reactionDrawables =
            new EnumMap<>(ReactionId.class);


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


        loadReactionAssets();

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

        /*
         * Keep match/time text out of the middle of the lawn.
         * It now sits in the top-left HUD area, just to the right of the sun bank.
         */
        matchInfoLayer.top()
                .left();


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
                .padLeft(150f);


        root.add(
                matchInfoLayer
        );

        actionPanel =
                new OnlineIZombieControlPanel(
                        game,
                        clientSession,
                        this::sendGameAction
                );


        if (clientSession.isPlantPlayer()) {
            root.add(
                    createPlantToolControls()
            );
        }


        root.add(
                createReactionControls()
        );


        reactionOverlay =
                new ReactionOverlay(
                        game,
                        reactionDrawables
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


        float controlsX =
                boardArea.x()
                        - actionPanel.getWidth()
                        - 12f;

        float controlsY =
                boardArea.y()
                        + (boardArea.height()
                        - actionPanel.getHeight()) / 2f;

        actionPanel.setPosition(
                Math.max(8f, controlsX),
                Math.max(8f, controlsY)
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

        resetPlantToolSelection();
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

        panel.setTouchable(
                Touchable.childrenOnly
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


        addReactionImageButton(
                panel,
                ReactionId.SMILE
        );

        addReactionImageButton(
                panel,
                ReactionId.LAUGH
        );

        addReactionImageButton(
                panel,
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
                .width(112f)
                .height(38f)
                .pad(3f);
    }


    private void addReactionImageButton(
            Table table,
            ReactionId reactionId
    ) {
        Drawable drawable =
                reactionDrawables.get(
                        reactionId
                );

        if (drawable == null) {
            return;
        }

        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.imageUp = drawable;
        style.imageDown = drawable;
        style.imageOver = drawable;

        ImageButton button =
                new ImageButton(
                        style
                );

        button.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        sendReaction(
                                reactionId
                        );
                    }
                }
        );

        table.add(button)
                .size(48f)
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


    private void loadReactionAssets() {
        loadReactionAsset(
                ReactionId.SMILE,
                REACTION_SMILE_PATH
        );

        loadReactionAsset(
                ReactionId.LAUGH,
                REACTION_LAUGH_PATH
        );

        loadReactionAsset(
                ReactionId.SHOCKED,
                REACTION_SHOCKED_PATH
        );
    }

    private void loadReactionAsset(
            ReactionId reactionId,
            String path
    ) {
        Texture texture =
                new Texture(
                        Gdx.files.internal(
                                path
                        )
                );

        texture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
        );

        reactionTextures.put(
                reactionId,
                texture
        );

        reactionDrawables.put(
                reactionId,
                new TextureRegionDrawable(
                        new TextureRegion(
                                texture
                        )
                )
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

        hideToolCursor();


        networkClient.setEventListener(
                null
        );


        super.hide();
    }


    @Override
    public void dispose() {
        super.dispose();

        for (Texture texture :
                reactionTextures.values()) {

            if (texture != null) {
                texture.dispose();
            }
        }

        reactionTextures.clear();
        reactionDrawables.clear();
    }


    @Override
    public void render(
            float delta
    ) {

        applyLatestSnapshot();
        updateToolCursorPosition();


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
                                / Math.max(
                                1,
                                snapshot.getTicksPerSecond()
                        )
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

                resetPlantToolSelection();


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

    private Table createPlantToolControls() {

        Table layer = new Table();
        layer.setFillParent(true);
        layer.top().right();
        layer.setTouchable(Touchable.childrenOnly);

        Table buttons = new Table();

        shovelButton = createHudImageButton(
                SHOVEL,
                SHOVEL_CLICKED
        );

        plantFoodButton = createHudImageButton(
                PLANT_FOOD_BUTTON,
                PLANT_FOOD_BUTTON_DOWN
        );

        plantToolGroup = new ButtonGroup<>();
        plantToolGroup.setMinCheckCount(0);
        plantToolGroup.setMaxCheckCount(1);
        plantToolGroup.setUncheckLast(true);
        plantToolGroup.add(shovelButton, plantFoodButton);

        shovelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (shovelButton.isChecked()) {
                    plantFoodButton.setChecked(false);
                    actionPanel.activatePluckMode();
                    showToolCursor(SHOVEL_CURSOR);
                } else {
                    actionPanel.activatePlaceMode();
                    hideToolCursor();
                }
            }
        });

        plantFoodButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (plantFoodButton.isChecked()) {
                    shovelButton.setChecked(false);
                    actionPanel.activateFeedMode();
                    showToolCursor(PLANT_FOOD_BUTTON);
                } else {
                    actionPanel.activatePlaceMode();
                    hideToolCursor();
                }
            }
        });

        buttons.add(shovelButton)
                .size(58f)
                .padRight(5f);

        buttons.add(plantFoodButton)
                .size(58f);

        layer.add(buttons)
                .padTop(8f)
                .padRight(12f);

        return layer;
    }

    private ImageButton createHudImageButton(
            String normalId,
            String pressedId
    ) {
        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        Drawable normal = requireDrawable(normalId);
        Drawable pressed = optionalDrawable(pressedId, normal);

        style.imageUp = normal;
        style.imageDown = pressed;
        style.imageOver = pressed;
        style.imageChecked = pressed;

        return new ImageButton(style);
    }

    private Drawable requireDrawable(String assetId) {
        TextureRegion region = tryRegion(assetId);
        if (region == null) {
            throw new IllegalStateException(
                    "Missing HUD asset: " + assetId
            );
        }
        return new TextureRegionDrawable(region);
    }

    private Drawable optionalDrawable(
            String assetId,
            Drawable fallback
    ) {
        TextureRegion region = tryRegion(assetId);
        return region == null
                ? fallback
                : new TextureRegionDrawable(region);
    }

    private TextureRegion tryRegion(String assetId) {
        try {
            return game.getTextureBank().region(assetId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void resetPlantToolSelection() {
        if (plantToolGroup != null) {
            plantToolGroup.uncheckAll();
        }

        if (actionPanel != null && clientSession.isPlantPlayer()) {
            actionPanel.activatePlaceMode();
        }

        hideToolCursor();
    }

    private void showToolCursor(String assetId) {
        TextureRegion region = tryRegion(assetId);
        if (region == null) {
            return;
        }

        if (toolCursorPreview == null) {
            toolCursorPreview = new Image();
            toolCursorPreview.setTouchable(Touchable.disabled);
            stage.addActor(toolCursorPreview);
        }

        toolCursorPreview.setDrawable(
                new TextureRegionDrawable(region)
        );

        toolCursorPreview.setSize(
                region.getRegionWidth() * TOOL_CURSOR_SCALE,
                region.getRegionHeight() * TOOL_CURSOR_SCALE
        );

        toolCursorPreview.setColor(
                1f,
                1f,
                1f,
                TOOL_CURSOR_ALPHA
        );

        toolCursorPreview.setVisible(true);
        toolCursorPreview.toFront();
    }

    private void hideToolCursor() {
        if (toolCursorPreview != null) {
            toolCursorPreview.setVisible(false);
        }
    }

    private void updateToolCursorPosition() {
        if (toolCursorPreview == null
                || !toolCursorPreview.isVisible()) {
            return;
        }

        toolCursorPosition.set(
                Gdx.input.getX(),
                Gdx.input.getY()
        );

        stage.screenToStageCoordinates(
                toolCursorPosition
        );

        toolCursorPreview.setPosition(
                toolCursorPosition.x
                        - toolCursorPreview.getWidth() / 2f,
                toolCursorPosition.y
                        - toolCursorPreview.getHeight() / 2f
        );

        toolCursorPreview.toFront();
    }
}
