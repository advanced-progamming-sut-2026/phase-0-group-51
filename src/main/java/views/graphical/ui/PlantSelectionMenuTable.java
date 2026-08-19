package views.graphical.ui;

import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controllers.PlantSelectionController;
import controllers.CollectionMenuController;
import graphics.PvzGame;
import models.App;
import models.Result;
import models.User;
import models.Plant.PlantTag;
import models.games.Game;
import models.games.LevelType;

import java.util.*;
import java.util.List;

public final class PlantSelectionMenuTable extends Table {

    private static final String TOP_PREVIEW_BACKGROUND =
        "IMAGE_UI_QUESTS_QUEST_PANEL_DAILY";

    private static final float PANEL_WIDTH = 650f;
    private static final String PLANT_WHAT_YOU_GET_LOCK = "IMAGE_UI_JOUST_LOCK_SMALL";

    private static final float PREVIEW_HEIGHT = 190f;

    private static final float SCREEN_PADDING = 16f;
    private static final float PLANT_SLOTS_TOP_OFFSET = 75f;
    private static final float PLANT_SLOTS_CELL_TOP_PADDING =
        PLANT_SLOTS_TOP_OFFSET - SCREEN_PADDING;

    private final PvzGame game;
    private final Runnable onSelectionComplete;

    private final Table cardsGrid;
    private final PlantSlotsBar plantSlotsBar;
    private final Table previewContent;
    private final PlantSelectionController controller =
        new PlantSelectionController();
    private final CollectionMenuController collectionController =
        new CollectionMenuController();

    private final Map<Integer, PlantCard> gridCardsByPlantId =
        new HashMap<>();

    private Set<Integer> unlockedPlants = Set.of();
    private Map<Integer, Integer> plantLevels = Map.of();
    private Map<Integer, Integer> seedPackets = Map.of();

    public PlantSelectionMenuTable(
        PvzGame game,
        PlantSlotsBar plantSlotsBar,
        Runnable onSelectionComplete
    ) {
        if (game == null) {
            throw new IllegalArgumentException(
                "game cannot be null"
            );
        }

        if (plantSlotsBar == null) {
            throw new IllegalArgumentException(
                "plantSlotsBar cannot be null"
            );
        }

        this.game = game;
        this.plantSlotsBar = plantSlotsBar;
        this.onSelectionComplete = onSelectionComplete;

        setFillParent(true);
        setTouchable(Touchable.enabled);

        pad(SCREEN_PADDING);

        refreshPlantData();

        plantSlotsBar.setMode(
            PlantSlotsBar.Mode.SELECTION
        );
        plantSlotsBar.setOnRemoveRequested(
            this::removePlantFromSlot
        );
        loadExistingSelectedPlants();

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

        mainLayout.add(plantSlotsBar)
            .top()
            .left()
            .padTop(PLANT_SLOTS_CELL_TOP_PADDING)
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
        Game currentGame =
            App.getInstance().getCurrentGame();

        if (currentGame == null) {
            plantSlotsBar.loadPlants(List.of());
            return;
        }

        plantSlotsBar.loadPlants(
            currentGame.getSelectedPlantsForThisGame()
        );
    }

    private void removePlantFromSlot(
        int slotIndex
    ) {
        PlantData plant =
            plantSlotsBar.getPlant(slotIndex);

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

        plantSlotsBar.removePlant(slotIndex);

        PlantCard gridCard =
            gridCardsByPlantId.get(
                plant.id()
            );

        if (gridCard != null) {
            gridCard.setChecked(false);
        }
    }

    private void selectPlantIntoSlot(
        PlantCard card
    ) {
        PlantData plant =
            card.getData().plant();

        if (plantSlotsBar.contains(plant)) {
            return;
        }

        if (plantSlotsBar.isFull()) {
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

        plantSlotsBar.addPlant(plant);
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

            boolean forbiddenForPlantWhatYouGet =
                isForbiddenForPlantWhatYouGet(plant);

            boolean specialSunProducerLock =
                isPlantWhatYouGetSunProducer(plant);

            if (forbiddenForPlantWhatYouGet) {
                card.setLocked(true);

                if (specialSunProducerLock) {
                    card.setLockAsset(
                        PLANT_WHAT_YOU_GET_LOCK
                    );
                }

                card.setChecked(false);
                card.setDisabled(true);
                card.setTouchable(
                    Touchable.disabled
                );
            } else if (unlocked) {
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
    private boolean isForbiddenForPlantWhatYouGet(
        PlantData plant
    ) {
        Game currentGame =
            App.getInstance().getCurrentGame();

        if (currentGame == null
            || plant == null
            || currentGame.getSelectedLevel().type()
            != LevelType.PLANT_WHAT_YOU_GET) {
            return false;
        }

        return isSunProducer(plant)
            || plant.id() == 58
            || plant.tags().contains(
            PlantTag.WATER
        );
    }

    private boolean isPlantWhatYouGetSunProducer(
        PlantData plant
    ) {
        Game currentGame =
            App.getInstance().getCurrentGame();

        return currentGame != null
            && plant != null
            && currentGame.getSelectedLevel().type()
            == LevelType.PLANT_WHAT_YOU_GET
            && isSunProducer(plant);
    }

    private boolean isSunProducer(
        PlantData plant
    ) {
        String category =
            plant.category() == null
                ? ""
                : plant.category()
                  .replaceAll(
                      "[^A-Za-z]",
                      ""
                  )
                  .toLowerCase();

        return category.equals(
            "sunproducer"
        )
            || plant.tags().contains(
            PlantTag.SUN
        )
            || (
            plant.id() >= 1
                && plant.id() <= 5
        );
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

        upgradeButton.addListener(
            new ChangeListener() {
                @Override
                public void changed(
                    ChangeEvent event,
                    Actor actor
                ) {
                    handlePlantManagementResult(
                        collectionController.upgrade(
                            plant.name()
                        )
                    );
                }
            }
        );

        boostButton.addListener(
            new ChangeListener() {
                @Override
                public void changed(
                    ChangeEvent event,
                    Actor actor
                ) {
                    handlePlantManagementResult(
                        controller.boostPlant(
                            plant.name()
                        )
                    );
                }
            }
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

    private void handlePlantManagementResult(
        Result result
    ) {
        if (result == null) {
            return;
        }

        if (!result.success()) {
            game.notifyError(
                result.message()
            );
            return;
        }

        game.notifyInfo(
            result.message()
        );

        refreshPlantData();
        loadExistingSelectedPlants();
        showPlants();
        buildPreviewPlaceholder();
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
