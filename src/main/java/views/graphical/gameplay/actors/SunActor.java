package views.graphical.gameplay.actors;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import graphics.PvzGame;
import lombok.Getter;
import models.sun.Sun;
import models.sun.SunType;
import views.graphical.animation.PamAnimationActor;

import java.util.function.Consumer;

public class SunActor extends Group {

    private static final String SUN_PAM =
        "768/INITIAL/EFFECTS/SUN/SUN.PAM";

    private static final String SUN_BOMB_PAM =
        "768/FULL/EFFECTS/SUN_BOMB/SUN_BOMB.PAM";

    private static final String NORMAL_IDLE =
        "animation";

    private static final String STOLEN_TRANSITION =
        "transition_red";

    private static final String STOLEN_IDLE =
        "red";

    private static final String RADIOACTIVE_IDLE =
        "animation";

    private static final String RADIOACTIVE_TRANSITION =
        "transition";

    private static final String RADIOACTIVE_EXPLOSION =
        "attack";

    private static final float CLICK_SIZE = 70f;

    private static final float ORDINARY_SKY_SCALE = 0.45f;
    private static final float SPECIAL_SKY_SCALE = 0.60f;
    private static final float RADIOACTIVE_SKY_SCALE = 0.45f;

    private static final float SMALL_PLANT_SUN_SCALE = 0.30f;
    private static final float BIG_PLANT_SUN_SCALE = 0.60f;

    private static final float APPEAR_DURATION = 0.25f;
    private static final float FALLBACK_RED_TRANSITION_DURATION = 0.35f;
    private static final float FALLBACK_RADIOACTIVE_TRANSITION_DURATION = 0.70f;
    private static final float FALLBACK_EXPLOSION_DURATION = 0.70f;

    @Getter
    private final Sun sun;

    private final PamAnimationActor normalAnimation;
    private final PamAnimationActor radioactiveAnimation;
    private final Consumer<Sun> onCollected;

    private final float redTransitionDuration;
    private final float radioactiveTransitionDuration;
    private final float explosionDuration;

    private SunType visualType;
    private boolean stolenVisual;
    private boolean redTransitionPlaying;
    private boolean radioactiveTransitionPlaying;
    private boolean collectionTriggered;

    @Getter
    private boolean terminalVisual;

