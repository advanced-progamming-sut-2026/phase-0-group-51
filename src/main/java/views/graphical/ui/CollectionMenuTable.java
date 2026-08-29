package views.graphical.ui;

import Data.database.NewsRepository;
import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import graphics.PvzGame;
import models.App;
import models.User;
import models.Zombie.Zombie;
import network.client.ClientPlantOwnershipState;
import network.client.ClientShopState;
import network.protocol.plants.PlantOwnershipResponse;
import network.protocol.shop.ShopResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CollectionMenuTable extends Table {
    private static final String PLANTS_ICON = "IMAGE_UI_STORE_TABICONS_PLANTS";
    private static final String ZOMBIES_ICON = "IMAGE_UI_STORE_TABICONS_ZOMBIES";
    private static final String PLANTS_TAB = "IMAGE_UI_ALMANAC_TABS_PLANTS_DOWN";
    private static final String PLANTS_TAB_SELECTED = "IMAGE_UI_ALMANAC_TABS_PLANTS_ACTIVE";
    private static final String ZOMBIES_TAB = "IMAGE_UI_ALMANAC_TABS_ZOMBIES_DOWN";
    private static final String ZOMBIES_TAB_SELECTED = "IMAGE_UI_ALMANAC_TABS_ZOMBIES_ACTIVE";
    private static final String CLOSE = "IMAGE_UI_ALMANAC_TABS_CLOSE_TAB";
    private static final String CLOSE_HOVER = "IMAGE_UI_ALMANAC_TABS_CLOSE_TAB_DOWN";

    private final PvzGame game;
    private final Table cardsGrid;
    private static final String ALL_FAMILIES = "ALL FAMILIES";
    private String selectedFamily = ALL_FAMILIES;
    private OwnershipFilter selectedOwnership = OwnershipFilter.ALL;
    private UpgradeFilter selectedUpgrade = UpgradeFilter.ALL;
    private boolean ownershipRequestInFlight;
    private boolean plantStateRequestInFlight;
    private boolean plantStateLoadedForView;

    private enum OwnershipFilter {
        ALL("ALL"),
        UNLOCKED("UNLOCKED"),
        LOCKED("LOCKED");
        private final String title;
        OwnershipFilter(String title) {
            this.title = title;
        }
        @Override
        public String toString() {
            return title;
        }
    }

    private enum UpgradeFilter {
        ALL("ALL"),
        UPGRADEABLE("UPGRADEABLE"),
        NOT_UPGRADEABLE("NOT UPGRADEABLE");
        private final String title;
        UpgradeFilter(String title) {
            this.title = title;
        }
        @Override
        public String toString() {
            return title;
        }
    }
    public CollectionMenuTable(PvzGame game) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }


        this.game = game;

        setFillParent(true);
        setTouchable(Touchable.enabled);
        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0f,
                                0f,
                                0f,
                                0.55f
                        )
                )
        );
        pad(22f);

        BorderedPanel outerPanel = new BorderedPanel(game, Color.valueOf("75452F"));
        Table content = outerPanel.getContent();
        content.top();

        cardsGrid = new Table();
        cardsGrid.top().left();

        ScrollPane cardsScroll = new ScrollPane(cardsGrid, game.getSkin());

        cardsScroll.setFadeScrollBars(false);
        cardsScroll.setOverscroll(false, false);
        cardsScroll.setScrollingDisabled(true, false);

        content.add(cardsScroll).grow().minWidth(0f).minHeight(0f);
        ImageButton plantsTab = createTabButton(PLANTS_TAB, PLANTS_TAB_SELECTED, PLANTS_ICON);
        ImageButton zombiesTab = createTabButton(ZOMBIES_TAB, ZOMBIES_TAB_SELECTED, ZOMBIES_ICON);
        ImageButton closeButton = createCloseButton();
        ButtonGroup<ImageButton> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.setUncheckLast(true);
        tabs.add(plantsTab);
        tabs.add(zombiesTab);
        float tabHeight = plantsTab.getPrefHeight();
        float panelTopOffset = tabHeight * 0.75f;
        float leftPadding = 40f;
        float tabGap = 8f;

        Stack menuStack = new Stack();
        menuStack.setTouchable(Touchable.childrenOnly);
        Table panelLayer = new Table();
        panelLayer.top();
        panelLayer.setTouchable(Touchable.childrenOnly);
        panelLayer.add(outerPanel).grow().minWidth(0f).minHeight(0f).padTop(panelTopOffset);

        Table plantsLayer = new Table();
        plantsLayer.top().left();
        plantsLayer.setTouchable(Touchable.childrenOnly);
        plantsLayer.add(plantsTab).padLeft(leftPadding);

        Table zombiesLayer = new Table();
        zombiesLayer.top().left();
        zombiesLayer.setTouchable(Touchable.childrenOnly);
        zombiesLayer.add(zombiesTab).padLeft(leftPadding + plantsTab.getPrefWidth() + tabGap);

        Table closeLayer = new Table();
        closeLayer.top().right();
        closeLayer.setTouchable(Touchable.childrenOnly);
        float closeButtonOffset = panelTopOffset - (closeButton.getPrefHeight()) + 2;
        closeLayer.add(closeButton).padTop(closeButtonOffset).padRight(50f);

        menuStack.add(zombiesLayer);
        menuStack.add(plantsLayer);
        menuStack.add(panelLayer);
        menuStack.add(closeLayer);

        add(menuStack).grow().minWidth(0f).minHeight(0f);

        plantsTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (plantsTab.isChecked()) {
                    plantsLayer.toFront();
                    closeLayer.toFront();
                    showPlants();
                } else {
                    plantsLayer.toBack();
                }
            }
        });

        zombiesTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (zombiesTab.isChecked()) {
                    zombiesLayer.toFront();
                    closeLayer.toFront();
                    showZombies();
                } else {
                    zombiesLayer.toBack();
                }
            }
        });

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CollectionMenuTable.this.remove();
            }
        });

        plantsTab.setChecked(true);
        plantsLayer.toFront();
        closeLayer.toFront();
        showPlants();
    }
    private ImageButton createTabButton(
            String normalBackground,
            String selectedBackground,
            String iconAsset
    ) {
        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.up = drawable(normalBackground);
        style.down = drawable(selectedBackground);
        style.checked = drawable(selectedBackground);

        style.imageUp = drawable(iconAsset);
        style.imageDown = drawable(iconAsset);
        style.imageChecked = drawable(iconAsset);

        style.unpressedOffsetY = 10f;
        style.pressedOffsetY = 10f;
        style.checkedOffsetY = 10f;

        return new ImageButton(style);
    }

    private ImageButton createCloseButton() {
        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.up = drawable(CLOSE);
        style.over = drawable(CLOSE_HOVER);
        style.down = drawable(CLOSE_HOVER);

        return new ImageButton(style);
    }

    private void showPlants() {
        cardsGrid.clearChildren();

        User user = App.loggedInUser;
        if (user == null) {
            return;
        }

        if (!plantStateLoadedForView) {
            requestPlantState();
            return;
        }

        Set<Integer> unlockedPlants =
                ClientShopState.unlockedPlantIds();

        Map<Integer, Integer> plantLevels =
                ClientShopState.plantLevels();

        Map<Integer, Integer> seedPackets =
                ClientShopState.seedPackets();

        List<PlantData> plants = new ArrayList<>(PlantRegistry.getAll());

        plants.sort(Comparator.comparingInt(PlantData::id));
        int column = 0;
        int columnsPerRow = 8;
        cardsGrid.add(createPlantFilterToolbar())
                .colspan(columnsPerRow)
                .growX()
                .padLeft(15f)
                .padRight(15f)
                .padTop(10f)
                .padBottom(5f);

        cardsGrid.row();

        plants = filterPlants(
                plants,
                user,
                unlockedPlants,
                plantLevels,
                seedPackets
        );

        if (plants.isEmpty()) {
            Label emptyLabel = new Label("NO PLANTS MATCH THESE FILTERS", game.getSkin());
            emptyLabel.setColor(
                    1f,
                    0.85f,
                    0.45f,
                    1f
            );
            cardsGrid.add(emptyLabel)
                    .colspan(columnsPerRow)
                    .padTop(60f);

            return;
        }
        ButtonGroup<PlantCard> plantGroup = new ButtonGroup<>();

        plantGroup.setMinCheckCount(0);
        plantGroup.setMaxCheckCount(1);
        plantGroup.setUncheckLast(true);
        for (PlantData plant : plants) {
            boolean unlocked =
                    unlockedPlants.contains(plant.id());

            boolean boosted =
                    unlocked
                            && ClientShopState.hasBoost(
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

            PlantCard card = new PlantCard(
                    game,
                    new PlantCard.ViewData(
                            plant,
                            unlocked,
                            boosted,
                            level,
                            packets,
                            requiredPackets,
                            false
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
                        openPlantDetails(card);
                    }
                }
            });

            cardsGrid.add(card).expandX().top().padBottom(10f).padTop(20f);

            column++;

            if (column >= columnsPerRow) {
                cardsGrid.row();
                column = 0;
            }
        }
    }
    private void requestPlantState() {
        if (plantStateRequestInFlight) {
            return;
        }

        plantStateRequestInFlight = true;
        cardsGrid.clearChildren();
        cardsGrid.add(
                new Label(
                        "LOADING PLANT DATA...",
                        game.getSkin()
                )
        ).padTop(60f);

        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> sendPlantStateRequest())
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishPlantStateRequest(
                                                response,
                                                throwable
                                        )
                                )
                );
    }

    private CompletableFuture<ShopResponse>
    sendPlantStateRequest() {
        try {
            return game.getNetworkManager()
                    .getShopClientService()
                    .getShop();
        } catch (IOException | RuntimeException exception) {
            return failedFuture(exception);
        }
    }

    private void finishPlantStateRequest(
            ShopResponse response,
            Throwable throwable
    ) {
        plantStateRequestInFlight = false;

        if (throwable != null) {
            showOwnershipLoadFailure(
                    "Could not load plant data: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()) {
            showOwnershipLoadFailure(
                    response == null
                            ? "Could not load plant data."
                            : response.getMessage()
            );
            return;
        }

        ClientShopState.apply(response);
        plantStateLoadedForView = true;
        showPlants();
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
        ).padTop(60f);

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
        cardsGrid.add(label).padTop(60f);
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

    private void openPlantDetails(
            PlantCard card
    ) {
        if (getStage() == null) {
            return;
        }

        setVisible(false);

        PlantDetailsTable details =
                new PlantDetailsTable(
                        game,
                        card.getData(),
                        () -> {
                            card.setChecked(false);
                            showPlants();
                            setVisible(true);
                        }
                );

        getStage().addActor(details);
    }
    private List<PlantData> filterPlants(
            List<PlantData> plants,
            User user,
            Set<Integer> unlockedPlants,
            Map<Integer, Integer> plantLevels,
            Map<Integer, Integer> seedPackets
    ) {
        List<PlantData> result = new ArrayList<>();
        for (PlantData plant : plants) {
            boolean unlocked = unlockedPlants.contains(plant.id());
            int level = plantLevels.getOrDefault(plant.id(), 1);
            int packets = seedPackets.getOrDefault(plant.id(), 0);
            boolean upgradeable = canUpgradePlant(plant, unlocked, level, packets);
            if (!matchesFamily(plant)) {
                continue;
            }

            if (!matchesOwnership(unlocked)) {
                continue;
            }

            if (!matchesUpgrade(upgradeable)) {
                continue;
            }

            result.add(plant);
        }

        result.sort(
                Comparator.comparingInt(
                        PlantData::id
                )
        );

        return result;
    }
    private boolean canUpgradePlant(
            PlantData plant,
            boolean unlocked,
            int currentLevel,
            int packets
    ) {
        if (!unlocked) {
            return false;
        }
        int maximumLevel = plant.upgrades() == null ? 1 : plant.upgrades().size() + 1;
        if (currentLevel >= maximumLevel) {
            return false;
        }
        int requiredPackets = requiredSeedPackets(plant, currentLevel);
        return packets >= requiredPackets;
    }
    private boolean matchesFamily(
            PlantData plant
    ) {
        if (ALL_FAMILIES.equals(
                selectedFamily
        )) {
            return true;
        }

        return plant.category() != null
                && plant.category()
                .equalsIgnoreCase(
                        selectedFamily
                );
    }

    private boolean matchesOwnership(
            boolean unlocked
    ) {
        return switch (selectedOwnership) {
            case ALL -> true;
            case UNLOCKED -> unlocked;
            case LOCKED -> !unlocked;
        };
    }

    private boolean matchesUpgrade(
            boolean upgradeable
    ) {
        return switch (selectedUpgrade) {
            case ALL -> true;
            case UPGRADEABLE -> upgradeable;
            case NOT_UPGRADEABLE -> !upgradeable;
        };
    }
    private int requiredCoinsForLevel(
            int targetLevel
    ) {
        return switch (targetLevel) {
            case 2 -> 1000;
            case 3 -> 2000;
            case 4 -> 4000;
            default ->
                    4000 * Math.max(
                            1,
                            targetLevel - 3
                    );
        };
    }
    private Table createPlantFilterToolbar() {
        Table toolbar = new Table();
        Label title =
                new Label("PLANTS", game.getSkin());
        title.setFontScale(1.15f);
        int activeFilters = activeFilterCount();
        String buttonText =
                activeFilters == 0
                        ? "FILTERS"
                        : "FILTERS (" + activeFilters + ")";

        TextButton filterButton =
                new TextButton(
                        buttonText,
                        game.getSkin(),
                        "green"
                );

        filterButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor) {
                        openPlantFilterDialog();
                    }
                }
        );

        toolbar.add(title)
                .expandX()
                .left();

        toolbar.add(filterButton)
                .width(150f)
                .height(42f)
                .right();

        return toolbar;
    }
    private int activeFilterCount() {
        int count = 0;

        if (!ALL_FAMILIES.equals(selectedFamily)) {
            count++;
        }

        if (selectedOwnership != OwnershipFilter.ALL) {
            count++;
        }

        if (selectedUpgrade != UpgradeFilter.ALL) {
            count++;
        }

        return count;
    }
    private void openPlantFilterDialog() {
        if (getStage() == null) {
            return;
        }

        SelectBox<String> familyBox =
                createFilterSelectBox();

        familyBox.setItems(
                getFamilyOptions()
        );

        familyBox.setSelected(
                selectedFamily
        );

        SelectBox<OwnershipFilter> ownershipBox =
                createFilterSelectBox();

        ownershipBox.setItems(
                OwnershipFilter.values()
        );

        ownershipBox.setSelected(
                selectedOwnership
        );

        SelectBox<UpgradeFilter> upgradeBox =
                createFilterSelectBox();

        upgradeBox.setItems(
                UpgradeFilter.values()
        );

        upgradeBox.setSelected(
                selectedUpgrade
        );

        // Dark fullscreen layer.
        Table overlay = new Table();

        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);

        overlay.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(
                                0f,
                                0f,
                                0f,
                                0.72f
                        )
                )
        );

        // Use the project's own popup style instead of Dialog.
        BorderedPanel panel =
                new BorderedPanel(
                        game,
                        Color.valueOf("75452F")
                );

        Table content =
                panel.getContent();

        content.clearChildren();
        content.pad(
                26f,
                34f,
                30f,
                34f
        );

        // ---------------- TITLE ----------------

        Label title =
                new Label(
                        "FILTER PLANTS",
                        game.getSkin()
                );

        title.setColor(
                Color.valueOf("FFE06A")
        );

        title.setFontScale(1.2f);

        content.add(title)
                .colspan(2)
                .padBottom(25f)
                .center()
                .row();

        // ---------------- FAMILY ----------------

        Label familyLabel =
                new Label(
                        "FAMILY",
                        game.getSkin()
                );

        familyLabel.setColor(Color.WHITE);

        content.add(familyLabel)
                .left()
                .padRight(25f)
                .padBottom(15f);

        content.add(familyBox)
                .width(280f)
                .height(45f)
                .padBottom(15f)
                .row();

        // ---------------- OWNERSHIP ----------------

        Label ownershipLabel =
                new Label(
                        "OWNERSHIP",
                        game.getSkin()
                );

        ownershipLabel.setColor(Color.WHITE);

        content.add(ownershipLabel)
                .left()
                .padRight(25f)
                .padBottom(15f);

        content.add(ownershipBox)
                .width(280f)
                .height(45f)
                .padBottom(15f)
                .row();

        // ---------------- UPGRADE ----------------

        Label upgradeLabel =
                new Label(
                        "UPGRADE",
                        game.getSkin()
                );

        upgradeLabel.setColor(Color.WHITE);

        content.add(upgradeLabel)
                .left()
                .padRight(25f)
                .padBottom(25f);

        content.add(upgradeBox)
                .width(280f)
                .height(45f)
                .padBottom(25f)
                .row();

        // ---------------- BUTTONS ----------------

        Table buttons =
                new Table();

        TextButton resetButton =
                new TextButton(
                        "RESET",
                        game.getSkin(),
                        "brown"
                );

        TextButton cancelButton =
                new TextButton(
                        "CANCEL",
                        game.getSkin(),
                        "brown"
                );

        TextButton applyButton =
                new TextButton(
                        "APPLY",
                        game.getSkin(),
                        "green"
                );

        resetButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        selectedFamily =
                                ALL_FAMILIES;

                        selectedOwnership =
                                OwnershipFilter.ALL;

                        selectedUpgrade =
                                UpgradeFilter.ALL;

                        overlay.remove();

                        showPlants();
                    }
                }
        );

        cancelButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        overlay.remove();
                    }
                }
        );

        applyButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        selectedFamily =
                                familyBox.getSelected();

                        selectedOwnership =
                                ownershipBox.getSelected();

                        selectedUpgrade =
                                upgradeBox.getSelected();

                        overlay.remove();

                        showPlants();
                    }
                }
        );

        buttons.add(resetButton)
                .width(125f)
                .height(45f)
                .padRight(10f);

        buttons.add(cancelButton)
                .width(125f)
                .height(45f)
                .padRight(10f);

        buttons.add(applyButton)
                .width(125f)
                .height(45f);

        content.add(buttons)
                .colspan(2)
                .center();

        panel.pack();

        overlay.add(panel)
                .center();

        getStage().addActor(overlay);
    }
    private <T> SelectBox<T> createFilterSelectBox() {
        Skin skin = game.getSkin();
        com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle =
                new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(
                        skin.get(
                                "default",
                                com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle.class
                        )
                );

        listStyle.font = skin.getFont("FBUSV8C5EI_2");
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.WHITE;
        listStyle.background = skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        listStyle.selection = skin.getDrawable("image_ui_generic_greenbutton_10");
        listStyle.over = skin.getDrawable("image_ui_generic_brownbutton_10");

        ScrollPane.ScrollPaneStyle scrollStyle =
                new ScrollPane.ScrollPaneStyle(
                        skin.get(
                                "default",
                                ScrollPane.ScrollPaneStyle.class
                        )
                );

        SelectBox.SelectBoxStyle selectStyle =
                new SelectBox.SelectBoxStyle();

        selectStyle.font =
                skin.getFont("FBUSV8C5EI_2");

        selectStyle.fontColor =
                Color.WHITE;

        selectStyle.disabledFontColor =
                Color.GRAY;

        selectStyle.background =
                skin.getDrawable(
                        "image_ui_generic_brownbutton_10"
                );

        selectStyle.backgroundOver =
                skin.getDrawable(
                        "image_ui_generic_brownbutton_down_10"
                );

        selectStyle.backgroundOpen =
                skin.getDrawable(
                        "image_ui_generic_greenbutton_10"
                );

        selectStyle.backgroundDisabled =
                skin.getDrawable(
                        "image_ui_generic_disabledbutton_10"
                );

        selectStyle.listStyle =
                listStyle;

        selectStyle.scrollStyle =
                scrollStyle;

        SelectBox<T> selectBox =
                new SelectBox<>(selectStyle);

        selectBox.setAlignment(
                com.badlogic.gdx.utils.Align.center
        );

        selectBox.getList().setAlignment(
                com.badlogic.gdx.utils.Align.center
        );

        selectBox.setMaxListCount(6);

        return selectBox;
    }
    private String[] getFamilyOptions() {
        List<String> families = new ArrayList<>();
        for (PlantData plant : PlantRegistry.getAll()) {
            String family = plant.category();
            if (family == null || family.isBlank()) {
                continue;
            }
            boolean alreadyExists = false;
            for (String existing : families) {
                if (existing.equalsIgnoreCase(family)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                families.add(family);
            }
        }

        families.sort(
                String.CASE_INSENSITIVE_ORDER
        );

        families.add(
                0,
                ALL_FAMILIES
        );

        return families.toArray(
                new String[0]
        );
    }
    private void showZombies() {
        cardsGrid.clearChildren();

        User user = App.loggedInUser;

        if (user == null) {
            return;
        }

        NewsRepository newsRepository =
                new NewsRepository();

        Set<String> discoveredZombies =
                newsRepository
                        .getDiscoveredZombieAliases(
                                user.getId()
                        );

        List<Zombie> zombies =
                new ArrayList<>(
                        ZombieRegistry
                                .getTemplates()
                                .values()
                );

        ButtonGroup<ZombieCard> zombieGroup =
                new ButtonGroup<>();

        zombieGroup.setMinCheckCount(0);
        zombieGroup.setMaxCheckCount(1);
        zombieGroup.setUncheckLast(true);

        int column = 0;
        int columnsPerRow = 8;

        for (Zombie zombie : zombies) {

            String alias =
                    zombie.getAlias();

            boolean unlocked =
                    containsIgnoreCase(
                            discoveredZombies,
                            alias
                    );

            String cardAssetId =
                    ZombieRegistry
                            .getCardAssetId(alias);

            ZombieCard card =
                    new ZombieCard(
                            game,
                            new ZombieCard.ViewData(
                                    alias,
                                    ZombieRegistry
                                            .getCardAssetId(alias),

                                    ZombieRegistry
                                            .getIdlePamPath(alias),
                                    ZombieRegistry
                                            .getIdleClip(alias),
                                    ZombieRegistry.getWalkClip(
                                            alias
                                    ),
                                    ZombieRegistry
                                            .getIdleVisibleParts(alias),

                                    unlocked
                            )
                    );

            if (unlocked) {
                zombieGroup.add(card);
                card.addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(
                                    ChangeEvent event,
                                    Actor actor
                            ) {
                                if (card.isChecked()) {
                                    openZombieDetails(card);
                                }
                            }
                        }
                );
            }

            cardsGrid.add(card)
                    .expandX()
                    .top()
                    .pad(10f);

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
    private void openZombieDetails(
            ZombieCard card
    ) {
        if (getStage() == null) {
            return;
        }

        setVisible(false);

        ZombieDetailsTable details =
                new ZombieDetailsTable(
                        game,
                        card.getData(),
                        () -> {
                            card.setChecked(false);
                            showPlants();
                            setVisible(true);
                        }
                );

        getStage().addActor(details);
    }
    private boolean containsIgnoreCase(
            Set<String> aliases,
            String wanted
    ) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(wanted)) {
                return true;
            }
        }

        return false;
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

        int targetLevel = currentLevel + 1;

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
}