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

public class StartGameMenuPopup extends Table {
    public StartGameMenuPopup(PvzGame game, String... objects) {

        TextureRegion circle = game.getTextureBank().region("IMAGE_UI_NIMBLE_RADIOEMPTY");
        TextureRegion greenTabRegion = game.getTextureBank().region("IMAGE_UI_GENERIC_GREENTAB_DOWN");

        this.setBackground(game.getSkin().getDrawable("image_ui_if_bundle_reward1_bg_10"));
        this.pad(30, 60, 20, 60);

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
            objectivesTable.add(bulletImage).size(20, 20).padRight(15).align(Align.center);
            objectivesTable.add(objLabel).left().expandX().row();
        }
        innerCard.add(objectivesTable).left().expandX().fillX().padTop(30).padLeft(50).padRight(50).padBottom(40).row();

        BitmapFont newFont = game.getSkin().get("FBUSV8C5EI_2", BitmapFont.class);
        TextButton.TextButtonStyle customPurpleStyle = new TextButton.TextButtonStyle(game.getSkin().get("purple", TextButton.TextButtonStyle.class));
        customPurpleStyle.font = newFont;
        TextButton continueButton = new TextButton("CONTINUE", customPurpleStyle);
        this.add(innerCard).expand().fill().row();
        this.add(continueButton).padBottom(-55).align(Align.center);

        this.pack();

    }
}
