package views.graphical.screens.minigamesScreen.iZombie;

import Data.loader.ZombieRegistry;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import graphics.PvzGame;
import lombok.Getter;
import models.minigames.iZombie.IZombie;
import views.graphical.ui.ZombieCard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
@Getter
public class IZombieBar extends Table {
    private final IZombie iZombie;
    private final Consumer<String> onZombieSelected;
    private final ButtonGroup<ZombieCard> buttonGroup = new ButtonGroup<>();
    private final Map<String, ZombieCard> cards = new LinkedHashMap<>();
    private final Map<String, Label> statusLabels = new LinkedHashMap<>();
    private String selectedZombieAlias;
    private static final float SCALE = 0.6f;
    private static final float GAP = 5f;
    public IZombieBar(PvzGame game, IZombie iZombie, Consumer<String> onZombieSelected) {
        this.iZombie = iZombie;
        this.onZombieSelected = onZombieSelected;
        setTouchable(
     Touchable.childrenOnly  );
        buttonGroup.setMinCheckCount(0);
        buttonGroup.setMaxCheckCount(1);
        buttonGroup.setUncheckLast(true);
        build(game);
        refresh();
    }


    private void build(PvzGame game) {
        List<Table> wrappers = new ArrayList<>();
        float maxWidth = 0f;
        float totalHeight = 0f;


        for (Map.Entry<String, Integer> entry :
                iZombie
                        .getRoster()
                        .entrySet()) {

            String alias =
                    entry.getKey();

            int cost =
                    entry.getValue();


            ZombieCard card =
                    createZombieCard(
                            game,
                            alias
                    );


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




            Stack cardStack = new Stack();
            cardStack.add(card);
            Table textOverlay = new Table();
            textOverlay.setTouchable(Touchable.disabled);
            textOverlay.bottom();
            textOverlay.add(
                            costLabel
                    )
                    .left()
                    .expandX()
                    .padLeft(12f)
                    .padBottom(8f);


            textOverlay.add(
                            statusLabel
                    )
                    .right()
                    .padRight(12f)
                    .padBottom(8f);


            cardStack.add(
                    textOverlay
            );


            Table wrapper = new Table();


            wrapper.add(
                            cardStack
                    )
                    .size(
                            card.getPrefWidth(),
                            card.getPrefHeight()
                    );

            wrapper.pack();


            wrapper.setTransform(true);
            wrapper.setScale(SCALE);


            wrappers.add(wrapper);


            cards.put(alias, card);
            statusLabels.put(alias, statusLabel);
            buttonGroup.add(card);

            card.addListener(
                    new ChangeListener() {

                        @Override
                        public void changed(ChangeEvent event, Actor actor) {

                            if (card.isChecked()) {

                                selectedZombieAlias =
                                        alias;

                            } else if (
                                    alias.equals(
                                            selectedZombieAlias
                                    )
                            ) {

                                selectedZombieAlias =
                                        null;
                            }


                            if (onZombieSelected != null) {

                                onZombieSelected.accept(
                                        selectedZombieAlias
                                );
                            }
                        }
                    }
            );


            float scaledWidth =
                    wrapper.getWidth()
                            * SCALE;

            float scaledHeight =
                    wrapper.getHeight()
                            * SCALE;


            maxWidth =
                    Math.max(
                            maxWidth,
                            scaledWidth
                    );


            totalHeight +=
                    scaledHeight + GAP;
        }


        if (!wrappers.isEmpty()) {

            totalHeight -= GAP;
        }


        setSize(
                maxWidth,
                totalHeight
        );


        float y =
                totalHeight;


        for (Table wrapper :
                wrappers) {

            float scaledHeight =
                    wrapper.getHeight()
                            * SCALE;


            y -= scaledHeight;


            wrapper.setPosition(
                    0f,
                    y
            );


            addActor(
                    wrapper
            );


            y -= GAP;
        }
    }


    private ZombieCard createZombieCard(PvzGame game,String alias) {

        return new ZombieCard(
                game,
                new ZombieCard.ViewData(
                        alias,

                        ZombieRegistry.getCardAssetId(
                                alias
                        ),

                        ZombieRegistry.getIdlePamPath(
                                alias
                        ),

                        ZombieRegistry.getIdleClip(
                                alias
                        ),

                        ZombieRegistry.getWalkClip(
                                alias
                        ),

                        ZombieRegistry.getIdleVisibleParts(
                                alias
                        ),

                        true
                )
        );
    }


    public void refresh() {

        int currentSun =
                iZombie
                        .getGameState()
                        .getSun();


        int ticksPerSecond =
                Math.max(
                        1,
                        iZombie
                                .getGameState()
                                .getTicksPerSecond()
                );


        for (Map.Entry<String, Integer> entry :
                iZombie.getRoster().entrySet()) {

            String alias =
                    entry.getKey();

            int cost =
                    entry.getValue();


            ZombieCard card =
                    cards.get(alias);

            Label statusLabel =
                    statusLabels.get(alias);


            int remainingTicks =
                    iZombie
                            .getZombieCooldownTicks(
                                    alias
                            );


            boolean enoughSun =
                    currentSun >= cost;

            boolean ready =
                    remainingTicks == 0;

            boolean available =
                    enoughSun && ready;


            card.setDisabled(
                    !available
            );


            card.setTouchable(
                    available
                            ? Touchable.enabled
                            : Touchable.disabled
            );


            if (!available
                    && card.isChecked()) {

                card.setChecked(false);
            }


            if (remainingTicks > 0) {

                int seconds =
                        (
                                remainingTicks
                                        + ticksPerSecond
                                        - 1
                        )
                                / ticksPerSecond;


                statusLabel.setText(
                        seconds + "s"
                );

            } else if (!enoughSun) {


            } else {

                statusLabel.setText(
                        "Ready"
                );
            }
        }
    }


    public String getSelectedZombieAlias() {

        return selectedZombieAlias;
    }


    public void clearSelection() {

        buttonGroup.uncheckAll();

        selectedZombieAlias =
                null;


        if (onZombieSelected != null) {

            onZombieSelected.accept(
                    null
            );
        }
    }
}
