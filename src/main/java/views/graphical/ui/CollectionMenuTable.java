package views.graphical.ui;

import Data.database.NewsRepository;
import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        Set<Integer> unlockedPlants =
                PlantRepository.loadUnlockedPlants(user.getId());

        Map<Integer, Integer> plantLevels =
                PlantRepository.loadPlantLevels(user.getId());

        Map<Integer, Integer> seedPackets =
                PlantRepository.loadSeedPackets(user.getId());

        List<PlantData> plants = new ArrayList<>(PlantRegistry.getAll());

        plants.sort(Comparator.comparingInt(PlantData::id));
        int column = 0;
        int columnsPerRow = 8;
        ButtonGroup<PlantCard> plantGroup = new ButtonGroup<>();

        plantGroup.setMinCheckCount(0);
        plantGroup.setMaxCheckCount(1);
        plantGroup.setUncheckLast(true);
        for (PlantData plant : plants) {
            boolean unlocked =
                    unlockedPlants.contains(plant.id());

            boolean boosted =
                    unlocked
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

            cardsGrid.add(card).expandX().top().pad(10f);

            column++;

            if (column >= columnsPerRow) {
                cardsGrid.row();
                column = 0;
            }
        }
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
                            setVisible(true);
                            card.setChecked(false);
                        }
                );

        getStage().addActor(details);
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

//            boolean unlocked =
//                    containsIgnoreCase(
//                            discoveredZombies,
//                            alias
//                    );
            boolean unlocked = true;

//            String cardAssetId =
//                    ZombieRegistry
//                            .getCardAssetId(alias);

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


                                    ZombieRegistry
                                            .getIdleVisibleParts(alias),

                                    unlocked
                            )
                    );
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

            cardsGrid.add(card)
                    .expandX()
                    .top()
                    .pad(10f);

            column++;

            if (column >= columnsPerRow) {
                cardsGrid.row();
                column = 0;
            }

//            if (unlocked) {
//                zombieGroup.add(card);
//            }
//
//            cardsGrid.add(card)
//                    .expandX()
//                    .top()
//                    .pad(10f);
//
//            column++;
//
//            if (column >= columnsPerRow) {
//                cardsGrid.row();
//                column = 0;
//            }
        }

//        if (column != 0) {
//            while (column < columnsPerRow) {
//                cardsGrid.add()
//                        .expandX();
//
//                column++;
//            }
//        }
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
                            setVisible(true);
                            card.setChecked(false);
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