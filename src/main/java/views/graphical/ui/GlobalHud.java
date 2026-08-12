package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.NewsMenuController;
import graphics.PvzGame;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import Data.database.UserRepository;
import models.App;
import models.User;

import com.badlogic.gdx.scenes.scene2d.Group;
import views.graphical.screens.GreenHouseScreen;
public final class GlobalHud extends Table {

    private final PvzGame game;
    private final Skin skin;

    private Label coinLabel;
    private Label gemLabel;
    private final UserRepository userRepository = new UserRepository();
    private float currencyRefreshTimer = 0f;

    private static final float CURRENCY_REFRESH = 0.25f;
    private Actor backButton;
    private Runnable backAction;

    private SettingsPopup settingsPopup;
    private static final String COIN = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL";
    private static final String COIN_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_SELECTED";
    private static final String GEMS = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL";
    private static final String GEMS_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_SELECTED";
    private static final String NEWS = "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_NORMAL";
    private static final String NEWS_SELECTED = "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_SELECTED";
    private static final String SHOP = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL";
    private static final String SHOP_CLICKED = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED";
    private final NewsMenuController newsController = new NewsMenuController();
    private Label unreadNewsLabel;
    private Table unreadNews;
    private Texture unreadBadgeTexture;
    private newsPopup newsPopup;
    private float newsRefreshTimer = 0f;
    private static final float NEWS_REFRESH = 0.5f;
    public GlobalHud(PvzGame game, Skin skin) {
        this.game = game;
        this.skin = skin;

        setFillParent(true);
        top();
        pad(10f);

        setTouchable(Touchable.childrenOnly);

        buildUi();
        setVisible(false);
    }

    private void buildUi() {
        Table topBar = new Table();
        topBar.setTouchable(Touchable.childrenOnly);

        Table leftBar = buildTopLeftIcons();
        Table rightBar = buildCurrencyBar();

        topBar.add(leftBar).left().top();
        topBar.add().expandX();
        topBar.add(rightBar).right().top();
        add(topBar).growX().top();
    }

