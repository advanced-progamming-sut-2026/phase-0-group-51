package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import graphics.PvzGame;

public class StartGameMenuPopup extends BorderedPanel {
    public StartGameMenuPopup(PvzGame game, String... objects) {
        super(game, com.badlogic.gdx.graphics.Color.valueOf("A0522D"));

        TextureRegion circle = game.getTextureBank().region("IMAGE_UI_NIMBLE_RADIOEMPTY");
        TextureRegion greenTabRegion = game.getTextureBank().region("IMAGE_UI_GENERIC_GREENTAB_DOWN");

        Table innerCard = new Table();
        innerCard.setBackground(game.getSkin().getDrawable("image_ui_powerups_powerup_cost_10"));

        Table titleTable = new Table();
        titleTable.setBackground(new TextureRegionDrawable(greenTabRegion));
        titleTable.pad(10, 40, 10, 40);

        Label titleLabel = new Label("Level Objectives", game.getSkin().get("big", Label.LabelStyle.class));
        titleLabel.setAlignment(Align.center);
        titleTable.add(titleLabel);
        innerCard.add(titleTable).expandX().fillX().top()
                .padTop(-10f).padLeft(-2f).padRight(-2f).row();

        Table objectivesTable = new Table();

        for (String objectiveText : objects) {
            Image bulletImage = new Image(circle);
            Label objLabel = new Label(objectiveText, game.getSkin().get("medium", Label.LabelStyle.class));
            objLabel.setColor(Color.BROWN);
            objLabel.setWrap(true);
            objectivesTable.add(bulletImage).size(20, 20).padRight(15).align(Align.topLeft).padTop(5f);
            objectivesTable.add(objLabel).left().expandX().fillX().padBottom(10).row();
        }
        innerCard.add(objectivesTable).left().expandX().fillX().padTop(30).padLeft(50).padRight(50).padBottom(40).row();

        BitmapFont newFont = game.getSkin().get("FBUSV8C5EI_2", BitmapFont.class);
        TextButton.TextButtonStyle customPurpleStyle = new TextButton.TextButtonStyle(game.getSkin().get("purple", TextButton.TextButtonStyle.class));
        customPurpleStyle.font = newFont;
        TextButton continueButton = new TextButton("CONTINUE", customPurpleStyle);

        this.getContent().add(innerCard).width(600f).pad(30,20,10,20).expand().fill().row();
        this.getContent().add(continueButton).padBottom(-55).align(Align.center);

        this.pack();

    }
}
