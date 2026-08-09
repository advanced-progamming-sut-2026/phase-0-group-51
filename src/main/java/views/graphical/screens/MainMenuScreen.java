package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
               ImageButton iZombie = createCards( "assets/backgrounds/Izombie.png");
               ImageButton beghouled = createCards("assets/backgrounds/Beghouled.png");
               ImageButton zombotany = createCards("assets/backgrounds/Zombotany.png");
               ImageButton wallnut = createCards("assets/backgrounds/Wallnut.png");
               ImageButton meowPoint = createCards("assets/backgrounds/Meow.png");
               ImageButton adventure = createCards("assets/backgrounds/Adventure.png");
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
}
