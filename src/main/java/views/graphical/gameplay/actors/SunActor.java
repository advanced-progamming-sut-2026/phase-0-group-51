package views.graphical.gameplay.actors;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
            "768/FULL/EFFECTS/SUN_BOMB/SUN_BOMB.PAM";

    private static final String NORMAL_IDLE =
            "normalSunIdle";

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

    private static final float TRANSITION_DURATION = 0.70f;
    private static final float EXPLOSION_DURATION = 0.70f;

    @Getter
    private final Sun sun;

    private final PamAnimationActor animation;
    private final Consumer<Sun> onCollected;

    private SunType visualType;

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

        setSize(
                CLICK_SIZE,
                CLICK_SIZE
        );

        setTouchable(
                Touchable.enabled
        );

        game.getPamPlayer().loadSync(
                SUN_PAM
        );

        animation = game.createPamActor(
                SUN_PAM,
                initialClip(),
                CLICK_SIZE / 2f,
                CLICK_SIZE / 2f,
                true
        );

        float finalScale =
                targetScale();

        animation.setScale(
                0.01f
        );

        animation.addAction(
                Actions.scaleTo(
                        finalScale,
                        finalScale,
                        APPEAR_DURATION,
                        Interpolation.pow2Out
                )
        );

        animation.setTouchable(
                Touchable.disabled
        );

        addActor(animation);

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

    private String initialClip() {
        if (sun.getSunType()
                == SunType.RADIOACTIVE) {

            return RADIOACTIVE_IDLE;
        }

        return NORMAL_IDLE;
    }

    public void syncVisualState() {

        if (terminalVisual) {
            return;
        }

        SunType currentType =
                sun.getSunType();

        if (visualType == SunType.RADIOACTIVE
                && currentType == SunType.ORDINARY) {

            visualType =
                    SunType.ORDINARY;

            animation.play(
                    RADIOACTIVE_TRANSITION,
                    false
            );

            animation.restart();

            clearActions();

            addAction(
                    Actions.sequence(

                            Actions.delay(
                                    TRANSITION_DURATION
                            ),

                            Actions.run(
                                    () -> {
                                        if (terminalVisual) {
                                            return;
                                        }

                                        animation.play(
                                                NORMAL_IDLE,
                                                true
                                        );

                                        animation.restart();
                                    }
                            )
                    )
            );

            return;
        }

        visualType =
                currentType;
    }

    public void playRadioactiveExplosion() {

        if (terminalVisual) {
            return;
        }

        terminalVisual = true;

        setTouchable(
                Touchable.disabled
        );

        clearActions();

        animation.clearActions();

        animation.setScale(
                targetScale()
        );

        animation.play(
                RADIOACTIVE_EXPLOSION,
                false
        );

        animation.restart();

        addAction(
                Actions.sequence(

                        Actions.delay(
                                EXPLOSION_DURATION
                        ),

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

        setTouchable(
                Touchable.disabled
        );

        boolean explodingRadioactive = sun.getSunType() == SunType.RADIOACTIVE && !sun.isGrounded();

        if (explodingRadioactive) {
            playRadioactiveExplosion();
        }

        if (onCollected != null) {
            onCollected.accept(
                    sun
            );
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
}