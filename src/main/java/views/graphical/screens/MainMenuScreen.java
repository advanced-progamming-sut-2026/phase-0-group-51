package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controllers.GreenHouseMenuController;
import controllers.MainMenuController;
import graphics.PvzGame;
import views.graphical.ui.CollectionMenuTable;
import views.graphical.ui.NotificationOverlay;
import views.graphical.ui.SettingsPopup;

public class MainMenuScreen extends BaseScreen{
    private SettingsPopup settingsPopup;
    private Stack root;
    private Texture backgroundTexture;
    private final MainMenuController controller = new MainMenuController();
    private NotificationOverlay notificationOverlay;
    private static final String EXIT_NORMAL_ID = "IMAGE_UI_DRAPER_CLOSE_BUTTON";
    private static final String EXIT_PRESSED_ID = "IMAGE_UI_DRAPER_CLOSE_BUTTON_DOWN";
    private static final String BOOK = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_NORMAL";
    private static final String BOOK_CLICKED = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_SELECTED";
    private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String WATERING_POT = "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL";
    private static final String WATERING_POT_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED";
    private static final String PROFILE = "IMAGE_UI_MAINMENU_MM_PLAYERICON";
    private static final String GAME_ICON = "IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL";
    private static final String VASE_BREAKER = "IMAGE_UI_FEATURE_UNLOCK_FEATURE_KEY_ART_VASEBREAKER";
    private static final String SETTINGS = "IMAGE_UI_HUD_SETTINGSBUTTON_BUTTONS_HUD_SETTINGS_NORMAL";
    private static final String SETTINGS_SELECTED = "IMAGE_UI_HUD_SETTINGSBUTTON_BUTTONS_HUD_SETTINGS_SELECTED";
    private static final String NEWS  = "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_NORMAL";
    private static final String NEWS_SELECTED  = "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_SELECTED";
    private static final String leaderBoard = "IMAGE_UI_GAMECENTER_ICON";
    private static final String Dokme = "IMAGE_UI_GENERIC_NAVDOT";
    private static final String Dokme_FILL = "IMAGE_UI_GENERIC_NAVDOT_FILL";
    private final java.util.List<ImageButton> cards = new java.util.ArrayList<>();
    private final java.util.List<Image> navDots = new java.util.ArrayList<>();

    private Group carouselGroup;
    private Table dotsTable;

