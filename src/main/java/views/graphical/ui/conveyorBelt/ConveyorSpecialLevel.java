package views.graphical.ui.conveyorBelt;

import Data.loader.PlantData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import graphics.PvzGame;
import views.graphical.ui.PlantCard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConveyorSpecialLevel extends Group{
    private static final float CARD_SCALE = 0.72f;
    private static final float STACK_STEP = 45f;
    private static final float TOP_OFFSET = 80f;
    private static final float ENTRY_DURATION = 0.9f;
    private static final float REFLOW_DURATION = 0.28f;


    private final PvzGame game;
    private final Texture beltTexture;
    private final Image background1;
    private final Image background2;
    private final Group itemLayer = new Group();

    private final List<ItemView> itemViews = new ArrayList<>();
    private final ButtonGroup<PlantCard> buttonGroup = new ButtonGroup<>();
    private final Consumer<PlantData> onSelectionChanged;
    private List<PlantData> lastSnapshot = List.of();
    private PlantData selectedPlant;


    private float offset = 0f;
    private float scrollSpeed = 25f;
    public ConveyorSpecialLevel(PvzGame game, String texturePath, Consumer<PlantData> onSelectionChanged) {

        this.game = game;
        this.onSelectionChanged = onSelectionChanged;
        beltTexture = new Texture(Gdx.files.internal(texturePath));

        background1 = new Image(beltTexture);
        background2 = new Image(beltTexture
        );

        addActor(
                background1
        );

        addActor(
                background2
        );

        addActor(
                itemLayer
        );


        setSize(
                beltTexture.getWidth(),
                beltTexture.getHeight()
        );


        background1.setPosition(
                0f,
                0f
        );

        background2.setPosition(
                0f,
                getHeight()
        );


        itemLayer.setTouchable(
                Touchable.childrenOnly
        );

        setTouchable(
                Touchable.childrenOnly
        );


        buttonGroup.setMinCheckCount(0);
        buttonGroup.setMaxCheckCount(1);
        buttonGroup.setUncheckLast(true);
    }


    public void sync(
            List<PlantData> plants) {

        List<PlantData> current =
                plants == null
                        ? List.of()
                        : List.copyOf(plants);


        if (samePlants(
                lastSnapshot,
                current
        )) {
            return;
        }

        if (isSingleAppend(
                lastSnapshot,
                current
        )) {

            PlantData added =
                    current.get(
                            current.size() - 1
                    );

            addPlant(
                    added,
                    true
            );

            lastSnapshot =
                    current;

            return;
        }

        int removedIndex =
                findRemovedIndex(
                        lastSnapshot,
                        current
                );


        if (removedIndex >= 0
                && removedIndex < itemViews.size()) {

            removePlantAt(
                    removedIndex
            );

            lastSnapshot =
                    current;

            return;
        }


        rebuild(
                current
        );

        lastSnapshot =
                current;
    }


    private void addPlant(
            PlantData plant,
            boolean animate
    ) {

        if (plant == null) {
            return;
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


        card.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (card.isChecked()) {
                            selectedPlant = plant;
                            notifySelection();

                        } else if (
                                selectedPlant != null && selectedPlant.id() == plant.id()
                        ) {

                            selectedPlant = null;

                            notifySelection();
                        }
                    }
                }
        );


        Stack stack =
                new Stack();

        stack.add(
                card
        );


        stack.setSize(
                card.getPrefWidth(),
                card.getPrefHeight()
        );

        stack.layout();


        int index =
                itemViews.size();


        float x =
                targetX(
                        stack
                );


        float y =
                targetY(
                        index
                );


        if (animate) {

            stack.setPosition(
                    x,
                    -stack.getHeight()
            );

        } else {

            stack.setPosition(
                    x,
                    y
            );
        }


        itemLayer.addActor(
                stack
        );


        ItemView item =
                new ItemView(
                        plant,
                        card,
                        stack
                );


        itemViews.add(
                item
        );


        if (animate) {

            stack.addAction(
                    Actions.moveTo(
                            x,
                            y,
                            ENTRY_DURATION,
                            Interpolation.smooth
                    )
            );
        }


        itemLayer.toFront();
    }


    private void removePlantAt(
            int index
    ) {

        ItemView removed =
                itemViews.remove(
                        index
                );


        removed.actor.clearActions();


        removed.actor.addAction(
                Actions.sequence(

                        Actions.parallel(

                                Actions.fadeOut(
                                        0.12f
                                ),

                                Actions.scaleTo(
                                        0.92f,
                                        0.92f,
                                        0.12f,
                                        Interpolation.smooth
                                )
                        ),

                        Actions.removeActor()
                )
        );


        rebuildButtonGroup();


        reflowPlants();
    }


    private void reflowPlants() {

        for (
                int i = 0;
                i < itemViews.size();
                i++
        ) {

            Actor actor =
                    itemViews.get(i)
                            .actor;


            float x =
                    targetX(
                            actor
                    );


            float y =
                    targetY(
                            i
                    );


            actor.clearActions();

            actor.addAction(
                    Actions.moveTo(
                            x,
                            y,
                            REFLOW_DURATION,
                            Interpolation.smooth
                    )
            );
        }
    }


    private void rebuild(
            List<PlantData> current
    ) {

        itemLayer.clearChildren();

        itemViews.clear();

        buttonGroup.clear();

        selectedPlant =
                null;


        for (
                PlantData plant :
                current
        ) {

            addPlant(
                    plant,
                    false
            );
        }


        notifySelection();
    }


    private void rebuildButtonGroup() {

        buttonGroup.clear();


        for (
                ItemView item :
                itemViews
        ) {

            buttonGroup.add(
                    item.card
            );
        }
    }


    private float targetX(
            Actor actor
    ) {

        return getWidth() / 2f
                - actor.getWidth() / 2f;
    }


    private float targetY(
            int index
    ) {

        return getHeight()
                - TOP_OFFSET
                - index * STACK_STEP;
    }


    public void update(
            float delta
    ) {

        offset +=
                scrollSpeed
                        * Math.max(
                        0f,
                        delta
                );


        if (offset >= getHeight()) {

            offset %=
                    getHeight();
        }

        background1.setY(
                -offset
        );


        background2.setY(
                getHeight()
                        - offset
        );
    }


    public void clearSelection() {

        selectedPlant =
                null;

        buttonGroup.uncheckAll();

        notifySelection();
    }


    public void setScrollSpeed(
            float scrollSpeed
    ) {

        this.scrollSpeed =
                Math.max(
                        0f,
                        scrollSpeed
                );
    }


    private void notifySelection() {

        if (onSelectionChanged != null) {

            onSelectionChanged.accept(
                    selectedPlant
            );
        }
    }


    private static boolean samePlants(
            List<PlantData> first,
            List<PlantData> second
    ) {

        if (first.size()
                != second.size()) {

            return false;
        }


        for (
                int i = 0;
                i < first.size();
                i++
        ) {

            if (first.get(i).id()
                    != second.get(i).id()) {

                return false;
            }
        }


        return true;
    }


    private static boolean isSingleAppend(
            List<PlantData> oldList,
            List<PlantData> newList
    ) {

        if (newList.size()
                != oldList.size() + 1) {

            return false;
        }


        for (
                int i = 0;
                i < oldList.size();
                i++
        ) {

            if (oldList.get(i).id()
                    != newList.get(i).id()) {

                return false;
            }
        }


        return true;
    }


    private static int findRemovedIndex(List<PlantData> oldList, List<PlantData> newList) {

        if (oldList.size() != newList.size() + 1) {
            return -1;
        }


        int oldIndex = 0;
        int newIndex = 0;


        while (oldIndex < oldList.size() && newIndex < newList.size()) {

            if (oldList.get(oldIndex).id()== newList.get(newIndex).id()) {

                oldIndex++;
                newIndex++;
                continue;
            }

            int removedIndex = oldIndex;
            oldIndex++;


            while (oldIndex < oldList.size() && newIndex < newList.size()) {

                if (oldList.get(oldIndex).id() != newList.get(newIndex).id()) {

                    return -1;
                }

                oldIndex++;
                newIndex++;
            }

            return removedIndex;
        }

        if (oldIndex == oldList.size() - 1) {
            return oldIndex;
        }
        return -1;
    }


    public void dispose() {
        remove();
        beltTexture.dispose();
    }

    private static final class ItemView {
        private final PlantData plant;
        private final PlantCard card;
        private final Stack actor;


        private ItemView(PlantData plant, PlantCard card, Stack actor) {
            this.plant = plant;
            this.card = card;
            this.actor = actor;
        }
    }

}
