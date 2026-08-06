package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controllers.GreenHouseMenuController;
import controllers.MainMenuController;
import graphics.PvzGame;
import views.graphical.ui.CollectionMenuTable;
import views.graphical.ui.NotificationOverlay;

public class MainMenuScreen extends BaseScreen{
    private Stack root;
    private Texture backgroundTexture;
    private final MainMenuController controller = new MainMenuController();
    private NotificationOverlay notificationOverlay;
    private static final String BOOK = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_NORMAL";
    private static final String BOOK_CLICKED = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_SELECTED";
    private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String WATERING_POT = "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL";
    private static final String WATERING_POT_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED";
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

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showScreen(new MainMenuScreen(game));
            }
        });
        collectionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stage.addActor(new CollectionMenuTable(game, ()-> game.showScreen(new FirstScreen(game))));
            }
        });
        greenHouseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showScreen(new GreenHouseScreen(game));
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
        container3.padLeft(190);
        root.add(backgroundImage);
        root.add(content);
        root.add(container);
        root.add(container2);
        root.add(container3);

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
}
