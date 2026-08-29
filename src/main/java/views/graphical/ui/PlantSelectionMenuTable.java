package views.graphical.ui;

import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controllers.CollectionMenuController;
import controllers.PlantSelectionController;
import graphics.PvzGame;
import models.App;
import models.Plant.PlantTag;
import models.Result;
import models.User;
import models.games.Game;
import models.games.LevelType;
import models.games.darkAges.LockedPlantsMode;
import network.client.ClientPlantOwnershipState;
import network.protocol.plants.PlantOwnershipResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PlantSelectionMenuTable extends Table {

    private static final String TOP_PREVIEW_BACKGROUND = "IMAGE_UI_QUESTS_QUEST_PANEL_DAILY";
    private static final String PLANT_WHAT_YOU_GET_LOCK = "IMAGE_UI_JOUST_LOCK_SMALL";
    private static final String FAMILY_LOCK_ICON = "IMAGE_UI_JOUST_LOCK_SMALL";

    private static final float PANEL_WIDTH = 650f;
    private static final float PREVIEW_HEIGHT = 190f;
    private static final float SCREEN_PADDING = 16f;
    private static final float PLANT_SLOTS_TOP_OFFSET = 75f;
    private static final float PLANT_SLOTS_CELL_TOP_PADDING = PLANT_SLOTS_TOP_OFFSET - SCREEN_PADDING;

    private final PvzGame game;
    private final Runnable onSelectionComplete;

    private final Table cardsGrid;
    private final PlantSlotsBar plantSlotsBar;
    private final Table previewContent;
    private final PlantSelectionController controller = new PlantSelectionController();
    private final CollectionMenuController collectionController = new CollectionMenuController();

    private final Map<Integer, PlantCard> gridCardsByPlantId = new HashMap<>();

    private Set<Integer> unlockedPlants = Set.of();
    private Map<Integer, Integer> plantLevels = Map.of();
    private Map<Integer, Integer> seedPackets = Map.of();

    private Table lockedModeLayer;
    private boolean ownershipRequestInFlight;

    public PlantSelectionMenuTable(PvzGame game, PlantSlotsBar plantSlotsBar, Runnable onSelectionComplete) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        if (plantSlotsBar == null) {
            throw new IllegalArgumentException("plantSlotsBar cannot be null");
        }

        this.game = game;
        this.plantSlotsBar = plantSlotsBar;
        this.onSelectionComplete = onSelectionComplete;

        setFillParent(true);
        setTouchable(Touchable.enabled);
        pad(SCREEN_PADDING);

        refreshPlantData();

        plantSlotsBar.setMode(PlantSlotsBar.Mode.SELECTION);
        plantSlotsBar.setOnRemoveRequested(this::removePlantFromSlot);
        loadExistingSelectedPlants();

        BorderedPanel outerPanel = new BorderedPanel(game, Color.valueOf("75452F"));
        Table content = outerPanel.getContent();
        content.top().left();

        previewContent = new Table(game.getSkin());
        previewContent.top().left();
        previewContent.setBackground(drawable(TOP_PREVIEW_BACKGROUND));
        previewContent.pad(12f);
        buildPreviewPlaceholder();

        cardsGrid = new Table();
        cardsGrid.top().left();
        cardsGrid.defaults().expandX().top().pad(8f);

        ScrollPane cardsScroll = new ScrollPane(cardsGrid, game.getSkin());
        cardsScroll.setFadeScrollBars(false);
        cardsScroll.setOverscroll(false, false);
        cardsScroll.setScrollingDisabled(true, false);

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

        TextButton letsRockButton = new TextButton("LET'S ROCK!", game.getSkin(), "purple");
        letsRockButton.addListener(new ChangeListener() {
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
        });

        Table mainLayout = new Table();
        mainLayout.top().left();
        mainLayout.add(plantSlotsBar)
            .top()
            .left()
            .padTop(PLANT_SLOTS_CELL_TOP_PADDING)
            .padRight(4f);

        mainLayout.add(outerPanel)
            .top()
            .left()
            .width(PANEL_WIDTH)
            .growY()
            .minHeight(0f);

        mainLayout.add().expandX();

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

        lockedModeLayer = buildLockedPlantsModeLayer();
        rootStack.add(lockedModeLayer);

        add(rootStack).grow().minWidth(0f).minHeight(0f);

        showPlants();
    }

    private Table buildLockedPlantsModeLayer() {
        Table layer = new Table();

        Game currentGame = App.getInstance().getCurrentGame();

        boolean shouldShow =
            currentGame != null
                && currentGame.isLockedPlantsLevel()
                && !currentGame.hasChosenLockedPlantsMode();

        layer.setVisible(shouldShow);

        if (!shouldShow) {
            layer.setTouchable(Touchable.disabled);
            return layer;
        }

        layer.setTouchable(Touchable.enabled);
        layer.setBackground(
            game.getSkin().newDrawable(
                "white_pixel",
                new Color(0f, 0f, 0f, 0.55f)
            )
        );

        BorderedPanel panel = new BorderedPanel(
            game,
            Color.valueOf("75452F")
        );

        Table content = panel.getContent();
        content.clearChildren();
        content.pad(22f, 28f, 24f, 28f);
        content.center();

        Label title = new Label(
            "LOCKED PLANTS",
            game.getSkin()
        );
        title.setColor(Color.valueOf("FFE06A"));
        title.setFontScale(1.18f);

        Label description = new Label(
            "Choose how this level locks your plants.",
            game.getSkin()
        );
        description.setColor(Color.WHITE);
        description.setWrap(true);

        TextButton familyButton = new TextButton(
            "FAMILY LOCK",
            game.getSkin(),
            "brown"
        );

        TextButton forcedButton = new TextButton(
            "FORCED LOADOUT",
            game.getSkin(),
            "purple"
        );

        Label familyInfo = new Label(
            "One random unlocked plant stays available from each restricted family.",
            game.getSkin()
        );
        familyInfo.setWrap(true);
        familyInfo.setColor(Color.LIGHT_GRAY);

        Label forcedInfo = new Label(
            "All eight slots are filled automatically and cannot be changed.",
            game.getSkin()
        );
        forcedInfo.setWrap(true);
        forcedInfo.setColor(Color.LIGHT_GRAY);

        familyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                chooseLockedPlantsMode(
                    LockedPlantsMode.FAMILY
                );
            }
        });

        forcedButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                chooseLockedPlantsMode(
                    LockedPlantsMode.FORCED
                );
            }
        });

        content.add(title)
            .center()
            .padBottom(8f)
            .row();

        content.add(description)
            .width(360f)
            .center()
            .padBottom(18f)
            .row();

        content.add(familyButton)
            .width(225f)
            .height(52f)
            .center()
            .padBottom(6f)
            .row();

        content.add(familyInfo)
            .width(360f)
            .center()
            .padBottom(18f)
            .row();

        content.add(forcedButton)
            .width(225f)
            .height(52f)
            .center()
            .padBottom(6f)
            .row();

        content.add(forcedInfo)
            .width(360f)
            .center()
            .row();

        layer.add(panel)
            .width(450f)
            .center();

        return layer;
    }

    private void chooseLockedPlantsMode(
        LockedPlantsMode mode
    ) {
        Game currentGame = App.getInstance().getCurrentGame();

        if (currentGame == null
            || !currentGame.isLockedPlantsLevel()) {
            game.notifyError(
                "Locked Plants level is not active."
            );
            return;
        }

        try {
            currentGame.chooseLockedPlantsMode(mode);
        } catch (RuntimeException exception) {
            String message = exception.getMessage();

            if (message == null || message.isBlank()) {
                message = "Could not prepare Locked Plants mode.";
            }

            game.notifyError(message);
            return;
        }

        loadExistingSelectedPlants();
        showPlants();
        buildPreviewPlaceholder();

        if (lockedModeLayer != null) {
            lockedModeLayer.setVisible(false);
            lockedModeLayer.setTouchable(Touchable.disabled);
        }

        if (mode == LockedPlantsMode.FAMILY) {
            game.notifyInfo("Family Lock selected.");
        } else {
            game.notifyInfo("Forced Loadout selected.");
        }
    }

    private void refreshPlantData() {
        User user = App.loggedInUser;
        if (user == null) {
            unlockedPlants = Set.of();
            plantLevels = Map.of();
            seedPackets = Map.of();
            return;
        }

        unlockedPlants = ClientPlantOwnershipState.snapshot();
        plantLevels = PlantRepository.loadPlantLevels(user.getId());
        seedPackets = PlantRepository.loadSeedPackets(user.getId());
    }

    private void loadExistingSelectedPlants() {
        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null) {
            plantSlotsBar.loadPlants(List.of());
            return;
        }
        plantSlotsBar.loadPlants(currentGame.getSelectedPlantsForThisGame());
    }

    private void removePlantFromSlot(int slotIndex) {
        PlantData plant = plantSlotsBar.getPlant(slotIndex);
        if (plant == null) {
            return;
        }

        Result result = controller.removePlant(plant.name());
        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        plantSlotsBar.removePlant(slotIndex);
        PlantCard gridCard = gridCardsByPlantId.get(plant.id());
        if (gridCard != null) {
            gridCard.setChecked(false);
        }
    }

    private void selectPlantIntoSlot(PlantCard card) {
        PlantData plant = card.getData().plant();

        if (plantSlotsBar.contains(plant)) {
            return;
        }
        if (plantSlotsBar.isFull()) {
            game.notifyError("Plant selection is full.");
            card.setChecked(false);
            return;
        }

        Result result = controller.addPlant(plant.name());
        if (!result.success()) {
            game.notifyError(result.message());
            card.setChecked(false);
            return;
        }

        plantSlotsBar.addPlant(plant);
    }

    private PlantCard.ViewData createViewData(PlantData plant) {
        User user = App.loggedInUser;
        boolean unlocked = unlockedPlants.contains(plant.id());
        boolean boosted = user != null && unlocked && PlantBoostRepository.hasBoost(user.getId(), plant.id());
        int level = plantLevels.getOrDefault(plant.id(), 1);
        int packets = seedPackets.getOrDefault(plant.id(), 0);
        int requiredPackets = requiredSeedPackets(plant, level);
        return new PlantCard.ViewData(plant, unlocked, boosted, level, packets, requiredPackets, true);
    }

    private void buildPreviewPlaceholder() {
        previewContent.clearChildren();

        Game currentGame = App.getInstance().getCurrentGame();
        String title = "Select a plant";
        String detail = "Choose plants for this level.";

        if (currentGame != null && currentGame.isLockedPlantsLevel() && currentGame.hasChosenLockedPlantsMode()) {
            if (currentGame.getLockedPlantsMode() == LockedPlantsMode.FAMILY) {
                title = "FAMILY LOCK";
                detail = "Grey cards are blocked. Cards marked CHOICE are the only allowed plants in their family.";
            } else if (currentGame.getLockedPlantsMode() == LockedPlantsMode.FORCED) {
                title = "FORCED LOADOUT";
                detail = "The eight plants in the left slots are fixed and cannot be removed.";
            }
        } else if (currentGame != null && currentGame.getSelectedLevel().type() == LevelType.PLANT_WHAT_YOU_GET) {
            title = "PLANT WHAT YOU GET";
            detail = "Sun producers and water-only plants are locked in this level.";
        }

        Label titleLabel = new Label(title, game.getSkin());
        titleLabel.setFontScale(1.05f);
        previewContent.add(titleLabel)
            .left()
            .padLeft(15f)
            .padTop(6f)
            .row();

        Label detailLabel = new Label(detail, game.getSkin());
        detailLabel.setWrap(true);
        previewContent.add(detailLabel)
            .growX()
            .width(560f)
            .left()
            .padLeft(15f)
            .padTop(6f)
            .row();

        previewContent.add().growX().height(62f).row();

        Table buttonsRow = new Table();
        TextButton upgradeButton = new TextButton("UPGRADE", game.getSkin());
        TextButton boostButton = new TextButton("BOOST", game.getSkin());
        buttonsRow.add(upgradeButton).padRight(10f);
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

        if (!ClientPlantOwnershipState.isLoaded()) {
            requestPlantOwnership();
            return;
        }

        refreshPlantData();

        List<PlantData> plants = new ArrayList<>(PlantRegistry.getAll());
        plants.sort(Comparator.comparingInt(PlantData::id));

        int column = 0;
        int columnsPerRow = 4;

        ButtonGroup<PlantCard> plantGroup = new ButtonGroup<>();
        plantGroup.setMinCheckCount(0);
        plantGroup.setMaxCheckCount(1);
        plantGroup.setUncheckLast(true);

        Game currentGame = App.getInstance().getCurrentGame();

        for (PlantData plant : plants) {
            boolean unlocked = unlockedPlants.contains(plant.id());
            PlantCard card = new PlantCard(game, createViewData(plant));
            gridCardsByPlantId.put(plant.id(), card);

            boolean forbiddenForPlantWhatYouGet = isForbiddenForPlantWhatYouGet(plant);
            boolean specialSunProducerLock = isPlantWhatYouGetSunProducer(plant);
            boolean familyLocked = currentGame != null && currentGame.isPlantLockedByFamilyMode(plant);
            boolean familyChoice = currentGame != null && currentGame.isFamilyChoicePlant(plant);
            boolean forcedMode = currentGame != null && currentGame.isForcedLoadoutMode();
            boolean forcedPlant = currentGame != null && currentGame.isForcedLockedPlant(plant);

            if (forbiddenForPlantWhatYouGet) {
                card.setLocked(true);
                card.setBadgeText(isSunProducer(plant) ? "SUN" : "LOCKED");
                if (specialSunProducerLock) {
                    card.setLockAsset(PLANT_WHAT_YOU_GET_LOCK);
                }
                card.setChecked(false);
                card.setDisabled(true);
                card.setTouchable(Touchable.disabled);
            } else if (forcedMode) {
                if (forcedPlant) {
                    card.setHighlighted(true);
                    card.setBadgeText("FORCED");
                } else {
                    card.setLocked(true);
                    card.setBadgeText("LOCKED");
                }
                card.setChecked(false);
                card.setDisabled(true);
                card.setTouchable(Touchable.disabled);
            } else if (familyLocked) {
                card.setLocked(true);
                card.setLockAsset(FAMILY_LOCK_ICON);
                card.setBadgeText("LOCKED");
                card.setChecked(false);
                card.setDisabled(true);
                card.setTouchable(Touchable.disabled);
            } else if (unlocked) {
                if (familyChoice) {
                    card.setHighlighted(true);
                    card.setBadgeText("CHOICE");
                }

                plantGroup.add(card);
                card.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (!card.isChecked()) {
                            return;
                        }
                        showPlantPreview(card);
                        selectPlantIntoSlot(card);
                    }
                });
            } else {
                card.setChecked(false);
                card.setDisabled(true);
                card.setTouchable(Touchable.disabled);
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
                cardsGrid.add().expandX();
                column++;
            }
        }
    }

    private void requestPlantOwnership() {
        if (ownershipRequestInFlight) {
            return;
        }

        ownershipRequestInFlight = true;
        cardsGrid.clearChildren();
        cardsGrid.add(
                new Label(
                        "LOADING PLANT OWNERSHIP...",
                        game.getSkin()
                )
        ).padTop(40f);

        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(
                        ignored -> sendPlantOwnershipRequest()
                )
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishPlantOwnershipRequest(
                                                response,
                                                throwable
                                        )
                                )
                );
    }

    private CompletableFuture<PlantOwnershipResponse>
    sendPlantOwnershipRequest() {
        try {
            return game.getNetworkManager()
                    .getPlantOwnershipClientService()
                    .getOwnership();
        } catch (IOException | RuntimeException exception) {
            return failedFuture(exception);
        }
    }

    private void finishPlantOwnershipRequest(
            PlantOwnershipResponse response,
            Throwable throwable
    ) {
        ownershipRequestInFlight = false;

        if (throwable != null) {
            showOwnershipLoadFailure(
                    "Could not load plant ownership: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()) {
            showOwnershipLoadFailure(
                    response == null
                            ? "Could not load plant ownership."
                            : response.getMessage()
            );
            return;
        }

        ClientPlantOwnershipState.replaceWith(
                response.getUnlockedPlantIds()
        );

        showPlants();
    }

    private void showOwnershipLoadFailure(String message) {
        cardsGrid.clearChildren();
        Label label = new Label(
                "COULD NOT LOAD PLANTS",
                game.getSkin()
        );
        label.setColor(Color.RED);
        cardsGrid.add(label).padTop(40f);
        game.notifyError(message);
    }

    private static <T> CompletableFuture<T> failedFuture(
            Throwable throwable
    ) {
        CompletableFuture<T> future =
                new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private boolean isForbiddenForPlantWhatYouGet(PlantData plant) {
        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || plant == null || currentGame.getSelectedLevel().type() != LevelType.PLANT_WHAT_YOU_GET) {
            return false;
        }
        return isSunProducer(plant) || plant.id() == 58 || plant.tags().contains(PlantTag.WATER);
    }

    private boolean isPlantWhatYouGetSunProducer(PlantData plant) {
        Game currentGame = App.getInstance().getCurrentGame();
        return currentGame != null
            && plant != null
            && currentGame.getSelectedLevel().type() == LevelType.PLANT_WHAT_YOU_GET
            && isSunProducer(plant);
    }

    private boolean isSunProducer(PlantData plant) {
        String category = plant.category() == null ? "" : plant.category().replaceAll("[^A-Za-z]", "").toLowerCase();
        return category.equals("sunproducer") || plant.tags().contains(PlantTag.SUN) || (plant.id() >= 1 && plant.id() <= 5);
    }

    private int requiredSeedPackets(PlantData plant, int currentLevel) {
        int maxLevel = plant.upgrades() == null ? 1 : plant.upgrades().size() + 1;
        if (currentLevel >= maxLevel) {
            return 1;
        }

        int targetLevel = currentLevel + 1;
        return switch (targetLevel) {
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default -> 20 * Math.max(1, targetLevel - 3);
        };
    }

    private void showPlantPreview(PlantCard card) {
        previewContent.clearChildren();

        PlantData plant = card.getData().plant();
        Game currentGame = App.getInstance().getCurrentGame();

        Image plantPreview = new Image(drawable(plant.cardAssetId()));
        plantPreview.setScaling(Scaling.none);

        Label plantName = new Label(plant.name(), game.getSkin());
        plantName.setFontScale(1.05f);

        Label statusLabel = null;
        if (currentGame != null && currentGame.isFamilyChoicePlant(plant)) {
            statusLabel = new Label("Family choice for " + plant.category(), game.getSkin());
            statusLabel.setColor(1f, 0.9f, 0.45f, 1f);
        } else if (currentGame != null && currentGame.isForcedLockedPlant(plant)) {
            statusLabel = new Label("Forced plant for this level", game.getSkin());
            statusLabel.setColor(1f, 0.9f, 0.45f, 1f);
        }

        Table plantArea = new Table();
        plantArea.left();
        plantArea.add(plantPreview).left().padRight(15f);

        Table nameStack = new Table();
        nameStack.left().top();
        nameStack.add(plantName).left().row();
        if (statusLabel != null) {
            nameStack.add(statusLabel).left().padTop(6f).row();
        }

        plantArea.add(nameStack).left().expandY().top().padTop(20f);

        previewContent.add(plantArea)
            .growX()
            .left()
            .padLeft(15f)
            .padTop(10f)
            .row();

        Table buttonsRow = new Table();
        TextButton upgradeButton = new TextButton("UPGRADE", game.getSkin());
        TextButton boostButton = new TextButton("BOOST", game.getSkin());

        upgradeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handlePlantManagementResult(collectionController.upgrade(plant.name()));
            }
        });

        boostButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handlePlantManagementResult(controller.boostPlant(plant.name()));
            }
        });

        buttonsRow.add(upgradeButton).padRight(10f);
        buttonsRow.add(boostButton);

        previewContent.add(buttonsRow)
            .left()
            .padLeft(15f)
            .padTop(5f)
            .padBottom(10f);
    }

    private void handlePlantManagementResult(Result result) {
        if (result == null) {
            return;
        }
        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        game.notifyInfo(result.message());
        refreshPlantData();
        loadExistingSelectedPlants();
        showPlants();
        buildPreviewPlaceholder();
    }

    private Drawable drawable(String assetId) {
        TextureRegion region = game.getTextureBank().region(assetId);
        if (region == null) {
            throw new IllegalStateException("TextureBank region was not found: " + assetId);
        }
        return new TextureRegionDrawable(region);
    }
}
