package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.ProfileMenuController;
import controllers.SettingMenuController;
import graphics.PvzGame;
import models.App;
import models.Result;
import models.User;

public class ProfilePopup extends BorderedPanel{
    PvzGame game;
    private final ProfileMenuController controller;
    private final Table content;
    private static final Color PAPER = Color.valueOf("EBE6D1");
    private static final Color BACKGROUND = Color.valueOf("D8D0AD");
    private static final Color TEXT = Color.valueOf("FFF4C2");
    private static final Color SECONDARY_TEXT = Color.valueOf("E6D9A8");
    private static final Color TITLE = Color.valueOf("FFE16A");
    private ProfileMenuController.ProfileData currentData;
    public ProfilePopup(PvzGame game) {
        super(game, com.badlogic.gdx.graphics.Color.valueOf("A0522D"));
        this.game = game;
        this.controller = new ProfileMenuController();
        this.content = getContent();
        buildProfile();
    }
    private void buildProfile() {
        content.clearChildren();
        content.top();
        content.pad(18f);
        currentData = controller.getProfileData();
        if (currentData == null) {
            Label error = new Label("No logged in user.", labelStyle("medium_outline"));
            error.setColor(Color.RED);
            content.add(error).pad(40f);
            return;
        }
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
    }

    private void buildHeader() {
        Table header = new Table();
        Label title =
                new Label("PLAYER PROFILE", labelStyle("big_outline"));
        title.setColor(TITLE);
        title.setAlignment(Align.center);
        TextButton closeButton = new TextButton("X", game.getSkin());

        closeButton.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        remove();
                    }
                }
        );

        header.add()
                .width(45f);

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
                game.getSkin().newDrawable("white_pixel", PAPER)
        );
        box.pad(18f);
        Table avatar = new Table();
        avatar.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel", Color.valueOf("C7B77F")));
        String nickname = currentData.nickname();
        String initial =nickname == null || nickname.isBlank() ? "?" : nickname.substring(0, 1)
                        .toUpperCase();

        Label initialLabel = new Label(initial, labelStyle("big_outline"));

        initialLabel.setColor(Color.WHITE);
        initialLabel.setAlignment(Align.center);

        avatar.add(initialLabel)
                .expand()
                .fill();

        Table information =
                new Table();

        information.left();

        Label nicknameLabel =
                new Label(
                        currentData.nickname(),
                        labelStyle("big_outline")
                );

        nicknameLabel.setColor(TEXT);

        Label usernameLabel =
                new Label(
                        "@" + currentData.username(),
                        labelStyle("medium_outline")
                );

        usernameLabel.setColor(
                SECONDARY_TEXT
        );

        Label emailLabel =
                new Label(
                        currentData.email(),
                        labelStyle("medium_outline")
                );

        emailLabel.setColor(
                SECONDARY_TEXT
        );

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
                                        currentData.gamesPlayed()
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
                                        currentData.passedLevels()
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
                                        currentData.coins()
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
                                        currentData.gems()
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
                                currentData.mostMeowPoint()
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

        titleLabel.setColor(
                SECONDARY_TEXT
        );

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
                        currentData.username(),
                        game.getSkin()
                );

        TextField nicknameField =
                new TextField(
                        currentData.nickname(),
                        game.getSkin()
                );

        TextField emailField =
                new TextField(
                        currentData.email(),
                        game.getSkin()
                );

        addField(
                form,
                "Username",
                usernameField
        );

        addField(
                form,
                "Nickname",
                nicknameField
        );

        addField(
                form,
                "Email",
                emailField
        );

        content.add(form)
                .width(500f)
                .padBottom(15f)
                .row();

        Table buttons = new Table();

        TextButton save = new TextButton("SAVE", game.getSkin());

        TextButton cancel = new TextButton("BACK", game.getSkin());

        save.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {

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
                    public void clicked(InputEvent event, float x, float y) {
                        buildProfile();
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
    }

    private void addField(Table table, String name, TextField field) {

        Label label = new Label(name, labelStyle("medium_outline"));

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

    private void saveProfile(String username, String nickname, String email) {

        username = username.trim();
        nickname = nickname.trim();
        email = email.trim();

        if (!username.equals(
                currentData.username()
        )) {

            Result result = controller.changeUsername(username);

            if (!result.success()) {game.notifyError(result.message());

                return;
            }
        }

        if (!nickname.equals(
                currentData.nickname())) {

            Result result = controller.changeNickname(nickname);

            if (!result.success()) {game.notifyError(result.message());

                return;
            }
        }

        if (!email.equals(currentData.email())) {

            Result result = controller.changeEmail(email);

            if (!result.success()) {
                game.notifyError(result.message());

                return;
            }
        }

        game.notifyInfo("Profile updated successfully.");
        buildProfile();
    }
    private void buildChangePassword() {
        content.clearChildren();
        content.top();
        content.pad(18f);

        Label title = new Label("CHANGE PASSWORD", labelStyle("big_outline"));
        title.setColor(TITLE);
        content.add(title)
                .padBottom(18f)
                .row();

        Table form =
                new Table();

        form.setBackground(game.getSkin().newDrawable("white_pixel", PAPER)
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

        Table buttons =
                new Table();

        TextButton back =
                new TextButton("BACK", game.getSkin());

        TextButton save =
                new TextButton("CHANGE", game.getSkin());

        back.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        buildProfile();
                    }
                }
        );

        save.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {

                        if (!newPassword.getText().equals(confirmPassword.getText())) {

                            game.notifyError("Passwords do not match.");
                            return;
                        }
                        Result result = controller.changePassword(newPassword.getText(), oldPassword.getText());
                        if (!result.success()) {
                            game.notifyError(result.message());
                            return;
                        }
                        game.notifyInfo(result.message());
                        buildProfile();
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
    }

    private TextField passwordField() {
        TextField field = new TextField("", game.getSkin());
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return game.getSkin().get(name, Label.LabelStyle.class);
        } catch (Exception exception) {
            return game.getSkin().get("default", Label.LabelStyle.class);
        }
    }
}

