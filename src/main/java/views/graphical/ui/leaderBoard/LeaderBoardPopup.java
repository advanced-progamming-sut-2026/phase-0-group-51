package views.graphical.ui.leaderBoard;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.LeaderboardMenuController;
import graphics.PvzGame;
import models.App;
import models.User;
import models.leaderBoard.LeaderBoard;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class LeaderBoardPopup extends Table{
    private final String BACKGROUND_POPUP = "IMAGE_UI_JOUST_LEADERBOARD_LEADERBOARD_SCROLL_MID";
    private final String HEADER_POPUP = "IMAGE_UI_JOUST_LEADERBOARD_LEADERBOARD_SCROLL_TOP";
    private final String YOU_BOX = "IMAGE_UI_JOUST_LEADERBOARD_LEADERBOARD_SCROLL_BOTTOM";
    private final String FIRST = "IMAGE_UI_JOUST_LEADERBOARD_BADGE_PROMOTED";
    private final String SECOND = "IMAGE_UI_JOUST_LEADERBOARD_BADGE_STANDSTILL";
    private final String THIRD_AND_REST = "IMAGE_UI_JOUST_LEADERBOARD_BADGE_DEMOTED";
    private final String CANDY = "IMAGE_UI_HUD_INGAME_MINT";
    private final String GREEN_CUP = "IMAGE_UI_JOUST_LEAGUES_CUP_JADE";
    private final String MINIGAME_COUNT = "IMAGE_UI_GAMECENTER_ANDROID_GAMECENTER_SELECTED";
    private final String LAST_PROGRESS = "IMAGE_UI_GAMECENTER_ANDROID_GAMECENTER";
    private final String CROWN = "IMAGE_UI_JOUST_ICONS_CROWNS_CROWNS_LARGE";
    private final String SORT_BUTTON = "IMAGE_UI_ALMANAC_FILTER_BUTTON_UP";
    private final String SORT_BUTTON_DOWN = "IMAGE_UI_ALMANAC_FILTER_BUTTON_DOWN";
    private final String LABEL_BACKGROUND = "IMAGE_UI_HUD_WORLDMAP_LEVEL_COUNTER";
    private final String CLOSE = "IMAGE_UI_MAINMENU_BACK_BTN_NORMAL";
    private final String CLOSE_DOWN = "IMAGE_UI_MAINMENU_BACK_BTN_PRESSED";
    private static final float POPUP_WIDTH = 780f;
    private static final float POPUP_HEIGHT = 650f;
    private static final float HEADER_HEIGHT = 120f;
    private static final float ROW_WIDTH = 700f;
    private static final float ROW_HEIGHT = 112f;
    private final LeaderboardMenuController controller;
    private List<LeaderBoard> entriesCache = new ArrayList<>();
    private int sortMode = 0;
    private String currentSortName = "MAX MEOW POINT";
    private boolean sortMenuOpen = false;
    private final PvzGame game;


    public LeaderBoardPopup(PvzGame game) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }

        this.game = game;
        this.controller = new LeaderboardMenuController();

        setFillParent(true);
        setTouchable(Touchable.enabled);
        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(0f, 0f, 0f, 0.70f)
                )
        );


        addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            int button
                    ) {
                        return true;
                    }
                }
        );

        buildUi();
    }

    private void buildUi() {
        Stack popup = new Stack();
        popup.setSize(POPUP_WIDTH, POPUP_HEIGHT);

        Image bodyBackground = image(BACKGROUND_POPUP);

        popup.add(bodyBackground);

        Table layout = new Table();
        layout.top();
        popup.add(layout);

        layout.add(buildHeader())
                .width(POPUP_WIDTH)
                .height(HEADER_HEIGHT)
                .top()
                .row();

        Table list = new Table();
        list.top();

        list.padTop(
                12f
        );


        List<LeaderBoard> entries;
        boolean loadFailed = false;
        try {

            entries = controller.loadLeaderboardEntries();
            entriesCache.clear();
            entriesCache.addAll(entries);

        } catch (
                IllegalStateException exception
        ) {

            loadFailed =
                    true;

            entries =
                    List.of();


            Label error = label(
                    "Could not load leaderboard",
                    "medium_outline",
                    Color.valueOf("7E332B"),
                    0.70f
            );
            error.setWrap(true);
            error.setAlignment(Align.center);
            list.add(error)
                    .width(ROW_WIDTH)
                    .height(80f)
                    .center();
        }

        if (!loadFailed && entries.isEmpty()) {
            Label empty = label(
                    "NO REGISTERED PLAYERS",
                    "medium_outline",
                    Color.valueOf("6D5B3B"),
                    0.75f
            );
            empty.setAlignment(Align.center);
            list.add(empty)
                    .width(ROW_WIDTH)
                    .height(100f)
                    .center();
        } else if (!loadFailed) {
            String currentUsername = currentUsername();

            for (int i = 0; i < entries.size(); i++) {
                LeaderBoard entry = entries.get(i);
                boolean currentUser =
                        currentUsername != null
                                && currentUsername.equalsIgnoreCase(entry.username());

                list.add(buildPlayerRow(entry, i + 1, currentUser))
                        .width(ROW_WIDTH)
                        .height(ROW_HEIGHT)
                        .padBottom(5f)
                        .left()
                        .row();
            }
        }

        ScrollPane.ScrollPaneStyle scrollStyle =
                new ScrollPane.ScrollPaneStyle();


        ScrollPane scrollPane =
                new ScrollPane(
                        list,
                        scrollStyle
                );


        scrollPane.setFadeScrollBars(
                false
        );

        scrollPane.setOverscroll(
                false,
                false
        );

        scrollPane.setScrollingDisabled(
                true,
                false
        );

        scrollPane.setForceScroll(
                false,
                true
        );


        layout.add(scrollPane)
                .width(710f)
                .height(POPUP_HEIGHT - HEADER_HEIGHT - 20f)
                .left()
                .padLeft(105f)
                .padBottom(10f);

        add(popup)
                .width(POPUP_WIDTH)
                .height(POPUP_HEIGHT)
                .center();
    }

    private Stack buildHeader() {
        Stack header = new Stack();


        Image headerBackground =
                image(
                        HEADER_POPUP
                );

        headerBackground.setScaling(
                Scaling.fit
        );


        header.add(
                headerBackground
        );
        Image cup = image(GREEN_CUP);

        cup.setScaling(Scaling.fit);


        Table content =
                new Table();

        content.pad(
                10f,
                20f,
                8f,
                20f
        );

        Label title = label(
                "LEADER BOARD",
                "big_outline",
                        Color.valueOf(
                                "FFF1B0"
                        ),
                        1.02f
                );


        title.setAlignment(
                Align.center
        );


        ImageButton close = createImageButton(CLOSE, CLOSE_DOWN);
        ImageButton sortButton =
                createImageButton(
                        SORT_BUTTON,
                        SORT_BUTTON_DOWN
                );

        Label sortLabel = label(
                currentSortName,
                "medium_outline",
                Color.WHITE,
                0.38f
        );


        sortButton.addListener(
                new ClickListener(){
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ){

                        openSortDialog();

                    }
                }
        );
        close.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        remove();
                    }
                }
        );
        Stack cupStack = new Stack();

        cupStack.add(cup);

        content.add(cupStack)
                .size(70f)
                .left()
                .padLeft(55f)
                .padRight(-25f);


        cupStack.addAction(
                Actions.moveBy(
                        170f,
                        16f
                )
        );

        content.add(title)
                .expandX()
                .right()
                .padRight(90f);
        content.add(sortLabel)
                .right()
                .padLeft(-180f)
                .padRight(8f);

        content.add(sortButton)
                .size(44f)
                .right()
                .padRight(8f);
        content.add(close)
                .size(44f)
                .right()
                .padRight(15f);

        header.add(content);
        return header;
    }
    private void openSortDialog() {
        LeaderBoardSortPopup popup =
                new LeaderBoardSortPopup(game,
                        mode -> {
                            sortMode = mode;
                            sortEntries();
                            clear();
                            buildUi();
                        }
                );

        popup.setPosition(
                getWidth() / 2f - popup.getWidth() / 2f,
                getHeight() / 2f - popup.getHeight() / 2f
        );
        getStage().addActor(popup);
    }
    private Stack buildPlayerRow(
            LeaderBoard entry,
            int rank,
            boolean currentUser
    ) {
        Stack row = new Stack();

        row.setSize(
                ROW_WIDTH,
                ROW_HEIGHT
        );


        if (currentUser) {

            Image youBackground = image(YOU_BOX);

            youBackground.setScaling(
                    Scaling.stretch
            );

            youBackground.setSize(
                    ROW_WIDTH-35f,
                    ROW_HEIGHT
            );
            youBackground.setScaleX(
                    0.9f
            );
            row.add(youBackground);
        }

        Table content = new Table();


        content.pad(
                5f,
                8f,
                5f,
                8f
        );


        content.add(
                        buildRankBadge(
                                rank
                        )
                )
                .width(
                        58f
                )
                .height(
                        92f
                )
                .left()
                .padRight(10f);

        Table player = new Table();
        player.top().left();

        String shownName = currentUser
                ? "You (" + entry.username() + ")"
                : entry.username();

        Label username = label(
                shownName,
                "medium_outline",
                currentUser
                                ? Color.valueOf(
                                "FFF2B0"
                        )
                                : Color.WHITE,
                        0.82f
                );


        username.setAlignment(
                Align.left
        );

        username.setEllipsis(
                true
        );


        player.add(username)
                .growX()
                .left()
                .height(
                        29f
                )
                .padLeft(
                        4f
                )
                .padBottom(
                        4f
                )
                .row();

        Stack statsStrip =
                new Stack();


        Image stripBackground =
                image(
                        LABEL_BACKGROUND
                );

        stripBackground.setScaling(
                Scaling.stretch
        );

        stripBackground.setColor(
                1f,
                1f,
                1f,
                0.86f
        );


        statsStrip.add(
                stripBackground
        );


        Table stats =
                new Table();

        stats.left();

        stats.pad(
                5f,
                8f,
                5f,
                8f
        );

        stats.add(
                        buildMetricItem(
                                CANDY,
                                "MAX MEOW POINT",
                                Integer.toString(
                                        Math.max(
                                                0,
                                                entry.highestScore()
                                        )
                                )
                        )
                )
                .width(
                        135f
                )
                .growY()
                .left();

        stats.add(
                        buildMetricItem(
                                CROWN,
                                "QUESTS",
                                "DAILY "
                                        + Math.max(
                                        0,
                                        entry.dailyQuestsCompleted()
                                )
                                        + "\nNON_Daily "
                                        + Math.max(
                                        0,
                        entry.nonDailyQuestsCompleted()
                                )
                        )
                )
                .width(
                        145f
                )
                .growY()
                .left();

        stats.add(
                        buildMetricItem(
                                MINIGAME_COUNT,
                                "MINIGAMES",
                                Integer.toString(
                                        Math.max(
                                                0,
                                                entry.minigamesCompleted()
                                        )
                                )
                        )
                )
                .width(
                        95f
                )
                .growY()
                .left();

        String progressValue;


        if (
                entry.completedChapter() <= 0
                        || entry.completedLevel() <= 0
        ) {

            progressValue =
                    "NONE";

        } else {

            progressValue =
                    "CH "
                            + entry.completedChapter()
                            + " / LV "
                            + entry.completedLevel();
        }


        stats.add(
                        buildMetricItem(
                                LAST_PROGRESS,
                                "LAST PROGRESS",
                                progressValue
                        ))
                .width(140f)
                .growY()
                .left();


        statsStrip.add(stats);
        player.add(statsStrip)
                .width(500f)
                .height(55f)
                .left()
                .padLeft(-12f)
                .row();


        content.add(player)
                .expandX()
                .fillX()
                .left();

        content.setSize(
                ROW_WIDTH,
                ROW_HEIGHT
        );

        row.add(content);
        return row;
    }


    private Stack buildRankBadge(
            int rank
    ) {

        Stack badge =
                new Stack();


        String asset = rank == 1
                ? FIRST
                : rank == 2
                ? SECOND
                : THIRD_AND_REST;

        Image background = image(asset);
        background.setScaling(Scaling.fit);
        badge.add(background);

        Table numberLayer = new Table();
        numberLayer.bottom();
        Label number = label(
                Integer.toString(rank),
                "big_outline",
                Color.WHITE,
                0.72f
        );
        number.setAlignment(Align.center);
        numberLayer.add(number)
                .width(48f)
                .padBottom(8f);
        badge.add(numberLayer);

        return badge;
    }


    private Table buildMetricItem(
            String iconAsset,
            String caption,
            String value
    ) {

        Table item =
                new Table();

        item.left();

        item.pad(
                0f,
                5f,
                0f,
                5f
        );


        Image icon =
                image(
                        iconAsset
                );

        icon.setScaling(
                Scaling.fit
        );



        Table text = new Table();
        text.left();

        Label captionLabel = label(
                caption,
                "medium_outline",
                Color.valueOf("F5EBCF"),
                        0.34f
                );


        captionLabel.setAlignment(
                Align.left
        );


        Label valueLabel = label(
                value,
                "medium_outline",
                Color.WHITE,
                        0.53f
                );


        valueLabel.setAlignment(
                Align.left
        );

        valueLabel.setEllipsis(
                true
        );


        text.add(captionLabel)
                .growX()
                .left()
                .row();
        text.add(valueLabel)
                .growX()
                .left();


        item.add(
                        icon
                )
                .size(
                        32f
                )
                .padRight(
                        5f
                )
                .center();


        item.add(
                        text
                )
                .expandX()
                .fillX()
                .left();


        return item;
    }

    private String currentUsername() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return null;
        }
        return user.getUsername();
    }

    private Image image(String assetId) {
        TextureRegion region = game.getTextureBank().region(assetId);
        if (region == null) {
            Image fallback = new Image(
                    game.getSkin().newDrawable(
                            "white_pixel",
                            new Color(1f, 1f, 1f, 0f)
                    )
            );
            fallback.setTouchable(Touchable.disabled);
            return fallback;
        }

        Image image = new Image(region);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private ImageButton createImageButton(
            String normalAsset,
            String pressedAsset
    ) {
        TextureRegion normal = game.getTextureBank().region(normalAsset);
        TextureRegion pressed = game.getTextureBank().region(pressedAsset);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();

        if (normal != null) {
            TextureRegion normalCopy = new TextureRegion(normal);
            normalCopy.flip(true, false);
            style.imageUp = new TextureRegionDrawable(normalCopy);
        }

        if (pressed != null) {

            TextureRegion pressedCopy = new TextureRegion(pressed);
            pressedCopy.flip(true, false);
            style.imageDown = new TextureRegionDrawable(pressedCopy);
            style.imageOver = new TextureRegionDrawable(pressedCopy);

        } else if (normal != null) {
            TextureRegion normalCopy = new TextureRegion(normal);
            normalCopy.flip(true, false);
            style.imageDown = new TextureRegionDrawable(normalCopy);
            style.imageOver = new TextureRegionDrawable(normalCopy);
        }


        return new ImageButton(style);
    }

    private Label label(
            String text,
            String styleName,
            Color color,
            float fontScale
    ) {
        Label.LabelStyle style;

        if (game.getSkin().has(
                styleName,
                Label.LabelStyle.class
        )) {
            style = game.getSkin().get(
                    styleName,
                    Label.LabelStyle.class
            );
        } else {
            style = game.getSkin().get(
                    "default",
                    Label.LabelStyle.class
            );
        }

        Label label = new Label(text, style);
        label.setColor(color);
        label.setFontScale(fontScale);
        return label;
    }

    private void sortEntries() {

        switch(sortMode){

            case 0:
                currentSortName = "MAX MEOW POINT";

                entriesCache.sort(
                        Comparator.comparingInt(
                                LeaderBoard::highestScore
                        ).reversed()
                );
                break;


            case 1:
                currentSortName = "MINIGAMES";

                entriesCache.sort(
                        Comparator.comparingInt(
                                LeaderBoard::minigamesCompleted
                        ).reversed()
                );
                break;


            case 2:
                currentSortName = "QUESTS";

                entriesCache.sort(
                        Comparator.comparingInt(
                                (LeaderBoard e) ->
                                        e.dailyQuestsCompleted()
                                                +
                                                e.nonDailyQuestsCompleted()
                        ).reversed()
                );
                break;


            case 3:
                currentSortName = "LAST PROGRESS";

                entriesCache.sort(
                        Comparator.comparingInt(
                                LeaderBoard::completedChapter
                        ).reversed()
                );
                break;
        }
    }
}
