package views.graphical.screens.minigamesScreen.iZombie.online;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import graphics.PvzGame;
import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;
import network.client.ClientSession;
import network.protocol.match.GameActionDto;
import network.protocol.match.GameActionType;
import network.protocol.match.MatchSnapshot;
import views.graphical.ui.PlantCard;
import views.graphical.ui.ZombieCard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server-authoritative online I, Zombie control strip.
 *
 * This deliberately reuses the exact PvZ card widgets used by the main game:
 * PlantCard for the plant player and ZombieCard for the zombie player.
 * There are no SelectBoxes or generic PLACE/PLUCK/FEED text buttons here.
 */
public final class OnlineIZombieControlPanel extends Table {

    private enum PlantMode {
        PLACE,
        PLUCK,
        FEED
    }

    private static final float PLANT_CARD_SCALE = 0.90f;
    private static final float ZOMBIE_CARD_SCALE = 0.60f;
    private static final float CARD_GAP = 3f;

    /* Keep the same compact defender roster that the old online seed bank exposed. */
    private static final List<String> ONLINE_PLANT_ROSTER = List.of(
            "Sunflower",
            "Peashooter",
            "Snow Pea",
            "Repeater",
            "Wall-nut"
    );

    private final PvzGame game;
    private final ClientSession clientSession;
    private final Consumer<GameActionDto> actionSender;

    private final ButtonGroup<PlantCard> plantGroup = new ButtonGroup<>();
    private final ButtonGroup<ZombieCard> zombieGroup = new ButtonGroup<>();

    private final Map<String, PlantCard> plantCards = new LinkedHashMap<>();
    private final Map<String, ZombieCard> zombieCards = new LinkedHashMap<>();
    private final Map<String, Label> zombieStatusLabels = new LinkedHashMap<>();
    private final Map<String, Integer> costs = new LinkedHashMap<>();

    private String selectedEntityName;
    private PlantMode plantMode = PlantMode.PLACE;
    private boolean disabled;
    private String lastStatus = "";

    public OnlineIZombieControlPanel(
            PvzGame game,
            ClientSession clientSession,
            Consumer<GameActionDto> actionSender
    ) {
        this.game = Objects.requireNonNull(game, "game cannot be null");
        this.clientSession = Objects.requireNonNull(clientSession, "clientSession cannot be null");
        this.actionSender = Objects.requireNonNull(actionSender, "actionSender cannot be null");

        setTouchable(Touchable.childrenOnly);
        top().left();

        plantGroup.setMinCheckCount(0);
        plantGroup.setMaxCheckCount(1);
        plantGroup.setUncheckLast(true);

        zombieGroup.setMinCheckCount(0);
        zombieGroup.setMaxCheckCount(1);
        zombieGroup.setUncheckLast(true);

        Group cards = clientSession.isPlantPlayer()
                ? buildPlantCards()
                : buildZombieCards();

        add(cards)
                .size(cards.getWidth(), cards.getHeight())
                .top()
                .left();

        pack();
    }

