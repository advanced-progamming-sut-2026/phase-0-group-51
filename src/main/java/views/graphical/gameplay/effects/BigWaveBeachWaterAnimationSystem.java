package views.graphical.gameplay.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import graphics.PvzGame;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Visual layer for Big Wave Beach water mechanics.
 *
 * The model remains authoritative: Board.isWater()/leftmostWaterColumn decide
 * where water exists. This class only mirrors that state visually.
 */
public final class BigWaveBeachWaterAnimationSystem {

    private static final String TIDE_LINE_PAM =
        "768/FULL/BACKGROUNDS/WATER_TIDE_LINE/WATER_TIDE_LINE.PAM";

    private static final String WATER_UNDERLAYER_PAM =
        "768/FULL/BACKGROUNDS/WATER_UNDERLAYER/WATER_UNDERLAYER.PAM";

    private static final String WATER_SPLASH_PAM =
        "768/FULL/EFFECTS/WATER_SPLASH/WATER_SPLASH.PAM";

    private static final String ZOMBIE_RIPPLE_PAM =
        "768/FULL/BACKGROUNDS/WATER_ZOMBIE_RIPPLE/WATER_ZOMBIE_RIPPLE.PAM";

    private static final String IMP_RIPPLE_PAM =
        "768/FULL/BACKGROUNDS/WATER_IMP_RIPPLE/WATER_IMP_RIPPLE.PAM";

    private static final String GARGANTUAR_RIPPLE_PAM =
        "768/FULL/BACKGROUNDS/WATER_GARGANTUAR_RIPPLE/WATER_GARGANTUAR_RIPPLE.PAM";

    private static final float BACKGROUND_SCALE = 0.61f;
    private static final float RIPPLE_SCALE = 0.61f;
    private static final float SPLASH_SCALE = 0.61f;
    private static final float SPLASH_FALLBACK_DURATION = 0.45f;
    private static final float TIDE_MOVE_SPEED = 7.5f;

    // Matches BigWaveBeachFeature.MAX_WATER_COLUMNS.
    // The tide line is a fixed visual marker for the farthest point the
    // water is ever allowed to reach.
    private static final int MAX_WATER_COLUMNS = 6;

    // Only the underlayer is slightly transparent. Tide line/ripples/splash
    // keep their normal alpha.
    private static final float WATER_ALPHA = 0.93f;

    // Water surface inside each tile, measured upward from the tile bottom.
    // IMPORTANT: Zombie actor Y is around the tile center, so using actorY
    // directly makes the water cut through the zombie's face.
    //
    // 0.25f = lower water line / more zombie visible
    // 0.30f = default
    // 0.40f = higher water line / less zombie visible
    private static final float WATER_SURFACE_TILE_RATIO = 0.30f;

    /**
     * Manually trims pixels/world-units from the TOP of the sea rendering.
     *
     * Increase this value to hide more of the water from the top.
     * Example:
     *   0f   = no top cut
     *   50f  = cut 50 units from the top
     *   100f = cut 100 units from the top
     *
     * This does NOT move the water actor; it only changes the clipping area.
     */
    private static final float WATER_TOP_CUT = 70f;

    private final PvzGame game;
    private final PamPlayer pamPlayer;
    private final BoardTransform transform;
    private final ChapterTheme theme;
    private final ZombieAnimationSystem zombieAnimationSystem;
    private final Group renderLayer;

    // Full-background-height clipping for WATER_UNDERLAYER only.
    private final WaterBackgroundClipGroup waterBackgroundClipLayer;

    // Tile/board-bounds clipping for the fixed tide line.
    private final BoardClipGroup boardClipLayer;

    // Ripple/splash need to render above entityDepthLayer. This contains only
    // effects; no duplicate WATER_UNDERLAYER is drawn here.
    private final BoardClipGroup foregroundEffectsClipLayer;

    private PamAnimationActor waterUnderlayer;
    private PamAnimationActor tideLine;
    private float renderedTideX = Float.NaN;

    private final Map<Zombie, RippleVisual> ripples =
        new IdentityHashMap<>();

    private final Map<Zombie, Boolean> previousWaterState =
        new IdentityHashMap<>();

