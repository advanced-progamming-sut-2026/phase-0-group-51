package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import models.App;
import models.User;
import network.protocol.profile.ProfileResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import views.graphical.gameplay.manager.AudioManager;

public class SettingsPopup extends BorderedPanel {
    private boolean difficultyRequestInFlight;
    private int currentDifficulty;
    private final PvzGame game;

    private static final String CHILI_ON  = "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_ICON_SMALL";
    private static final String CHILI_OFF = "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_HOLLOW_ICON_SMALL";
    private static final int CHILI_COUNT = 5;

    private final Table chilis = new Table();
    private Drawable shadowOverlay;

    public SettingsPopup(PvzGame game) {
        super(game, com.badlogic.gdx.graphics.Color.valueOf("A0522D"));
        this.game = game;
        User user = App.getInstance().getLoggedInUser();
        this.currentDifficulty = user == null ? 3 : user.getDifficultyLevel();
        shadowOverlay = game.getSkin().newDrawable("white_pixel", new Color(0, 0, 0, 0.75f));

        TextureRegion topperRegion = game.getTextureBank().region("IMAGE_UI_PAUSEMENU_WINDOWTOPPER");
        TextureRegion sunflowerRegion = game.getTextureBank().region("IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER");
        TextureRegion sliderKnob = game.getTextureBank().region("IMAGE_UI_PAUSEMENU_SLIDER_BOLT");

        Stack topDecoration = new Stack();

        Image topperImage = new Image(topperRegion);
        topperImage.setScaling(Scaling.none);

        Image sunflowerImage = new Image(sunflowerRegion);
        sunflowerImage.setScaling(Scaling.none);
        Container<Image> sunflowerContainer = new Container<>(sunflowerImage);
        sunflowerContainer.align(Align.top | Align.center);
        sunflowerContainer.padTop(-25f);

        Table titleTable = new Table();
        titleTable.pad(10, 40, 10, 40);

        Container<Table> titleContainer = new Container<>(titleTable);
        titleContainer.align(Align.top | Align.center);
        titleContainer.padTop(45f);

        topDecoration.add(topperImage);
        topDecoration.add(titleContainer);
        topDecoration.add(sunflowerContainer);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle(
            game.getSkin().get("default-horizontal", Slider.SliderStyle.class));
        sliderStyle.knob = new TextureRegionDrawable(sliderKnob);

        Table optionsTable = new Table();

        rebuildChilis();
        Table difficultyCell = cell(label("Difficulty"), chilis, 160);

        Slider musicSlider = new Slider(0, 100, 1, false, sliderStyle);
        musicSlider.setValue(GameSettings.music * 100f);
        musicSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                GameSettings.music = ((Slider) a).getValue() / 100f;
                AudioManager.getInstance().updateMusicVolume();
            }
        });
        Table musicCell = cell(label("Music"), musicSlider, 160);

        Slider sfxSlider = new Slider(0, 100, 1, false, sliderStyle);
        sfxSlider.setValue(GameSettings.soundFx * 100f);
        sfxSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                GameSettings.soundFx = ((Slider) a).getValue() / 100f;
            }
        });
        Table sfxCell = cell(label("Sound FX"), sfxSlider, 160);

        final Label speedValue = new Label("x" + GameSettings.gameSpeed,
            game.getSkin().get("medium_outline", Label.LabelStyle.class));
        Slider speedSlider = new Slider(1, 3, 1, false, sliderStyle);
        speedSlider.setValue(GameSettings.gameSpeed);
        speedSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                GameSettings.gameSpeed = (int) ((Slider) a).getValue();
                speedValue.setText("x" + GameSettings.gameSpeed);
            }
        });
        Table speedRow = new Table();
        speedRow.add(speedSlider).width(120).padRight(10);
        speedRow.add(speedValue).width(34);
        Table speedCell = cell(label("Game Speed"), speedRow, 160);

        Table gridCell = cell(label("Show Grid"),
            toggle(GameSettings.showGrid, v -> GameSettings.showGrid = v), 160);
        Table debugCell = cell(label("Debug Mode"),
            toggle(GameSettings.debugMode, v -> GameSettings.debugMode = v), 160);

        optionsTable.defaults().pad(8).expandX();
        optionsTable.add(difficultyCell);
        optionsTable.add(musicCell).row();
        optionsTable.add(sfxCell);
        optionsTable.add(speedCell).row();
        optionsTable.add(gridCell);
        optionsTable.add(debugCell).row();

        TextButton exitButton = new TextButton("EXIT",
            game.getSkin().get("purple", TextButton.TextButtonStyle.class));
        exitButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) { remove(); }
        });

        this.getContent().add(topDecoration).align(Align.center).padTop(-50).row();
        this.getContent().add(optionsTable).width(680f).padTop(15).padBottom(20).row();
        this.getContent().add(exitButton).padBottom(-70).align(Align.center);

        this.pack();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Stage s = getStage();
        if (s != null && shadowOverlay != null) {
            shadowOverlay.draw(batch, 0, 0, s.getWidth(), s.getHeight());
        }
        super.draw(batch, parentAlpha);
    }

    private Table cell(Label lbl, Actor control, float controlWidth) {
        Table t = new Table();
        t.add(lbl).padRight(15).align(Align.right).expandX();
        t.add(control).width(controlWidth).align(Align.left);
        return t;
    }

    private Label label(String text) {
        Label l = new Label(text, game.getSkin().get("medium_outline", Label.LabelStyle.class));
        l.setColor(Color.WHITE);
        return l;
    }

    private interface OnBool { void set(boolean v); }

    private TextButton toggle(boolean initial, final OnBool cb) {
        final boolean[] on = { initial };
        final TextButton btn = new TextButton(
            initial ? "ON" : "OFF",
            game.getSkin(), initial ? "green" : "brown");
        btn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                on[0] = !on[0];
                btn.setStyle(game.getSkin().get(on[0] ? "green" : "brown",
                    TextButton.TextButtonStyle.class));
                btn.setText(on[0] ? "ON" : "OFF");
                cb.set(on[0]);
            }
        });
        return btn;
    }

    private void rebuildChilis() {
        chilis.clear();
        for (int i = 1; i <= CHILI_COUNT; i++) {
            final int level = i;
            boolean on = i <= currentDifficulty;
            Drawable d = safeRegion(on ? CHILI_ON : CHILI_OFF);
            Actor chili;
            if (d != null) {
                Image img = new Image(d);
                img.setScaling(Scaling.fit);
                chili = img;
            } else {
                Label l = new Label(
                        on ? "\u2588" : "\u2591",
                        game.getSkin().get("medium_outline", Label.LabelStyle.class)
                );

                try {
                    l.setColor(game.getSkin().getColor(on ? "PlantFamilyPeppermint" : "Grey"));
                } catch (Exception ex) {
                    l.setColor(on ? Color.RED : Color.GRAY);
                }
                chili = l;
            }

            chili.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    requestDifficultyChange(level);
                }
            });

            chilis.add(chili).size(26f).padLeft(4f);
        }
    }
    private void requestDifficultyChange(int level) {
        if (difficultyRequestInFlight) {
            return;
        }

        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            game.notifyError("You must be logged in to change difficulty.");
            return;
        }

        difficultyRequestInFlight = true;

        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> sendDifficultyUpdate(level))
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishDifficultyUpdate(
                                                level,
                                                response,
                                                throwable
                                        )
                                )
                );
    }

    private CompletableFuture<ProfileResponse> sendDifficultyUpdate(
            int level
    ) {
        try {
            return game.getNetworkManager()
                    .getProfileClientService()
                    .updateDifficulty(level);
        } catch (IOException | RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private void finishDifficultyUpdate(
            int requestedLevel,
            ProfileResponse response,
            Throwable throwable
    ) {
        difficultyRequestInFlight = false;

        if (throwable != null) {
            game.notifyError(
                    "Could not save difficulty on server: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()) {
            game.notifyError(
                    response == null
                            ? "Difficulty update failed."
                            : response.getMessage()
            );
            return;
        }

        int savedLevel = requestedLevel;
        if (response.getProfile() != null) {
            savedLevel = response.getProfile().getDifficultyLevel();
        }

        currentDifficulty = savedLevel;

        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            user.setDifficultyLevel(savedLevel);
        }

        rebuildChilis();
        game.notifyInfo(response.getMessage());
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private Drawable safeRegion(String id) {
        try {
            TextureRegion r = game.getTextureBank().region(id);
            return (r == null) ? null : new TextureRegionDrawable(r);
        } catch (Exception e) {
            return null;
        }
    }
}
