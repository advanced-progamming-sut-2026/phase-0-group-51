package views.graphical.gameplay.manager;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import graphics.PvzGame;
import models.Zombie.Zombie;
import models.games.GameState;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

public final class DeadlineOverlayManager extends Group {

    private static final float DANGER_DISTANCE_COLUMNS = 3f;
    private static final float PULSE_DISTANCE_COLUMNS = 1.25f;

    private static final float MIN_LINE_WIDTH = 4f;
    private static final float MAX_LINE_WIDTH = 6f;
    private static final float GLOW_EXTRA_WIDTH = 12f;

    private static final float BASE_CORE_ALPHA = 0.58f;
    private static final float BASE_GLOW_ALPHA = 0.08f;

    private final BoardTransform transform;
    private final Image glow;
    private final Image core;

    private float pulseTime;

    public DeadlineOverlayManager(
        PvzGame game,
        BoardTransform transform
    ) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        if (transform == null) {
            throw new IllegalArgumentException("transform cannot be null");
        }

        this.transform = transform;

        glow = new Image(
            game.getSkin().newDrawable(
                "white_pixel",
                Color.WHITE
            )
        );

        core = new Image(
            game.getSkin().newDrawable(
                "white_pixel",
                Color.WHITE
            )
        );

        setTouchable(Touchable.disabled);
        glow.setTouchable(Touchable.disabled);
        core.setTouchable(Touchable.disabled);

        addActor(glow);
        addActor(core);

        setVisible(false);
    }

    public void sync(
        GameState state,
        float delta
    ) {
        if (state == null || !state.hasDeadline()) {
            setVisible(false);
            return;
        }

        setVisible(true);
        pulseTime += Math.max(0f, delta);

        float deadlineModelX =
            state.getDeadlineColumn() - 1f;

        float nearestDistance =
            findNearestThreatDistance(
                state,
                deadlineModelX
            );

        float proximity = 0f;
        if (nearestDistance != Float.POSITIVE_INFINITY) {
            proximity = 1f - MathUtils.clamp(
                nearestDistance / DANGER_DISTANCE_COLUMNS,
                0f,
                1f
            );
        }

        float pulse = 0f;
        if (nearestDistance <= PULSE_DISTANCE_COLUMNS) {
            float wave =
                0.5f
                    + 0.5f * (float) Math.sin(
                    pulseTime * 9f
                );
            pulse = wave * 0.12f * proximity;
        }

        float coreAlpha = MathUtils.clamp(
            BASE_CORE_ALPHA
                + proximity * 0.56f
                + pulse,
            0f,
            1f
        );

        float glowAlpha = MathUtils.clamp(
            BASE_GLOW_ALPHA
                + proximity * 0.28f
                + pulse * 0.55f,
            0f,
            0.42f
        );

        float lineWidth = MathUtils.lerp(
            MIN_LINE_WIDTH,
            MAX_LINE_WIDTH,
            proximity
        );

        float green = MathUtils.lerp(
            0.10f,
            0.02f,
            proximity
        );

        float blue = MathUtils.lerp(
            0.04f,
            0.01f,
            proximity
        );

        layoutLine(
            state,
            lineWidth
        );

        core.setColor(
            1f,
            green,
            blue,
            coreAlpha
        );

        glow.setColor(
            1f,
            0.08f,
            0.03f,
            glowAlpha
        );

    }

    private float findNearestThreatDistance(
        GameState state,
        float deadlineModelX
    ) {
        float nearest = Float.POSITIVE_INFINITY;

        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie == null
                || zombie.isDead()
                || zombie.isHypnotized()) {
                continue;
            }

            float distance =
                zombie.getX() - deadlineModelX;

            if (distance <= 0f) {
                return 0f;
            }

            nearest = Math.min(
                nearest,
                distance
            );
        }

        return nearest;
    }

    private void layoutLine(
        GameState state,
        float lineWidth
    ) {
        BoardArea area = transform.getArea();

        float deadlineX =
            area.x()
                + state.getDeadlineColumn()
                * transform.tileWidth();

        float bottomTrim = 12f;

        float boardBottom = area.y() + bottomTrim;
        float boardHeight = area.height() - bottomTrim;

        core.setBounds(
            deadlineX - lineWidth / 2f,
            boardBottom,
            lineWidth,
            boardHeight
        );

        float glowWidth =
            lineWidth + GLOW_EXTRA_WIDTH;

        glow.setBounds(
            deadlineX - glowWidth / 2f,
            boardBottom,
            glowWidth,
            boardHeight
        );

    }

    public void clearVisuals() {
        setVisible(false);
        pulseTime = 0f;
    }
}
