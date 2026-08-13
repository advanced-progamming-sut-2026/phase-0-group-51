package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;

public class StartGameMenuPopup extends BorderedPanel {
    public StartGameMenuPopup(
            PvzGame game,
            Runnable onContinue,
            String chapterName,
            int levelNumber,
            String description,
            String... objectives
    ) {
        super(game, Color.valueOf("8F4909"));

        TextureRegion circle =
                game.getTextureBank().region(
                        "IMAGE_UI_NIMBLE_RADIOEMPTY"
                );

        TextureRegion greenTabRegion =
                game.getTextureBank().region(
                        "IMAGE_UI_GENERIC_GREENTAB_DOWN"
                );

        TextureRegion innerBackgroundRegion =
                game.getTextureBank().region(
                        "IMAGE_UI_CARDS_CARD_TABLE_FRAME"
                );

        if (circle == null) {
            throw new IllegalStateException(
                    "Missing objective bullet asset: "
                            + "IMAGE_UI_NIMBLE_RADIOEMPTY"
            );
        }

        if (greenTabRegion == null) {
            throw new IllegalStateException(
                    "Missing title background asset: "
                            + "IMAGE_UI_GENERIC_GREENTAB_DOWN"
            );
        }

        if (innerBackgroundRegion == null) {
            throw new IllegalStateException(
                    "Missing objective background asset: "
                            + "IMAGE_UI_VASEBREAKER_ENDLESS_BG"
            );
        }

        Table innerCard = new Table();
        innerCard.setBackground(
                new TextureRegionDrawable(
                        innerBackgroundRegion
                )
        );
        innerCard.pad(
                18f,
                28f,
                18f,
                28f
        );

        Table titleTable = new Table();
        titleTable.setBackground(
                new TextureRegionDrawable(
                        greenTabRegion
                )
        );
        titleTable.pad(
                10f,
                26f,
                10f,
                26f
        );

        Label titleLabel =
                new Label(
                        "Level Objectives",
                        game.getSkin().get(
                                "big",
                                Label.LabelStyle.class
                        )
                );

        titleLabel.setAlignment(Align.center);

        titleTable.add(titleLabel)
                .expandX()
                .fillX();

        innerCard.add(titleTable)
                .width(410f)
                .center()
                .padTop(4f)
                .padBottom(12f)
                .row();

        Label levelLabel =
                new Label(
                        chapterName
                                + " - Level "
                                + levelNumber,
                        game.getSkin().get(
                                "medium",
                                Label.LabelStyle.class
                        )
                );

        levelLabel.setColor(Color.BROWN);
        levelLabel.setAlignment(Align.center);

        innerCard.add(levelLabel)
                .expandX()
                .fillX()
                .padBottom(16f)
                .row();

        Table objectivesTable = new Table();
        objectivesTable.left();

        if (description != null
                && !description.isBlank()) {
            addObjectiveRow(
                    objectivesTable,
                    circle,
                    description,
                    game
            );
        }

        if (objectives != null) {
            for (String objectiveText : objectives) {
                if (objectiveText == null
                        || objectiveText.isBlank()) {
                    continue;
                }

                addObjectiveRow(
                        objectivesTable,
                        circle,
                        objectiveText,
                        game
                );
            }
        }

        innerCard.add(objectivesTable)
                .width(430f)
                .left()
                .padTop(2f)
                .padBottom(12f)
                .row();

        BitmapFont newFont =
                game.getSkin().get(
                        "FBUSV8C5EI_2",
                        BitmapFont.class
                );

        TextButton.TextButtonStyle customPurpleStyle =
                new TextButton.TextButtonStyle(
                        game.getSkin().get(
                                "purple",
                                TextButton.TextButtonStyle.class
                        )
                );

        customPurpleStyle.font = newFont;

        TextButton continueButton =
                new TextButton(
                        "CONTINUE",
                        customPurpleStyle
                );

        continueButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        if (onContinue != null) {
                            onContinue.run();
                        }
                    }
                }
        );

        this.getContent()
                .add(innerCard)
                .width(500f)
                .pad(
                        16f,
                        18f,
                        4f,
                        18f
                )
                .row();

        this.getContent()
                .add(continueButton)
                .padTop(4f)
                .padBottom(-26f)
                .align(Align.center);

        this.pack();
    }

    private void addObjectiveRow(
            Table objectivesTable,
            TextureRegion circle,
            String text,
            PvzGame game
    ) {
        Image bulletImage =
                new Image(circle);

        bulletImage.setScaling(
                Scaling.fit
        );

        Label objectiveLabel =
                new Label(
                        text,
                        game.getSkin().get(
                                "medium",
                                Label.LabelStyle.class
                        )
                );

        objectiveLabel.setColor(
                Color.BROWN
        );
        objectiveLabel.setAlignment(Align.left);
        objectiveLabel.setWrap(true);

        objectivesTable.add(bulletImage)
                .size(14f, 14f)
                .top()
                .padTop(4f)
                .padRight(12f);

        objectivesTable.add(objectiveLabel)
                .left()
                .expandX()
                .fillX()
                .width(400f)
                .padBottom(12f)
                .row();
    }
}
