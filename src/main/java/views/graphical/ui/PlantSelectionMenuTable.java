package views.graphical.ui;

import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controllers.PlantSelectionController;
import graphics.PvzGame;
import models.App;
import models.Result;
import models.User;
import models.games.Game;

import java.util.*;
import java.util.List;

public final class PlantSelectionMenuTable extends Table {

    private static final String EMPTY_PACKET =
            "IMAGE_UI_SEASONS_UNCOMPRESSED_PVZ2_SEASONS_UIASSET_PRIZE_WINDOW_UPPER_UNLOCKED";

    private static final String TOP_PREVIEW_BACKGROUND =
            "IMAGE_UI_QUESTS_QUEST_PANEL_DAILY";

    private static final int MAX_SELECTED_PLANTS = 8;

    private static final float EMPTY_PACKET_WIDTH = 120f;
    private static final float EMPTY_PACKET_HEIGHT = 75f;

    private static final float PANEL_WIDTH = 650f;

    private static final float PREVIEW_HEIGHT = 190f;

    private final PvzGame game;
    private final Runnable onSelectionComplete;

    private final Table cardsGrid;
    private final Table selectedSlotsTable;
    private final Table previewContent;

    private final PlantSelectionController controller =
            new PlantSelectionController();

    private final PlantData[] selectedSlots =
            new PlantData[MAX_SELECTED_PLANTS];

    private final Map<Integer, PlantCard> gridCardsByPlantId =
            new HashMap<>();

    private Set<Integer> unlockedPlants = Set.of();
    private Map<Integer, Integer> plantLevels = Map.of();
    private Map<Integer, Integer> seedPackets = Map.of();

    public PlantSelectionMenuTable(PvzGame game, Runnable onSelectionComplete) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        this.game = game;
        this.onSelectionComplete = onSelectionComplete;

        setFillParent(true);
        setTouchable(Touchable.enabled);
        pad(16f);

        selectedSlotsTable = new Table();
        selectedSlotsTable.top().left();

        refreshPlantData();
        loadExistingSelectedPlants();
        buildSelectedSlots();

        BorderedPanel outerPanel = new BorderedPanel(
                game,
                Color.valueOf("75452F")
        );

        Table content = outerPanel.getContent();
        content.top().left();

        previewContent = new Table(game.getSkin());
        previewContent.top().left();

        previewContent.setBackground(
                drawable(TOP_PREVIEW_BACKGROUND)
        );

        previewContent.pad(12f);

        buildPreviewPlaceholder();

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
        cardsScroll.setScrollingDisabled(
                true,
                false
        );

        content.add(previewContent)
                .growX()
                .height(PREVIEW_HEIGHT)
                .padLeft(15f)
                .padRight(15f)
                .padTop(20f)
                .padBottom(15f)
                .row();

        content.add(cardsScroll)
                .grow()
                .minWidth(0f)
                .minHeight(0f)
                .padLeft(10f)
                .padRight(10f)
                .padBottom(10f);

