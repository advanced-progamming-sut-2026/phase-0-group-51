package views.graphical.ui;

import Data.database.QuestsRepository;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import graphics.PvzGame;
import lombok.Getter;
import models.quests.QuestType;

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
    private final QuestsRepository.QuestEntry entry;

    public QuestCard(
            PvzGame game,
            QuestsRepository.QuestEntry entry,
            String description
    ) {
        this(
                game,
                entry,
                description,
                "",
                null
        );
    }

    public QuestCard(
            PvzGame game,
            QuestsRepository.QuestEntry entry,
            String description,
            String rewardText,
            Runnable onClaim
    ) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        if (entry == null) {
            throw new IllegalArgumentException(
                    "entry cannot be null"
            );
        }

        this.game = game;
        this.entry = entry;

        setTouchable(Touchable.childrenOnly);

        String background =
                entry.quest().getType() == QuestType.EPIC
                        ? EPIC_BACKGROUND
                        : DEFAULT_BACKGROUND;

        setBackground(bankDrawable(background));

        Label.LabelStyle nameStyle =
                new Label.LabelStyle(
                        game.getSkin().getFont(NAME_FONT),
                        Color.valueOf("4B3824")
                );

        Label.LabelStyle descriptionStyle =
                new Label.LabelStyle(
                        game.getSkin().getFont(
                                DESCRIPTION_FONT
                        ),
                        Color.valueOf("4B3824")
                );

        Label nameLabel = new Label(
                entry.quest().getName(),
                nameStyle
        );

        Label descriptionLabel = new Label(
                description,
                descriptionStyle
        );

        Label rewardLabel = new Label(
                rewardText == null || rewardText.isBlank()
                        ? ""
                        : "Reward: " + rewardText,
                descriptionStyle
        );

        ProgressBar progressBar =
                new ProgressBar(
                        0f,
                        Math.max(
                                1,
                                entry.userQuest()
                                        .getTargetAmount()
                        ),
                        1f,
                        false,
                        game.getSkin(),
                        PROGRESS_STYLE
                );

        progressBar.setValue(
                Math.min(
                        entry.userQuest()
                                .getProgress(),
                        entry.userQuest()
                                .getTargetAmount()
                )
        );

        progressBar.setAnimateDuration(0f);
        progressBar.setTouchable(
                Touchable.disabled
        );

        boolean claimed =
                entry.userQuest().isClaimed();

        boolean completed =
                entry.userQuest().isCompleted();

        TextButton actionButton =
                new TextButton(
                        claimed
                                ? "CLAIMED"
                                : completed
                                ? "CLAIM"
                                : "PLAY",
                        game.getSkin(),
                        completed
                                ? "green"
                                : "purple"
                );

        if (claimed) {
            actionButton.setDisabled(true);
            actionButton.setTouchable(
                    Touchable.disabled
            );
        } else if (completed && onClaim != null) {
            actionButton.addListener(
                    new ChangeListener() {
                        @Override
                        public void changed(
                                ChangeEvent event,
                                Actor actor
                        ) {
                            onClaim.run();
                        }
                    }
            );
        }

        Table textArea = new Table();
        textArea.left();

        textArea.add(nameLabel)
                .left()
                .growX()
                .row();

        textArea.add(descriptionLabel)
                .left()
                .growX()
                .padTop(2f)
                .row();

        textArea.add(rewardLabel)
                .left()
                .growX()
                .padTop(2f)
                .row();

        textArea.add(progressBar)
                .growX()
                .height(12f)
                .padTop(7f);

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
        TextureRegion region =
                game.getTextureBank()
                        .region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        return new TextureRegionDrawable(region);
    }
}