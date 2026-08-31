package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
import graphics.PvzGame;

import network.client.ClientQuestState;
import network.protocol.quests.QuestEntryDto;
import network.protocol.quests.QuestResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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
    private static final float PANEL_WIDTH = 950f;

    //test
    private static final float QUEST_ROW_HEIGHT = 150f;
    private static final float QUEST_ROW_SIDE_PADDING = 240f;
    private static final float QUEST_ROW_GAP = 12f;

    private final PvzGame game;
    private final Table contentList;

    private boolean requestInFlight;

    public TravelLogMenuTable(PvzGame game) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
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

        ImageButton closeButton = createCloseButton();

        ButtonGroup<ImageButton> tabGroup =
                new ButtonGroup<>();

        tabGroup.setMinCheckCount(1);
        tabGroup.setMaxCheckCount(1);
        tabGroup.setUncheckLast(true);

        tabGroup.add(questsTab);
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
                .width(PANEL_WIDTH)
                .growY()
                .minHeight(0f)
                .padTop(panelTopOffset);

        Table questsLayer = new Table();
        questsLayer.top().left();
        questsLayer.setTouchable(Touchable.childrenOnly);
        float panelSideSpace =
                (PvzGame.VIRTUAL_WIDTH - PANEL_WIDTH) / 2f;
        questsLayer.add(questsTab).padLeft(panelSideSpace + leftPadding);


        Table minigamesLayer = new Table();
        minigamesLayer.top().left();
        minigamesLayer.setTouchable(Touchable.childrenOnly);


        Table closeLayer = new Table();
        closeLayer.top().right();
        closeLayer.setTouchable(Touchable.childrenOnly);

        float closeButtonOffset =
                panelTopOffset
                        - closeButton.getPrefHeight()
                        + 2f;

        closeLayer.add(closeButton)
                .padTop(closeButtonOffset)
                .padRight(
                        panelSideSpace
                                + 50f
                );
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
        loadQuestsFromServer();
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

        if (!ClientQuestState.isLoaded()) {
            Label loading = new Label(
                    requestInFlight ? "LOADING QUESTS..." : "QUESTS ARE NOT LOADED",
                    game.getSkin()
            );
            contentList.add(loading).padTop(50f);
            return;
        }

        for (QuestEntryDto entry : ClientQuestState.getEntries()) {
            QuestCard card = new QuestCard(
                    game,
                    entry,
                    () -> claimQuest(entry.getQuestId())
            );

            contentList.add(card)
                    .growX()
                    .height(120f)
                    .padLeft(160f)
                    .padRight(160f)
                    .padBottom(12f)
                    .row();
        }
    }

    private void loadQuestsFromServer() {
        if (requestInFlight) {
            return;
        }
        requestInFlight = true;
        showQuests();

        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> sendQuestGet())
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishQuestRequest(
                                                response,
                                                throwable,
                                                false
                                        )
                                )
                );
    }

    private CompletableFuture<QuestResponse> sendQuestGet() {
        try {
            return game.getNetworkManager()
                    .getQuestClientService()
                    .getQuests();
        } catch (IOException | RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private void claimQuest(int questId) {
        if (requestInFlight) {
            return;
        }
        requestInFlight = true;

        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> sendQuestClaim(questId))
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishQuestRequest(
                                                response,
                                                throwable,
                                                true
                                        )
                                )
                );
    }

    private CompletableFuture<QuestResponse> sendQuestClaim(int questId) {
        try {
            return game.getNetworkManager()
                    .getQuestClientService()
                    .claimQuest(questId);
        } catch (IOException | RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private void finishQuestRequest(
            QuestResponse response,
            Throwable throwable,
            boolean claimRequest
    ) {
        requestInFlight = false;

        if (throwable != null) {
            game.notifyError(
                    "Quest request failed: " + rootMessage(throwable)
            );
            showQuests();
            return;
        }

        if (response == null) {
            game.notifyError("Server returned no quest response.");
            showQuests();
            return;
        }

        ClientQuestState.apply(response, claimRequest && response.isSuccess());

        if (!response.isSuccess()) {
            game.notifyError(response.getMessage());
        } else if (claimRequest) {
            game.notifyInfo(response.getMessage());
        }

        showQuests();
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