    public BigWaveBeachWaterAnimationSystem(
        PvzGame game,
        BoardTransform transform,
        ChapterTheme theme,
        ZombieAnimationSystem zombieAnimationSystem,
        Group renderLayer
    ) {
        this.game = Objects.requireNonNull(game, "game");
        this.pamPlayer = Objects.requireNonNull(game.getPamPlayer(), "pamPlayer");
        this.transform = Objects.requireNonNull(transform, "transform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.zombieAnimationSystem = Objects.requireNonNull(
            zombieAnimationSystem,
            "zombieAnimationSystem"
        );
        this.renderLayer = Objects.requireNonNull(renderLayer, "renderLayer");

        // WATER_UNDERLAYER may use the full gameplay-background height,
        // but it is still horizontally restricted to the board strip.
        this.waterBackgroundClipLayer =
            new WaterBackgroundClipGroup(this.transform);
        this.waterBackgroundClipLayer.setTouchable(Touchable.disabled);
        this.renderLayer.addActor(this.waterBackgroundClipLayer);

        // Tide line, ripples and splashes are restricted to the actual
        // tile/board rectangle.
        this.boardClipLayer = new BoardClipGroup(this.transform);
        this.boardClipLayer.setTouchable(Touchable.disabled);
        this.renderLayer.addActor(this.boardClipLayer);

        this.foregroundEffectsClipLayer =
            new BoardClipGroup(this.transform);
        this.foregroundEffectsClipLayer.setTouchable(Touchable.disabled);

        if (theme == ChapterTheme.BIG_WAVE_BEACH) {
            safeLoad(TIDE_LINE_PAM);
            safeLoad(WATER_UNDERLAYER_PAM);
            safeLoad(WATER_SPLASH_PAM);
            safeLoad(ZOMBIE_RIPPLE_PAM);
            safeLoad(IMP_RIPPLE_PAM);
            safeLoad(GARGANTUAR_RIPPLE_PAM);
        }
    }

    public void sync(
        Board board,
        Collection<Zombie> zombies,
        float delta
    ) {
        if (theme != ChapterTheme.BIG_WAVE_BEACH || board == null) {
            clear();
            return;
        }

        ensureForegroundEffectsPlacement();
        ensureBackgroundVisuals();
        syncTide(board, delta);
        syncZombieRipples(board, zombies);

        // World actors may be added after this system is constructed. Keep
        // ripple/splash over the zombie/entity layer.
        foregroundEffectsClipLayer.toFront();
    }

    public void clear() {
        if (waterUnderlayer != null) {
            waterUnderlayer.remove();
            waterUnderlayer = null;
        }
        if (tideLine != null) {
            tideLine.remove();
            tideLine = null;
        }

        for (Zombie zombie : previousWaterState.keySet()) {
            PamAnimationActor zombieActor =
                zombieAnimationSystem.getActor(zombie);

            if (zombieActor != null) {
                zombieActor.clearDrawClip();
            }
        }

        for (RippleVisual visual : ripples.values()) {
            visual.actor.remove();
        }
        ripples.clear();
        previousWaterState.clear();
        renderedTideX = Float.NaN;
    }

    private void ensureBackgroundVisuals() {
        if (waterUnderlayer == null) {
            waterUnderlayer = createLoopingActor(
                WATER_UNDERLAYER_PAM,
                BACKGROUND_SCALE,
                "idle",
                "loop",
                "animation",
                "anim"
            );
            if (waterUnderlayer != null) {
                waterUnderlayer.setColor(
                    1f,
                    1f,
                    1f,
                    WATER_ALPHA
                );
                waterBackgroundClipLayer.addActor(waterUnderlayer);
            }
        }

        if (tideLine == null) {
            tideLine = createLoopingActor(
                TIDE_LINE_PAM,
                BACKGROUND_SCALE,
                "idle",
                "loop",
                "animation",
                "anim"
            );
            if (tideLine != null) {
                boardClipLayer.addActor(tideLine);
            }
        }
    }