    public SunActor(
        PvzGame game,
        Sun sun,
        Consumer<Sun> onCollected
    ) {
        this.sun = sun;
        this.onCollected = onCollected;
        this.visualType = sun.getSunType();

        setTransform(true);
        setSize(CLICK_SIZE, CLICK_SIZE);
        setTouchable(Touchable.enabled);

        game.getPamPlayer().loadSync(SUN_PAM);
        game.getPamPlayer().loadSync(SUN_BOMB_PAM);

        normalAnimation = game.createPamActor(
            SUN_PAM,
            NORMAL_IDLE,
            CLICK_SIZE / 2f,
            CLICK_SIZE / 2f,
            true
        );

        radioactiveAnimation = game.createPamActor(
            SUN_BOMB_PAM,
            RADIOACTIVE_IDLE,
            CLICK_SIZE / 2f,
            CLICK_SIZE / 2f,
            true
        );

        normalAnimation.setTouchable(Touchable.disabled);
        radioactiveAnimation.setTouchable(Touchable.disabled);

        float finalScale = targetScale();
        normalAnimation.setScale(finalScale);
        radioactiveAnimation.setScale(finalScale);

        boolean radioactive = sun.getSunType() == SunType.RADIOACTIVE;
        normalAnimation.setVisible(!radioactive);
        radioactiveAnimation.setVisible(radioactive);

        PamAnimationActor initialActor =
            radioactive ? radioactiveAnimation : normalAnimation;

        initialActor.setScale(0.01f);
        initialActor.addAction(
            Actions.scaleTo(
                finalScale,
                finalScale,
                APPEAR_DURATION,
                Interpolation.pow2Out
            )
        );

        addActor(normalAnimation);
        addActor(radioactiveAnimation);

        redTransitionDuration = safeDuration(
            game,
            SUN_PAM,
            STOLEN_TRANSITION,
            FALLBACK_RED_TRANSITION_DURATION
        );

        radioactiveTransitionDuration = safeDuration(
            game,
            SUN_BOMB_PAM,
            RADIOACTIVE_TRANSITION,
            FALLBACK_RADIOACTIVE_TRANSITION_DURATION
        );

        explosionDuration = safeDuration(
            game,
            SUN_BOMB_PAM,
            RADIOACTIVE_EXPLOSION,
            FALLBACK_EXPLOSION_DURATION
        );

        addListener(
            new InputListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    triggerCollection();
                    return true;
                }
            }
        );
    }

    public void syncVisualState() {
        if (terminalVisual) {
            return;
        }

        SunType currentType = sun.getSunType();

        if (visualType == SunType.RADIOACTIVE
            && currentType != SunType.RADIOACTIVE
            && !radioactiveTransitionPlaying) {
            startRadioactiveLandingTransition(currentType);
            return;
        }

        if (radioactiveTransitionPlaying) {
            return;
        }

        visualType = currentType;

        if (currentType == SunType.RADIOACTIVE) {
            showRadioactiveIdle();
            return;
        }

        if (sun.isBeingStolen()) {
            if (!stolenVisual) {
                startStolenTransition();
            }
            return;
        }

        if (stolenVisual || redTransitionPlaying) {
            showNormalIdle();
            return;
        }

        normalAnimation.setVisible(true);
        radioactiveAnimation.setVisible(false);
    }

    private void startStolenTransition() {
        stolenVisual = true;
        redTransitionPlaying = true;

        clearActions();
        normalAnimation.clearActions();
        radioactiveAnimation.clearActions();

        radioactiveAnimation.setVisible(false);
        normalAnimation.setVisible(true);
        normalAnimation.setScale(targetScale());
        normalAnimation.play(STOLEN_TRANSITION, false);
        normalAnimation.restart();

        addAction(
            Actions.sequence(
                Actions.delay(redTransitionDuration),
                Actions.run(
                    () -> {
                        redTransitionPlaying = false;

                        if (terminalVisual || !sun.isBeingStolen()) {
                            return;
                        }

                        normalAnimation.play(STOLEN_IDLE, true);
                        normalAnimation.restart();
                    }
                )
            )
        );
    }

    private void showNormalIdle() {
        stolenVisual = false;
        redTransitionPlaying = false;
        radioactiveTransitionPlaying = false;

        clearActions();
        normalAnimation.clearActions();
        radioactiveAnimation.clearActions();

        radioactiveAnimation.setVisible(false);
        normalAnimation.setVisible(true);
        normalAnimation.setScale(targetScale());
        normalAnimation.play(NORMAL_IDLE, true);
        normalAnimation.restart();
    }

    private void showRadioactiveIdle() {
        stolenVisual = false;
        redTransitionPlaying = false;

        normalAnimation.setVisible(false);
        radioactiveAnimation.setVisible(true);

        if (!RADIOACTIVE_IDLE.equals(radioactiveAnimation.getClip())) {
            radioactiveAnimation.play(RADIOACTIVE_IDLE, true);
            radioactiveAnimation.restart();
        }
    }

    private void startRadioactiveLandingTransition(
        SunType currentType
    ) {
        visualType = currentType;
        radioactiveTransitionPlaying = true;
        stolenVisual = false;
        redTransitionPlaying = false;

        clearActions();
        normalAnimation.clearActions();
        radioactiveAnimation.clearActions();

        normalAnimation.setVisible(false);
        radioactiveAnimation.setVisible(true);
        radioactiveAnimation.setScale(targetScale());
        radioactiveAnimation.play(RADIOACTIVE_TRANSITION, false);
        radioactiveAnimation.restart();

        addAction(
            Actions.sequence(
                Actions.delay(radioactiveTransitionDuration),
                Actions.run(
                    () -> {
                        if (terminalVisual) {
                            return;
                        }

                        radioactiveTransitionPlaying = false;
                        radioactiveAnimation.setVisible(false);
                        normalAnimation.setVisible(true);
                        normalAnimation.setScale(targetScale());

                        if (sun.isBeingStolen()) {
                            startStolenTransition();
                            return;
                        }

                        normalAnimation.play(NORMAL_IDLE, true);
                        normalAnimation.restart();
                    }
                )
            )
        );
    }

    public void playRadioactiveExplosion() {
        if (terminalVisual) {
            return;
        }

        terminalVisual = true;
        setTouchable(Touchable.disabled);

        clearActions();
        normalAnimation.clearActions();
        radioactiveAnimation.clearActions();

        normalAnimation.setVisible(false);
        radioactiveAnimation.setVisible(true);
        radioactiveAnimation.setScale(targetScale());
        radioactiveAnimation.play(RADIOACTIVE_EXPLOSION, false);
        radioactiveAnimation.restart();

        addAction(
            Actions.sequence(
                Actions.delay(explosionDuration),
                Actions.removeActor()
            )
        );
    }

    private void triggerCollection() {
        if (collectionTriggered
            || terminalVisual
            || !sun.isActive()) {
            return;
        }

        collectionTriggered = true;
        setTouchable(Touchable.disabled);

        boolean explodingRadioactive =
            sun.getSunType() == SunType.RADIOACTIVE
                && !sun.isGrounded();

        if (explodingRadioactive) {
            playRadioactiveExplosion();
        }

        if (onCollected != null) {
            onCollected.accept(sun);
        }
    }

    private float targetScale() {
        if (sun.getSourcePlant() != null) {
            if (sun.getAmount() <= 50) {
                return SMALL_PLANT_SUN_SCALE;
            }
            return BIG_PLANT_SUN_SCALE;
        }

        return switch (sun.getSunType()) {
            case ORDINARY -> ORDINARY_SKY_SCALE;
            case SPECIAL -> SPECIAL_SKY_SCALE;
            case RADIOACTIVE -> RADIOACTIVE_SKY_SCALE;
        };
    }

    public void setCenterPosition(
        float x,
        float y
    ) {
        setPosition(
            x - getWidth() / 2f,
            y - getHeight() / 2f
        );
    }

    private static float safeDuration(
        PvzGame game,
        String pamPath,
        String clip,
        float fallback
    ) {
        try {
            return Math.max(
                0.05f,
                game.getPamPlayer().clipDurationSeconds(
                    pamPath,
                    clip
                )
            );
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
