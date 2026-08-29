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
import com.badlogic.gdx.Gdx;
import network.client.service.AccountClientService;
import network.protocol.auth.ForgotPasswordAnswerRequest;
import network.protocol.auth.ForgotPasswordStartRequest;
import network.protocol.auth.ForgotPasswordStartResponse;
import network.protocol.auth.PasswordResetRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ForgotPassPopup extends BorderedPanel{
    PvzGame game;
    private NotificationOverlay notificationOverlay;
    TextField usernameF,emailF,answerF,newPassF;
    private final Runnable closeAction;
    private final Label questionLabel;
    private final Table content;
    public ForgotPassPopup(PvzGame game, Color bgColor,NotificationOverlay notificationOverlay,  Runnable closeAction) {
        super(game, Color.valueOf("#BC6334"));
         emailF = new TextField("", game.getSkin());
        emailF.setMessageText("Enter your email...");
        content = getContent();

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
    private void handleForgotPass() {
        String username =
                usernameF.getText().trim();

        String email =
                emailF.getText().trim();

        if (username.isEmpty()) {
            notificationOverlay.showError(
                    "Please enter your username."
            );
            return;
        }

        if (email.isEmpty()) {
            notificationOverlay.showError(
                    "Please enter your email."
            );
            return;
        }

        ForgotPasswordStartRequest request =
                new ForgotPasswordStartRequest(
                        username,
                        email
                );

        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(
                        ignored ->
                                sendPasswordRecoveryStart(request)
                )
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(() -> {
                                    if (throwable != null) {
                                        notificationOverlay.showError(
                                                "Could not connect to server: "
                                                        + rootMessage(throwable)
                                        );
                                        return;
                                    }

                                    if (response == null
                                            || !response.isSuccess()) {
                                        notificationOverlay.showError(
                                                response == null
                                                        ? "Password recovery failed."
                                                        : response.getMessage()
                                        );
                                        return;
                                    }

                                    questionLabel.setText(
                                            response.getSecurityQuestion()
                                    );

                                    showAnswerStep();
                                })
                );
    }
    private CompletableFuture<ForgotPasswordStartResponse>
    sendPasswordRecoveryStart(
            ForgotPasswordStartRequest request
    ) {
        try {
            AccountClientService service =
                    game.getNetworkManager()
                            .getAccountClientService();

            return service.startPasswordRecovery(
                    request
            );
        } catch (IOException | RuntimeException exception) {
            return failedFuture(exception);
        }
    }
    private static <T> CompletableFuture<T> failedFuture(
            Throwable throwable
    ) {
        CompletableFuture<T> future =
                new CompletableFuture<>();

        future.completeExceptionally(throwable);

        return future;
    }
    private void showAnswerStep() {
        content.clearChildren();
        content.defaults().pad(8f);
        Label title = createTitle("Security Question");
        TextButton continueButton = createButton("Continue", this::handleAnswer);
        TextButton cancelButton = createButton("Cancel", this::close);
        content.add(title).padBottom(15f).row();
        content.add(questionLabel).width(430f).padBottom(10f).row();

        content.add(answerF).width(430f).height(60f).padBottom(10f).row();

        Table buttons = new Table();
        buttons.add(cancelButton).width(150f).height(55f).padRight(10f);
        buttons.add(continueButton).width(180f).height(55f);
        content.add(buttons).padTop(10f).row();

        focus(answerF);
    }
    private void handleAnswer() {
        ForgotPasswordAnswerRequest request =
                new ForgotPasswordAnswerRequest(
                        answerF.getText()
                );

        try {
            game.getNetworkManager()
                    .getAccountClientService()
                    .answerSecurityQuestion(request)
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(() -> {
                                        if (throwable != null) {
                                            notificationOverlay.showError(
                                                    rootMessage(throwable)
                                            );
                                            return;
                                        }

                                        if (!response.isSuccess()) {
                                            notificationOverlay.showError(
                                                    response.getMessage()
                                            );
                                            return;
                                        }

                                        showNewPasswordStep();
                                    })
                    );
        } catch (IOException | RuntimeException exception) {
            notificationOverlay.showError(
                    rootMessage(exception)
            );
        }
    }
    private void handleChangePass() {
        PasswordResetRequest request =
                new PasswordResetRequest(
                        newPassF.getText()
                );

        try {
            game.getNetworkManager()
                    .getAccountClientService()
                    .resetPassword(request)
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(() -> {
                                        if (throwable != null) {
                                            notificationOverlay.showError(
                                                    rootMessage(throwable)
                                            );
                                            return;
                                        }

                                        if (!response.isSuccess()) {
                                            notificationOverlay.showError(
                                                    response.getMessage()
                                            );
                                            return;
                                        }

                                        notificationOverlay.showInfo(
                                                response.getMessage()
                                        );

                                        close();
                                    })
                    );
        } catch (IOException | RuntimeException exception) {
            notificationOverlay.showError(
                    rootMessage(exception)
            );
        }
    }
    private static String rootMessage(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }

        return message;
    }
    private void showNewPasswordStep() {
        content.clearChildren();
        content.defaults().pad(8f);

        Label title = createTitle("New Password");
        TextButton changeButton = createButton("Change Password", this::handleChangePass);
        TextButton cancelButton = createButton("Cancel", this::close);

        content.add(title).padBottom(15f).row();
        content.add(newPassF).width(430f).height(60f).padBottom(10f).row();
        Table buttons = new Table();
        buttons.add(cancelButton).width(150f).height(55f).padRight(10f);
        buttons.add(changeButton).width(200f).height(55f);
        content.add(buttons).padTop(10f).row();
        focus(newPassF);
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
