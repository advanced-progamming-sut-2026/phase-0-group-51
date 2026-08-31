package views.graphical.screens.minigamesScreen.iZombie.online;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import graphics.PvzGame;

import lombok.Getter;
import network.client.ClientSession;
import network.client.NetworkClient;
import network.client.service.MatchClientService;

import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;

import network.protocol.match.MatchEndedDto;
import network.protocol.match.MatchSnapshot;

import views.graphical.screens.BaseScreen;
import views.graphical.ui.BorderedPanel;

import java.util.Objects;

@Getter
public final class OnlineIZombieScreen
        extends BaseScreen {

    private static final String BG_LEFT =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_LEFT";

    private static final String BG_MID =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE";

    private static final String BG_RIGHT =
            "IMAGE_BACKGROUNDS_SPORTZBALL_TEXTURE_RIGHT";


    private final ClientSession clientSession;

    private final NetworkClient networkClient;

    private final MatchClientService matchClientService;

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    private final RemoteMatchMirror mirror;



    private Label roleLabel;

    private Label opponentLabel;

    private Label timerLabel;

    private Label sunLabel;

    private Label brainLabel;

    private Label entityLabel;

    private Label tickLabel;

    private Label statusLabel;

    private boolean active;

    private int lastRenderedTick =
            -1;

    private boolean matchEnded;


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
        Table hudLayer =
                new Table();

        hudLayer.setFillParent(
                true
        );

        hudLayer.top();


        BorderedPanel hudPanel =
                new BorderedPanel(
                        game,
                        Color.valueOf("7A431D")
                );


        Table hud =
                hudPanel.getContent();

        hud.clearChildren();

        hud.pad(
                8f,
                16f,
                8f,
                16f
        );


        roleLabel =
                createHudLabel(
                        "ROLE: "
                                + clientSession.getRole()
                );


        opponentLabel =
                createHudLabel(
                        "VS: "
                                + clientSession.getOpponentUsername()
                );


        timerLabel =
                createHudLabel(
                        "TIME: --"
                );


        sunLabel =
                createHudLabel(
                        "SUN: --"
                );


        hud.add(
                        roleLabel
                )
                .padRight(20f);


        hud.add(
                        opponentLabel
                )
                .padRight(20f);


        hud.add(
                        timerLabel
                )
                .padRight(20f);


        hud.add(
                sunLabel
        );


        hudLayer.add(
                        hudPanel
                )
                .width(850f)
                .height(80f)
                .padTop(10f);


        root.add(
                hudLayer
        );

        Table statusLayer =
                new Table();

        statusLayer.setFillParent(
                true
        );

        statusLayer.bottom()
                .right();


        BorderedPanel statusPanel =
                new BorderedPanel(
                        game,
                        Color.valueOf("8F4909")
                );


        Table content =
                statusPanel.getContent();

        content.clearChildren();

        content.pad(
                10f,
                15f,
                10f,
                15f
        );


        statusLabel =
                new Label(
                        "Waiting for first server snapshot...",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        statusLabel.setColor(
                Color.valueOf("FFE16A")
        );

        statusLabel.setAlignment(
                Align.center
        );


        brainLabel =
                createHudLabel(
                        "BRAINS: --"
                );


        entityLabel =
                createHudLabel(
                        "PLANTS: --   ZOMBIES: --   PROJECTILES: --"
                );


        tickLabel =
                createHudLabel(
                        "SERVER TICK: --"
                );


        content.add(
                        statusLabel
                )
                .width(460f)
                .padBottom(5f)
                .row();


        content.add(
                        brainLabel
                )
                .padBottom(3f)
                .row();


        content.add(
                        entityLabel
                )
                .padBottom(3f)
                .row();


        content.add(
                tickLabel
        );


        statusLayer.add(
                        statusPanel
                )
                .width(520f)
                .height(175f)
                .padRight(12f)
                .padBottom(12f);


        root.add(
                statusLayer
        );


        stage.addActor(
                root
        );
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


        if (networkClient != null) {

            networkClient.setEventListener(
                    null
            );
        }


        super.hide();
    }

    @Override
    public void render(
            float delta
    ) {

        applyLatestSnapshotToUi();


        super.render(
                delta
        );
    }


    private void applyLatestSnapshotToUi() {

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


        statusLabel.setText(
                "LIVE - SERVER AUTHORITATIVE"
        );


        tickLabel.setText(
                "SERVER TICK: "
                        + snapshot.getTick()
        );


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
                        "TIME: %d:%02d",
                        minutes,
                        remainingSeconds
                )
        );


        if (clientSession.isPlantPlayer()) {

            sunLabel.setText(
                    "PLANT SUN: "
                            + snapshot.getPlantSun()
            );

        } else {

            sunLabel.setText(
                    "ZOMBIE SUN: "
                            + snapshot.getZombieSun()
            );
        }

        int aliveBrains =
                0;


        if (snapshot.getBrains() != null) {

            for (var brain :
                    snapshot.getBrains()) {

                if (brain != null
                        && !brain.isEaten()) {

                    aliveBrains++;
                }
            }
        }


        brainLabel.setText(
                "BRAINS LEFT: "
                        + aliveBrains
        );


        int plants =
                snapshot.getPlants() == null
                        ? 0
                        : snapshot.getPlants().size();


        int zombies =
                snapshot.getZombies() == null
                        ? 0
                        : snapshot.getZombies().size();


        int projectiles =
                snapshot.getProjectiles() == null
                        ? 0
                        : snapshot.getProjectiles().size();


        entityLabel.setText(
                "PLANTS: "
                        + plants
                        + "   ZOMBIES: "
                        + zombies
                        + "   PROJECTILES: "
                        + projectiles
        );
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

    private Actor createBackground() {

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


        return new Actor() {

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


                float y =
                        getY();


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
                        firstX,
                        y,
                        leftWidth,
                        getHeight()
                );


                float currentX =
                        firstX
                                + leftWidth;


                batch.draw(
                        middle,
                        currentX,
                        y,
                        middleWidth,
                        getHeight()
                );


                currentX +=
                        middleWidth;


                batch.draw(
                        right,
                        currentX,
                        y,
                        rightWidth,
                        getHeight()
                );
            }
        };
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
                ? current.getClass()
                .getSimpleName()
                : message;
    }
}