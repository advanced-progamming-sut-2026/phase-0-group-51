package views.graphical.ui;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PlantSelectionMenuTable extends Table {
    private static final String EMPTY_PACKET =
            "IMAGE_UI_SEASONS_UNCOMPRESSED_PVZ2_SEASONS_UIASSET_PRIZE_WINDOW_UPPER_UNLOCKED";

    private static final String TOP_PREVIEW_BACKGROUND =
            "IMAGE_UI_QUESTS_QUEST_PANEL_DAILY";

    private static final int MAX_SELECTED_PLANTS = 8;
    private static final float EMPTY_PACKET_WIDTH = 120f;
    private static final float EMPTY_PACKET_HEIGHT = 75f;

    private final PvzGame game;
    private final Table cardsGrid;
    private final Table selectedSlotsTable;
    private final Table previewContent;

    private final List<PlantCard> selectedCards = new ArrayList<>();

    public PlantSelectionMenuTable(
            PvzGame game,
            Runnable onClose
    ) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        if (onClose == null) {
            throw new IllegalArgumentException(
                    "onClose cannot be null"
            );
        }

        this.game = game;

        setFillParent(true);
        setTouchable(Touchable.childrenOnly);
        pad(16f);

        BorderedPanel outerPanel = new BorderedPanel(
                game,
                Color.valueOf("75452F")
        );

        Table content = outerPanel.getContent();
        content.top().left();

        Table topSection = new Table();
        topSection.top().left();

        selectedSlotsTable = new Table();
        selectedSlotsTable.top().left();

        previewContent = new Table();
        previewContent.top().left();
        previewContent.setBackground(
                drawable(TOP_PREVIEW_BACKGROUND)
        );
        previewContent.pad(12f);

        buildSelectedSlots();
        buildPreviewPlaceholder();

        topSection.add(selectedSlotsTable)
                .top()
                .left()
                .padRight(20f);

        topSection.add(previewContent)
                .growX()
                .fillX()
                .top();

        cardsGrid = new Table();
        cardsGrid.top().left();
        cardsGrid.defaults()
                .expandX()
                .top()
                .pad(8f);

        ScrollPane cardsScroll = new ScrollPane(
                cardsGrid,
                game.getSkin()
        );

        cardsScroll.setFadeScrollBars(false);
        cardsScroll.setOverscroll(false, false);
        cardsScroll.setScrollingDisabled(true, false);

        TextButton letsRockButton = new TextButton(
                "LET'S ROCK!",
                game.getSkin()
        );

        content.add(topSection)
                .growX()
                .fillX()
                .padBottom(14f)
                .row();

        content.add(cardsScroll)
                .grow()
                .minWidth(0f)
                .minHeight(0f)
                .row();

        content.add(letsRockButton)
                .right()
                .padTop(12f);

        add(outerPanel).grow();

        showPlants();
    }

    private void buildSelectedSlots() {
        selectedSlotsTable.clearChildren();

        for (int i = 0; i < MAX_SELECTED_PLANTS; i++) {
            Image emptySlot = new Image(drawable(EMPTY_PACKET));
            emptySlot.setScaling(Scaling.stretch);

            selectedSlotsTable.add(emptySlot)
                    .size(
                            EMPTY_PACKET_WIDTH,
                            EMPTY_PACKET_HEIGHT
                    )
                    .padBottom(6f)
                    .row();
        }
    }

    private void buildPreviewPlaceholder() {
        previewContent.clearChildren();

        previewContent.add("Select a plant")
                .left()
                .padBottom(10f)
                .row();

        previewContent.add()
                .growX()
                .height(120f)
                .row();

        Table buttonsRow = new Table();

        TextButton upgradeButton = new TextButton(
                "UPGRADE",
                game.getSkin()
        );

        TextButton boostButton = new TextButton(
                "BOOST",
                game.getSkin()
        );

        buttonsRow.add(upgradeButton)
                .padRight(10f);

        buttonsRow.add(boostButton);

        previewContent.add(buttonsRow)
                .left()
                .padTop(10f);
    }

    private void showPlants() {
        cardsGrid.clearChildren();

        List<PlantData> plants =
                new ArrayList<>(PlantRegistry.getAll());

        plants.sort(
                Comparator.comparingInt(PlantData::id)
        );

        int column = 0;
        int columnsPerRow = 8;

        ButtonGroup<PlantCard> plantGroup =
                new ButtonGroup<>();

        plantGroup.setMinCheckCount(0);
        plantGroup.setMaxCheckCount(1);
        plantGroup.setUncheckLast(true);

        for (PlantData plant : plants) {
            PlantCard card = new PlantCard(
                    game,
                    new PlantCard.ViewData(
                            plant,
                            true,
                            false,
                            1,
                            0,
                            10
                    )
            );

            plantGroup.add(card);

            card.addListener(new ChangeListener() {
                @Override
                public void changed(
                        ChangeEvent event,
                        Actor actor
                ) {
                    if (card.isChecked()) {
                        showPlantPreview(card);
                    }
                }
            });

            cardsGrid.add(card);

            column++;

            if (column >= columnsPerRow) {
                cardsGrid.row();
                column = 0;
            }
        }

        if (column != 0) {
            while (column < columnsPerRow) {
                cardsGrid.add().expandX();
                column++;
            }
        }
    }

    private void showPlantPreview(
            PlantCard card
    ) {
        previewContent.clearChildren();

        Image plantPreview = new Image(
                drawable(
                        card.getData()
                                .plant()
                                .cardAssetId()
                )
        );

        plantPreview.setScaling(Scaling.none);

        previewContent.add(
                        card.getData()
                                .plant()
                                .name()
                )
                .left()
                .padBottom(10f)
                .row();

        previewContent.add(plantPreview)
                .left()
                .padBottom(12f)
                .row();

        Table buttonsRow = new Table();

        TextButton upgradeButton = new TextButton(
                "UPGRADE",
                game.getSkin()
        );

        TextButton boostButton = new TextButton(
                "BOOST",
                game.getSkin()
        );

        buttonsRow.add(upgradeButton)
                .padRight(10f);

        buttonsRow.add(boostButton);

        previewContent.add(buttonsRow)
                .left();
    }

    private Drawable drawable(String assetId) {
        TextureRegion region =
                game.getTextureBank().region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        return new TextureRegionDrawable(region);
    }
}