    private int currentCardIndex = 0;
    protected MainMenuScreen(PvzGame game) {
        super(game);
        buildUi();

    }
    private void buildUi() {
        root = new Stack();
        root.setFillParent(true);
        backgroundTexture = new Texture(
                Gdx.files.internal("assets/backgrounds/MainMenuBG.png")
        );

        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Image backgroundImage = new Image(backgroundTexture);

        backgroundImage.setFillParent(true);
        backgroundImage.setTouchable(Touchable.disabled);

        carouselGroup = new Group();
        carouselGroup.setTouchable(Touchable.childrenOnly);
        dotsTable = new Table();
        cards.clear();

        cards.add(createCards("assets/backgrounds/Adventure.png"));
        cards.add(createCards("assets/backgrounds/vase.png"));
        cards.add(createCards("assets/backgrounds/Beghouled.png"));
        cards.add(createCards("assets/backgrounds/Zombotany.png"));
        cards.add(createCards("assets/backgrounds/Wallnut.png"));
        cards.add(createCards("assets/backgrounds/Izombie.png"));
        cards.add(createCards("assets/backgrounds/Meow.png"));

        Table content = new Table();
        content.center().center();


        content.setWidth(450f);
        ImageButton backButton = createBackButton();
        ImageButton collectionButton = createCollectionButton();
        ImageButton greenHouseButton = createGreenhouseButton();
        ImageButton exitButton = createExitButton();
        ImageButton profile = createProfileButton();
        ImageButton setting = createSettingButton();
        ImageButton news = createNewsButton();
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(controller.logout().success()){
                    game.showScreen(new FirstScreen(game));
                }
                else{
                    System.out.println(controller.logout().message()); // برای دیباگ گذاشتم(کنسول)
                }
            }
        });
        collectionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stage.addActor(new CollectionMenuTable(game));
            }
        });
        greenHouseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showScreen(new GreenHouseScreen(game));
            }
        });
        news.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });

        setting.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (settingsPopup != null && settingsPopup.hasParent()) {
                    settingsPopup.remove();
                    return;
                }
                settingsPopup = new SettingsPopup(game);
                settingsPopup.pack();
                settingsPopup.setPosition(
                        (stage.getWidth() - settingsPopup.getWidth()) / 2f,
                        (stage.getHeight() - settingsPopup.getHeight()) / 2f
                );
                stage.addActor(settingsPopup);
            }
        });
        cards.get(0).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              game.showScreen(new ChapterSelectScreen(game));
            }
        });
        cards.get(1).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // game.showScreen(new AdventureScreen(game));
            }
        });
        cards.get(2).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // game.showScreen(new AdventureScreen(game));
            }
        });
        cards.get(3).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // game.showScreen(new AdventureScreen(game));
            }
        });
        cards.get(4).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // game.showScreen(new AdventureScreen(game));
            }
        });cards.get(5).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // game.showScreen(new AdventureScreen(game));
            }
        });cards.get(6).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // game.showScreen(new AdventureScreen(game));
            }
        });


        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        Container<ImageButton> container = new Container<>(backButton);
        container.setFillParent(true);
        container.top().left();
        container.padTop(15);
        container.padLeft(15);
        Container<ImageButton> container2 = new Container<>(collectionButton);
        container2.setFillParent(true);
        container2.top().left();
        container2.padTop(15);
        container2.padLeft(95);
        Container<ImageButton> container3 = new Container<>(greenHouseButton);
        container3.setFillParent(true);
        container3.top().left();
        container3.padTop(15);
        container3.padLeft(175);
        Container<ImageButton> container4 = new Container<>(exitButton);
        container4.setFillParent(true);
        container4.bottom().left();
        container4.padBottom(15);
        container4.padLeft(15);
        container4.size(60f, 60f);
        Container<ImageButton> container5 = new Container<>(profile);
        container5.setFillParent(true);
        container5.bottom().right();
        container5.padBottom(15);
        container5.padRight(15);
        TextureRegion normal = game.getTextureBank().region(GAME_ICON);
        Image i = new Image(normal) ;
        Container<Image> container6 = new Container<>(i);
        container6.setFillParent(true);
        container6.center().center();
        container6.padBottom(300);
        Container<ImageButton> container7 = new Container<>(setting);
        container7.setFillParent(true);
        container7.bottom().left();
        container7.padBottom(15);
        container7.padLeft(95);
        Container<ImageButton> container8 = new Container<>(news);
        container8.setFillParent(true);
        container8.top().left();
        container8.padTop(15);
        container8.padLeft(255);
        root.add(backgroundImage);
        root.add(content);
        root.add(container);
        root.add(container2);
        root.add(container3);
        root.add(container4);
        root.add(container5);
        root.add(container6);
        root.add(container7);
        root.add(container8);

        notificationOverlay = new NotificationOverlay(game.getSkin());
        root.add(notificationOverlay);

        stage.addActor(root);
        buildDots();
        refreshCarousel();

        root.add(carouselGroup);
        stage.addActor(dotsTable);
    }
    private ImageButton createBackButton() {
        TextureRegion normalRegion = game.getTextureBank().region(BACK);
        TextureRegion pressedRegion = game.getTextureBank().region(BACK_PRESSED);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);

        return new ImageButton(style);
    }
    private ImageButton createCollectionButton() {
        TextureRegion normalRegion = game.getTextureBank().region(BOOK);
        TextureRegion pressedRegion = game.getTextureBank().region(BOOK_CLICKED);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);

        return new ImageButton(style);
    }
    private ImageButton createGreenhouseButton() {
        TextureRegion normalRegion = game.getTextureBank().region(WATERING_POT);
        TextureRegion pressedRegion = game.getTextureBank().region(WATERING_POT_CLICKED);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);

        return new ImageButton(style);
    }
    private ImageButton createExitButton() {
        TextureRegion normalRegion = game.getTextureBank().region(EXIT_NORMAL_ID);
        TextureRegion pressedRegion = game.getTextureBank().region(EXIT_PRESSED_ID);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);
        ImageButton imageButton = new ImageButton(style);
        return imageButton;
    }
    private ImageButton createSettingButton() {
        TextureRegion normalRegion = game.getTextureBank().region(SETTINGS);
        TextureRegion pressedRegion = game.getTextureBank().region(SETTINGS_SELECTED);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);
        ImageButton imageButton = new ImageButton(style);
        return imageButton;
    }
    private ImageButton createNewsButton() {
        TextureRegion normalRegion = game.getTextureBank().region(NEWS);
        TextureRegion pressedRegion = game.getTextureBank().region(NEWS_SELECTED);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);
        ImageButton imageButton = new ImageButton(style);
        return imageButton;
    }
    private ImageButton createProfileButton(){
        TextureRegion normalRegion = game.getTextureBank().region(PROFILE);
        TextureRegionDrawable normal = new TextureRegionDrawable(normalRegion);
        Drawable faded = normal.tint(new Color(1f, 1f, 1f, 0.65f));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = normal;
        style.imageDown = faded;
        style.imageOver = faded;

        return new ImageButton(style);
    }
    private ImageButton createCards(String path){
        Texture texture = new Texture(Gdx.files.internal(path));
        TextureRegion normalRegion =new TextureRegion(texture);;
        TextureRegionDrawable normal = new TextureRegionDrawable(normalRegion);
        Drawable faded = normal.tint(new Color(1f, 1f, 1f, 0.65f));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = normal;
        style.imageDown = faded;
        style.imageOver = faded;

        return new ImageButton(style);
    }
    private void buildDots() {
        navDots.clear();
        dotsTable.clearChildren();

        for (int i = 0; i < 7; i++) {
            TextureRegion region = game.getTextureBank().region(Dokme);
            Image dot = new Image(region);
            navDots.add(dot);
            dotsTable.add(dot).pad(6f).size(18f, 18f);
        }
        dotsTable.pack();
        dotsTable.setPosition((stage.getWidth() - dotsTable.getWidth()) / 2f, 250f);
        refreshDots();
    }
    private void refreshDots() {
        for (int i = 0; i < navDots.size(); i++) {
            TextureRegion region = game.getTextureBank().region(i == currentCardIndex ? Dokme_FILL : Dokme);
            navDots.get(i).setDrawable(new TextureRegionDrawable(region));
        }
    }
    private void refreshCarousel() {
        carouselGroup.clearChildren();

        float mainCardWidth = 520f;
        float mainCardHeight = 175f;

        float previewWidth = 250f;
        float previewHeight = 175f;

        float mainX = 380f;
        float mainY = 280f;

        float previewX = 930f;
        float previewY = 280f;


        ImageButton currentCard = cards.get(currentCardIndex);
        currentCard.setSize(mainCardWidth, mainCardHeight);
        currentCard.getImageCell().expand().fill();
        currentCard.getImage().setScaling(Scaling.stretch);
        currentCard.setPosition(mainX, mainY);
        carouselGroup.addActor(currentCard);

        int nextIndex = (currentCardIndex + 1) % cards.size();
        Drawable previewDrawable = cards.get(nextIndex).getStyle().imageUp;

        Image previewCard = new Image(previewDrawable);
        previewCard.setScaling(Scaling.stretch);
        previewCard.setBounds(previewX, previewY, previewWidth, previewHeight);

        previewCard.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showNextCard();
            }
        });

        carouselGroup.addActor(previewCard);

        refreshDots();
    }
    private void showNextCard() {
        currentCardIndex++;
        if (currentCardIndex >= cards.size()) {
            currentCardIndex = 0;
        }
        refreshCarousel();
    }
}
