package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import graphics.PvzGame;
import models.App;
import models.User;
import network.client.ClientSessionTokenStore;
import network.client.service.ProfileClientService;
import network.protocol.profile.ProfileDataDto;
import network.protocol.profile.ProfilePasswordChangeRequest;
import network.protocol.profile.ProfileResponse;
import network.protocol.profile.ProfileUpdateRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ProfilePopup extends BorderedPanel {
    private final PvzGame game;
    private final Table content;

    private static final Color PAPER = Color.valueOf("EBE6D1");
    private static final Color BACKGROUND = Color.valueOf("D8D0AD");
    private static final Color TEXT = Color.valueOf("FFF4C2");
    private static final Color SECONDARY_TEXT = Color.valueOf("E6D9A8");
    private static final Color TITLE = Color.valueOf("FFE16A");

    private ProfileDataDto currentData;
    private boolean requestInFlight;

    public ProfilePopup(PvzGame game) {
        super(game, Color.valueOf("A0522D"));
        this.game = game;
        this.content = getContent();
        buildProfile();
    }

    private void buildProfile() {
        content.clearChildren();
        content.top();
        content.pad(18f);

        Label loading = new Label(
                "Loading profile...",
                labelStyle("medium_outline")
        );
        loading.setColor(TEXT);

        content.add(loading)
                .pad(40f);

        loadProfile();
    }

    private void loadProfile() {
        if (requestInFlight) {
            return;
        }

        requestInFlight = true;

        sendGetProfile()
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishLoadProfile(
                                                response,
                                                throwable
                                        )
                                )
                );
    }

    private CompletableFuture<ProfileResponse> sendGetProfile() {
        try {
            return profileService().getProfile();
        } catch (IOException | RuntimeException exception) {
            return failedFuture(exception);
        }
    }

    private void finishLoadProfile(
            ProfileResponse response,
            Throwable throwable
    ) {
        requestInFlight = false;

        if (throwable != null) {
            showLoadError(
                    "Could not load profile: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()
                || response.getProfile() == null) {
            showLoadError(
                    response == null
                            ? "Could not load profile."
                            : response.getMessage()
            );
            return;
        }

        currentData = response.getProfile();
        applyProfileToClient(currentData);
        showProfileData();
    }

    private void showLoadError(String message) {
        content.clearChildren();
        content.top();
        content.pad(18f);

        Label error = new Label(
                message,
                labelStyle("medium_outline")
        );
        error.setColor(Color.RED);
        error.setWrap(true);

        TextButton closeButton =
                new TextButton("CLOSE", game.getSkin());

        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(
                    InputEvent event,
                    float x,
                    float y
            ) {
                remove();
            }
        });

        content.add(error)
                .width(500f)
                .pad(25f)
                .row();

        content.add(closeButton)
                .width(160f)
                .height(45f);

        refreshPopupSize();
    }

    private void showProfileData() {
        if (currentData == null) {
            showLoadError("No profile data available.");
            return;
        }

        content.clearChildren();
        content.top();
        content.pad(18f);

        buildHeader();

        content.add(buildIdentity())
                .width(580f)
                .padTop(8f)
                .padBottom(12f)
                .row();

        content.add(buildStats())
                .width(580f)
                .padBottom(14f)
                .row();

        content.add(buildButtons())
                .growX()
                .padBottom(8f);

        refreshPopupSize();
    }

    private void buildHeader() {
        Table header = new Table();

        Label title =
                new Label(
                        "PLAYER PROFILE",
                        labelStyle("big_outline")
                );

        title.setColor(TITLE);
        title.setAlignment(Align.center);

        TextButton closeButton =
                new TextButton("X", game.getSkin());

        closeButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        remove();
                    }
                }
        );

        header.add().width(45f);

        header.add(title)
                .expandX()
                .center();

        header.add(closeButton)
                .size(45f);

        content.add(header)
                .growX()
                .padBottom(12f)
                .row();
    }

    private Table buildIdentity() {
        Table box = new Table();

        box.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        PAPER
                )
        );

        box.pad(18f);

        Table avatar = new Table();

        avatar.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("C7B77F")
                )
        );

        String nickname = currentData.getNickname();

        String initial =
                nickname == null || nickname.isBlank()
                        ? "?"
                        : nickname.substring(0, 1)
                        .toUpperCase();

        Label initialLabel =
                new Label(
                        initial,
                        labelStyle("big_outline")
                );

        initialLabel.setColor(Color.WHITE);
        initialLabel.setAlignment(Align.center);

        avatar.add(initialLabel)
                .expand()
                .fill();

        Table information = new Table();
        information.left();

        Label nicknameLabel =
                new Label(
                        currentData.getNickname(),
                        labelStyle("big_outline")
                );

        nicknameLabel.setColor(TEXT);

        Label usernameLabel =
                new Label(
                        "@" + currentData.getUsername(),
                        labelStyle("medium_outline")
                );

        usernameLabel.setColor(SECONDARY_TEXT);

        Label emailLabel =
                new Label(
                        currentData.getEmail(),
                        labelStyle("medium_outline")
                );

        emailLabel.setColor(SECONDARY_TEXT);

        information.add(nicknameLabel)
                .left()
                .row();

        information.add(usernameLabel)
                .left()
                .padTop(3f)
                .row();

        information.add(emailLabel)
                .left()
                .padTop(5f);

        box.add(avatar)
                .size(95f)
                .padRight(22f);

        box.add(information)
                .expandX()
                .left();

        return box;
    }

    private Table buildStats() {
        Table stats = new Table();

        stats.add(
                        createStatCard(
                                "GAMES PLAYED",
                                String.valueOf(
                                        currentData.getGamesPlayed()
                                )
                        )
                )
                .width(275f)
                .height(82f)
                .pad(5f);

        stats.add(
                        createStatCard(
                                "LEVELS PASSED",
                                String.valueOf(
                                        currentData.getPassedLevels()
                                )
                        )
                )
                .width(275f)
                .height(82f)
                .pad(5f)
                .row();

        stats.add(
                        createStatCard(
                                "COINS",
                                String.format(
                                        "%,d",
                                        currentData.getCoins()
                                )
                        )
                )
                .width(275f)
                .height(82f)
                .pad(5f);

        stats.add(
                        createStatCard(
                                "GEMS",
                                String.format(
                                        "%,d",
                                        currentData.getGems()
                                )
                        )
                )
                .width(275f)
                .height(82f)
                .pad(5f)
                .row();

        Table meowPoint =
                createStatCard(
                        "BEST MEOWPOINT",
                        String.format(
                                "%,d",
                                currentData.getMostMeowPoint()
                        )
                );

        stats.add(meowPoint)
                .colspan(2)
                .width(560f)
                .height(90f)
                .padTop(7f);

        return stats;
    }

    private Table createStatCard(
            String title,
            String value
    ) {
        Table card = new Table();

        card.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        BACKGROUND
                )
        );

        Label titleLabel =
                new Label(
                        title,
                        labelStyle("medium_outline")
                );

        titleLabel.setColor(SECONDARY_TEXT);

        Label valueLabel =
                new Label(
                        value,
                        labelStyle("big_outline")
                );

        valueLabel.setColor(TEXT);

        card.add(titleLabel)
                .padTop(8f)
                .row();

        card.add(valueLabel)
                .padTop(3f)
                .padBottom(8f);

        return card;
    }

    private Table buildButtons() {
        Table buttons = new Table();

        TextButton editButton =
                new TextButton(
                        "EDIT PROFILE",
                        game.getSkin()
                );

        TextButton passwordButton =
                new TextButton(
                        "CHANGE PASSWORD",
                        game.getSkin()
                );

        editButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        buildEditProfile();
                    }
                }
        );

        passwordButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        buildChangePassword();
                    }
                }
        );

        buttons.add(editButton)
                .width(220f)
                .height(45f)
                .padRight(10f);

        buttons.add(passwordButton)
                .width(220f)
                .height(45f);

        return buttons;
    }

    private void buildEditProfile() {
        content.clearChildren();
        content.top();
        content.pad(18f);

        Label title =
                new Label(
                        "EDIT PROFILE",
                        labelStyle("big_outline")
                );

        title.setColor(TITLE);

        content.add(title)
                .padBottom(18f)
                .row();

        Table form = new Table();

        form.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        PAPER
                )
        );

        form.pad(20f);

        TextField usernameField =
                new TextField(
                        currentData.getUsername(),
                        game.getSkin()
                );

        TextField nicknameField =
                new TextField(
                        currentData.getNickname(),
                        game.getSkin()
                );

        TextField emailField =
                new TextField(
                        currentData.getEmail(),
                        game.getSkin()
                );

        addField(form, "Username", usernameField);
        addField(form, "Nickname", nicknameField);
        addField(form, "Email", emailField);

        content.add(form)
                .width(500f)
                .padBottom(15f)
                .row();

        Table buttons = new Table();

        TextButton save =
                new TextButton("SAVE", game.getSkin());

        TextButton cancel =
                new TextButton("BACK", game.getSkin());

        save.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        saveProfile(
                                usernameField.getText(),
                                nicknameField.getText(),
                                emailField.getText()
                        );
                    }
                }
        );

        cancel.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        showProfileData();
                    }
                }
        );

        buttons.add(cancel)
                .width(130f)
                .height(42f)
                .padRight(10f);

        buttons.add(save)
                .width(160f)
                .height(42f);

        content.add(buttons);
        refreshPopupSize();
    }

    private void addField(
            Table table,
            String name,
            TextField field
    ) {
        Label label =
                new Label(
                        name,
                        labelStyle("medium_outline")
                );

        label.setColor(TEXT);

        table.add(label)
                .width(120f)
                .left()
                .padBottom(12f);

        table.add(field)
                .width(300f)
                .height(42f)
                .padBottom(12f)
                .row();
    }

    private void saveProfile(
            String username,
            String nickname,
            String email
    ) {
        if (requestInFlight) {
            return;
        }

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        username.trim(),
                        nickname.trim(),
                        email.trim()
                );

        requestInFlight = true;

        sendUpdateProfile(request)
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishProfileUpdate(
                                                response,
                                                throwable
                                        )
                                )
                );
    }

    private CompletableFuture<ProfileResponse> sendUpdateProfile(
            ProfileUpdateRequest request
    ) {
        try {
            return profileService().updateProfile(request);
        } catch (IOException | RuntimeException exception) {
            return failedFuture(exception);
        }
    }

    private void finishProfileUpdate(
            ProfileResponse response,
            Throwable throwable
    ) {
        requestInFlight = false;

        if (throwable != null) {
            game.notifyError(
                    "Profile update failed: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()
                || response.getProfile() == null) {
            game.notifyError(
                    response == null
                            ? "Profile update failed."
                            : response.getMessage()
            );
            return;
        }

        currentData = response.getProfile();
        applyProfileToClient(currentData);

        game.notifyInfo(response.getMessage());
        showProfileData();
    }

    private void buildChangePassword() {
        content.clearChildren();
        content.top();
        content.pad(18f);

        Label title =
                new Label(
                        "CHANGE PASSWORD",
                        labelStyle("big_outline")
                );

        title.setColor(TITLE);

        content.add(title)
                .padBottom(18f)
                .row();

        Table form = new Table();

        form.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        PAPER
                )
        );

        form.pad(20f);

        TextField oldPassword = passwordField();
        TextField newPassword = passwordField();
        TextField confirmPassword = passwordField();

        addField(form, "Current", oldPassword);
        addField(form, "New", newPassword);
        addField(form, "Confirm", confirmPassword);

        content.add(form)
                .width(500f)
                .padBottom(15f)
                .row();

        Table buttons = new Table();

        TextButton back =
                new TextButton("BACK", game.getSkin());

        TextButton save =
                new TextButton("CHANGE", game.getSkin());

        back.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        showProfileData();
                    }
                }
        );

        save.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                            InputEvent event,
                            float x,
                            float y
                    ) {
                        if (!newPassword.getText()
                                .equals(
                                        confirmPassword.getText()
                                )) {
                            game.notifyError(
                                    "Passwords do not match."
                            );
                            return;
                        }

                        changePassword(
                                oldPassword.getText(),
                                newPassword.getText()
                        );
                    }
                }
        );

        buttons.add(back)
                .width(130f)
                .height(42f)
                .padRight(10f);

        buttons.add(save)
                .width(160f)
                .height(42f);

        content.add(buttons);
        refreshPopupSize();
    }

    private void changePassword(
            String oldPassword,
            String newPassword
    ) {
        if (requestInFlight) {
            return;
        }

        ProfilePasswordChangeRequest request =
                new ProfilePasswordChangeRequest(
                        oldPassword,
                        newPassword
                );

        requestInFlight = true;

        sendPasswordChange(request)
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishPasswordChange(
                                                response,
                                                throwable
                                        )
                                )
                );
    }

    private CompletableFuture<ProfileResponse> sendPasswordChange(
            ProfilePasswordChangeRequest request
    ) {
        try {
            return profileService().changePassword(request);
        } catch (IOException | RuntimeException exception) {
            return failedFuture(exception);
        }
    }

    private void finishPasswordChange(
            ProfileResponse response,
            Throwable throwable
    ) {
        requestInFlight = false;

        if (throwable != null) {
            game.notifyError(
                    "Password change failed: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()) {
            game.notifyError(
                    response == null
                            ? "Password change failed."
                            : response.getMessage()
            );
            return;
        }

        ClientSessionTokenStore.clear();

        if (response.getProfile() != null) {
            currentData = response.getProfile();
            applyProfileToClient(currentData);
        }

        game.notifyInfo(response.getMessage());
        showProfileData();
    }

    private ProfileClientService profileService() {
        return new ProfileClientService(
                game.getNetworkManager()
                        .getNetworkClient()
        );
    }

    private void applyProfileToClient(
            ProfileDataDto profile
    ) {
        User user = App.getInstance().getLoggedInUser();

        if (user == null || profile == null) {
            return;
        }

        user.setUsername(profile.getUsername());
        user.setNickname(profile.getNickname());
        user.setEmail(profile.getEmail());
        user.setGamesPlayed(profile.getGamesPlayed());
        user.setCoins(profile.getCoins());
        user.setGems(profile.getGems());
        user.setMostMeowPoint(profile.getMostMeowPoint());
    }

    private void refreshPopupSize() {
        pack();

        if (getStage() != null) {
            setPosition(
                    (getStage().getWidth() - getWidth()) / 2f,
                    (getStage().getHeight() - getHeight()) / 2f
            );
        }
    }

    private TextField passwordField() {
        TextField field =
                new TextField("", game.getSkin());

        field.setPasswordMode(true);
        field.setPasswordCharacter('*');

        return field;
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return game.getSkin().get(
                    name,
                    Label.LabelStyle.class
            );
        } catch (Exception exception) {
            return game.getSkin().get(
                    "default",
                    Label.LabelStyle.class
            );
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
}
