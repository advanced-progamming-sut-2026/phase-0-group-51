package views.graphical.ui;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;

public final class CollectionMenuTable extends Table {
    private static final String PLANTS_ICON = "IMAGE_UI_STORE_TABICONS_PLANTS";
    private static final String ZOMBIES_ICON = "IMAGE_UI_STORE_TABICONS_ZOMBIES";
    private static final String PLANTS_TAB = "IMAGE_UI_ALMANAC_TABS_PLANTS_DOWN";
    private static final String PLANTS_TAB_SELECTED = "IMAGE_UI_ALMANAC_TABS_PLANTS_ACTIVE";
    private static final String ZOMBIES_TAB = "IMAGE_UI_ALMANAC_TABS_ZOMBIES_DOWN";
    private static final String ZOMBIES_TAB_SELECTED = "IMAGE_UI_ALMANAC_TABS_ZOMBIES_ACTIVE";
    private static final String CLOSE = "IMAGE_UI_ALMANAC_TABS_CLOSE_TAB";
    private static final String CLOSE_HOVER = "IMAGE_UI_ALMANAC_TABS_CLOSE_TAB_DOWN";
    private static final String DETAILS_BACKGROUND = "IMAGE_UI_ALMANAC_ALMANAC_STAT_BACKGROUND";
    private static final String PLANT_CARD_BG = "IMAGE_UI_CARDS_CHOOSER_CHOOSER_PLANT_CARD";
    //private static final String PLANT_CARD_BG = "IMAGE_UI_CARDS_ALMANAC_PLANT_CARD";
    private static final String STATS_FRAME_BG = "IMAGE_UI_CARDS_CARD_TABLE_FRAME";

    private final PvzGame game;
    private final Table detailsArea;
    private final Table cardsGrid;

    public CollectionMenuTable(PvzGame game, Runnable onClose) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }

        if (onClose == null) {
            throw new IllegalArgumentException("onClose cannot be null");
        }

        this.game = game;

        setFillParent(true);
        setTouchable(Touchable.childrenOnly);
        pad(22f);

        BorderedPanel outerPanel = new BorderedPanel(game, Color.valueOf("75452F"));
        Table content = outerPanel.getContent();
        content.top();

        detailsArea = new Table();
        //detailsArea.setBackground(drawable(DETAILS_BACKGROUND));
        detailsArea.pad(12f);
//        Table plantCardArea = new Table();
//        plantCardArea.setBackground(drawable(PLANT_CARD_BG));
//        Table animationPlaceholder = new Table();
//        plantCardArea.add(animationPlaceholder).grow();
//        Table statsArea = new Table();
//        statsArea.setBackground(drawable(DETAILS_BACKGROUND));
//        detailsArea.add(plantCardArea).expandY().fillY().width(360f).padRight(12f);
//        detailsArea.add(statsArea).grow();
        Stack plantCardArea = new Stack();

        Image plantCardBackground = createBackgroundImage(
                PLANT_CARD_BG,
                Scaling.none
        );

        Table animationPlaceholder = new Table();

        plantCardArea.add(plantCardBackground);
        plantCardArea.add(animationPlaceholder);


        Stack statsArea = new Stack();

        Image statsBackground = createBackgroundImage(
                DETAILS_BACKGROUND,
                Scaling.fit
        );

        Table statsContent = new Table();

        statsArea.add(statsBackground);
        statsArea.add(statsContent);


        detailsArea.add(plantCardArea)
                .expandY()
                .fillY()
                .width(360f)
                .padRight(12f);

        detailsArea.add(statsArea).grow();

        cardsGrid = new Table();
        cardsGrid.top().left();

        ScrollPane cardsScroll = new ScrollPane(cardsGrid, game.getSkin());

        cardsScroll.setFadeScrollBars(false);
        cardsScroll.setOverscroll(false, false);
        cardsScroll.setScrollingDisabled(true, false);

        content.add(detailsArea).grow().minWidth(0f).minHeight(0f).row();
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
                onClose.run();
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

        PlantData sunflower =
                PlantRegistry.getByName("Sunflower");

        if (sunflower == null) {
            throw new IllegalStateException(
                    "Sunflower is not registered."
            );
        }

        PlantCard card = new PlantCard(
                game,
                new PlantCard.ViewData(
                        sunflower,
                        true,
                        false,
                        1,
                        4,
                        10
                )
        );

        cardsGrid.add(card)
                .padLeft(12f).padTop(8f);
    }

    private void showZombies() {
        cardsGrid.clearChildren();

        // add your ZombieCard objects here later
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
    private Image createBackgroundImage(
            String assetId,
            Scaling scaling
    ) {
        Image image = new Image(drawable(assetId));

        image.setScaling(scaling);
        image.setTouchable(Touchable.disabled);

        return image;
    }
}