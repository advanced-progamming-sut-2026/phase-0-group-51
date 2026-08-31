package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import graphics.PvzGame;
import lombok.Getter;
import models.quests.QuestType;
import network.protocol.quests.QuestEntryDto;

public final class QuestCard extends Table {
    private static final String DEFAULT_BACKGROUND =
            "IMAGE_UI_QUEST_TOAST_QUEST_TOAST_DEFAULT";
    private static final String EPIC_BACKGROUND =
            "IMAGE_UI_QUEST_TOAST_QUEST_TOAST_EPIC";
    private static final String NAME_FONT =
            "AVENIRNEXTLTPRO-DEMICN";
    private static final String DESCRIPTION_FONT =
            "FBUSV8C6EI_3";
    private static final String PROGRESS_STYLE =
            "xp_green";

    private final PvzGame game;
    @Getter
    private final QuestEntryDto entry;

    public QuestCard(
            PvzGame game,
            QuestEntryDto entry,
            Runnable onClaim
    ) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        if (entry == null) {
            throw new IllegalArgumentException("entry cannot be null");
        }

        this.game = game;
        this.entry = entry;
        setTouchable(Touchable.childrenOnly);

        String background = entry.getType() == QuestType.EPIC
                ? EPIC_BACKGROUND
                : DEFAULT_BACKGROUND;
        setBackground(bankDrawable(background));

        Label.LabelStyle nameStyle = new Label.LabelStyle(
                game.getSkin().getFont(NAME_FONT),
                Color.valueOf("4B3824")
        );
        Label.LabelStyle descriptionStyle = new Label.LabelStyle(
                game.getSkin().getFont(DESCRIPTION_FONT),
                Color.valueOf("4B3824")
        );

        Label nameLabel = new Label(entry.getName(), nameStyle);
        Label descriptionLabel = new Label(
                entry.getDescription() == null ? "" : entry.getDescription(),
                descriptionStyle
        );
        Label rewardLabel = new Label(
                entry.getRewardText() == null || entry.getRewardText().isBlank()
                        ? ""
                        : "Reward: " + entry.getRewardText(),
                descriptionStyle
        );

        ProgressBar progressBar = new ProgressBar(
                0f,
                Math.max(1, entry.getTargetAmount()),
                1f,
                false,
                game.getSkin(),
                PROGRESS_STYLE
        );
        progressBar.setValue(
                Math.min(entry.getProgress(), entry.getTargetAmount())
        );
        progressBar.setAnimateDuration(0f);
        progressBar.setTouchable(Touchable.disabled);

        TextButton actionButton = new TextButton(
                entry.isClaimed()
                        ? "CLAIMED"
                        : entry.isCompleted()
                        ? "CLAIM"
                        : "PLAY",
                game.getSkin(),
                entry.isCompleted() ? "green" : "purple"
        );

        if (entry.isClaimed()) {
            actionButton.setDisabled(true);
            actionButton.setTouchable(Touchable.disabled);
        } else if (entry.isCompleted() && onClaim != null) {
            actionButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onClaim.run();
                }
            });
        }

        Table textArea = new Table();
        textArea.left();
        textArea.add(nameLabel).left().growX().row();
        textArea.add(descriptionLabel).left().growX().padTop(2f).row();
        textArea.add(rewardLabel).left().growX().padTop(2f).row();
        textArea.add(progressBar).growX().height(12f).padTop(7f);

        add(textArea)
                .growX()
                .minWidth(0f)
                .padLeft(25f)
                .padRight(20f);
        add(actionButton)
                .width(120f)
                .height(48f)
                .padRight(20f);
    }

    private Drawable bankDrawable(String assetId) {
        TextureRegion region = game.getTextureBank().region(assetId);
        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: " + assetId
            );
        }
        return new TextureRegionDrawable(region);
    }
}
