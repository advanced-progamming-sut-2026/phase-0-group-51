package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import graphics.PvzGame;

public class LoginScreen extends BaseScreen {
    private Stack root;
    private Texture backgroundTexture;
    private static final String TEXT_FIELD = "IMAGE_UI_GENERIC_BUTTON_GENERIC_LTECURRENCY";
    private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String LOGIN = "IMAGE_UI_GENERIC_VTB";

    public LoginScreen(PvzGame game) {
        super(game);
        buildUi();
    }

    private void buildUi() {
        root = new Stack();
        root.setFillParent(true);
        backgroundTexture = new Texture(
                Gdx.files.internal("assets/backgrounds/LoginMenuBG.png")
        );

        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setScaleY(1.2f);
        backgroundImage.setFillParent(true);
        backgroundImage.setTouchable(Touchable.disabled);


        Table content = new Table();
        content.center().center();
        content.add(createUsernameBox()).size(450f, 80f).padRight(50f).center().row();
        content.add(createPassBox()).size(450f, 80f).padBottom(70f).padRight(50f).center().row();
        content.add(createLoginButton().center());
        root.add(backgroundImage);
        content.setWidth(450f);
        ImageButton backButton = createBackButton();

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showScreen(new FirstScreen(game));
            }
        });
        Container<ImageButton> container = new Container<>(backButton);
        container.setFillParent(true);
        container.top().left();
        container.padTop(15);
        container.padLeft(15);
        root.add(backgroundImage);
        root.add(content);
        root.add(container);

        stage.addActor(root);
    }

    @Override
    public void show() {
        game.hideHud();

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.05f, 0.05f, 0.05f, 1f);
        super.render(delta);
    }

    @Override
    public void hide() {

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
    private TextButton createLoginButton() {
        TextureRegion normalRegion = game.getTextureBank().region(LOGIN);
        TextButton.TextButtonStyle style =
                new TextButton.TextButtonStyle(game.getSkin().get(TextButton.TextButtonStyle.class));
        style.up = new TextureRegionDrawable(normalRegion);
        return new TextButton("Login",style);
    }

    private TextField createUsernameBox() {
        return createTextBox("Enter your username...", false);
    }
    private TextField createPassBox() {
        return createTextBox("Enter your password...", true);
    }
    private TextField createTextBox(String message, boolean passwordMode) {
        TextureRegion region = game.getTextureBank().region(TEXT_FIELD);
        TextureRegionDrawable background = new TextureRegionDrawable(region);

        background.setLeftWidth(95f);
        background.setRightWidth(35f);
        background.setTopHeight(12f);
        background.setBottomHeight(12f);

        TextField.TextFieldStyle style = new TextField.TextFieldStyle(game.getSkin().get(TextField.TextFieldStyle.class));


        style.background = background;
        style.focusedBackground = background;
        style.disabledBackground = background;
        style.fontColor = Color.WHITE;
        style.focusedFontColor = Color.WHITE;
        style.disabledFontColor = Color.GRAY;
        style.messageFontColor = Color.WHITE;

        TextField textField = new TextField("", style);
        textField.setMessageText(message);

        if (passwordMode) {
            textField.setPasswordMode(true);
            textField.setPasswordCharacter('*');
        }
        return textField;
    }

}
