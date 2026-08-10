package views.graphical.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import lombok.Getter;

import java.util.List;

public final class ZombieCard extends Button {
    private static final String READY_BACKGROUND =
            "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_READY";

    private static final String SELECTED_BACKGROUND =
            "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_SELECTED";

    public record ViewData(
            String alias,
            String cardAssetId,
            String idlePamPath,
            String idleClip,
            String walkClip,
            List<String> idleVisibleParts,
            boolean unlocked
    ) {
        public ViewData {
            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException(
                        "alias cannot be null or blank"
                );
            }

            if (cardAssetId == null
                    || cardAssetId.isBlank()) {
                throw new IllegalArgumentException(
                        "cardAssetId cannot be null or blank"
                );
            }

            if (idlePamPath == null
                    || idlePamPath.isBlank()) {
                throw new IllegalArgumentException(
                        "idlePamPath cannot be null or blank"
                );
            }

            if (idleClip == null
                    || idleClip.isBlank()) {
                idleClip = "idle";
            }
            if (walkClip == null
                    || walkClip.isBlank()) {
                walkClip = "walk";
            }

            if (idleVisibleParts == null) {
                idleVisibleParts = List.of();
            } else {
                idleVisibleParts =
                        List.copyOf(idleVisibleParts);
            }
        }
    }

    private final PvzGame game;

    @Getter
    private final ViewData data;

    private final Image stateBackground;
    private final Image zombieImage;

    private final float cardWidth;
    private final float cardHeight;

    private boolean hovered;

    public ZombieCard(
            PvzGame game,
            ViewData data
    ) {
        super(new ButtonStyle());

        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        if (data == null) {
            throw new IllegalArgumentException(
                    "data cannot be null"
            );
        }

        this.game = game;
        this.data = data;

        setProgrammaticChangeEvents(true);
        pad(0f);

        TextureRegion backgroundRegion =
                requireRegion(READY_BACKGROUND);

        this.cardWidth =
                backgroundRegion.getRegionWidth();

        this.cardHeight =
                backgroundRegion.getRegionHeight();

        Stack cardStack = new Stack();
        cardStack.setTouchable(Touchable.disabled);

        stateBackground = createImage(
                READY_BACKGROUND,
                Scaling.none
        );

        cardStack.add(stateBackground);

        if (data.unlocked()) {

            zombieImage = createImage(
                    data.cardAssetId(),
                    Scaling.none
            );

            cardStack.add(zombieImage);

        } else {

            zombieImage = null;

            setChecked(false);
            setDisabled(true);
            setTouchable(
                    Touchable.disabled
            );
        }

        add(cardStack).size(cardWidth, cardHeight);

        refreshVisualState();

        addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                refreshVisualState();
            }
        });

        addListener(new InputListener() {
            @Override
            public void enter(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor fromActor
            ) {
                hovered = true;
                refreshVisualState();
            }

            @Override
            public void exit(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor toActor
            ) {
                hovered = false;
                refreshVisualState();
            }
        });
    }

    private void refreshVisualState() {
        String backgroundAsset;

        if (!data.unlocked()) {

            backgroundAsset =
                    READY_BACKGROUND;

        } else if (
                isChecked()
                        || hovered
        ) {

            backgroundAsset =
                    SELECTED_BACKGROUND;

        } else {

            backgroundAsset =
                    READY_BACKGROUND;
        }

        stateBackground.setDrawable(
                new TextureRegionDrawable(
                        requireRegion(
                                backgroundAsset
                        )
                )
        );
    }

    private Image createImage(
            String assetId,
            Scaling scaling
    ) {
        Image image = new Image(
                new TextureRegionDrawable(
                        requireRegion(assetId)
                )
        );

        image.setScaling(scaling);
        image.setTouchable(Touchable.disabled);

        return image;
    }

    private TextureRegion requireRegion(
            String assetId
    ) {
        TextureRegion region =
                game.getTextureBank().region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        return region;
    }

    @Override
    public float getPrefWidth() {
        return cardWidth;
    }

    @Override
    public float getPrefHeight() {
        return cardHeight;
    }
}