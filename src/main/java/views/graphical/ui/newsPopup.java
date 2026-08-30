package views.graphical.ui;

import com.badlogic.gdx.Gdx;
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
import graphics.PvzGame;
import network.client.ClientNewsState;
import network.protocol.news.NewsItemDto;
import network.protocol.news.NewsResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class newsPopup extends Table {
    private final PvzGame game;
    private final Runnable onNewsOpened;
    private final Table newsList = new Table();
    private boolean requestInFlight;

    private static final String BACK = "IMAGE_UI_MAINMENU_BACK_BTN_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_MAINMENU_BACK_BTN_PRESSED";

    public newsPopup(PvzGame game, Runnable onNewsOpened) {
        this.game = game;
        this.onNewsOpened = onNewsOpened;

        setFillParent(true);
        setTouchable(Touchable.enabled);
        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(0f, 0f, 0f, 0.68f)
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
        loadAndMarkNewsRead();
    }

    private void buildUi() {
        BorderedPanel panel = new BorderedPanel(
                game,
                Color.valueOf("A87349")
        );

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

        Label title = new Label(
                "News and Updates",
                labelStyle("big_outline")
        );
        title.setAlignment(Align.center);
        title.setColor(Color.valueOf("FFE16A"));

        header.add(backButton).size(44f).left();
        header.add(title).expandX().center();
        header.add().width(44f);

        content.add(header).growX().padBottom(8f).row();

        newsList.top().left();
        newsList.pad(8f);
        showLoading();

        ScrollPane scrollPane = new ScrollPane(
                newsList,
                game.getSkin()
        );
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);

        Table paper = new Table();
        paper.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("EBE6D1")
                )
        );
        paper.add(scrollPane).grow().pad(5f);

        content.add(paper)
                .width(440f)
                .height(365f)
                .grow()
                .row();

        add(panel)
                .width(510f)
                .height(475f)
                .center();
    }

    private void loadAndMarkNewsRead() {
        if (requestInFlight) {
            return;
        }

        requestInFlight = true;

        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> sendMarkReadRequest())
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishNewsLoad(
                                                response,
                                                throwable
                                        )
                                )
                );
    }

    private CompletableFuture<NewsResponse> sendMarkReadRequest() {
        try {
            return game.getNetworkManager()
                    .getNewsClientService()
                    .markAllRead();
        } catch (IOException | RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private void finishNewsLoad(
            NewsResponse response,
            Throwable throwable
    ) {
        requestInFlight = false;

        if (throwable != null) {
            showError(
                    "Could not load news: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()) {
            showError(
                    response == null
                            ? "Could not load news."
                            : response.getMessage()
            );
            return;
        }

        ClientNewsState.apply(response);
        renderNews(ClientNewsState.news());

        if (onNewsOpened != null) {
            onNewsOpened.run();
        }
    }

    private void showLoading() {
        newsList.clearChildren();
        Label loading = new Label(
                "Loading news...",
                labelStyle("medium_outline")
        );
        loading.setColor(Color.valueOf("51472B"));
        newsList.add(loading).expand().center();
    }

    private void showError(String message) {
        newsList.clearChildren();
        Label error = new Label(
                message,
                labelStyle("medium_outline")
        );
        error.setWrap(true);
        error.setColor(Color.valueOf("8A2F2F"));
        newsList.add(error).width(395f).expand().center();
    }

    private void renderNews(List<NewsItemDto> news) {
        newsList.clearChildren();

        if (news == null || news.isEmpty()) {
            Label empty = new Label(
                    "No news at the moment.",
                    labelStyle("medium_outline")
            );
            empty.setColor(Color.valueOf("51472B"));
            newsList.add(empty).expand().center();
            return;
        }

        for (NewsItemDto item : news) {
            addNews(newsList, item);
        }
    }

    private void addNews(Table table, NewsItemDto news) {
        Label date = new Label(
                formatDate(news.getCreatedAt()),
                labelStyle("medium_outline")
        );
        date.setColor(Color.valueOf("275A5B"));

        Label message = new Label(
                news.getMessage(),
                labelStyle("medium_outline")
        );
        message.setColor(Color.valueOf("7A6F4D"));
        message.setWrap(true);
        message.setAlignment(Align.left);

        table.add(date).growX().left().padTop(8f).row();
        table.add(message)
                .width(395f)
                .left()
                .padBottom(18f)
                .row();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "";
        }

        try {
            DateTimeFormatter input = DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );
            LocalDateTime date = LocalDateTime.parse(rawDate, input);
            DateTimeFormatter output = DateTimeFormatter.ofPattern(
                    "EEE MMM d HH:mm:ss yyyy",
                    Locale.ENGLISH
            );
            return date.format(output);
        } catch (Exception exception) {
            return rawDate;
        }
    }

    private ImageButton createImageButton(
            String normalAsset,
            String pressedAsset
    ) {
        TextureRegion normal = game.getTextureBank().region(normalAsset);
        TextureRegion pressed = game.getTextureBank().region(pressedAsset);

        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normal);
        style.imageDown = new TextureRegionDrawable(pressed);
        style.imageOver = new TextureRegionDrawable(pressed);
        return new ImageButton(style);
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return game.getSkin().get(name, Label.LabelStyle.class);
        } catch (Exception exception) {
            return game.getSkin().get(
                    "default",
                    Label.LabelStyle.class
            );
        }
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

    private void closePopup() {
        remove();
    }
}
