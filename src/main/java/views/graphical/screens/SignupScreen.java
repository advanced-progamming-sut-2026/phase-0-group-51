package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controllers.LoginMenuController;
import controllers.SignUpMenuController;
import graphics.PvzGame;
import models.App;
import models.Result;
import models.enums.Menu;
import models.enums.SecurityQuestions;
import views.graphical.ui.BorderedPanel;
import views.graphical.ui.ForgotPassPopup;
import views.graphical.ui.NotificationOverlay;

public class SignupScreen extends BaseScreen{
    private static final float PANEL_WIDTH = 950f;
    private static final float FIELD_WIDTH = 420f;
    private static final float FIELD_HEIGHT = 60f;
    private static final float FULL_WIDTH = 858f;
    private static final float COLUMN_GAP = 18f;
    private static final float ROW_GAP = 10f;
    private Stack root;
    private Texture backgroundTexture;
    private static final String TEXT_FIELD = "IMAGE_UI_SEASONS_UNCOMPRESSED_PVZ2_SEASONS_UIASSET_PRIZE_WINDOW_UPPER_UNLOCKED";
    private final SignUpMenuController controller = new SignUpMenuController();
    private final LoginMenuController loginController = new LoginMenuController();
    private TextField usernameField;
    private TextField passwordField;
    private TextField confirmPasswordField;
    private TextField nicknameField;
    private TextField emailField;
    private TextField genderField;
    private TextField answerField;
    private TextField confirmAnswerField;
    private SelectBox<SecurityQuestions> securityQuestionBox;
    private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private NotificationOverlay notificationOverlay;
    public SignupScreen(PvzGame game){
        super(game);
        buildUi();
    }
    private void buildUi() {
        root = new Stack();
        root.setFillParent(true);
        backgroundTexture = new Texture(Gdx.files.internal("assets/backgrounds/SignupBG.jpg"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true);
        backgroundImage.setTouchable(Touchable.disabled);

        Table content = new Table();
        content.center();

        BorderedPanel panel = new BorderedPanel(game, Color.valueOf("EB8634"));
        Table form = panel.getContent();
        content.add(panel).width(PANEL_WIDTH).center();
        panel.setTransform(true);
        panel.setOriginX(PANEL_WIDTH / 2f);
        panel.setScale(0.8f, 0.8f);
        Label title = new Label("Signing up...", game.getSkin());

        title.setFontScale(1.25f);
        title.setAlignment(Align.center);

        usernameField = createUsernameBox();
        nicknameField = createNicknameBox();
        emailField = createEmailBox();
        genderField = createGenderBox();
        passwordField = createPassBox();
        confirmPasswordField = createConfirmPassBox();
        securityQuestionBox = createQuestionBox();
        securityQuestionBox.setItems(SecurityQuestions.values());
        securityQuestionBox.setSelected(SecurityQuestions.SELECT);
        answerField = createAnswerBox();
        confirmAnswerField = createConfirmAnswerBox();

        TextButton signupButton = createSignupButton();
        signupButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        handleSignup();
                    }
                }
        );

        form.add(title).colspan(2).center().padBottom(18f).row();
        form.add(usernameField).size(FIELD_WIDTH, FIELD_HEIGHT).padRight(COLUMN_GAP).padBottom(ROW_GAP);
        form.add(nicknameField).size(FIELD_WIDTH,FIELD_HEIGHT).padBottom(ROW_GAP).row();
        form.add(emailField).size(FIELD_WIDTH, FIELD_HEIGHT).padRight(COLUMN_GAP).padBottom(ROW_GAP);
        form.add(genderField).size(FIELD_WIDTH, FIELD_HEIGHT).padBottom(ROW_GAP).row();
        form.add(passwordField).size(FIELD_WIDTH, FIELD_HEIGHT).padRight(COLUMN_GAP).padBottom(ROW_GAP);
        form.add(confirmPasswordField).size(FIELD_WIDTH, FIELD_HEIGHT).padBottom(ROW_GAP).row();
        form.add(securityQuestionBox).colspan(2).size(FULL_WIDTH, 65f).padBottom(ROW_GAP).row();
        form.add(answerField).size(FIELD_WIDTH, FIELD_HEIGHT).padRight(COLUMN_GAP).padBottom(18f);
        form.add(confirmAnswerField).size(FIELD_WIDTH, FIELD_HEIGHT).padBottom(18f).row();
        form.add(signupButton).colspan(2).size(240f, 65f).center();

        content.padBottom(180f);
        ImageButton backButton = createBackButton();
        backButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,Actor actor) {
                        game.showScreen(new FirstScreen(game));
                    }
                }
        );

        Container<ImageButton> backContainer = new Container<>(backButton);

        backContainer.setFillParent(true);
        backContainer.top().left();
        backContainer.padTop(15f);
        backContainer.padLeft(15f);

        notificationOverlay = new NotificationOverlay(game.getSkin());
        makeAllTextBlack(content);
        root.add(backgroundImage);
        root.add(content);
        root.add(backContainer);
        root.add(notificationOverlay);

        stage.addActor(root);
    }
    private void makeAllTextBlack(Actor actor) {
        if (actor instanceof Label label) {
            label.setColor(Color.BLACK);
        }

        if (actor instanceof TextField textField) {
            TextField.TextFieldStyle style =
                    new TextField.TextFieldStyle(textField.getStyle());

            style.fontColor = Color.BLACK;
            style.focusedFontColor = Color.BLACK;
            style.messageFontColor = Color.BLACK;
            style.disabledFontColor = Color.DARK_GRAY;

            textField.setStyle(style);
        }

        if (actor instanceof SelectBox<?> selectBox) {
            SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle(selectBox.getStyle());

            style.fontColor = Color.BLACK;
            style.disabledFontColor = Color.DARK_GRAY;

            List.ListStyle listStyle =
                    new List.ListStyle(style.listStyle);

            listStyle.fontColorSelected = Color.BLACK;
            listStyle.fontColorUnselected = Color.BLACK;

            style.listStyle = listStyle;
            selectBox.setStyle(style);
        }

        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                makeAllTextBlack(child);
            }
        }
    }
    private SelectBox<SecurityQuestions> createQuestionBox() {
        Skin skin = game.getSkin();

        List.ListStyle listStyle = new List.ListStyle(skin.get("default", List.ListStyle.class));

        listStyle.font = skin.getFont("FBUSV8C5EI_2");
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.WHITE;

        listStyle.background = skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        );

        listStyle.selection = skin.getDrawable(
                "image_ui_generic_greenbutton_10"
        );

        listStyle.over = skin.getDrawable(
                "image_ui_generic_brownbutton_10"
        );

        ScrollPane.ScrollPaneStyle scrollStyle =
                new ScrollPane.ScrollPaneStyle(
                        skin.get("default", ScrollPane.ScrollPaneStyle.class)
                );


        SelectBox.SelectBoxStyle selectStyle = new SelectBox.SelectBoxStyle();

        selectStyle.font = skin.getFont("FBUSV8C5EI_2");
        selectStyle.fontColor = Color.WHITE;
        selectStyle.disabledFontColor = Color.GRAY;

        selectStyle.background = skin.getDrawable(
                "image_ui_generic_brownbutton_10"
        );


        selectStyle.backgroundOver = skin.getDrawable(
                "image_ui_generic_brownbutton_down_10"
        );

        selectStyle.backgroundOpen = skin.getDrawable(
                "image_ui_generic_greenbutton_10"
        );

        selectStyle.backgroundDisabled = skin.getDrawable(
                "image_ui_generic_disabledbutton_10"
        );

        selectStyle.listStyle = listStyle;
        selectStyle.scrollStyle = scrollStyle;

        SelectBox<SecurityQuestions> selectBox = new SelectBox<>(selectStyle);

        selectBox.setAlignment(Align.center);
        selectBox.getList().setAlignment(Align.center);

        selectBox.setMaxListCount(4);
        return selectBox;
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
    private TextField createUsernameBox() {
        return createTextBox("Enter your username...", false);
    }
    private TextField createPassBox() {
        return createTextBox("Enter your password...", true);
    }
    private TextField createConfirmPassBox() {
        return createTextBox("Confirm your password...", true);
    }
    private TextField createNicknameBox() {
        return createTextBox("Enter your nickname...", false);
    }
    private TextField createEmailBox() {
        return createTextBox("Enter your email...", false);
    }
    private TextField createGenderBox() {
        return createTextBox("Enter your gender...", false);
    }
    private TextField createAnswerBox() {
        return createTextBox("Enter your answer...", false);
    }
    private TextField createConfirmAnswerBox() {
        return createTextBox("Confirm your security answer...", false
        );}
    private void handleSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String nickname = nicknameField.getText().trim();
        String email = emailField.getText().trim();
        String gender = genderField.getText().trim();
        String answer = answerField.getText().trim();
        String confirmAnswer = confirmAnswerField.getText().trim();
        if (!isSuccessful(controller.setUsername(username))) {
            return;
        }

        if (!isSuccessful(controller.setPassword(password))) {
            return;
        }

        if (!isSuccessful(controller.setPasswordConfirm(confirmPassword, password))) {
            return;
        }

        if (!isSuccessful(controller.setNickname(nickname))) {
            return;
        }

        if (!isSuccessful(controller.setEmail(email))) {
            return;
        }

        if (!isSuccessful(controller.setGender(gender))) {
            return;
        }

        SecurityQuestions selectedQuestion = securityQuestionBox.getSelected();

        if (selectedQuestion == SecurityQuestions.SELECT) {
            notificationOverlay.showError(
                    "Please select a security question."
            );
            return;
        }

        Result result = controller.setQuestion(String.valueOf(selectedQuestion.getNum()), answer, confirmAnswer);
        if (!result.success()) {
            notificationOverlay.showError(result.message());
            return;
        }
        notificationOverlay.showInfo(result.message());
        clearForm();
        Result loginResult = loginController.login(username, password, false);
        if (!loginResult.success()) {
            notificationOverlay.showError(
                    "Registration was successful, but automatic login failed: " + loginResult.message());
            return;
        }
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
        game.showScreen(new MainMenuScreen(game));

    }
    private boolean isSuccessful(Result result) {
        if (!result.success()) {
            notificationOverlay.showError(
                    result.message()
            );
            return false;
        }

        return true;
    }
    private TextButton createSignupButton() {
        TextButton button = new TextButton("Sign Up", game.getSkin(), "green");
        button.getLabel().setFontScale(0.9f);
        return button;
    }
    private void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        nicknameField.setText("");
        emailField.setText("");
        genderField.setText("");
        answerField.setText("");
        confirmAnswerField.setText("");

        securityQuestionBox.setSelectedIndex(0);

        stage.setKeyboardFocus(usernameField);
    }
    @Override
    public void show() {
        super.show();
        game.hideHud();
    }
}
