package views.graphical.gameplay.effects;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;

import java.util.Objects;

public final class FrostbiteSnowstormEffect {

    private static final String REAR_PAM =
        "768/FULL/EFFECTS/SNOWSTORM_REAR/SNOWSTORM_REAR.PAM";

    private static final String TOP_PAM =
        "768/FULL/EFFECTS/SNOWSTORM_TOP/SNOWSTORM_TOP.PAM";

    private static final String INTRO = "intro";
    private static final String LOOP = "loop";
    private static final String OUTRO = "outro";

    private final PamPlayer pamPlayer;
    private final Stage worldStage;

    private final PamAnimationActor rearActor;
    private final PamAnimationActor topActor;

    private State state = State.INTRO;
    private float stateTime;

    private final float introDuration;
    private final float outroDuration;

    private boolean stopRequested;
    private boolean removed;

    private float x;
    private float y;

    private enum State {
        INTRO,
        LOOP,
        OUTRO,
        FINISHED
    }

    public FrostbiteSnowstormEffect(
        PamPlayer pamPlayer,
        Stage worldStage,
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

        pamPlayer.loadSync(REAR_PAM);
        pamPlayer.loadSync(TOP_PAM);

        rearActor =
            new PamAnimationActor(
                pamPlayer,
                REAR_PAM,
                INTRO,
                false
            );

        topActor =
            new PamAnimationActor(
                pamPlayer,
                TOP_PAM,
                INTRO,
                false
            );

        rearActor.setTouchable(
            Touchable.disabled
        );

        topActor.setTouchable(
            Touchable.disabled
        );

        rearActor.setScale(
            scale,
            scale
        );

        topActor.setScale(
            scale,
            scale
        );

        introDuration =
            Math.max(
                safeDuration(
                    REAR_PAM,
                    INTRO,
                    0.20f
                ),
                safeDuration(
                    TOP_PAM,
                    INTRO,
                    0.20f
                )
            );

        outroDuration =
            Math.max(
                safeDuration(
                    REAR_PAM,
                    OUTRO,
                    0.20f
                ),
                safeDuration(
                    TOP_PAM,
                    OUTRO,
                    0.20f
                )
            );

        worldStage.addActor(
            rearActor
        );

        worldStage.addActor(
            topActor
        );

        rearActor.restart();
        topActor.restart();

        stateTime = 0f;

        updateDrawOrder();
    }

    public void update(float delta) {
        if (removed
            || state == State.FINISHED) {
            return;
        }

        applyPosition();

        stateTime +=
            Math.max(
                0f,
                delta
            );

        switch (state) {
            case INTRO -> {
                if (stateTime >= introDuration) {
                    if (stopRequested) {
                        playOutro();
                    } else {
                        playLoop();
                    }
                }
            }

            case LOOP -> {
                if (stopRequested) {
                    playOutro();
                }
            }

            case OUTRO -> {
                if (stateTime >= outroDuration) {
                    finish();
                }
            }

            case FINISHED -> {
            }
        }

        updateDrawOrder();
    }

    public void setPosition(
        float x,
        float y
    ) {
        this.x = x;
        this.y = y;

        applyPosition();
    }

    public void stop() {
        if (removed
            || state == State.OUTRO
            || state == State.FINISHED) {
            return;
        }

        stopRequested = true;

        if (state == State.LOOP) {
            playOutro();
        }
    }

    public void removeImmediately() {
        if (removed) {
            return;
        }

        removed = true;
        state = State.FINISHED;

        rearActor.remove();
        topActor.remove();
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public boolean isLooping() {
        return state == State.LOOP;
    }

    private void playLoop() {
        if (state == State.LOOP
            || state == State.OUTRO
            || state == State.FINISHED) {
            return;
        }

        state = State.LOOP;
        stateTime = 0f;

        rearActor.play(
            LOOP,
            true
        );

        topActor.play(
            LOOP,
            true
        );

        rearActor.restart();
        topActor.restart();
    }

    private void playOutro() {
        if (state == State.OUTRO
            || state == State.FINISHED) {
            return;
        }

        state = State.OUTRO;
        stateTime = 0f;

        rearActor.play(
            OUTRO,
            false
        );

        topActor.play(
            OUTRO,
            false
        );

        rearActor.restart();
        topActor.restart();
    }

    private void finish() {
        if (removed) {
            return;
        }

        state = State.FINISHED;
        removed = true;

        rearActor.remove();
        topActor.remove();
    }

    private void applyPosition() {
        rearActor.setPosition(
            x,
            y
        );

        topActor.setPosition(
            x,
            y
        );
    }

    private void updateDrawOrder() {
        if (rearActor.getParent() == null
            || topActor.getParent() == null) {
            return;
        }

        rearActor.setZIndex(
            0
        );

        topActor.toFront();
    }

    private float safeDuration(
        String pamPath,
        String clip,
        float fallback
    ) {
        try {
            return Math.max(
                0.01f,
                pamPlayer.clipDurationSeconds(
                    pamPath,
                    clip
                )
            );
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
