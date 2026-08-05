package views.graphical.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;

public class PauseMenuPopup extends BorderedPanel {

    public PauseMenuPopup(PvzGame game) {
        super(game, com.badlogic.gdx.graphics.Color.valueOf("A0522D"));
        TextureRegion topperRegion = game.getTextureBank().region("IMAGE_UI_PAUSEMENU_WINDOWTOPPER");
        TextureRegion sunflowerRegion = game.getTextureBank().region("IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER");
        TextureRegion sliderKnob = game.getTextureBank().region("IMAGE_UI_PAUSEMENU_SLIDER_BOLT");

        Stack topDecoration = new Stack();
        Image topperImage = new Image(topperRegion);
        Image sunflowerImage = new Image(sunflowerRegion);

        topperImage.setScaling(Scaling.none);
        sunflowerImage.setScaling(Scaling.none);

        Container<Image> sunflowerContainer = new Container<>(sunflowerImage);
        sunflowerContainer.align(Align.top | Align.center);

        sunflowerContainer.padTop(-25f);

        topDecoration.add(topperImage);
        topDecoration.add(sunflowerContainer);

        Label titleLabel = new Label("Game Paused", game.getSkin().get("big_outline", Label.LabelStyle.class));
        titleLabel.setAlignment(Align.center);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle(game.getSkin().get("default-horizontal", Slider.SliderStyle.class));
        sliderStyle.knob = new TextureRegionDrawable(sliderKnob);

        Table slidersTable = new Table();
        Label musicLabel = new Label("Music", game.getSkin().get("medium_outline",  Label.LabelStyle.class));
        Slider musicSlider = new Slider(0, 100, 1, false, sliderStyle);
        slidersTable.add(musicLabel).padRight(15).align(Align.right);
        slidersTable.add(musicSlider).width(200).row();

        Label sfxLabel = new Label("Sound FX", game.getSkin().get("medium_outline",  Label.LabelStyle.class));
        Slider sfxSlider = new Slider(0, 100, 1, false, sliderStyle);
        slidersTable.add(sfxLabel).padRight(15).align(Align.right).padTop(10);
        slidersTable.add(sfxSlider).width(200).padTop(10).row();

        TextButton exitButton = new TextButton("SAVE AND EXIT", game.getSkin().get("purple", TextButton.TextButtonStyle.class));
        TextButton restartButton = new TextButton("RESTART", game.getSkin(), "purple");
        TextButton resumeButton = new TextButton("RESUME", game.getSkin(), "brown");

        Table buttonsTable = new Table();
        buttonsTable.add(exitButton).padRight(10);
        buttonsTable.add(restartButton).padRight(10);
        buttonsTable.add(resumeButton);

        this.getContent().add(topDecoration).align(Align.center).padTop(-50).row();
        this.getContent().add(titleLabel).padTop(-400).row();
        this.getContent().add(slidersTable).padBottom(30).row();
        this.getContent().add(buttonsTable).align(Align.bottom).padBottom(-100);

        this.pack();
    }
}
