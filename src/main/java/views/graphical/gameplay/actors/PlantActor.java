package views.graphical.gameplay.actors;

import Data.loader.PlantAnimationData;
import Data.loader.PlantData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import graphics.PvzGame;
import lombok.Getter;
import views.graphical.animation.PamAnimationActor;

public class PlantActor extends Group {

    private static final float PREVIEW_ALPHA = 0.58f;

    private final PvzGame game;
    private String baseAnimationKey;

    private boolean temporaryAnimation;
    private boolean terminalAnimation;

    private float animationTimeRemaining;
    @Getter
    private PlantData plantData;
    private PamAnimationActor animation;

    private boolean previewMode;

    private final Vector2 cursorPosition = new Vector2();

    public PlantActor(PvzGame game) {
        this.game = game;
        setTransform(true);

        setTouchable(Touchable.disabled);
        setVisible(false);
    }

    public void setPlant(PlantData plantData) {
        clearPlant();

        if (plantData == null) {
            return;
        }

        this.plantData = plantData;

        if (plantData.idlePamPath() == null
                || plantData.idlePamPath().isBlank()
                || plantData.idleClip() == null
                || plantData.idleClip().isBlank()) {
            return;
        }

        game.getPamPlayer().loadSync(
                plantData.idlePamPath()
        );

        animation = game.createPamActor(
                plantData.idlePamPath(),
                plantData.idleClip(),
                0f,
                0f,
                true
        );

        animation.setTouchable(Touchable.disabled);

        addActor(animation);

        applyVisualMode();

        setVisible(true);
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;

        applyVisualMode();
    }

    private void applyVisualMode() {
        if (animation == null) {
            return;
        }

        if (previewMode) {
            animation.setColor(
                    1f,
                    1f,
                    1f,
                    PREVIEW_ALPHA
            );
            setScale(0.65f);
        } else {
            animation.setColor(
                    1f,
                    1f,
                    1f,
                    1f
            );
            setScale(0.65f);
        }
    }

    public void setPlantScale(float scale) {
        if (scale <= 0f) {
            throw new IllegalArgumentException(
                    "Plant scale must be positive."
            );
        }

        setScale(scale);
    }

    public void setBaseAnimation(String key) {
        if (key != null && key.equals(baseAnimationKey)) {
            return;
        }
        baseAnimationKey = key;
        if (!temporaryAnimation && !terminalAnimation) {
            if (key != null && plantData.hasAnimation(key)) {
                playAnimation(key);
            } else {
                playFallbackIdle();
            }
        }
    }
    private void playFallbackIdle() {
        if (plantData == null || animation == null) {
            return;
        }

        animation.play(
                plantData.idleClip(),
                true
        );
    }

    public void playTemporaryAnimation(String key) {
        if (plantData == null || animation == null || !plantData.hasAnimation(key)) {
            return;
        }
        PlantAnimationData data = plantData.animation(key);

        temporaryAnimation = true;
        terminalAnimation = false;
        animationTimeRemaining = data.duration();

        animation.play(data.clip(), data.loop());

        animation.restart();
    }
    public void playTerminalAnimation(String key) {
        if (plantData == null || animation == null || !plantData.hasAnimation(key)) {
            return;
        }

        PlantAnimationData data = plantData.animation(key);

        temporaryAnimation = false;
        terminalAnimation = true;
        animationTimeRemaining = data.duration();
        animation.play(data.clip(), data.loop());
        animation.restart();
    }

    private void playAnimation(String key) {
        if (plantData == null || animation == null || !plantData.hasAnimation(key)) {
            return;
        }

        PlantAnimationData data = plantData.animation(key);

        animation.play(data.clip(), data.loop());
    }

    public void clearPlant() {
        clearChildren();

        animation = null;
        plantData = null;

        baseAnimationKey = null;
        temporaryAnimation = false;
        terminalAnimation = false;
        animationTimeRemaining = 0f;

        setVisible(false);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (!previewMode) {
            updateAnimationState(delta);
        }

        if (!previewMode
                || !isVisible()
                || getStage() == null) {
            return;
        }

        cursorPosition.set(
                Gdx.input.getX(),
                Gdx.input.getY()
        );

        getStage().screenToStageCoordinates(
                cursorPosition
        );

        setPosition(
                cursorPosition.x,
                cursorPosition.y
        );
    }
    private void updateAnimationState(
            float delta
    ) {
        if (!temporaryAnimation
                && !terminalAnimation) {
            return;
        }

        animationTimeRemaining -= delta;

        if (animationTimeRemaining > 0f) {
            return;
        }

        if (terminalAnimation) {
            remove();
            return;
        }

        temporaryAnimation = false;

        if (baseAnimationKey != null
                && plantData.hasAnimation(
                baseAnimationKey
        )) {
            playAnimation(baseAnimationKey);
        } else {
            playFallbackIdle();
        }
    }
}