    private void syncTide(Board board, float delta) {
        BoardArea area = transform.getArea();

        int leftmostWaterColumn = Math.max(
            0,
            Math.min(
                board.getColumnCount(),
                board.getColumnCount() - board.getWaterColumnCount()
            )
        );

        float targetTideX = leftmostWaterColumn >= board.getColumnCount()
            ? area.x() + area.width()
            : transform.tileX(leftmostWaterColumn);

        if (Float.isNaN(renderedTideX)) {
            renderedTideX = targetTideX;
        } else {
            float maxMove = Math.max(0f, delta)
                * transform.tileWidth()
                * TIDE_MOVE_SPEED;

            float distance = targetTideX - renderedTideX;
            if (Math.abs(distance) <= maxMove) {
                renderedTideX = targetTideX;
            } else {
                renderedTideX += Math.signum(distance) * maxMove;
            }
        }

        // WATER_UNDERLAYER keeps the full gameplay-background height.
        float backgroundHeight = getGameplayBackgroundHeight();
        float waterCenterY = backgroundHeight * 0.5f;

        if (waterUnderlayer != null) {
            // The water body may still follow the current model tide.
            waterUnderlayer.setPosition(
                renderedTideX,
                waterCenterY
            );
            waterUnderlayer.setVisible(
                board.getWaterColumnCount() > 0
            );
        }

        if (tideLine != null) {
            // Fixed maximum shoreline:
            // Big Wave Beach allows at most MAX_WATER_COLUMNS water columns.
            // For a 9-column board this is column index 3 (the fourth tile).
            int maxTideLeftmostColumn = Math.max(
                0,
                board.getColumnCount() - MAX_WATER_COLUMNS
            );

            float fixedTideX =
                transform.tileX(maxTideLeftmostColumn);

            // The tide line belongs to the tile area, not to the full
            // background height.
            float tideCenterY =
                area.y() + area.height() * 0.5f;

            tideLine.setPosition(
                fixedTideX,
                tideCenterY
            );
            tideLine.setVisible(
                board.getWaterColumnCount() > 0
            );
        }
    }

    private void syncZombieRipples(
        Board board,
        Collection<Zombie> zombies
    ) {
        Collection<Zombie> safeZombies = zombies == null
            ? Collections.emptyList()
            : zombies;

        Set<Zombie> present = Collections.newSetFromMap(
            new IdentityHashMap<>()
        );

        for (Zombie zombie : safeZombies) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            present.add(zombie);

            PamAnimationActor zombieActor =
                zombieAnimationSystem.getActor(zombie);

            int column = (int) Math.floor(zombie.getX());

            boolean modelSaysWater = board.isWaterTile(
                zombie.getLane(),
                column
            );

            // Avoid visually treating the neighboring dry tile as water while
            // the rendered shoreline is still between tile boundaries.
            boolean visuallyPastShoreline =
                zombieActor == null
                    || Float.isNaN(renderedTideX)
                    || zombieActor.getX() >= renderedTideX;

            boolean inWater =
                modelSaysWater && visuallyPastShoreline;

            boolean wasInWater = previousWaterState.getOrDefault(
                zombie,
                false
            );

            if (inWater && !wasInWater) {
                if (zombieActor != null) {
                    playSplash(
                        zombieActor.getX(),
                        zombieActor.getY()
                    );
                } else {
                    playSplashAtBoardPosition(
                        zombie.getX(),
                        zombie.getLane()
                    );
                }
            }

            previousWaterState.put(zombie, inWater);

            if (!inWater) {
                if (zombieActor != null) {
                    zombieActor.clearDrawClip();
                }

                RippleVisual old = ripples.remove(zombie);
                if (old != null) {
                    old.actor.remove();
                }
                continue;
            }

            if (zombieActor == null) {
                continue;
            }

            // One physical water surface per lane. Calculate it from the
            // tile bottom instead of the zombie actor origin.
            float laneBottom =
                transform.tileY(zombie.getLane());

            float surfaceY =
                laneBottom
                    + transform.tileHeight()
                    * WATER_SURFACE_TILE_RATIO;

            // Do NOT draw another water image over the zombie. Instead clip
            // the zombie itself, so every pixel below the ripple disappears
            // while the original background water remains untouched.
            float worldWidth = getGameplayBackgroundWidth();
            float worldHeight = getGameplayBackgroundHeight();

            zombieActor.setDrawClip(
                0f,
                surfaceY,
                worldWidth,
                Math.max(
                    1f,
                    worldHeight - surfaceY
                )
            );

            String pamPath = ripplePamFor(zombie);
            RippleVisual visual = ripples.get(zombie);

            if (visual == null || !visual.pamPath.equals(pamPath)) {
                if (visual != null) {
                    visual.actor.remove();
                }

                PamAnimationActor ripple = createLoopingActor(
                    pamPath,
                    RIPPLE_SCALE,
                    "idle",
                    "loop",
                    "animation",
                    "anim"
                );

                if (ripple == null) {
                    continue;
                }

                foregroundEffectsClipLayer.addActor(ripple);
                visual = new RippleVisual(pamPath, ripple);
                ripples.put(zombie, visual);
            }

            visual.actor.setPosition(
                zombieActor.getX(),
                surfaceY
            );

            visual.actor.setScale(
                Math.copySign(
                    RIPPLE_SCALE,
                    zombieActor.getScaleX()
                ),
                RIPPLE_SCALE
            );

            visual.actor.toFront();
        }

