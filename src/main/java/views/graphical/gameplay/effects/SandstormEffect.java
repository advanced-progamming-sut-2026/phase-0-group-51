package views.graphical.gameplay.effects;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;

import java.util.Objects;

public final class SandstormEffect {

    private static final String REAR_PAM =
        "768/INITIAL/EFFECTS/SANDSTORM_REAR/SANDSTORM_REAR.PAM";

    private static final String TOP_PAM =
        "768/INITIAL/EFFECTS/SANDSTORM_TOP/SANDSTORM_TOP.PAM";

    private static final String INTRO = "intro";
    private static final String LOOP = "loop";
    private static final String OUTRO = "outro";

    private static final float DEFAULT_SCALE = 1f;

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

    private Actor targetActor;

    private float x;
    private float y;

    private float targetOffsetX;
    private float targetOffsetY;

    private enum State {
        INTRO,
        LOOP,
        OUTRO,
        FINISHED
    }

    public SandstormEffect(
        PamPlayer pamPlayer,
        Stage worldStage
    ) {
        this(
            pamPlayer,
            worldStage,
            DEFAULT_SCALE
        );
    }

    public SandstormEffect(
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
                    0.2f
                ),
                safeDuration(
                    TOP_PAM,
                    INTRO,
                    0.2f
                )
            );

        outroDuration =
            Math.max(
                safeDuration(
                    REAR_PAM,
                    OUTRO,
                    0.2f
                ),
                safeDuration(
                    TOP_PAM,
                    OUTRO,
                    0.2f
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
    }

    public void update(float delta) {
        if (removed
            || state == State.FINISHED) {
            return;
        }

        updateTargetPosition();

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

        if (targetActor == null) {
            applyPosition(
                x,
                y
            );
        }
    }

    public void bindTo(
        Actor targetActor
    ) {
        bindTo(
            targetActor,
            0f,
            0f
        );
    }

    public void bindTo(
        Actor targetActor,
        float offsetX,
        float offsetY
    ) {
        this.targetActor =
            targetActor;

        this.targetOffsetX =
            offsetX;

        this.targetOffsetY =
            offsetY;

        updateTargetPosition();
        updateDrawOrder();
    }

    public void unbind() {
        if (targetActor != null) {
            x =
                targetActor.getX()
                    + targetOffsetX;

            y =
                targetActor.getY()
                    + targetOffsetY;
        }

        targetActor = null;

        applyPosition(
            x,
            y
        );
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

    public boolean isIntro() {
        return state == State.INTRO;
    }

    public boolean isLooping() {
        return state == State.LOOP;
    }

    public boolean isOutro() {
        return state == State.OUTRO;
    }

    public PamAnimationActor getRearActor() {
        return rearActor;
    }

    public PamAnimationActor getTopActor() {
        return topActor;
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

    private void updateTargetPosition() {
        if (targetActor == null) {
            applyPosition(
                x,
                y
            );
            return;
        }

        if (targetActor.getParent() == null) {
            targetActor = null;
            stop();
            return;
        }

        x =
            targetActor.getX()
                + targetOffsetX;

        y =
            targetActor.getY()
                + targetOffsetY;

        applyPosition(
            x,
            y
        );
    }

    private void applyPosition(
        float x,
        float y
    ) {
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
        if (targetActor == null) {
            return;
        }

        Group targetParent =
            targetActor.getParent();

        if (targetParent == null
            || rearActor.getParent() != targetParent
            || topActor.getParent() != targetParent) {
            return;
        }

        int targetIndex =
            targetActor.getZIndex();

        rearActor.setZIndex(
            Math.max(
                0,
                targetIndex
            )
        );

        int newTargetIndex =
            targetActor.getZIndex();

        int maxIndex =
            targetParent
                .getChildren()
                .size - 1;

        topActor.setZIndex(
            Math.min(
                maxIndex,
                newTargetIndex + 1
            )
        );
    }

    private float safeDuration(
        String pamPath,
        String clip,
        float fallback
    ) {
        try {
            return Math.max(
                0.01f,
                pamPlayer
                    .clipDurationSeconds(
                        pamPath,
                        clip
                    )
            );
        } catch (
            RuntimeException ignored
        ) {
            return fallback;
        }
    }
}
