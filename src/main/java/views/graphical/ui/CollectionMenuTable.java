package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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

    private final PvzGame game;
    private final Table detailsArea;
    private final Table cardsGrid;

    public CollectionMenuTable(
            PvzGame game,
            Runnable onClose
    ) {
        this.game = game;

        setFillParent(true);
        pad(25f);

        BorderedPanel outerPanel = new BorderedPanel(
                game,
                Color.valueOf("75452F")
        );

        Table content = outerPanel.getContent();
        content.top();

        Table header = createHeader(onClose);

        detailsArea = new Table();
        detailsArea.setBackground(
                drawable(DETAILS_BACKGROUND)
        );

        cardsGrid = new Table();
        cardsGrid.top().left();

        ScrollPane cardsScroll = new ScrollPane(
                cardsGrid,
                game.getSkin()
        );

        cardsScroll.setFadeScrollBars(false);
        cardsScroll.setOverscroll(false, false);
        cardsScroll.setScrollingDisabled(true, false);

        content.add(header)
                .growX()
                .height(70f)
                .row();

        content.add(detailsArea)
                .growX()
                .height(300f)
                .padTop(6f)
                .row();

        content.add(cardsScroll)
                .grow()
                .padTop(8f);

        add(outerPanel).grow();
    }

    private Table createHeader(Runnable onClose) {
        Table header = new Table();
        header.left();

        ImageButton plantsTab = createTabButton(
                PLANTS_TAB,
                PLANTS_TAB_SELECTED,
                PLANTS_ICON
        );

        ImageButton zombiesTab = createTabButton(
                ZOMBIES_TAB,
                ZOMBIES_TAB_SELECTED,
                ZOMBIES_ICON
        );

        ImageButton closeButton =
                createCloseButton();

        ButtonGroup<ImageButton> tabs =
                new ButtonGroup<>();

        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.setUncheckLast(true);

        tabs.add(plantsTab);
        tabs.add(zombiesTab);

        plantsTab.setChecked(true);

        plantsTab.addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                if (plantsTab.isChecked()) {
                    showPlants();
                }
            }
        });

        zombiesTab.addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                if (zombiesTab.isChecked()) {
                    showZombies();
                }
            }
        });

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                onClose.run();
            }
        });

        header.add(plantsTab)
                .size(105f, 70f);

        header.add(zombiesTab)
                .size(105f, 70f)
                .padLeft(4f);

        header.add()
                .expandX();

        header.add(closeButton)
                .size(64f, 64f)
                .padRight(4f);

        return header;
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

        // Add PlantCard objects here later.
    }

    private void showZombies() {
        cardsGrid.clearChildren();

        // Add ZombieCard objects here later.
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