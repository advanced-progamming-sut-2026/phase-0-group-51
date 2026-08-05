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
import controllers.LoginMenuController;
import graphics.PvzGame;
import models.Result;
import views.graphical.ui.NotificationOverlay;

public class LoginScreen extends BaseScreen {
    private Stack root;
    private Texture backgroundTexture;
    private static final String TEXT_FIELD = "IMAGE_UI_GENERIC_BUTTON_GENERIC_LTECURRENCY";
    private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String LOGIN = "IMAGE_UI_GENERIC_VTB";
    private final LoginMenuController controller = new LoginMenuController();
    private TextField usernameField;
    private TextField passwordField;
    private NotificationOverlay notificationOverlay;
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
        usernameField = createUsernameBox();
        passwordField = createPassBox();
        TextButton loginButton = createLoginButton();

        content.add(usernameField).size(450f, 80f).padRight(50f).center().row();
        content.add(passwordField).size(450f, 80f).padBottom(20f).padRight(50f).center().row();
        content.add(createStayLoggedIn()).padBottom(30f).row();
        content.add(loginButton.center()).padBottom(15f).row();
        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleLogin();
            }
        });
        content.add(createForgotPass()).width(150f);
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

        notificationOverlay = new NotificationOverlay(game.getSkin());
        root.add(notificationOverlay);

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
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        Result result = controller.login(username, password, false);
        if (!result.success()) {
            notificationOverlay.showError(result.message());
            return;
        }
        notificationOverlay.showInfo(result.message());
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
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(game.getSkin().get(TextButton.TextButtonStyle.class));
        TextureRegionDrawable background = new TextureRegionDrawable(normalRegion);
        style.up = background;
        style.down = background.tint(new Color(1, 1, 1, 0.8f));
        style.over = background.tint(new Color(1, 1, 1, 0.8f));
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
    private  TextButton createForgotPass(){
        TextureRegion region = game.getTextureBank().region("IMAGE_UI_SEASONS_UNCOMPRESSED_RED_FLAG");
        TextureRegion region2 = game.getTextureBank().region("IMAGE_UI_GENERIC_TIMER_RIBBON_RED");
        TextureRegionDrawable background = new TextureRegionDrawable(region);
        TextureRegionDrawable background2 = new TextureRegionDrawable(region2);
        TextButton.TextButtonStyle style =
                new TextButton.TextButtonStyle(game.getSkin().get(TextButton.TextButtonStyle.class));
        style.up = background;
        style.down = background2;
        style.over = background2;
        style.font.getData().setScale(0.7f);
        return new TextButton("Forgot Password",style);
    }
    private CheckBox createStayLoggedIn(){
        TextureRegion offRegion = game.getTextureBank().region("IMAGE_UI_ALMANAC_CHECKBOX_DISABLED_SHARP");
        TextureRegion onRegion = game.getTextureBank().region("IMAGE_UI_ALMANAC_CHECKBOX_ENABLED_SHARP");
        TextureRegionDrawable offDrawable = new TextureRegionDrawable(offRegion);
        TextureRegionDrawable onDrawable = new TextureRegionDrawable(onRegion);

        offDrawable.setMinSize(25f, 25f);
        onDrawable.setMinSize(25f, 25f);

        TextButton.TextButtonStyle textStyle = game.getSkin().get(TextButton.TextButtonStyle.class);
        CheckBox.CheckBoxStyle style = new CheckBox.CheckBoxStyle();

        style.checkboxOff = offDrawable;
        style.checkboxOn = onDrawable;

        style.checkboxOver = offDrawable;
        style.checkboxOnOver = onDrawable;

        style.checkboxOffDisabled = offDrawable;
        style.checkboxOnDisabled = onDrawable;

        style.font = textStyle.font;
        style.font = game.getSkin().getFont(
                "FBUSV8C5EI_2_outline"
        );
        style.fontColor = Color.WHITE;

        return new CheckBox("Stay logged in", style);
    }
}
