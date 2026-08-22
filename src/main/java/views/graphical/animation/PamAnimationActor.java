package views.graphical.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
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

    private float additiveFlashRemaining = 0f;
    private float additiveFlashDuration = 0f;
    private float additiveFlashAlpha = 0f;

    private boolean outlineEnabled = false;

    private float outlineThickness = 0f;

    private final Color outlineColor =
        new Color(0f, 1f, 0f, 1f);

    private float outlinePulseBaseAlpha = 0.22f;
    private float outlinePulseAmplitude = 0.16f;
    private float outlinePulseSpeed = 2.8f;

    private static final int GLOW_DIRECTIONS = 12;

    private final Map<String, Boolean> visibilityMap =
        new HashMap<>();

    private String groundingClip;
    private float[] groundCenterXByFrame = new float[0];
    private float groundingDuration = 0f;

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

    public boolean isAnimationPaused() {
        return animationPaused;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = Math.max(0f, playbackSpeed);
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public float getStateTime() {
        return stateTime;
    }

    public void flashAdditive(
        float duration,
        float alpha
    ) {
        additiveFlashDuration = Math.max(0.01f, duration);
        additiveFlashRemaining = additiveFlashDuration;
        additiveFlashAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    public void setOutline(
        boolean enabled,
        Color color,
        float thickness
    ) {
        outlineEnabled = enabled;
        outlineThickness = Math.max(0f, thickness);

        if (color != null) {
            outlineColor.set(color);
        }
    }

    public void setOutlinePulse(
        float baseAlpha,
        float amplitude,
        float speed
    ) {
        outlinePulseBaseAlpha =
            Math.max(
                0f,
                Math.min(
                    1f,
                    baseAlpha
                )
            );

        outlinePulseAmplitude =
            Math.max(
                0f,
                Math.min(
                    1f,
                    amplitude
                )
            );

        outlinePulseSpeed =
            Math.max(
                0f,
                speed
            );
    }

    public boolean isOutlineEnabled() {
        return outlineEnabled;
    }

    public float getOutlineThickness() {
        return outlineThickness;
    }

    public String getClip() {
        return clip;
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

    public void setPartVisible(String part, boolean visible) {
        if (part == null || part.isBlank()) {
            return;
        }
        visibilityMap.put(part, visible);
    }

    public Map<String, Boolean> getVisibilityMap() {
        return visibilityMap;
    }

    public void setGroundingCurve(
        String clip,
        Rectangle[] boundsByFrame,
        float clipDuration
    ) {
        clearGrounding();

        if (clip == null
            || clip.isBlank()
            || boundsByFrame == null
            || boundsByFrame.length < 2
            || clipDuration <= 0f) {
            return;
        }

        float[] centers = new float[boundsByFrame.length];
        int validCount = 0;

        for (int i = 0; i < boundsByFrame.length; i++) {
            Rectangle bounds = boundsByFrame[i];

            if (bounds == null) {
                centers[i] = Float.NaN;
                continue;
            }

            centers[i] = bounds.x + bounds.width * 0.5f;
            validCount++;
        }

        if (validCount < 2) {
            return;
        }

        fillMissingValues(centers);

        this.groundingClip = clip;
        this.groundCenterXByFrame = centers;
        this.groundingDuration = clipDuration;
    }

    public void clearGrounding() {
        groundingClip = null;
        groundCenterXByFrame = new float[0];
        groundingDuration = 0f;
    }

    public void clearGroundingKeepingVisualPosition() {
        float correctionX = currentGroundingOffsetX();

        setX(getX() + correctionX);

        clearGrounding();
    }

    public boolean hasGrounding() {
        return groundingClip != null
            && groundCenterXByFrame.length >= 2
            && groundingDuration > 0f;
    }

    public int getGroundingFrameCount() {
        return groundCenterXByFrame.length;
    }

    public float getGroundingDuration() {
        return groundingDuration;
    }

    public float getGroundingStepDistanceCanvas() {
        if (!hasGrounding()) {
            return 0f;
        }

        return Math.abs(
            groundCenterXByFrame[
                groundCenterXByFrame.length - 1
                ]
                - groundCenterXByFrame[0]
        );
    }

    public float getGroundingStepDistanceWorld() {
        return getGroundingStepDistanceCanvas()
            * Math.abs(getScaleX());
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (!animationPaused) {
            stateTime += delta * playbackSpeed;
        }

        if (additiveFlashRemaining > 0f) {
            additiveFlashRemaining = Math.max(
                0f,
                additiveFlashRemaining - Math.max(0f, delta)
            );
        }
    }

    @Override
    public void draw(
        Batch batch,
        float parentAlpha
    ) {
        float drawX =
            getX()
                + currentGroundingOffsetX();

        float drawY =
            getY();

        Color oldColor =
            new Color(batch.getColor());

        Color actorColor =
            getColor();

        try {
            if (outlineEnabled
                && outlineThickness > 0f) {
                drawOutline(
                    batch,
                    parentAlpha,
                    drawX,
                    drawY
                );
            }

            batch.setColor(
                actorColor.r,
                actorColor.g,
                actorColor.b,
                actorColor.a * parentAlpha
            );

            drawPam(
                batch,
                drawX,
                drawY
            );

            if (additiveFlashRemaining > 0f
                && additiveFlashDuration > 0f
                && additiveFlashAlpha > 0f) {
                float progress =
                    additiveFlashRemaining
                        / additiveFlashDuration;

                batch.setBlendFunction(
                    GL20.GL_SRC_ALPHA,
                    GL20.GL_ONE
                );

                batch.setColor(
                    1f,
                    1f,
                    1f,
                    additiveFlashAlpha
                        * progress
                        * actorColor.a
                        * parentAlpha
                );

                drawPam(
                    batch,
                    drawX,
                    drawY
                );

                batch.setBlendFunction(
                    GL20.GL_SRC_ALPHA,
                    GL20.GL_ONE_MINUS_SRC_ALPHA
                );
            }
        } finally {
            batch.setBlendFunction(
                GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA
            );
            batch.setColor(oldColor);
        }
    }

    private void drawOutline(
        Batch batch,
        float parentAlpha,
        float drawX,
        float drawY
    ) {

        float radius =
            outlineThickness;

        float pulseAlpha =
            outlinePulseBaseAlpha
                + outlinePulseAmplitude
                * (0.5f
                + 0.5f
                * (float) Math.sin(
                stateTime
                    * outlinePulseSpeed
            ));

        drawGlowRing(
            batch,
            parentAlpha,
            drawX,
            drawY,
            radius * 0.35f,
            pulseAlpha * 0.36f
        );

        drawGlowRing(
            batch,
            parentAlpha,
            drawX,
            drawY,
            radius * 0.68f,
            pulseAlpha * 0.22f
        );

        drawGlowRing(
            batch,
            parentAlpha,
            drawX,
            drawY,
            radius,
            pulseAlpha * 0.12f
        );
    }

    private void drawGlowRing(
        Batch batch,
        float parentAlpha,
        float drawX,
        float drawY,
        float radius,
        float alpha
    ) {
        if (radius <= 0f
            || alpha <= 0f) {
            return;
        }

        batch.setColor(
            outlineColor.r,
            outlineColor.g,
            outlineColor.b,
            outlineColor.a
                * alpha
                * getColor().a
                * parentAlpha
        );

        for (int i = 0; i < GLOW_DIRECTIONS; i++) {
            double angle =
                Math.PI
                    * 2.0
                    * i
                    / GLOW_DIRECTIONS;

            float offsetX =
                (float) Math.cos(angle)
                    * radius;

            float offsetY =
                (float) Math.sin(angle)
                    * radius;

            drawPam(
                batch,
                drawX + offsetX,
                drawY + offsetY
            );
        }
    }

    private void drawPam(
        Batch batch,
        float x,
        float y
    ) {
        pamPlayer.draw(
            batch,
            pamPath,
            clip,
            stateTime,
            x,
            y,
            getScaleX(),
            getScaleY(),
            loop,
            visibilityMap
        );
    }

    private float currentGroundingOffsetX() {
        if (!hasGrounding()
            || !groundingClip.equals(clip)
            || !loop) {
            return 0f;
        }

        int frameCount =
            groundCenterXByFrame.length;

        int frameIndex =
            currentGroundingFrameIndex();

        float progress =
            frameCount <= 1
                ? 0f
                : frameIndex
                  / (float) (frameCount - 1);

        float startX =
            groundCenterXByFrame[0];

        float endX =
            groundCenterXByFrame[
                frameCount - 1
                ];

        float currentX =
            groundCenterXByFrame[
                frameIndex
                ];

        float expectedLinearX =
            startX
                + (endX - startX)
                * progress;

        return (
            expectedLinearX
                - currentX
        ) * getScaleX();
    }

    private int currentGroundingFrameIndex() {
        int frameCount =
            groundCenterXByFrame.length;

        if (frameCount <= 1
            || groundingDuration <= 0f) {
            return 0;
        }

        float localTime =
            stateTime
                % groundingDuration;

        if (localTime < 0f) {
            localTime += groundingDuration;
        }

        float frameDuration =
            groundingDuration
                / frameCount;

        if (frameDuration <= 0f) {
            return 0;
        }

        int frameIndex =
            (int) Math.floor(
                localTime
                    / frameDuration
            );

        if (frameIndex < 0) {
            return 0;
        }

        return Math.min(
            frameIndex,
            frameCount - 1
        );
    }

    private static void fillMissingValues(
        float[] values
    ) {
        int firstValid = -1;

        for (int i = 0; i < values.length; i++) {
            if (!Float.isNaN(values[i])) {
                firstValid = i;
                break;
            }
        }

        if (firstValid < 0) {
            return;
        }

        for (int i = 0; i < firstValid; i++) {
            values[i] = values[firstValid];
        }

        int previousValid =
            firstValid;

        for (int i = firstValid + 1;
             i < values.length;
             i++) {

            if (Float.isNaN(values[i])) {
                continue;
            }

            int nextValid = i;
            int gap =
                nextValid
                    - previousValid;

            if (gap > 1) {
                float from =
                    values[previousValid];

                float to =
                    values[nextValid];

                for (int j = 1; j < gap; j++) {
                    float alpha =
                        j / (float) gap;

                    values[
                        previousValid + j
                        ] =
                        from
                            + (to - from)
                            * alpha;
                }
            }

            previousValid =
                nextValid;
        }

        for (int i = previousValid + 1;
             i < values.length;
             i++) {

            values[i] =
                values[previousValid];
        }
    }
}
