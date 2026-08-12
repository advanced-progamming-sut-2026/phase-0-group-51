package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controllers.NewsMenuController;
import graphics.PvzGame;
import models.items.News;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class newsPopup extends Table{
    private final PvzGame game;
    private final NewsMenuController controller;
    private final Runnable onNewsOpened;
    private final static String BACK = "IMAGE_UI_MAINMENU_BACK_BTN_NORMAL";
    private final static String BACK_PRESSED = "IMAGE_UI_MAINMENU_BACK_BTN_PRESSED";
    public newsPopup(PvzGame game ,Runnable onNewsOpened){
        this.game = game;
        this.controller = new NewsMenuController();
        this.onNewsOpened = onNewsOpened;
        setFillParent(true);
        setTouchable(Touchable.enabled);
        setBackground(game.getSkin().newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.68f))
        );

        addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        return true;
                    }
                });


        buildUi();
    }
    private void buildUi() {
        BorderedPanel panel = new BorderedPanel(game, Color.valueOf("A87349"));
        Table content = panel.getContent();
        content.top();
        Table header = new Table();
        ImageButton backButton = createImageButton(BACK, BACK_PRESSED);

        backButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        closePopup();
                    }
                }
        );
        Label title = new Label("News and Updates", labelStyle("big_outline"));
        title.setAlignment(Align.center);

        title.setColor(Color.valueOf("FFE16A"));
        header.add(backButton).size(44f).left();
        header.add(title).expandX().center();

        header.add().width(44f);
        content.add(header).growX().padBottom(8f).row();

        Table newsList = new Table();

        newsList.top().left();
        newsList.pad(8f);
        List<News> news = controller.openAllNews();
        if (news.isEmpty()) {
            Label empty = new Label("No news at the moment.", labelStyle("medium_outline"));
            empty.setColor(Color.valueOf("51472B"));
            newsList.add(empty).expand().center();
        } else {

            for (News item : news) {
                addNews(newsList, item);
            }
        }


        ScrollPane scrollPane = new ScrollPane(newsList, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        Table paper = new Table();
        paper.setBackground(game.getSkin().newDrawable("white_pixel", Color.valueOf("EBE6D1")));

        paper.add(scrollPane)
                .grow()
                .pad(5f);

        content.add(paper)
                .width(440f)
                .height(365f)
                .grow()
                .row();

        add(panel)
                .width(510f)
                .height(475f)
                .center();

        if (onNewsOpened != null) {
            onNewsOpened.run();
        }
    }
    private void addNews(Table table, News news) {
        Label date = new Label(formatDate(news.getCreatedAt()), labelStyle("medium_outline"));
        date.setColor(Color.valueOf("275A5B"));

        Label message = new Label(news.getMessage(), labelStyle("medium_outline"));
        message.setColor(Color.valueOf("7A6F4D"));
        message.setWrap(true);
        message.setAlignment(Align.left);

        table.add(date).growX().left().padTop(8f).row();
        table.add(message).width(395f).left().padBottom(18f).row();
    }


    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "";
        }

        try {
            DateTimeFormatter input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime date = LocalDateTime.parse(rawDate, input);
            DateTimeFormatter output = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
            return date.format(output);

        } catch (Exception exception) {
            return rawDate;
        }
    }


    private ImageButton createImageButton(String normalAsset, String pressedAsset) {
        TextureRegion normal = game.getTextureBank().region(normalAsset);
        TextureRegion pressed = game.getTextureBank().region(pressedAsset);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normal);

        style.imageDown = new TextureRegionDrawable(pressed);
        style.imageOver = new TextureRegionDrawable(pressed);
        return new ImageButton(style);
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return game.getSkin().get(name, Label.LabelStyle.class);
        } catch (Exception exception) {
            return game.getSkin().get("default", Label.LabelStyle.class);
        }
    }

    private void closePopup() {
        remove();
    }
}
