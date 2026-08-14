package views.graphical.screens.minigamesScreen.vaseBreaker;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import graphics.PvzGame;
import lombok.Getter;
import models.minigames.vaseBreaker.VaseBreaker;
import views.graphical.ui.PlantCard;

import java.util.Map;
import java.util.function.Consumer;
@Getter
public class VasePacketBar extends Table {
    private static final float CARD_SCALE = 0.78f;
    private final PvzGame game;
    private final VaseBreaker vaseBreaker;
    private final Consumer<String> onSelectionChanged;
    private String selectedPlantName;
    private ButtonGroup<PlantCard> buttonGroup;
    public VasePacketBar(PvzGame game, VaseBreaker vaseBreaker, Consumer<String> onSelectionChanged) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        if (vaseBreaker == null) {
            throw new IllegalArgumentException(
                    "vaseBreaker cannot be null"
            );
        }

        this.game = game;
        this.vaseBreaker = vaseBreaker;
        this.onSelectionChanged = onSelectionChanged;
        setFillParent(true);
        top().left();
        setTouchable(
                Touchable.childrenOnly
        );


        refresh();
    }


    public void refresh() {

        clearChildren();

        if (selectedPlantName != null
                && vaseBreaker
                .getPacketInventory()
                .getOrDefault(
                        selectedPlantName,
                        0
                ) <= 0) {

            selectedPlantName = null;

            notifySelectionChanged();
        }


        buttonGroup =
                new ButtonGroup<>();

        buttonGroup.setMinCheckCount(0);

        buttonGroup.setMaxCheckCount(1);

        buttonGroup.setUncheckLast(true);


        Table packets =
                new Table();

        packets.left();


        for (Map.Entry<String, Integer> entry :
                vaseBreaker.getPacketInventory().entrySet()) {

            String plantName = entry.getKey();
            int amount = entry.getValue();
            PlantData plant = PlantRegistry.getByName(plantName);


            if (plant == null) {
                continue;
            }


            PlantCard card =
                    new PlantCard(
                            game,
                            new PlantCard.ViewData(
                                    plant,
                                    true,
                                    false,
                                    1,
                                    0,
                                    1,
                                    false,
                                    false
                            ),
                            CARD_SCALE
                    );


            buttonGroup.add(
                    card
            );

            Label countLabel = new Label("x" + amount, game.getSkin());

            countLabel.setTouchable(
                    Touchable.disabled
            );

            Table countLayer = new Table();
            countLayer.bottom().right();
            countLayer.setTouchable(
                    Touchable.disabled
            );


            countLayer.add(countLabel).padRight(5f).padBottom(3f);
            Stack slot = new Stack();

            slot.add(card);

            slot.add(countLayer);


            if (plantName.equalsIgnoreCase(
                    selectedPlantName
            )) {

                card.setChecked(
                        true
                );
            }


            card.addListener(
                    new ChangeListener() {

                        @Override
                        public void changed(ChangeEvent event, Actor actor) {

                            if (card.isChecked()) {
                                selectedPlantName = plantName;
                            } else if (
                                    selectedPlantName != null && selectedPlantName.equalsIgnoreCase(plantName)
                            ) {
                                selectedPlantName = null;
                            }

                            notifySelectionChanged();
                        }
                    }
            );


            packets.add(
                    slot
            ).padRight(5f);
        }


        add(
                packets
        )
                .top()
                .left()
                .padTop(95f)
                .padLeft(15f);
    }


    private void notifySelectionChanged() {

        if (onSelectionChanged != null) {

            onSelectionChanged.accept(
                    selectedPlantName
            );
        }
    }


    public void clearSelection() {

        selectedPlantName =
                null;


        if (buttonGroup != null) {

            buttonGroup.uncheckAll();
        }


        notifySelectionChanged();
    }
}
