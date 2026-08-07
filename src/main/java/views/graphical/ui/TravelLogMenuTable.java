package views.graphical.ui;

import Data.database.QuestsRepository;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controllers.TravelLogController;
import graphics.PvzGame;
import models.App;
import models.User;
import models.quests.QuestType;

import java.util.List;

public final class TravelLogMenuTable extends Table {
    private static final String PANEL_BACKGROUND =
            "image_ui_quests_panel_edge_to_edge_ten";

    private static final String QUESTS_TAB =
            "IMAGE_UI_QUESTS_DAILY_INACTIVE";

    private static final String QUESTS_TAB_SELECTED =
            "IMAGE_UI_QUESTS_DAILY_ACTIVE";

    private static final String MINIGAMES_TAB =
            "IMAGE_UI_QUESTS_EPIC_INACTIVE";

    private static final String MINIGAMES_TAB_SELECTED =
            "IMAGE_UI_QUESTS_EPIC_ACTIVE";

    private static final String CLOSE_BUTTON =
            "generic_close";

    private static final String QUEST_BACKGROUND =
            "IMAGE_UI_QUEST_TOAST_QUEST_TOAST_DEFAULT";

    //test
    private static final float QUEST_ROW_HEIGHT = 150f;
    private static final float QUEST_ROW_SIDE_PADDING = 240f;
    private static final float QUEST_ROW_GAP = 12f;

    private final PvzGame game;
    private final Table contentList;

    private final TravelLogController controller = new TravelLogController();

    public TravelLogMenuTable(PvzGame game) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        this.game = game;

        setFillParent(true);
        setTouchable(Touchable.childrenOnly);
        pad(22f);

        Table panel = new Table();
        panel.top();
        panel.setBackground(
                skinDrawable(PANEL_BACKGROUND)
        );
        panel.pad(42f, 30f, 25f, 30f);

        contentList = new Table();
        contentList.top();

        ScrollPane scrollPane = new ScrollPane(
                contentList,
                game.getSkin()
        );

        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);

        panel.add(scrollPane)
                .grow()
                .minWidth(0f)
                .minHeight(0f);

        ImageButton questsTab = createTabButton(
                QUESTS_TAB,
                QUESTS_TAB_SELECTED
        );

        ImageButton minigamesTab = createTabButton(
                MINIGAMES_TAB,
                MINIGAMES_TAB_SELECTED
        );

        ImageButton closeButton = createCloseButton();

        ButtonGroup<ImageButton> tabGroup =
                new ButtonGroup<>();

        tabGroup.setMinCheckCount(1);
        tabGroup.setMaxCheckCount(1);
        tabGroup.setUncheckLast(true);

        tabGroup.add(questsTab);
        tabGroup.add(minigamesTab);
        float tabHeight = questsTab.getPrefHeight();
        float panelTopOffset = tabHeight * 0.75f;
        float leftPadding = 40f;
        float tabGap = 8f;

        Stack menuStack = new Stack();
        menuStack.setTouchable(Touchable.childrenOnly);

        Table panelLayer = new Table();
        panelLayer.top();
        panelLayer.setTouchable(Touchable.childrenOnly);

        panelLayer.add(panel)
                .grow()
                .minWidth(0f)
                .minHeight(0f)
                .padTop(panelTopOffset);

        Table questsLayer = new Table();
        questsLayer.top().left();
        questsLayer.setTouchable(Touchable.childrenOnly);

        questsLayer.add(questsTab)
                .padLeft(leftPadding);

        Table minigamesLayer = new Table();
        minigamesLayer.top().left();
        minigamesLayer.setTouchable(Touchable.childrenOnly);

        minigamesLayer.add(minigamesTab)
                .padLeft(
                        leftPadding
                                + questsTab.getPrefWidth()
                                + tabGap
                );

        Table closeLayer = new Table();
        closeLayer.top().right();
        closeLayer.setTouchable(Touchable.childrenOnly);

        float closeButtonOffset =
                panelTopOffset
                        - closeButton.getPrefHeight()
                        + 2f;

        closeLayer.add(closeButton)
                .padTop(closeButtonOffset)
                .padRight(50f);

        menuStack.add(minigamesLayer);
        menuStack.add(questsLayer);
        menuStack.add(panelLayer);
        menuStack.add(closeLayer);

        add(menuStack)
                .grow()
                .minWidth(0f)
                .minHeight(0f);

        questsTab.addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                if (questsTab.isChecked()) {
                    questsLayer.toFront();
                    closeLayer.toFront();
                    showQuests();
                } else {
                    questsLayer.toBack();
                }
            }
        });

        minigamesTab.addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                if (minigamesTab.isChecked()) {
                    minigamesLayer.toFront();
                    closeLayer.toFront();
                    showMinigames();
                } else {
                    minigamesLayer.toBack();
                }
            }
        });

        closeButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        TravelLogMenuTable.this.remove();
                    }
                }
        );

        questsTab.setChecked(true);
        questsLayer.toFront();
        closeLayer.toFront();
        showQuests();
    }

    private ImageButton createTabButton(
            String inactiveAsset,
            String activeAsset
    ) {
        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.up = bankDrawable(inactiveAsset);
        style.over = bankDrawable(inactiveAsset);
        style.down = bankDrawable(activeAsset);
        style.checked = bankDrawable(activeAsset);

        return new ImageButton(style);
    }

    private ImageButton createCloseButton() {
        return new ImageButton(
                game.getSkin(),
                CLOSE_BUTTON
        );
    }

    private void showQuests() {
        contentList.clearChildren();

        for (QuestsRepository.QuestEntry entry
                : controller.getAllQuestEntries()) {

            QuestCard card =
                    new QuestCard(
                            game,
                            entry,
                            controller.getQuestDescription(
                                    entry
                            )
                    );

            contentList.add(card)
                    .growX()
                    .height(105f)
                    .padLeft(160f)
                    .padRight(160f)
                    .padBottom(12f)
                    .row();
        }
    }

    private void showMinigames() {
        contentList.clearChildren();

        Label placeholder = new Label(
                "Minigames will be shown here",
                game.getSkin()
        );

        contentList.add(placeholder)
                .expand()
                .center();
    }

    private Stack createQuestPlaceholder(
            String title,
            String description
    ) {
        Stack questRow = new Stack();

        Image background = new Image(
                bankDrawable(QUEST_BACKGROUND)
        );

        background.setScaling(Scaling.stretch);
        background.setTouchable(Touchable.disabled);

        Table information = new Table();
        information.left();
        information.pad(8f, 20f, 8f, 20f);
        information.setTouchable(Touchable.disabled);

        Label titleLabel = new Label(
                title,
                game.getSkin()
        );

        Label descriptionLabel = new Label(
                description,
                game.getSkin()
        );

        information.add(titleLabel)
                .left()
                .row();

        information.add(descriptionLabel)
                .left()
                .padTop(3f);

        questRow.add(background);
        questRow.add(information);

        return questRow;
    }

    private Drawable bankDrawable(String assetId) {
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


    private Drawable skinDrawable(String drawableId) {
        return game.getSkin().getDrawable(drawableId);
    }
}