package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controllers.LoginMenuController;
import graphics.PvzGame;
import models.Result;

public class ForgotPassPopup extends BorderedPanel{
    PvzGame game;
    private final LoginMenuController controller = new LoginMenuController();
    private NotificationOverlay notificationOverlay;
    TextField usernameF,emailF,answerF,newPassF;
    private final Runnable closeAction;
    private final Label questionLabel;
    public ForgotPassPopup(PvzGame game, Color bgColor,NotificationOverlay notificationOverlay,  Runnable closeAction) {
        super(game, Color.valueOf("#BC6334"));
         emailF = new TextField("", game.getSkin());
        emailF.setMessageText("Enter your email...");
         usernameF = new TextField("", game.getSkin());
        usernameF.setMessageText("Enter your username...");
         answerF = new TextField("", game.getSkin());
        answerF.setMessageText("Enter your security answer...");
        newPassF = new TextField("",game.getSkin());
        newPassF.setMessageText("Enter your new password...");
        newPassF.setPasswordMode(true);
        newPassF.setPasswordCharacter('*');
        this.game = game;
        this.notificationOverlay = notificationOverlay;
        questionLabel = new Label("", game.getSkin());
        questionLabel.setWrap(true);
        this.closeAction = closeAction;
        showIdentityStep();

    }
    private void showIdentityStep() {
        Table content = getContent();
        content.clearChildren();
        content.defaults().pad(8f);
        Label title = createTitle("Forgot Password");
        TextButton continueButton = createButton("Continue", this::handleForgotPass);
        TextButton cancelButton = createButton("Cancel", this::close);
        content.add(title)
                .padBottom(15f)
                .row();

        content.add(usernameF)
                .width(430f)
                .height(60f)
                .row();

        content.add(emailF)
                .width(430f)
                .height(60f)
                .row();

        Table buttons = new Table();
        buttons.add(cancelButton)
                .width(150f)
                .height(55f)
                .padRight(10f);

        buttons.add(continueButton)
                .width(180f)
                .height(55f);

        content.add(buttons)
                .padTop(10f)
                .row();

        focus(usernameF);
    }
    private void handleForgotPass(){
        String username = usernameF.getText().trim();
        String email = emailF.getText();
        Result result = controller.forgetPassword(username,email);
        if (!result.success()) {
            notificationOverlay.showError(result.message());
            return;
        }
        handleAnswer();
        notificationOverlay.showInfo(result.message());
    }
    private void handleAnswer(){
        String answer = answerF.getText();
        Result result = controller.answerQuestion(answer);
        if (!result.success()) {
            notificationOverlay.showError(result.message());
            return;
        }
        handleChangePass();;
        notificationOverlay.showInfo(result.message());
    }
    private void handleChangePass(){
        String newPass = newPassF.getText();
        Result result = controller.setNewPassword(newPass);
        if (!result.success()) {
            notificationOverlay.showError(result.message());
            newPassF.selectAll();
            focus(newPassF);
            return;
        }
        notificationOverlay.showInfo(result.message());
        close();
    }

    private Label createTitle(String text) {
        Label title = new Label(text, game.getSkin());
        title.setFontScale(1.4f);
        return title;
    }
    private TextButton createButton(String text, Runnable action) {
        TextButton button = new TextButton(text, game.getSkin());
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });

        return button;
    }
    private void focus(TextField field) {
        Stage stage = getStage();

        if (stage != null) {
            stage.setKeyboardFocus(field);
        }
    }
    private void close() {
        if (closeAction != null) {
            closeAction.run();
        } else {
            remove();
        }
    }
}