        // Zombies that disappeared from the living collection must not keep
        // a stale clip while their death animation is still being rendered.
        for (Zombie zombie :
            new java.util.ArrayList<>(previousWaterState.keySet())) {

            if (present.contains(zombie)) {
                continue;
            }

            PamAnimationActor zombieActor =
                zombieAnimationSystem.getActor(zombie);

            if (zombieActor != null) {
                zombieActor.clearDrawClip();
            }

            previousWaterState.remove(zombie);
        }

        ripples.entrySet().removeIf(entry -> {
            Zombie zombie = entry.getKey();

            if (present.contains(zombie)) {
                return false;
            }

            entry.getValue().actor.remove();
            return true;
        });
    }

    private String ripplePamFor(Zombie zombie) {
        String alias = zombie.getAlias() == null
            ? ""
            : zombie.getAlias().toLowerCase(Locale.ROOT);

        if (alias.contains("gargantuar")) {
            return GARGANTUAR_RIPPLE_PAM;
        }

        if (alias.contains("imp")) {
            return IMP_RIPPLE_PAM;
        }

        return ZOMBIE_RIPPLE_PAM;
    }

    private void playSplashAtBoardPosition(double posX, double posY) {
        BoardArea area = transform.getArea();
        float x = area.x()
            + ((float) posX + 0.5f)
            * transform.tileWidth();
        float y = area.y()
            + (BoardTransform.ROWS - 1f - (float) posY + 0.5f)
            * transform.tileHeight();
        playSplash(x, y);
    }

    private void playSplash(float x, float y) {
        try {
            EffectPamFactory.OneShot splash = EffectPamFactory.create(
                game,
                WATER_SPLASH_PAM,
                SPLASH_SCALE,
                SPLASH_FALLBACK_DURATION,
                "splash",
                "water_splash",
                "effect",
                "animation",
                "anim"
            );

            PamAnimationActor actor = splash.actor();
            actor.setPosition(x, y);
            foregroundEffectsClipLayer.addActor(actor);
            actor.toFront();
            actor.addAction(
                Actions.sequence(
                    Actions.delay(splash.duration()),
                    Actions.removeActor()
                )
            );
        } catch (RuntimeException e) {
            logError("Could not play water splash", e);
        }
    }

    private PamAnimationActor createLoopingActor(
        String pamPath,
        float scale,
        String... clipCandidates
    ) {
        try {
            safeLoad(pamPath);
            List<String> clips = pamPlayer.clips(pamPath);
            if (clips == null || clips.isEmpty()) {
                return null;
            }

            String clip = findClip(clips, clipCandidates);
            if (clip == null) {
                clip = clips.getFirst();
            }

            PamAnimationActor actor = new PamAnimationActor(
                pamPlayer,
                pamPath,
                clip,
                true
            );
            actor.setScale(scale, scale);
            actor.setTouchable(Touchable.disabled);
            actor.restart();
            return actor;
        } catch (RuntimeException e) {
            logError("Could not create water visual: " + pamPath, e);
            return null;
        }
    }

    private String findClip(List<String> clips, String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            for (String clip : clips) {
                if (clip.equalsIgnoreCase(candidate)) {
                    return clip;
                }
            }
        }
        return null;
    }

    private void ensureForegroundEffectsPlacement() {
        Group targetParent = renderLayer.getParent();

        if (targetParent == null) {
            return;
        }

        if (foregroundEffectsClipLayer.getParent() != targetParent) {
            foregroundEffectsClipLayer.remove();
            targetParent.addActor(foregroundEffectsClipLayer);
        }
    }

    private float getGameplayBackgroundWidth() {
        if (renderLayer.getStage() != null
            && renderLayer.getStage().getViewport() != null) {
            return renderLayer.getStage().getViewport().getWorldWidth();
        }

        return 1600f;
    }

    private float getGameplayBackgroundHeight() {
        if (renderLayer.getStage() != null
            && renderLayer.getStage().getViewport() != null) {
            return renderLayer.getStage().getViewport().getWorldHeight();
        }

        // GameScreen's gameplay background is authored at 600 world units.
        return 600f;
    }

    private void safeLoad(String pamPath) {
        try {
            pamPlayer.loadSync(pamPath);
        } catch (RuntimeException e) {
            logError("Could not load PAM: " + pamPath, e);
        }
    }

    private void logError(String message, RuntimeException e) {
        if (Gdx.app != null) {
            Gdx.app.error("BigWaveBeachWater", message, e);
        }
    }

    /**
     * Clips WATER_UNDERLAYER to the board's horizontal span, while allowing
     * it to use the full gameplay-background height. WATER_TOP_CUT only
     * trims the top of this background-water layer.
     */
    private static final class WaterBackgroundClipGroup extends Group {
        private final BoardTransform transform;
        private final Rectangle clipBounds = new Rectangle();
        private final Rectangle scissorBounds = new Rectangle();

        private WaterBackgroundClipGroup(BoardTransform transform) {
            this.transform = Objects.requireNonNull(
                transform,
                "transform"
            );

            setTransform(false);
            setTouchable(Touchable.disabled);
        }

        @Override
        public void draw(
            Batch batch,
            float parentAlpha
        ) {
            if (getStage() == null || getChildren().isEmpty()) {
                return;
            }

            BoardArea area = transform.getArea();

            float backgroundHeight =
                getStage().getViewport().getWorldHeight();

            float topCut = Math.max(
                0f,
                Math.min(
                    WATER_TOP_CUT,
                    backgroundHeight
                )
            );

            clipBounds.set(
                area.x(),
                0f,
                area.width(),
                backgroundHeight - topCut
            );

            batch.flush();

            ScissorStack.calculateScissors(
                getStage().getCamera(),
                batch.getTransformMatrix(),
                clipBounds,
                scissorBounds
            );

            if (!ScissorStack.pushScissors(scissorBounds)) {
                return;
            }

            try {
                super.draw(batch, parentAlpha);
                batch.flush();
            } finally {
                ScissorStack.popScissors();
            }
        }
    }

    /**
     * Clips Tide Line, ripples and splashes to the actual 5x9 tile area.
     * This prevents the tide-line PAM from stretching visually through the
     * full gameplay background.
     */
    private static final class BoardClipGroup extends Group {
        private final BoardTransform transform;
        private final Rectangle clipBounds = new Rectangle();
        private final Rectangle scissorBounds = new Rectangle();

        private BoardClipGroup(BoardTransform transform) {
            this.transform = Objects.requireNonNull(
                transform,
                "transform"
            );

            setTransform(false);
            setTouchable(Touchable.disabled);
        }

        @Override
        public void draw(
            Batch batch,
            float parentAlpha
        ) {
            if (getStage() == null || getChildren().isEmpty()) {
                return;
            }

            BoardArea area = transform.getArea();

            clipBounds.set(
                area.x(),
                area.y(),
                area.width(),
                area.height()
            );

            batch.flush();

            ScissorStack.calculateScissors(
                getStage().getCamera(),
                batch.getTransformMatrix(),
                clipBounds,
                scissorBounds
            );

            if (!ScissorStack.pushScissors(scissorBounds)) {
                return;
            }

            try {
                super.draw(batch, parentAlpha);
                batch.flush();
            } finally {
                ScissorStack.popScissors();
            }
        }
    }

    private record RippleVisual(
        String pamPath,
        PamAnimationActor actor
    ) {
    }
}
