package views.graphical.screens.minigamesScreen.iZombie.online;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;

import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import com.badlogic.gdx.utils.Align;

import graphics.PvzGame;

import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;

import network.client.ClientSession;

import network.protocol.match.GameActionDto;
import network.protocol.match.GameActionType;
import network.protocol.match.MatchSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;


public final class OnlineIZombieControlPanel
        extends Table {

    private enum PlantMode {

        PLACE,
        PLUCK,
        FEED
    }


    private final PvzGame game;

    private final ClientSession clientSession;

    private final Consumer<GameActionDto>
            actionSender;


    private final SelectBox<String>
            entitySelect;

    private final Label
            statusLabel;

    private final Label
            sunLabel;


    private final TextButton
            placeButton;

    private TextButton
            pluckButton;

    private TextButton
            feedButton;


    private PlantMode plantMode =
            PlantMode.PLACE;


    private boolean disabled;


    public OnlineIZombieControlPanel(
            PvzGame game,
            ClientSession clientSession,
            Consumer<GameActionDto> actionSender
    ) {

        this.game =
                Objects.requireNonNull(
                        game,
                        "game cannot be null"
                );


        this.clientSession =
                Objects.requireNonNull(
                        clientSession,
                        "clientSession cannot be null"
                );


        this.actionSender =
                Objects.requireNonNull(
                        actionSender,
                        "actionSender cannot be null"
                );


        setTouchable(
                Touchable.childrenOnly
        );


        pad(
                10f
        );


        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0.05f,
                                0.05f,
                                0.05f,
                                0.86f
                        )
                )
        );


        Label title =
                new Label(
                        clientSession.isZombiePlayer()
                                ? "ZOMBIE CONTROLS"
                                : "PLANT CONTROLS",
                        game.getSkin().get(
                                "medium_outline",
                                Label.LabelStyle.class
                        )
                );


        title.setAlignment(
                Align.center
        );


        add(
                title
        )
                .colspan(3)
                .growX()
                .padBottom(8f)
                .row();


        sunLabel =
                new Label(
                        "SUN: 0",
                        game.getSkin()
                );


        add(
                sunLabel
        )
                .colspan(3)
                .left()
                .padBottom(6f)
                .row();


        entitySelect =
                new SelectBox<>(
                        game.getSkin()
                );


        if (clientSession.isZombiePlayer()) {

            configureZombieItems();

        } else {

            configurePlantItems();
        }


        add(
                entitySelect
        )
                .colspan(3)
                .width(270f)
                .height(42f)
                .padBottom(8f)
                .row();


        placeButton =
                new TextButton(
                        clientSession.isZombiePlayer()
                                ? "PLACE ZOMBIE"
                                : "PLACE PLANT",
                        game.getSkin(),
                        "green"
                );


        placeButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        if (clientSession.isPlantPlayer()) {

                            setPlantMode(
                                    PlantMode.PLACE
                            );
                        }


                        setStatus(
                                "Select a board tile."
                        );
                    }
                }
        );


        add(
                placeButton
        )
                .width(132f)
                .height(42f);


        if (clientSession.isPlantPlayer()) {

            pluckButton =
                    new TextButton(
                            "PLUCK",
                            game.getSkin(),
                            "green"
                    );


            pluckButton.addListener(
                    new ChangeListener() {

                        @Override
                        public void changed(
                                ChangeEvent event,
                                Actor actor
                        ) {

                            setPlantMode(
                                    PlantMode.PLUCK
                            );


                            setStatus(
                                    "Click a plant tile to remove it."
                            );
                        }
                    }
            );


            add(
                    pluckButton
            )
                    .width(88f)
                    .height(42f)
                    .padLeft(4f);


            feedButton =
                    new TextButton(
                            "FEED",
                            game.getSkin(),
                            "green"
                    );


            feedButton.addListener(
                    new ChangeListener() {

                        @Override
                        public void changed(
                                ChangeEvent event,
                                Actor actor
                        ) {

                            setPlantMode(
                                    PlantMode.FEED
                            );


                            setStatus(
                                    "Click a plant tile to feed it."
                            );
                        }
                    }
            );


            add(
                    feedButton
            )
                    .width(88f)
                    .height(42f)
                    .padLeft(4f);
        }


        row();


        statusLabel =
                new Label(
                        "Select an action, then click a tile.",
                        game.getSkin()
                );


        statusLabel.setWrap(
                true
        );


        statusLabel.setAlignment(
                Align.left
        );


        add(
                statusLabel
        )
                .colspan(3)
                .width(310f)
                .padTop(8f)
                .left();


        pack();
    }


    private void configureZombieItems() {

        Map<String, Integer> roster =
                MultiplayerIZombieGame
                        .getRosterForStage(
                                clientSession
                                        .getStageNumber()
                        );


        List<String> aliases =
                new ArrayList<>(
                        roster.keySet()
                );


        entitySelect.setItems(
                aliases.toArray(
                        new String[0]
                )
        );
    }


    private void configurePlantItems() {

        List<PlantData> plants =
                new ArrayList<>(
                        PlantRegistry.getAll()
                );


        plants.sort(
                Comparator
                        .comparing(
                                PlantData::name,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );


        List<String> names =
                plants.stream()
                        .map(
                                PlantData::name
                        )
                        .toList();


        entitySelect.setItems(
                names.toArray(
                        new String[0]
                )
        );
    }


    public void handleTileClick(
            int lane,
            int column
    ) {

        if (disabled
                || !clientSession.isMatchRunning()) {

            return;
        }


        if (lane < 0
                || lane >= 5
                || column < 0
                || column >= 9) {

            return;
        }


        if (clientSession.isZombiePlayer()) {

            handleZombieTileClick(
                    lane,
                    column
            );

            return;
        }


        if (clientSession.isPlantPlayer()) {

            handlePlantTileClick(
                    lane,
                    column
            );
        }
    }


    private void handleZombieTileClick(
            int lane,
            int column
    ) {

        /*
         * Server rule:
         * user-coordinate x must be greater than RED_LINE_COLUMN.
         * GameActionDto uses zero-based column, so column 6 is user x=7.
         */
        if (column
                < MultiplayerIZombieGame.RED_LINE_COLUMN) {

            setStatus(
                    "Zombies can only be placed to the right of the red line."
            );

            return;
        }


        String alias =
                entitySelect.getSelected();


        if (alias == null
                || alias.isBlank()) {

            setStatus(
                    "Select a zombie first."
            );

            return;
        }


        send(
                GameActionType.PLACE_ZOMBIE,
                alias,
                lane,
                column
        );
    }


    private void handlePlantTileClick(
            int lane,
            int column
    ) {

        int userColumn =
                column + 1;


        if (userColumn
                < MultiplayerIZombieGame.PLANT_START_COLUMN
                || userColumn
                > MultiplayerIZombieGame.PLANT_END_COLUMN) {

            setStatus(
                    "Plant actions are only allowed between columns "
                            + MultiplayerIZombieGame.PLANT_START_COLUMN
                            + " and "
                            + MultiplayerIZombieGame.PLANT_END_COLUMN
                            + "."
            );

            return;
        }


        switch (plantMode) {

            case PLACE -> {

                String plantName =
                        entitySelect.getSelected();


                if (plantName == null
                        || plantName.isBlank()) {

                    setStatus(
                            "Select a plant first."
                    );

                    return;
                }


                send(
                        GameActionType.PLACE_PLANT,
                        plantName,
                        lane,
                        column
                );
            }


            case PLUCK ->
                    send(
                            GameActionType.PLUCK_PLANT,
                            null,
                            lane,
                            column
                    );


            case FEED ->
                    send(
                            GameActionType.FEED_PLANT,
                            null,
                            lane,
                            column
                    );
        }
    }


    private void send(
            GameActionType type,
            String entityName,
            int lane,
            int column
    ) {

        GameActionDto action =
                new GameActionDto();


        action.setType(
                type
        );


        action.setEntityName(
                entityName
        );


        action.setRow(
                lane
        );


        action.setColumn(
                column
        );


        action.setClientActionId(
                UUID.randomUUID()
                        .toString()
        );


        setStatus(
                "Sending "
                        + type
                        + "..."
        );


        actionSender.accept(
                action
        );
    }


    private void setPlantMode(
            PlantMode mode
    ) {

        if (mode == null) {
            return;
        }


        plantMode =
                mode;
    }


    public void updateFromSnapshot(
            MatchSnapshot snapshot
    ) {

        if (snapshot == null) {
            return;
        }


        int sun =
                clientSession.isPlantPlayer()
                        ? snapshot.getPlantSun()
                        : snapshot.getZombieSun();


        sunLabel.setText(
                "SUN: " + sun
        );
    }


    public void onActionAccepted() {

        setStatus(
                "Action accepted."
        );
    }


    public void setStatus(
            String text
    ) {

        statusLabel.setText(
                text == null
                        ? ""
                        : text
        );
    }


    public void setDisabled(
            boolean disabled
    ) {

        this.disabled =
                disabled;


        entitySelect.setDisabled(
                disabled
        );


        placeButton.setDisabled(
                disabled
        );


        if (pluckButton != null) {
            pluckButton.setDisabled(
                    disabled
            );
        }


        if (feedButton != null) {
            feedButton.setDisabled(
                    disabled
            );
        }


        setTouchable(
                disabled
                        ? Touchable.disabled
                        : Touchable.childrenOnly
        );
    }
}