        TextButton letsRockButton = new TextButton(
                "LET'S ROCK!",
                game.getSkin(),
                "purple"
        );
        letsRockButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Result result = controller.startGame();
                        if (!result.success()) {
                            game.notifyError(result.message());
                            return;
                        }
                        if (onSelectionComplete != null) {
                            onSelectionComplete.run();
                        }
                    }
                }
        );

        Table mainLayout = new Table();
        mainLayout.top().left();

        mainLayout.add(selectedSlotsTable)
                .top()
                .left()
                .padTop(8f)
                .padRight(4f);

        mainLayout.add(outerPanel)
                .top().left().width(PANEL_WIDTH)
                .growY().minHeight(0f);
        mainLayout.add()
                .expandX();
        Table rockLayer = new Table();
        rockLayer.bottom().right();
        rockLayer.setTouchable(Touchable.childrenOnly);

        rockLayer.add(letsRockButton)
                .right()
                .bottom()
                .padRight(20f)
                .padBottom(20f);


        Stack rootStack = new Stack();
        rootStack.setTouchable(Touchable.childrenOnly);

        rootStack.add(mainLayout);
        rootStack.add(rockLayer);

        add(rootStack)
                .grow()
                .minWidth(0f)
                .minHeight(0f);

        showPlants();
    }
    private void refreshPlantData() {
        User user = App.loggedInUser;

        if (user == null) {
            unlockedPlants = Set.of();
            plantLevels = Map.of();
            seedPackets = Map.of();
            return;
        }

        unlockedPlants =
                PlantRepository.loadUnlockedPlants(
                        user.getId()
                );

        plantLevels =
                PlantRepository.loadPlantLevels(
                        user.getId()
                );

        seedPackets =
                PlantRepository.loadSeedPackets(
                        user.getId()
                );
    }

    private void loadExistingSelectedPlants() {
        Arrays.fill(selectedSlots, null);

        Game currentGame =
                App.getInstance().getCurrentGame();

        if (currentGame == null) {
            return;
        }

        List<PlantData> selected =
                currentGame.getSelectedPlantsForThisGame();

        int count = Math.min(
                selected.size(),
                MAX_SELECTED_PLANTS
        );

        for (int i = 0; i < count; i++) {
            selectedSlots[i] = selected.get(i);
        }
    }

    private void buildSelectedSlots() {
        selectedSlotsTable.clearChildren();

        for (int i = 0; i < MAX_SELECTED_PLANTS; i++) {

            PlantData plant =
                    selectedSlots[i];

            if (plant == null) {
                Image emptySlot =
                        new Image(
                                drawable(EMPTY_PACKET)
                        );

                emptySlot.setScaling(
                        Scaling.stretch
                );

                emptySlot.setTouchable(
                        Touchable.disabled
                );

                selectedSlotsTable.add(emptySlot)
                        .size(
                                EMPTY_PACKET_WIDTH,
                                EMPTY_PACKET_HEIGHT
                        )
                        .padBottom(2f)
                        .row();

            } else {
                PlantCard selectedCard =
                        createSelectedSlotCard(
                                plant,
                                i
                        );

                selectedSlotsTable.add(selectedCard)
                        .size(
                                EMPTY_PACKET_WIDTH,
                                EMPTY_PACKET_HEIGHT
                        )
                        .padBottom(2f)
                        .row();
            }
        }
    }
    private PlantCard createSelectedSlotCard(
            PlantData plant,
            int slotIndex
    ) {
        PlantCard card = new PlantCard(
                game,
                createViewData(plant)
        );

        card.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        removePlantFromSlot(
                                slotIndex
                        );
                    }
                }
        );

        return card;
    }
    private void removePlantFromSlot(
            int slotIndex
    ) {
        PlantData plant =
                selectedSlots[slotIndex];

        if (plant == null) {
            return;
        }

        Result result =
                controller.removePlant(
                        plant.name()
                );

        if (!result.success()) {
            game.notifyError(
                    result.message()
            );

            return;
        }

        selectedSlots[slotIndex] =
                null;

        PlantCard gridCard =
                gridCardsByPlantId.get(
                        plant.id()
                );

        if (gridCard != null) {
            gridCard.setChecked(false);
        }

        buildSelectedSlots();
    }
    private void selectPlantIntoSlot(
            PlantCard card
    ) {
        PlantData plant =
                card.getData().plant();

        if (isPlantAlreadySelected(plant)) {
            return;
        }

        int emptySlot =
                findFirstEmptySlot();

        if (emptySlot == -1) {
            game.notifyError(
                    "Plant selection is full."
            );

            card.setChecked(false);
            return;
        }

        Result result =
                controller.addPlant(
                        plant.name()
                );

        if (!result.success()) {
            game.notifyError(
                    result.message()
            );

            card.setChecked(false);
            return;
        }

        selectedSlots[emptySlot] =
                plant;

        buildSelectedSlots();
    }
    private int findFirstEmptySlot() {
        for (int i = 0; i < selectedSlots.length; i++) {
            if (selectedSlots[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private boolean isPlantAlreadySelected(
            PlantData plant
    ) {
        for (PlantData selected : selectedSlots) {
            if (plant.equals(selected)) {
                return true;
            }
        }

        return false;
    }
    private PlantCard.ViewData createViewData(
            PlantData plant
    ) {
        User user = App.loggedInUser;

        boolean unlocked =
                unlockedPlants.contains(
                        plant.id()
                );

        boolean boosted =
                user != null
                        && unlocked
                        && PlantBoostRepository.hasBoost(
                        user.getId(),
                        plant.id()
                );

        int level =
                plantLevels.getOrDefault(
                        plant.id(),
                        1
                );

        int packets =
                seedPackets.getOrDefault(
                        plant.id(),
                        0
                );

        int requiredPackets =
                requiredSeedPackets(
                        plant,
                        level
                );

        return new PlantCard.ViewData(
                plant,
                unlocked,
                boosted,
                level,
                packets,
                requiredPackets,
                true
        );
    }

    private void buildPreviewPlaceholder() {
        previewContent.clearChildren();

        Label selectLabel = new Label(
                "Select a plant",
                game.getSkin()
        );

        previewContent.add(selectLabel)
                .left()
                .padLeft(15f)
                .padTop(10f)
                .padBottom(10f)
                .row();

        previewContent.add()
                .growX()
                .height(85f)
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
                .padLeft(15f)
                .padBottom(10f);
    }

    private void showPlants() {
        cardsGrid.clearChildren();
        gridCardsByPlantId.clear();

        User user = App.loggedInUser;

        if (user == null) {
            return;
        }

        refreshPlantData();

        List<PlantData> plants =
                new ArrayList<>(
                        PlantRegistry.getAll()
                );

        plants.sort(
                Comparator.comparingInt(
                        PlantData::id
                )
        );

        int column = 0;
        int columnsPerRow = 4;

        ButtonGroup<PlantCard> plantGroup =
                new ButtonGroup<>();

        plantGroup.setMinCheckCount(0);
        plantGroup.setMaxCheckCount(1);
        plantGroup.setUncheckLast(true);

        for (PlantData plant : plants) {

            boolean unlocked =
                    unlockedPlants.contains(
                            plant.id()
                    );

            PlantCard card =
                    new PlantCard(
                            game,
                            createViewData(plant)
                    );

            gridCardsByPlantId.put(
                    plant.id(),
                    card
            );

            if (unlocked) {
                plantGroup.add(card);

                card.addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(
                                    ChangeEvent event,
                                    Actor actor
                            ) {
                                if (!card.isChecked()) {
                                    return;
                                }

                                showPlantPreview(card);

                                selectPlantIntoSlot(card);
                            }
                        }
                );
            } else {
                card.setChecked(false);
                card.setDisabled(true);
                card.setTouchable(
                        Touchable.disabled
                );
            }

            cardsGrid.add(card);

            column++;

            if (column >= columnsPerRow) {
                cardsGrid.row();
                column = 0;
            }
        }

        if (column != 0) {
            while (column < columnsPerRow) {
                cardsGrid.add()
                        .expandX();

                column++;
            }
        }
    }
    private int requiredSeedPackets(
            PlantData plant,
            int currentLevel
    ) {
        int maxLevel =
                plant.upgrades() == null
                        ? 1
                        : plant.upgrades().size() + 1;

        if (currentLevel >= maxLevel) {
            return 1;
        }

        int targetLevel =
                currentLevel + 1;

        return switch (targetLevel) {
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default ->
                    20 * Math.max(
                            1,
                            targetLevel - 3
                    );
        };
    }

    private void showPlantPreview(
            PlantCard card
    ) {
        previewContent.clearChildren();

        PlantData plant =
                card.getData().plant();

        Image plantPreview = new Image(
                drawable(
                        plant.cardAssetId()
                )
        );

        plantPreview.setScaling(
                Scaling.none
        );

        Label plantName = new Label(
                plant.name(),
                game.getSkin()
        );

        Table plantArea = new Table();
        plantArea.left();

        plantArea.add(plantPreview)
                .left()
                .padRight(15f);

        plantArea.add(plantName)
                .left()
                .expandY()
                .top()
                .padTop(20f);

        previewContent.add(plantArea)
                .growX()
                .left()
                .padLeft(15f)
                .padTop(10f)
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
                .padLeft(15f)
                .padTop(5f)
                .padBottom(10f);
    }

    private Drawable drawable(
            String assetId
    ) {
        TextureRegion region =
                game.getTextureBank()
                        .region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        return new TextureRegionDrawable(
                region
        );
    }
}