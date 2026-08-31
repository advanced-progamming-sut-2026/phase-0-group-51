package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import graphics.PvzGame;

import models.minigames.MinigameType;

import network.client.ClientSession;
import network.client.NetworkClient;
import network.client.service.MatchClientService;

import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;

import network.protocol.match.MatchEndedDto;
import network.protocol.match.MatchStartDto;

import network.protocol.matchmaking.InviteReceived;
import network.protocol.matchmaking.MatchFoundDto;

import views.graphical.screens.BaseScreen;
import views.graphical.screens.minigamesScreen.iZombie.IZombieScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.BorderedPanel;

import java.io.IOException;


public final class OnlineIZombieLobbyScreen
        extends BaseScreen {

    private static final String BG_LEFT =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_LEFT";

    private static final String BG_MID =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE";

    private static final String BG_RIGHT =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_RIGHT";

    private static final String TEXT_FIELD =
            "IMAGE_UI_GENERIC_BUTTON_GENERIC_LTECURRENCY";

    private static final String BACK =
            "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";

    private static final String BACK_PRESSED =
            "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";

    private static final String TOPPER =
            "IMAGE_UI_PAUSEMENU_WINDOWTOPPER";

    private static final String SUNFLOWER_TOPPER =
            "IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER";


    private final int stageNumber;
    private final ClientSession clientSession = new ClientSession();
    private final NetworkJsonCodec codec = new NetworkJsonCodec();
    private NetworkClient networkClient;
    private MatchClientService matchClientService;
    private TextField usernameField;
    private TextButton challengeButton;
    private TextButton randomButton;
    private TextButton cancelQueueButton;
    private TextButton localButton;
    private Label statusLabel;
    private Table challengeModalLayer;
    private ChallengePopup challengePopup;
    private boolean active;
    private boolean waitingInQueue;
    private boolean invitePending;
    private boolean matchCommitted;

    public OnlineIZombieLobbyScreen(PvzGame game, int stageNumber) {
        super(game);
        this.stageNumber = stageNumber;
        buildUi();
    }

    private void buildUi() {
        Stack root = new Stack();
        root.setFillParent(true);
        root.add(createIZombieBackground());

        Table shade = new Table();
        shade.setFillParent(true);
        shade.setTouchable(Touchable.disabled);
        shade.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0f,
                                0f,
                                0f,
                                0.22f
                        )
                )
        );


        root.add(shade);

        BorderedPanel panel =
                new BorderedPanel(
                        game,
                        Color.valueOf("8F4909")
                );


        buildPanel(
                panel
        );


        Table panelLayer =
                new Table();

        panelLayer.setFillParent(
                true
        );

        panelLayer.add(
                        panel
                )
                .width(620f)
                .height(590f)
                .center();


        root.add(
                panelLayer
        );

        ImageButton backButton =
                createBackButton();


        backButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        handleBack();
                    }
                }
        );


        Container<ImageButton> backContainer =
                new Container<>(
                        backButton
                );

        backContainer.setFillParent(
                true
        );

        backContainer.top()
                .left();

        backContainer.padTop(
                15f
        );

        backContainer.padLeft(
                15f
        );


        root.add(
                backContainer
        );


        stage.addActor(
                root
        );
    }


    private void buildPanel(
            BorderedPanel panel
    ) {

        Table content =
                panel.getContent();

        content.clearChildren();

        content.top();

        content.pad(
                10f,
                28f,
                25f,
                28f
        );

        content.add(
                        createTopDecoration()
                )
                .width(470f)
                .height(105f)
                .padTop(-48f)
                .padBottom(-10f)
                .row();

        Label title =
                new Label(
                        "I, ZOMBIE ONLINE",
                        game.getSkin().get(
                                "big_outline",
                                Label.LabelStyle.class
                        )
                );


        title.setColor(
                Color.valueOf("FFE16A")
        );

        title.setAlignment(
                Align.center
        );


        content.add(
                        title
                )
                .growX()
                .center()
                .padBottom(4f)
                .row();


        Label subtitle =
                new Label(
                        "FIND AN OPPONENT",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        subtitle.setColor(
                Color.valueOf("F6E8B1")
        );

        subtitle.setAlignment(
                Align.center
        );


        content.add(
                        subtitle
                )
                .growX()
                .center()
                .padBottom(16f)
                .row();

        Table challengePaper =
                createPaper();


        Label challengeLabel =
                new Label(
                        "Challenge a Player",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        challengeLabel.setColor(
                Color.valueOf("6A3E22")
        );

        challengeLabel.setAlignment(
                Align.center
        );


        challengePaper.add(
                        challengeLabel
                )
                .growX()
                .padBottom(8f)
                .row();


        usernameField =
                createUsernameField();


        challengePaper.add(
                        usernameField
                )
                .width(390f)
                .height(57f)
                .padBottom(8f)
                .row();


        challengeButton =
                new TextButton(
                        "CHALLENGE",
                        game.getSkin(),
                        "green"
                );


        challengeButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        sendChallenge();
                    }
                }
        );


        challengePaper.add(
                        challengeButton
                )
                .width(220f)
                .height(52f);


        content.add(
                        challengePaper
                )
                .width(465f)
                .padBottom(9f)
                .row();

        Label or =
                new Label(
                        "-  OR  -",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        or.setColor(
                Color.valueOf("FFE16A")
        );

        or.setAlignment(
                Align.center
        );


        content.add(
                        or
                )
                .center()
                .padBottom(7f)
                .row();

        randomButton =
                new TextButton(
                        "RANDOM MATCH",
                        game.getSkin(),
                        "purple"
                );


        randomButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        joinRandomQueue();
                    }
                }
        );


        content.add(
                        randomButton
                )
                .width(260f)
                .height(55f)
                .padBottom(5f)
                .row();


        cancelQueueButton =
                new TextButton(
                        "CANCEL QUEUE",
                        game.getSkin(),
                        "brown"
                );


        cancelQueueButton.setVisible(
                false
        );


        cancelQueueButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        leaveRandomQueue();
                    }
                }
        );


        content.add(
                        cancelQueueButton
                )
                .width(220f)
                .height(46f)
                .padBottom(9f)
                .row();

        Table statusPaper =
                createStatusPaper();


        statusLabel =
                new Label(
                        "Connecting to server...",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        statusLabel.setColor(
                Color.valueOf("51472B")
        );

        statusLabel.setAlignment(
                Align.center
        );

        statusLabel.setWrap(
                true
        );


        statusPaper.add(
                        statusLabel
                )
                .width(420f)
                .center();


        content.add(
                        statusPaper
                )
                .width(465f)
                .height(55f)
                .padBottom(9f)
                .row();

        localButton =
                new TextButton(
                        "PLAY LOCAL",
                        game.getSkin(),
                        "brown"
                );


        localButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        handlePlayLocal();
                    }
                }
        );


        content.add(
                        localButton
                )
                .width(190f)
                .height(45f);


        setPrimaryControlsEnabled(
                false
        );
    }

    @Override
    public void show() {

        super.show();

        active =
                true;


        game.hideHud();


        connectNetwork();
    }


    @Override
    public void hide() {
        active = false;
        detachEventListener();
        super.hide();
    }

    private void connectNetwork() {
        setStatus("Connecting to server...");


        setPrimaryControlsEnabled(
                false
        );


        game.getNetworkManager()
                .ensureConnectedAsync()
                .whenComplete(
                        (ignored, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> {

                                            if (!active) {
                                                return;
                                            }


                                            if (throwable != null) {

                                                setStatus(
                                                        "Server unavailable."
                                                );


                                                game.notifyError(
                                                        rootMessage(
                                                                throwable
                                                        )
                                                );

                                                return;
                                            }


                                            try {

                                                networkClient =
                                                        game
                                                                .getNetworkManager()
                                                                .getNetworkClient();


                                                matchClientService =
                                                        new MatchClientService(
                                                                networkClient
                                                        );


                                                networkClient.setEventListener(
                                                        this::receiveServerEvent
                                                );


                                                setStatus(
                                                        "Connected. Choose an opponent."
                                                );


                                                setPrimaryControlsEnabled(
                                                        true
                                                );

                                            } catch (
                                                    RuntimeException exception
                                            ) {

                                                setStatus(
                                                        "Network initialization failed."
                                                );


                                                game.notifyError(
                                                        rootMessage(
                                                                exception
                                                        )
                                                );
                                            }
                                        }
                                )
                );
    }

    private void sendChallenge() {
        if (matchClientService == null || waitingInQueue || invitePending || matchCommitted) {
            return;
        }
        String target = usernameField.getText().trim();
        if (target.isBlank()) {
            game.notifyError(
                    "Enter an opponent username."
            );

            return;
        }


        invitePending =
                true;


        setPrimaryControlsEnabled(
                false
        );


        setStatus(
                "Sending challenge to "
                        + target
                        + "..."
        );


        try {

            matchClientService
                    .challenge(
                            target
                    )
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(
                                            () -> {

                                                if (!active) {
                                                    return;
                                                }


                                                if (throwable != null) {

                                                    invitePending =
                                                            false;


                                                    setPrimaryControlsEnabled(
                                                            true
                                                    );


                                                    setStatus(
                                                            "Challenge failed."
                                                    );


                                                    game.notifyError(
                                                            rootMessage(
                                                                    throwable
                                                            )
                                                    );

                                                    return;
                                                }


                                                if (!response.success()) {

                                                    invitePending =
                                                            false;


                                                    setPrimaryControlsEnabled(
                                                            true
                                                    );


                                                    setStatus(
                                                            response.message()
                                                    );


                                                    game.notifyError(
                                                            response.message()
                                                    );

                                                    return;
                                                }


                                                setStatus(
                                                        response.message()
                                                                + " Waiting for response..."
                                                );
                                            }
                                    )
                    );

        } catch (IOException exception) {

            invitePending =
                    false;


            setPrimaryControlsEnabled(
                    true
            );


            setStatus(
                    "Challenge failed."
            );


            game.notifyError(
                    exception.getMessage()
            );
        }
    }

    private void joinRandomQueue() {

        if (matchClientService == null || waitingInQueue || invitePending || matchCommitted) {
            return;
        }
        setPrimaryControlsEnabled(false);
        setStatus("Joining random queue...");
        try {

            matchClientService
                    .joinRandomQueue()
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(
                                            () -> {

                                                if (!active) {
                                                    return;
                                                }


                                                if (throwable != null) {

                                                    waitingInQueue =
                                                            false;


                                                    cancelQueueButton.setVisible(
                                                            false
                                                    );


                                                    setPrimaryControlsEnabled(
                                                            true
                                                    );


                                                    setStatus(
                                                            "Could not join queue."
                                                    );


                                                    game.notifyError(
                                                            rootMessage(
                                                                    throwable
                                                            )
                                                    );

                                                    return;
                                                }

                                                if (matchCommitted
                                                        || clientSession.isInMatch()) {

                                                    waitingInQueue =
                                                            false;


                                                    cancelQueueButton.setVisible(
                                                            false
                                                    );

                                                    return;
                                                }


                                                waitingInQueue =
                                                        response.waiting();


                                                cancelQueueButton.setVisible(
                                                        waitingInQueue
                                                );


                                                setStatus(
                                                        response.message()
                                                );


                                                if (waitingInQueue) {

                                                    setPrimaryControlsEnabled(
                                                            false
                                                    );


                                                    cancelQueueButton.setDisabled(
                                                            false
                                                    );

                                                } else {

                                                    setPrimaryControlsEnabled(
                                                            true
                                                    );
                                                }
                                            }
                                    )
                    );

        } catch (IOException exception) {

            waitingInQueue =
                    false;


            cancelQueueButton.setVisible(
                    false
            );


            setPrimaryControlsEnabled(
                    true
            );


            setStatus(
                    "Could not join queue."
            );


            game.notifyError(
                    exception.getMessage()
            );
        }
    }


    private void leaveRandomQueue() {

        leaveQueueThen(
                null
        );
    }


    private void leaveQueueThen(
            Runnable afterLeave
    ) {

        if (!waitingInQueue) {

            if (afterLeave != null) {
                afterLeave.run();
            }

            return;
        }


        if (matchClientService == null) {

            game.notifyError(
                    "Network is not ready."
            );

            return;
        }


        cancelQueueButton.setDisabled(
                true
        );


        setStatus(
                "Leaving queue..."
        );


        try {

            matchClientService
                    .leaveRandomQueue()
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(
                                            () -> {

                                                if (!active) {
                                                    return;
                                                }


                                                cancelQueueButton.setDisabled(
                                                        false
                                                );


                                                if (throwable != null) {

                                                    setStatus(
                                                            "Could not leave queue."
                                                    );


                                                    game.notifyError(
                                                            rootMessage(
                                                                    throwable
                                                            )
                                                    );

                                                    return;
                                                }


                                                if (!response.success()) {

                                                    setStatus(
                                                            response.message()
                                                    );


                                                    game.notifyError(
                                                            response.message()
                                                    );

                                                    return;
                                                }


                                                waitingInQueue =
                                                        false;


                                                cancelQueueButton.setVisible(
                                                        false
                                                );


                                                setPrimaryControlsEnabled(
                                                        true
                                                );


                                                setStatus(
                                                        response.message()
                                                );


                                                if (afterLeave != null) {

                                                    afterLeave.run();
                                                }
                                            }
                                    )
                    );

        } catch (IOException exception) {

            cancelQueueButton.setDisabled(
                    false
            );


            game.notifyError(
                    exception.getMessage()
            );
        }
    }

    private void receiveServerEvent(
            NetworkMessage message
    ) {

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
                    == MessageType.MATCHMAKING_INVITE_RECEIVED) {

                InviteReceived invite =
                        codec.decodePayload(
                                message.getPayload(),
                                InviteReceived.class
                        );


                invitePending =
                        true;


                setPrimaryControlsEnabled(
                        false
                );


                setStatus(
                        "Challenge received from "
                                + invite.challengerUsername()
                );


                showChallengeModal(
                        invite.challengerUsername()
                );

                return;
            }

            if (message.getType()
                    == MessageType.MATCHMAKING_INVITE_REJECTED) {

                invitePending =
                        false;


                closeChallengeModal();


                setPrimaryControlsEnabled(
                        true
                );


                setStatus(
                        "Challenge rejected or cancelled."
                );


                return;
            }

            if (message.getType()
                    == MessageType.MATCHMAKING_MATCH_FOUND) {

                MatchFoundDto found =
                        codec.decodePayload(
                                message.getPayload(),
                                MatchFoundDto.class
                        );


                if (!clientSession.applyMatchFound(
                        found
                )) {

                    game.notifyError(
                            "Received invalid match information."
                    );

                    return;
                }


                waitingInQueue =
                        false;

                invitePending =
                        false;

                matchCommitted =
                        true;


                closeChallengeModal();


                cancelQueueButton.setVisible(
                        false
                );


                setPrimaryControlsEnabled(
                        false
                );


                setStatus(
                        "Opponent: "
                                + found.opponentUsername()
                                + "   |   Role: "
                                + found.role()
                );

                return;
            }

            if (message.getType() == MessageType.MATCH_START) {
                MatchStartDto start = codec.decodePayload(message.getPayload(), MatchStartDto.class);

                if (!clientSession.applyMatchStart(start)) {
                    game.notifyError("Received invalid MATCH_START.");
                    return;
                }
                matchCommitted = true;
                setStatus("Match starting... Role: " + start.getRole());
                game.showScreen(new OnlineIZombieScreen(game, clientSession, networkClient));
                return;
            }

            if (message.getType()
                    == MessageType.MATCH_ENDED) {

                MatchEndedDto ended =
                        codec.decodePayload(
                                message.getPayload(),
                                MatchEndedDto.class
                        );


                clientSession.applyMatchEnded(
                        ended
                );


                matchCommitted =
                        false;

                invitePending =
                        false;

                waitingInQueue =
                        false;


                closeChallengeModal();


                setPrimaryControlsEnabled(
                        true
                );


                String reason =
                        ended.getReason();


                if (reason == null
                        || reason.isBlank()) {

                    reason =
                            "Match ended.";
                }


                setStatus(
                        reason
                );
            }

        } catch (Exception exception) {

            game.notifyError(
                    "Could not process server event: "
                            + rootMessage(
                            exception
                    )
            );
        }
    }

    private void showChallengeModal(
            String challengerUsername
    ) {

        closeChallengeModal();


        challengeModalLayer =
                new Table();

        challengeModalLayer.setFillParent(
                true
        );


        challengeModalLayer.setTouchable(
                Touchable.enabled
        );


        challengeModalLayer.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0f,
                                0f,
                                0f,
                                0.68f
                        )
                )
        );

        challengeModalLayer.addListener(
                new InputListener() {

                    @Override
                    public boolean touchDown(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            int button
                    ) {

                        return true;
                    }
                }
        );


        challengePopup =
                new ChallengePopup(
                        game,
                        challengerUsername,
                        accepted -> {

                            closeChallengeModal();


                            respondToChallenge(
                                    challengerUsername,
                                    accepted
                            );
                        }
                );


        challengeModalLayer.add(
                        challengePopup
                )
                .width(540f)
                .height(350f)
                .center();


        stage.addActor(
                challengeModalLayer
        );


        challengeModalLayer.toFront();
    }


    private void closeChallengeModal() {

        if (challengeModalLayer != null) {

            challengeModalLayer.remove();

            challengeModalLayer =
                    null;
        }


        challengePopup =
                null;
    }


    private void respondToChallenge(
            String challengerUsername,
            boolean accepted
    ) {

        if (matchClientService == null) {

            invitePending =
                    false;


            setPrimaryControlsEnabled(
                    true
            );

            return;
        }


        setStatus(
                accepted
                        ? "Accepting challenge..."
                        : "Rejecting challenge..."
        );


        try {

            matchClientService
                    .respondToInvite(
                            challengerUsername,
                            accepted
                    )
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(
                                            () -> {

                                                if (!active) {
                                                    return;
                                                }


                                                if (throwable != null) {

                                                    invitePending =
                                                            false;


                                                    setPrimaryControlsEnabled(
                                                            true
                                                    );


                                                    setStatus(
                                                            "Could not respond to challenge."
                                                    );


                                                    game.notifyError(
                                                            rootMessage(
                                                                    throwable
                                                            )
                                                    );

                                                    return;
                                                }


                                                setStatus(
                                                        response.message()
                                                );

                                                if (!accepted
                                                        || !response.success()) {

                                                    invitePending =
                                                            false;


                                                    setPrimaryControlsEnabled(
                                                            true
                                                    );
                                                }


                                                if (!response.success()) {

                                                    game.notifyError(
                                                            response.message()
                                                    );
                                                }
                                            }
                                    )
                    );

        } catch (IOException exception) {

            invitePending =
                    false;


            setPrimaryControlsEnabled(
                    true
            );


            setStatus(
                    "Could not respond to challenge."
            );


            game.notifyError(
                    exception.getMessage()
            );
        }
    }

    private void handlePlayLocal() {

        if (matchCommitted) {

            game.notifyInfo(
                    "The online match is already starting."
            );

            return;
        }


        if (invitePending) {

            game.notifyInfo(
                    "Finish the current challenge first."
            );

            return;
        }


        if (waitingInQueue) {

            leaveQueueThen(
                    this::openLocalGame
            );

            return;
        }


        openLocalGame();
    }


    private void openLocalGame() {

        game.showScreen(
                new IZombieScreen(
                        game,
                        stageNumber
                )
        );
    }


    private void handleBack() {

        if (matchCommitted) {

            game.notifyInfo(
                    "The online match is already starting."
            );

            return;
        }


        if (invitePending) {

            game.notifyInfo(
                    "Finish the current challenge first."
            );

            return;
        }


        if (waitingInQueue) {

            leaveQueueThen(
                    this::goBack
            );

            return;
        }


        goBack();
    }


    private void goBack() {

        game.showScreen(
                new minigames(
                        game,
                        MinigameType.IZOMBIE
                )
        );
    }

    private Actor createIZombieBackground() {

        TextureRegion left =
                game.getTextureBank().region(
                        BG_LEFT
                );

        TextureRegion middle =
                game.getTextureBank().region(
                        BG_MID
                );

        TextureRegion right =
                game.getTextureBank().region(
                        BG_RIGHT
                );


        Actor background =
                new Actor() {

                    @Override
                    public void draw(
                            Batch batch,
                            float parentAlpha
                    ) {

                        final float originalWorldHeight =
                                600f;


                        float scale =
                                getHeight()
                                        / originalWorldHeight;


                        float gameplayCenterWorldX =
                                left.getRegionWidth()
                                        + middle.getRegionWidth()
                                        / 2f;


                        float screenCenterX =
                                getX()
                                        + getWidth()
                                        / 2f;


                        float firstX =
                                screenCenterX
                                        - gameplayCenterWorldX
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


                        float currentX =
                                firstX;


                        batch.draw(
                                left,
                                currentX,
                                getY(),
                                leftWidth,
                                getHeight()
                        );


                        currentX +=
                                leftWidth;


                        batch.draw(
                                middle,
                                currentX,
                                getY(),
                                middleWidth,
                                getHeight()
                        );


                        currentX +=
                                middleWidth;


                        batch.draw(
                                right,
                                currentX,
                                getY(),
                                rightWidth,
                                getHeight()
                        );
                    }
                };


        background.setBounds(
                0f,
                0f,
                PvzGame.VIRTUAL_WIDTH,
                PvzGame.VIRTUAL_HEIGHT
        );


        background.setTouchable(
                Touchable.disabled
        );


        return background;
    }


    private Stack createTopDecoration() {

        Stack stack =
                new Stack();


        TextureRegion topperRegion =
                game.getTextureBank()
                        .region(
                                TOPPER
                        );


        if (topperRegion != null) {

            Image topper =
                    new Image(
                            topperRegion
                    );


            topper.setScaling(
                    Scaling.fit
            );


            stack.add(
                    topper
            );
        }


        TextureRegion sunflowerRegion =
                game.getTextureBank()
                        .region(
                                SUNFLOWER_TOPPER
                        );


        if (sunflowerRegion != null) {

            Image sunflower =
                    new Image(
                            sunflowerRegion
                    );


            sunflower.setScaling(
                    Scaling.fit
            );


            Table layer =
                    new Table();

            layer.top();


            layer.add(
                            sunflower
                    )
                    .size(70f)
                    .padTop(-12f);


            stack.add(
                    layer
            );
        }


        return stack;
    }


    private TextField createUsernameField() {

        TextureRegion region =
                game.getTextureBank()
                        .region(
                                TEXT_FIELD
                        );


        TextureRegionDrawable background =
                new TextureRegionDrawable(
                        region
                );


        background.setLeftWidth(
                95f
        );

        background.setRightWidth(
                35f
        );

        background.setTopHeight(
                12f
        );

        background.setBottomHeight(
                12f
        );


        TextField.TextFieldStyle style =
                new TextField.TextFieldStyle(
                        game.getSkin().get(
                                TextField.TextFieldStyle.class
                        )
                );


        style.background =
                background;

        style.focusedBackground =
                background;

        style.disabledBackground =
                background;

        style.fontColor =
                Color.WHITE;

        style.focusedFontColor =
                Color.WHITE;

        style.messageFontColor =
                Color.WHITE;

        style.disabledFontColor =
                Color.GRAY;


        TextField field =
                new TextField(
                        "",
                        style
                );


        field.setMessageText(
                "Enter opponent username..."
        );


        return field;
    }


    private ImageButton createBackButton() {

        TextureRegion normal =
                game.getTextureBank()
                        .region(
                                BACK
                        );


        TextureRegion pressed =
                game.getTextureBank()
                        .region(
                                BACK_PRESSED
                        );


        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();


        style.imageUp =
                new TextureRegionDrawable(
                        normal
                );


        style.imageDown =
                new TextureRegionDrawable(
                        pressed
                );


        style.imageOver =
                new TextureRegionDrawable(
                        pressed
                );


        return new ImageButton(
                style
        );
    }


    private Table createPaper() {

        Table paper =
                new Table();


        paper.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("E4D3A7")
                )
        );


        paper.pad(
                10f,
                15f,
                10f,
                15f
        );


        return paper;
    }


    private Table createStatusPaper() {

        Table paper =
                new Table();


        paper.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("EBE6D1")
                )
        );


        return paper;
    }


    private void setPrimaryControlsEnabled(
            boolean enabled
    ) {

        challengeButton.setDisabled(
                !enabled
        );


        randomButton.setDisabled(
                !enabled
        );


        usernameField.setDisabled(
                !enabled
        );


        localButton.setDisabled(
                !enabled
        );
    }


    private void setStatus(
            String text
    ) {

        if (statusLabel == null) {
            return;
        }


        if (text == null
                || text.isBlank()) {

            statusLabel.setText(
                    " "
            );

        } else {

            statusLabel.setText(
                    text
            );
        }
    }

    private void detachEventListener() {

        if (networkClient != null) {

            networkClient.setEventListener(
                    null
            );
        }
    }


    private static String rootMessage(
            Throwable throwable
    ) {

        if (throwable == null) {

            return "Unknown network error.";
        }


        Throwable current =
                throwable;


        while (current.getCause()
                != null) {

            current =
                    current.getCause();
        }


        String message =
                current.getMessage();


        if (message == null
                || message.isBlank()) {

            return current
                    .getClass()
                    .getSimpleName();
        }


        return message;
    }
}