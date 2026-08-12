package views.graphical.animation;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import pvz.libpvz.pam.PamPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PamAnimationActor extends Actor {

    private final PamPlayer pamPlayer;
    private final String pamPath;

    private String clip;
    private boolean loop;

    private float stateTime = 0f;
    private float playbackSpeed = 1f;
    private boolean animationPaused = false;

    private final Map<String, Boolean> visibilityMap =
            new HashMap<>();

    public PamAnimationActor(
            PamPlayer pamPlayer,
            String pamPath,
            String clip,
            boolean loop
    ) {
        this.pamPlayer = Objects.requireNonNull(pamPlayer);
        this.pamPath = Objects.requireNonNull(pamPath);
        this.clip = Objects.requireNonNull(clip);
        this.loop = loop;

        setTouchable(Touchable.disabled);
    }

    public void play(String nextClip, boolean loop) {
        if (nextClip == null || nextClip.isBlank()) {
            return;
        }

        if (!nextClip.equals(this.clip)) {
            this.clip = nextClip;
            this.stateTime = 0f;
        }

        this.loop = loop;
    }

    public void restart() {
        stateTime = 0f;
    }

    public void pauseAnimation() {
        animationPaused = true;
    }

    public void resumeAnimation() {
        animationPaused = false;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = Math.max(0f, playbackSpeed);
    }

    public void setVisibleParts(Collection<String> parts) {
        visibilityMap.clear();

        if (parts == null) {
            return;
        }

        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                visibilityMap.put(part, true);
            }
        }
    }

    public Map<String, Boolean> getVisibilityMap() {
        return visibilityMap;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (!animationPaused) {
            stateTime += delta * playbackSpeed;
        }
    }

    @Override
    public void draw(
            Batch batch,
            float parentAlpha
    ) {
        pamPlayer.draw(
                batch,
                pamPath,
                clip,
                stateTime,
                getX(),
                getY(),
                loop,
                visibilityMap
        );
    }
}
