package views.graphical.screens.minigamesScreen.iZombie.online;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import graphics.PvzGame;
import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;
import views.graphical.ui.PlantCard;
import views.graphical.ui.ZombieCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Role-specific online packet bar built from the same cards as local play. */
public final class OnlineUnitCardBar extends Table {

    private static final float PLANT_CARD_SCALE = 0.82f;

    private final ButtonGroup<Button> cardGroup = new ButtonGroup<>();
    private final Map<String, Button> cards = new LinkedHashMap<>();
    private final Map<String, Integer> costs = new LinkedHashMap<>();
    private final Consumer<String> onSelected;

    private String selectedEntity;

    public OnlineUnitCardBar(
            PvzGame game,
            boolean plantRole,
            int stageNumber,
            Consumer<String> onSelected
    ) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }

        this.onSelected = onSelected;

        setTouchable(Touchable.childrenOnly);
        cardGroup.setMinCheckCount(0);
        cardGroup.setMaxCheckCount(1);
        cardGroup.setUncheckLast(true);

        Table strip = new Table();
        strip.left();

        if (plantRole) {
            buildPlantCards(game, strip);
        } else {
            buildZombieCards(game, stageNumber, strip);
        }

        if (cards.isEmpty()) {
            throw new IllegalStateException(
                    "No online placement cards are available."
            );
        }

        ScrollPane scrollPane = new ScrollPane(strip, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(false, true);
        scrollPane.setOverscroll(false, false);

        add(scrollPane).grow();
    }

    private void buildPlantCards(PvzGame game, Table strip) {
        List<PlantData> plants = new ArrayList<>(PlantRegistry.getAll());
        plants.removeIf(plant -> plant == null);
        plants.sort(Comparator.comparing(
                PlantData::name,
                String.CASE_INSENSITIVE_ORDER
        ));

        for (PlantData plant : plants) {
            PlantCard card = new PlantCard(
                    game,
                    new PlantCard.ViewData(
                            plant,
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

            registerCard(
                    plant.name(),
                    plant.cost(),
                    card
            );

            strip.add(card)
                    .size(card.getPrefWidth(), card.getPrefHeight())
                    .padRight(5f);
        }
    }

    private void buildZombieCards(
            PvzGame game,
            int stageNumber,
            Table strip
    ) {
        for (Map.Entry<String, Integer> entry :
                MultiplayerIZombieGame.rosterForStage(stageNumber).entrySet()) {

            String alias = entry.getKey();
            if (ZombieRegistry.getTemplate(alias) == null) {
                continue;
            }

            ZombieCard card = new ZombieCard(
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

            registerCard(alias, entry.getValue(), card);

            Label cost = new Label(
                    Integer.toString(entry.getValue()),
                    game.getSkin().get(
                            "medium_outline",
                            Label.LabelStyle.class
                    )
            );
            cost.setFontScale(0.72f);

            Table costLayer = new Table();
            costLayer.setTouchable(Touchable.disabled);
            costLayer.bottom().right();
            costLayer.add(cost).padRight(8f).padBottom(5f);

            Stack packet = new Stack();
            packet.add(card);
            packet.add(costLayer);

            strip.add(packet)
                    .size(card.getPrefWidth(), card.getPrefHeight())
                    .padRight(5f);
        }
    }

    private void registerCard(
            String entityName,
            int cost,
            Button card
    ) {
        cards.put(entityName, card);
        costs.put(entityName, cost);
        cardGroup.add(card);

        card.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (card.isChecked()) {
                    selectedEntity = entityName;
                } else if (entityName.equals(selectedEntity)) {
                    selectedEntity = null;
                }

                if (onSelected != null) {
                    onSelected.accept(selectedEntity);
                }
            }
        });
    }

    public String getSelectedEntity() {
        return selectedEntity;
    }

    public void setAvailableSun(int sun) {
        int availableSun = Math.max(0, sun);

        for (Map.Entry<String, Button> entry : cards.entrySet()) {
            int cost = costs.getOrDefault(entry.getKey(), Integer.MAX_VALUE);
            boolean affordable = availableSun >= cost;
            Button card = entry.getValue();

            if (card instanceof PlantCard plantCard) {
                plantCard.setEnoughSun(affordable);
                plantCard.setAvailable(affordable);
            } else {
                if (!affordable && card.isChecked()) {
                    card.setChecked(false);
                }
                card.setDisabled(!affordable);
                card.setTouchable(
                        affordable ? Touchable.enabled : Touchable.disabled
                );
            }
        }
    }
}
