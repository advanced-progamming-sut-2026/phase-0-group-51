package views.graphical.gameplay.effects;

import com.badlogic.gdx.scenes.scene2d.Stage;
import models.games.ChapterTheme;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

import java.util.Objects;

public final class FrostbiteSnowstormAnimationSystem {

    public static final float DEFAULT_SCALE = 0.6f;

    private static final float SNOWSTORM_OFFSET_X = 0f;
    private static final float SNOWSTORM_OFFSET_Y = 10f;

    private static final float LOOP_HOLD_SECONDS = 1.25f;

    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final BoardTransform boardTransform;
    private final ChapterTheme theme;
    private final float scale;

    private FrostbiteSnowstormEffect activeEffect;
    private float loopHoldRemaining;

    public FrostbiteSnowstormAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme
    ) {
        this(
            pamPlayer,
            worldStage,
            boardTransform,
            theme,
            DEFAULT_SCALE
        );
    }

    public FrostbiteSnowstormAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme,
        float scale
    ) {
        this.pamPlayer =
            Objects.requireNonNull(
                pamPlayer,
                "pamPlayer"
            );

        this.worldStage =
            Objects.requireNonNull(
                worldStage,
                "worldStage"
            );

        this.boardTransform =
            Objects.requireNonNull(
                boardTransform,
                "boardTransform"
            );

        this.theme =
            Objects.requireNonNull(
                theme,
                "theme"
            );

        this.scale =
            Math.max(
                0.01f,
                scale
            );
    }

    public void play() {
        if (theme != ChapterTheme.FROSTBITE_CAVES) {
            return;
        }

        if (activeEffect != null) {
            activeEffect.removeImmediately();
        }

        activeEffect =
            new FrostbiteSnowstormEffect(
                pamPlayer,
                worldStage,
                scale
            );

        positionEffect();

        loopHoldRemaining =
            LOOP_HOLD_SECONDS;
    }

    public void update(float delta) {
        if (theme != ChapterTheme.FROSTBITE_CAVES) {
            clear();
            return;
        }

        if (activeEffect == null) {
            return;
        }

        positionEffect();

        activeEffect.update(
            delta
        );

        if (activeEffect.isLooping()) {
            loopHoldRemaining -=
                Math.max(
                    0f,
                    delta
                );

            if (loopHoldRemaining <= 0f) {
                activeEffect.stop();
            }
        }

        if (activeEffect.isFinished()) {
            activeEffect = null;
            loopHoldRemaining = 0f;
        }
    }

    public boolean isActive() {
        return activeEffect != null
            && !activeEffect.isFinished();
    }

    public void clear() {
        if (activeEffect != null) {
            activeEffect.removeImmediately();
            activeEffect = null;
        }

        loopHoldRemaining = 0f;
    }

    private void positionEffect() {
        if (activeEffect == null) {
            return;
        }

        BoardArea area =
            boardTransform.getArea();

        float x =
            area.x()
                + area.width()
                * 0.5f
                + SNOWSTORM_OFFSET_X;

        float y =
            area.y()
                + area.height()
                * 0.5f
                + SNOWSTORM_OFFSET_Y;

        activeEffect.setPosition(
            x,
            y
        );
    }
}