    private Group buildPlantCards() {
        Group group = new Group();
        List<PlantCard> orderedCards = new ArrayList<>();

        float width = 0f;
        float height = 0f;

        for (String requestedName : ONLINE_PLANT_ROSTER) {
            PlantData data = findPlant(requestedName);
            if (data == null) {
                continue;
            }

            PlantCard card = new PlantCard(
                    game,
                    new PlantCard.ViewData(
                            data,
                            true,
                            false,
                            1,
                            0,
                            1,
                            true,
                            false
                    ),
                    PLANT_CARD_SCALE
            );

            final String plantName = data.name();

            plantGroup.add(card);
            plantCards.put(plantName, card);
            costs.put(plantName, data.cost());
            orderedCards.add(card);

            card.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (card.isChecked()) {
                        selectedEntityName = plantName;
                        plantMode = PlantMode.PLACE;
                    } else if (plantName.equals(selectedEntityName)) {
                        selectedEntityName = null;
                    }
                }
            });

            width = Math.max(width, card.getPrefWidth());
            height += card.getPrefHeight() + CARD_GAP;
        }

        if (!orderedCards.isEmpty()) {
            height -= CARD_GAP;
        }

        group.setSize(width, height);

        float y = height;
        for (PlantCard card : orderedCards) {
            y -= card.getPrefHeight();
            card.setPosition(0f, y);
            group.addActor(card);
            y -= CARD_GAP;
        }

        return group;
    }

    private Group buildZombieCards() {
        Group group = new Group();
        List<Table> wrappers = new ArrayList<>();

        float width = 0f;
        float height = 0f;

        Map<String, Integer> roster =
                MultiplayerIZombieGame.getRosterForStage(clientSession.getStageNumber());

        for (Map.Entry<String, Integer> entry : roster.entrySet()) {
            String alias = entry.getKey();
            int cost = entry.getValue();

            ZombieCard card = createZombieCard(alias);
            zombieGroup.add(card);
            zombieCards.put(alias, card);
            costs.put(alias, cost);

            Label costLabel =
                    new Label(
                            Integer.toString(cost),
                            game.getSkin()
                    );

            Label statusLabel =
                    new Label(
                            "Ready",
                            game.getSkin()
                    );

            zombieStatusLabels.put(
                    alias,
                    statusLabel
            );

            Stack stack = new Stack();
            stack.add(card);

            /*
             * Match the local IZombieBar packet overlay:
             * sun cost on the left, recharge/Ready on the right.
             */
            Table overlay = new Table();
            overlay.setTouchable(Touchable.disabled);
            overlay.bottom();

            overlay.add(costLabel)
                    .left()
                    .expandX()
                    .padLeft(12f)
                    .padBottom(8f);

            overlay.add(statusLabel)
                    .right()
                    .padRight(12f)
                    .padBottom(8f);

            stack.add(overlay);

            Table wrapper = new Table();
            wrapper.add(stack)
                    .size(card.getPrefWidth(), card.getPrefHeight());
            wrapper.pack();
            wrapper.setTransform(true);
            wrapper.setScale(ZOMBIE_CARD_SCALE);

            wrappers.add(wrapper);

            float scaledWidth = wrapper.getWidth() * ZOMBIE_CARD_SCALE;
            float scaledHeight = wrapper.getHeight() * ZOMBIE_CARD_SCALE;
            width = Math.max(width, scaledWidth);
            height += scaledHeight + CARD_GAP;

            card.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (card.isChecked()) {
                        selectedEntityName = alias;
                    } else if (alias.equals(selectedEntityName)) {
                        selectedEntityName = null;
                    }
                }
            });
        }

        if (!wrappers.isEmpty()) {
            height -= CARD_GAP;
        }

        group.setSize(width, height);

        float y = height;
        for (Table wrapper : wrappers) {
            float scaledHeight = wrapper.getHeight() * ZOMBIE_CARD_SCALE;
            y -= scaledHeight;
            wrapper.setPosition(0f, y);
            group.addActor(wrapper);
            y -= CARD_GAP;
        }

        return group;
    }

    private ZombieCard createZombieCard(String alias) {
        return new ZombieCard(
                game,
                new ZombieCard.ViewData(
                        alias,
                        ZombieRegistry.getCardAssetId(alias),
                        ZombieRegistry.getIdlePamPath(alias),
                        ZombieRegistry.getIdleClip(alias),
                        ZombieRegistry.getWalkClip(alias),
                        ZombieRegistry.getIdleVisibleParts(alias),
                        true
                )
        );
    }

    private PlantData findPlant(String name) {
        for (PlantData data : PlantRegistry.getAll()) {
            if (data.name().equalsIgnoreCase(name)) {
                return data;
            }
        }
        return null;
    }

    public void handleTileClick(int lane, int column) {
        if (disabled || !clientSession.isMatchRunning()) {
            return;
        }

        if (lane < 0
                || lane >= 5
                || column < 0
                || column >= 9) {
            return;
        }

        if (clientSession.isZombiePlayer()) {
            handleZombieTileClick(lane, column);
        } else if (clientSession.isPlantPlayer()) {
            handlePlantTileClick(lane, column);
        }
    }

    private void handleZombieTileClick(int lane, int column) {
        if (column < MultiplayerIZombieGame.RED_LINE_COLUMN) {
            setStatus("Zombies can only be placed to the right of the red line.");
            return;
        }

        if (selectedEntityName == null || selectedEntityName.isBlank()) {
            setStatus("Select a zombie card first.");
            return;
        }

        send(
                GameActionType.PLACE_ZOMBIE,
                selectedEntityName,
                lane,
                column
        );
    }

    private void handlePlantTileClick(int lane, int column) {
        int userColumn = column + 1;

        if (userColumn < MultiplayerIZombieGame.PLANT_START_COLUMN
                || userColumn > MultiplayerIZombieGame.PLANT_END_COLUMN) {
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
                if (selectedEntityName == null || selectedEntityName.isBlank()) {
                    setStatus("Select a plant card first.");
                    return;
                }

                send(
                        GameActionType.PLACE_PLANT,
                        selectedEntityName,
                        lane,
                        column
                );
            }
            case PLUCK -> send(
                    GameActionType.PLUCK_PLANT,
                    null,
                    lane,
                    column
            );
            case FEED -> send(
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
        GameActionDto action = new GameActionDto();
        action.setType(type);
        action.setEntityName(entityName);
        action.setRow(lane);
        action.setColumn(column);
        action.setClientActionId(UUID.randomUUID().toString());

        actionSender.accept(action);
    }


    public void activatePluckMode() {
        if (!clientSession.isPlantPlayer() || disabled) {
            return;
        }

        clearCardSelection();
        plantMode = PlantMode.PLUCK;
    }


    public void activateFeedMode() {
        if (!clientSession.isPlantPlayer() || disabled) {
            return;
        }

        clearCardSelection();
        plantMode = PlantMode.FEED;
    }

    public void activatePlaceMode() {
        if (!clientSession.isPlantPlayer() || disabled) {
            return;
        }
        plantMode = PlantMode.PLACE;
    }

    public void updateFromSnapshot(MatchSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        int currentSun = clientSession.isPlantPlayer()
                ? snapshot.getPlantSun()
                : snapshot.getZombieSun();

        Map<String, Integer> plantRemaining =
                snapshot.getPlantCooldownTicks();

        Map<String, Integer> plantTotal =
                snapshot.getPlantCooldownTotalTicks();

        for (Map.Entry<String, PlantCard> entry : plantCards.entrySet()) {
            String plantName =
                    entry.getKey();

            int cost =
                    costs.getOrDefault(
                            plantName,
                            Integer.MAX_VALUE
                    );

            int remaining =
                    plantRemaining == null
                            ? 0
                            : Math.max(
                            0,
                            plantRemaining.getOrDefault(
                                    plantName,
                                    0
                            )
                    );

            int total =
                    plantTotal == null
                            ? 0
                            : Math.max(
                            0,
                            plantTotal.getOrDefault(
                                    plantName,
                                    0
                            )
                    );

            float cooldownFraction =
                    total <= 0
                            ? 0f
                            : Math.min(
                            1f,
                            remaining
                                    / (float) total
                    );

            boolean enoughSun =
                    currentSun >= cost;

            boolean ready =
                    remaining == 0;

            PlantCard card =
                    entry.getValue();

            card.setCooldownFraction(
                    cooldownFraction
            );

            card.setEnoughSun(
                    enoughSun
            );

            card.setAvailable(
                    !disabled
                            && enoughSun
                            && ready
            );
        }

        Map<String, Integer> zombieRemaining =
                snapshot.getZombieCooldownTicks();

        int ticksPerSecond =
                Math.max(
                        1,
                        snapshot.getTicksPerSecond()
                );

        for (Map.Entry<String, ZombieCard> entry : zombieCards.entrySet()) {
            String alias =
                    entry.getKey();

            int cost =
                    costs.getOrDefault(
                            alias,
                            Integer.MAX_VALUE
                    );

            int remaining =
                    zombieRemaining == null
                            ? 0
                            : Math.max(
                            0,
                            zombieRemaining.getOrDefault(
                                    alias,
                                    0
                            )
                    );

            boolean ready =
                    remaining == 0;

            boolean enoughSun =
                    currentSun >= cost;

            boolean available =
                    !disabled
                            && enoughSun
                            && ready;

            ZombieCard card =
                    entry.getValue();

            Label statusLabel =
                    zombieStatusLabels.get(
                            alias
                    );

            if (statusLabel != null) {
                if (remaining > 0) {
                    int seconds =
                            Math.max(
                                    1,
                                    (
                                            remaining
                                                    + ticksPerSecond
                                                    - 1
                                    )
                                            / ticksPerSecond
                            );

                    statusLabel.setText(
                            seconds + "s"
                    );

                } else if (!enoughSun) {

                    statusLabel.setText(
                            ""
                    );

                } else {

                    statusLabel.setText(
                            "Ready"
                    );
                }
            }

            if (!available
                    && card.isChecked()) {

                card.setChecked(
                        false
                );

                if (alias.equals(
                        selectedEntityName
                )) {
                    selectedEntityName =
                            null;
                }
            }

            card.setDisabled(
                    !available
            );

            card.setTouchable(
                    available
                            ? Touchable.enabled
                            : Touchable.disabled
            );
        }
    }

    public void onActionAccepted() {
        clearCardSelection();
        plantMode = PlantMode.PLACE;
    }

    private void clearCardSelection() {
        plantGroup.uncheckAll();
        zombieGroup.uncheckAll();
        selectedEntityName = null;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;

        if (disabled) {
            clearCardSelection();
            plantMode = PlantMode.PLACE;
        }

        for (PlantCard card : plantCards.values()) {
            if (disabled) {
                card.setAvailable(false);
            }
        }

        for (ZombieCard card : zombieCards.values()) {
            card.setDisabled(disabled);
            card.setTouchable(disabled ? Touchable.disabled : Touchable.enabled);
        }
    }

    public void setStatus(String text) {
        lastStatus = text == null ? "" : text;
    }

    public String getLastStatus() {
        return lastStatus;
    }
}