    private Table buildTopLeftIcons() {

        Table topRow = new Table();
        topRow.setTouchable(Touchable.childrenOnly);
        backButton = gameIcon(
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_NORMAL",
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_SELECTED",
                "BACK"
        );

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (backAction != null) {
                    backAction.run();
                }
            }
        });

        topRow.add(backButton).size(52f).padRight(6f);

        Actor settingsButton = gameIcon(
                "IMAGE_UI_HUD_SETTINGSBUTTON_BUTTONS_HUD_SETTINGS_NORMAL",
                "IMAGE_UI_HUD_SETTINGSBUTTON_BUTTONS_HUD_SETTINGS_SELECTED",
                "SETTING"
        );

        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleSettings();
            }
        });

        topRow.add(settingsButton).size(52f).padRight(6f);


        Actor almanacButton = gameIcon(
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_NORMAL",
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_SELECTED",
                "BOOK"
        );

        almanacButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                getStage().addActor(new CollectionMenuTable(game));
            }
        });

        topRow.add(almanacButton).size(52f).padRight(6f);

        Actor minigamesButton = gameIcon(
                "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_NORMAL",
                "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_SELECTED",
                "POT"
        );

        minigamesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

            }
        });

        topRow.add(minigamesButton).size(52f).padRight(6f);

        Actor zenGardenButton = gameIcon(
                "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL",
                "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED",
                "CAN"
        );

        zenGardenButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showScreen(new GreenHouseScreen(game));}
        });
        topRow.add(zenGardenButton).size(52f).padRight(6f);;
        Actor newsButton = gameIcon(NEWS, NEWS_SELECTED, "NEWS");
        newsButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        toggleNews();
                    }
                }
        );

        Stack newsStack = new Stack();
        Table iconLayer = new Table();
        iconLayer.add(newsButton).size(52f);
        newsStack.add(iconLayer);

        unreadNews = createUnreadNews();
        Table badgeLayer = new Table();
        badgeLayer.top().right();
        badgeLayer.add(unreadNews).size(24f).padTop(-2f).padRight(-2f);
        newsStack.add(badgeLayer);
        topRow.add(newsStack).size(58f);
        return topRow;
    }
    private void toggleNews() {
        if (newsPopup != null && newsPopup.hasParent()) {
            newsPopup.remove();
            newsPopup = null;
            return;
        }
        newsPopup = new newsPopup(game, this::refreshUnreadNews);
        getStage().addActor(newsPopup);
        newsPopup.toFront();
        refreshUnreadNews();
    }
    private Table createUnreadNews() {
        final int textureSize = 64;
        Pixmap pixmap = new Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.valueOf("841919"));
        pixmap.fillCircle(
                textureSize / 2,
                textureSize / 2,
                textureSize / 2 - 1
        );

        pixmap.setColor(
                Color.valueOf("E53935")
        );
        pixmap.fillCircle(
                textureSize / 2,
                textureSize / 2,
                textureSize / 2 - 5
        );

        unreadBadgeTexture = new Texture(pixmap);
        pixmap.dispose();
        unreadBadgeTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Table badge = new Table();
        badge.setBackground(new TextureRegionDrawable(new TextureRegion(unreadBadgeTexture)));


        unreadNewsLabel = new Label("0", labelStyle("medium_outline"));
        unreadNewsLabel.setColor(Color.WHITE);
        unreadNewsLabel.setAlignment(Align.center);
        unreadNewsLabel.setFontScale(0.65f);
        unreadNewsLabel.setTouchable(Touchable.disabled);
        badge.add(unreadNewsLabel).expand().fill();
        badge.setTouchable(Touchable.disabled);
        badge.setVisible(false);
        return badge;
    }
    public void configure(int coins, int gems, boolean showBackButton, Runnable backAction) {
        this.backAction = backAction;
        if (backButton != null) {
            backButton.setVisible(showBackButton);
            backButton.setTouchable(
                    showBackButton ? Touchable.enabled : Touchable.disabled
            );
        }
        setVisible(true);
        currencyRefreshTimer = 0f;
        refreshCurrencyLabels();
        newsRefreshTimer = 0f;
        refreshUnreadNews();
    }
    public void updateCurrencies(int coins, int gems) {
        if (coinLabel != null) coinLabel.setText(String.valueOf(coins));
        if (gemLabel != null) gemLabel.setText(String.valueOf(gems));
    }

    public void hideHud() {
        setVisible(false);
    }

    private Drawable safeRegion(String id) {
        try {
            TextureRegion r = game.getTextureBank().region(id);
            return (r == null) ? null : new TextureRegionDrawable(r);
        } catch (Exception e) {
            return null;
        }
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return skin.get(name, Label.LabelStyle.class);
        } catch (Exception e) {
            return skin.get("default", Label.LabelStyle.class);
        }
    }

    private Actor gameIcon(String normalRegion, String selectedRegion, String fallbackLabel) {
        Drawable normal = safeRegion(normalRegion);
        Drawable selected = safeRegion(selectedRegion);
        if (normal != null) {
            Drawable sel = (selected != null) ? selected : normal;
            ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
            st.imageUp = normal;
            st.imageOver = sel;
            st.imageDown = sel;
            return new ImageButton(st);
        }
        Table ph = new Table();
        ph.setBackground(skin.newDrawable("white_pixel", new Color(1, 1, 1, 0.18f)));
        Label l = new Label(fallbackLabel, labelStyle("medium"));
        l.setColor(Color.WHITE);
        ph.add(l);
        return ph;
    }
    private void toggleSettings() {
        if (settingsPopup != null && settingsPopup.hasParent()) {
            settingsPopup.remove();
            return;
        }
        settingsPopup = new SettingsPopup(game);
        settingsPopup.pack();
        settingsPopup.setPosition(
                (getStage().getWidth() - settingsPopup.getWidth()) / 2f,
                (getStage().getHeight() - settingsPopup.getHeight()) / 2f
        );
        getStage().addActor(settingsPopup);
    }
    private Table buildCurrencyBar() {

        Table bar = new Table();
        bar.setTouchable(Touchable.childrenOnly);

        gemLabel = new Label("0", skin);
        gemLabel.setTouchable(Touchable.disabled);
        gemLabel.setFontScale(1.1f);

        Group gemDisplay = createCurrencyDisplay(GEMS, GEMS_CLICKED, gemLabel);
        bar.add(gemDisplay).size(gemDisplay.getWidth(), gemDisplay.getHeight()).padRight(15f);

        coinLabel = new Label("0", skin);
        coinLabel.setTouchable(Touchable.disabled);
        coinLabel.setFontScale(1.1f);

        Group coinDisplay = createCurrencyDisplay(COIN, COIN_CLICKED, coinLabel);
        bar.add(coinDisplay).size(coinDisplay.getWidth(), coinDisplay.getHeight());
        Actor shopButton = gameIcon(SHOP, SHOP_CLICKED, "SHOP");

        shopButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // game.showScreen(new ShopScreen(game));
            }
        });

        bar.add(shopButton).size(60f).padLeft(10f);
        return bar;
    }
    private Group createCurrencyDisplay(String normalAsset, String pressedAsset, Label label) {
        ImageButton button = createCurrencyButton(normalAsset, pressedAsset);
        float width = button.getPrefWidth();
        float height = button.getPrefHeight();

        Group group = new Group();

        group.setSize(
                width,
                height
        );

        button.setBounds(
                0f,
                0f,
                width,
                height
        );

        label.setPosition(
                70f,
                20f
        );

        group.addActor(button);
        group.addActor(label);

        return group;
    }
    private ImageButton createCurrencyButton(String normalAsset, String pressedAsset) {
        TextureRegion normalRegion = game.getTextureBank().region(normalAsset);
        TextureRegion pressedRegion = game.getTextureBank().region(pressedAsset);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);
        ImageButton button = new ImageButton(style);
        button.getImageCell().expand().fill();
        button.getImage().setScaling(Scaling.stretch);

        return button;
    }
    private void refreshCurrencyLabels() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            coinLabel.setText("0");
            gemLabel.setText("0");
            return;
        }

        UserRepository.CurrencyBalance balance = userRepository.getCurrencyBalance(user.getId());
        if (balance == null) {
            coinLabel.setText("0");
            gemLabel.setText("0");
            return;
        }

        user.setCoins(balance.coins());
        user.setGems(balance.gems());

        coinLabel.setText(String.format("%,d", balance.coins()));
        gemLabel.setText(String.format("%,d", balance.gems()));
    }
    @Override
    public void act(float delta) {
        super.act(delta);
        if (!isVisible()) {
            return;
        }
        currencyRefreshTimer += delta;
        if (currencyRefreshTimer >= CURRENCY_REFRESH) {
            currencyRefreshTimer = 0f;
            refreshCurrencyLabels();
        }
        newsRefreshTimer += delta;
        if (newsRefreshTimer >= NEWS_REFRESH) {
            newsRefreshTimer = 0f;
            refreshUnreadNews();
        }
    }
    private void refreshUnreadNews() {
        if (unreadNews == null || unreadNewsLabel == null) {return;}
        int unreadCount = newsController.getUnreadNewsCount();
        if (unreadCount <= 0) {
            unreadNews.setVisible(false);
            return;
        }
        unreadNewsLabel.setText(
                unreadCount > 99 ? "99+" : String.valueOf(unreadCount)
        );
        unreadNews.setVisible(true);
    }
}
