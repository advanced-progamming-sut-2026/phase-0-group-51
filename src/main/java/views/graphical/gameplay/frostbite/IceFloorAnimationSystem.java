package views.graphical.gameplay.frostbite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import models.Board.Board;
import models.Board.Tile;
import models.games.ChapterTheme;
import models.games.frostbite.IceFloorDirection;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public final class IceFloorAnimationSystem extends Group {

    private static final String ICE_FLOOR_UP_PAM =
        "768/FULL/EFFECTS/TILESLIDER_ICEAGE_UP/TILESLIDER_ICEAGE_UP.PAM";

    private static final String ICE_FLOOR_DOWN_PAM =
        "768/FULL/EFFECTS/TILESLIDER_ICEAGE_DOWN/TILESLIDER_ICEAGE_DOWN.PAM";

    private static final String IDLE_CLIP = "idle";

    private static final String ACTIVE_START_CLIP = "active_start";

    private static final String ACTIVE_IDLE_CLIP = "active_idle";

    private static final String ACTIVE_END_CLIP = "active_end";

    private static final String WHOLE_ANIMATION_CLIP = "";


    private static final float TILE_FILL = 1.24f;
    private static final float MIN_SCALE = 0.05f;
    private static final float MAX_SCALE = 4f;

    private final PamPlayer pamPlayer;
    private final BoardTransform transform;
    private final ChapterTheme theme;


    private final Map<Long, PamAnimationActor> visuals =
        new HashMap<>();

    private final Map<String, Rectangle> resolvedBounds = new HashMap<>();
    private final Map<String, Float> resolvedScale = new HashMap<>();
    private boolean loadAttempted;

    public IceFloorAnimationSystem(
        PamPlayer pamPlayer,
        BoardTransform transform,
        ChapterTheme theme
    ) {
        this.pamPlayer =
            Objects.requireNonNull(
                pamPlayer,
                "pamPlayer"
            );

        this.transform =
            Objects.requireNonNull(
                transform,
                "transform"
            );

        this.theme =
            Objects.requireNonNull(
                theme,
                "theme"
            );

        setTouchable(
            Touchable.disabled
        );

        if (theme == ChapterTheme.FROSTBITE_CAVES) {
            loadPam();
        }
    }

    public void sync(
        Board board
    ) {
        if (theme != ChapterTheme.FROSTBITE_CAVES
            || board == null) {
            clearVisuals();
            return;
        }

        if (!loadAttempted) {
            loadPam();
        }

        Map<Long, Boolean> active =
            new HashMap<>();

        int modelIceFloorCount = 0;

        for (
            int lane = 0;
            lane < board.getLaneCount();
            lane++
        ) {
            for (
                int column = 0;
                column < board.getColumnCount();
                column++
            ) {
                Tile tile =
                    board.getTile(
                        lane,
                        column
                    );

                if (tile == null
                    || tile.getIceFloorDirection() == null) {
                    continue;
                }

                modelIceFloorCount++;

                long key =
                    key(
                        lane,
                        column
                    );

                active.put(
                    key,
                    Boolean.TRUE
                );

                PamAnimationActor actor =
                    visuals.get(
                        key
                    );

                if (actor == null) {
                    actor =
                        createVisual(
                            lane,
                            column,
                            tile.getIceFloorDirection()
                        );

                    if (actor == null) {
                        continue;
                    }

                    visuals.put(
                        key,
                        actor
                    );
                }
                String pamPath =
                    tile.getIceFloorDirection() == IceFloorDirection.UP
                        ? ICE_FLOOR_UP_PAM
                        : ICE_FLOOR_DOWN_PAM;

                positionActor(
                    actor,
                    lane,
                    column,
                    resolvedBounds.get(pamPath),
                    resolvedScale.get(pamPath)
                );
            }
        }

        Iterator<Map.Entry<Long, PamAnimationActor>> iterator =
            visuals.entrySet()
                .iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, PamAnimationActor> entry =
                iterator.next();

            if (active.containsKey(
                entry.getKey()
            )) {
                continue;
            }

            entry.getValue()
                .remove();

            iterator.remove();
        }

        if (Gdx.app != null
            && modelIceFloorCount > 0
            && visuals.isEmpty()) {
            Gdx.app.error(
                "IceFloorAnimation",
                "Model has "
                    + modelIceFloorCount
                    + " ice-floor tiles, but no PAM visual could be created."
            );
        }
    }

    public void clearVisuals() {
        for (
            PamAnimationActor actor :
            visuals.values()
        ) {
            actor.remove();
        }

        visuals.clear();
        clearChildren();
    }

    public int getVisibleCount() {
        return visuals.size();
    }

    private float clipDuration(String pamPath, String clip, float fallback) {
        try {
            return Math.max(0.05f, pamPlayer.clipDurationSeconds(pamPath, clip));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private void loadPam() {
        loadAttempted = true;
        loadSinglePam(ICE_FLOOR_UP_PAM);
        loadSinglePam(ICE_FLOOR_DOWN_PAM);
    }

    private void loadSinglePam(String pamPath) {
        try {
            pamPlayer.loadSync(pamPath);

            Rectangle bounds = pamPlayer.bounds(pamPath);
            if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
                throw new IllegalStateException("Invalid PAM bounds: " + pamPath);
            }

            resolvedBounds.put(pamPath, bounds);
            resolvedScale.put(pamPath, calculateScale(bounds));

            if (Gdx.app != null) {
                Gdx.app.log(
                    "IceFloorAnimation",
                    "Loaded " + pamPath + " clips=" + pamPlayer.clips(pamPath)
                );
            }
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "IceFloorAnimation",
                    "Could not load " + pamPath,
                    exception
                );
            }
        }
    }

    private String pamFor(IceFloorDirection direction) {
        return direction == IceFloorDirection.DOWN
            ? ICE_FLOOR_DOWN_PAM
            : ICE_FLOOR_UP_PAM;
    }

    private PamAnimationActor createVisual(
        int lane,
        int column,
        IceFloorDirection direction
    ) {
        String pamPath = pamFor(direction);
        Rectangle bounds = resolvedBounds.get(pamPath);

        if (bounds == null) {
            return null;
        }

        try {
            PamAnimationActor actor =
                new IceFloorTileActor(
                    pamPlayer,
                    pamPath,
                    ACTIVE_START_CLIP,
                    ACTIVE_IDLE_CLIP,
                    ACTIVE_END_CLIP,
                    clipDuration(pamPath, ACTIVE_START_CLIP, 0.5f),
                    clipDuration(pamPath, ACTIVE_IDLE_CLIP, 0.6f),
                    clipDuration(pamPath, ACTIVE_END_CLIP, 0.5f)
                );
            actor.setScale(1.5f);

            actor.setTouchable(Touchable.disabled);

            actor.setScaleX(
                transform.tileWidth() * 1.17f / bounds.width
            );

            actor.setScaleY(
                transform.tileHeight() * 1.09f / bounds.height
            );

            positionActor(actor, lane, column, bounds, resolvedScale.get(pamPath));

            addActor(actor);
            actor.restart();
            forceAllPartsVisible(actor, pamPath);

            return actor;

        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "IceFloorAnimation",
                    "Could not create ice floor visual.",
                    exception
                );
            }
            return null;
        }
    }

    private void positionActor(
        PamAnimationActor actor,
        int lane,
        int column,
        Rectangle resolvedBounds,
        Float scaleValue
    ) {
        if (resolvedBounds == null || scaleValue == null) {
            return;
        }

        float tileCenterX =
            transform.tileX(
                column
            )
                + transform.tileWidth()
                * 0.5f;

        float tileCenterY =
            transform.tileY(
                lane
            )
                + transform.tileHeight()
                * 0.5f;

        float pamCenterX =
            (
                resolvedBounds.x
                    + resolvedBounds.width
                    * 0.5f
            )
                * scaleValue;

        float pamCenterY =
            (
                resolvedBounds.y
                    + resolvedBounds.height
                    * 0.5f
            )
                * scaleValue;

        actor.setPosition(
            tileCenterX
                - pamCenterX,
            // libPVZ flips PAM's Y-down canvas into LibGDX's Y-up world.
            // Therefore the bounds-center Y offset must be ADDED here.
            tileCenterY
                + pamCenterY
        );
    }

    private void forceAllPartsVisible(
        PamAnimationActor actor,
        String pamPath
    ) {
        try {
            PamPlayer.AnimationPart root =
                pamPlayer.getParts(pamPath);

            if (root == null) {
                return;
            }

            forcePartTreeVisible(root, actor);

        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "IceFloorAnimation",
                    "Could not enable ice-floor PAM parts.",
                    exception
                );
            }
        }
    }


    private void forcePartTreeVisible(
        PamPlayer.AnimationPart part,
        PamAnimationActor actor
    ) {
        if (part == null) {
            return;
        }

        if (part.name != null
            && !part.name.isBlank()) {
            actor.getVisibilityMap().put(
                part.name,
                true
            );
        }

        if (part.children == null) {
            return;
        }

        for (PamPlayer.AnimationPart child : part.children) {
            forcePartTreeVisible(child, actor);
        }
    }

    private float calculateScale(
        Rectangle bounds
    ) {
        float widthScale =
            transform.tileWidth()
                / bounds.width;

        float heightScale =
            transform.tileHeight()
                / bounds.height;

        float scale =
            Math.min(
                widthScale,
                heightScale
            )
                * TILE_FILL;

        return Math.max(
            MIN_SCALE,
            Math.min(
                MAX_SCALE,
                scale
            )
        );
    }

    private static long key(
        int lane,
        int column
    ) {
        return (
            ((long) lane) << 32
        )
            ^ (
            column
                & 0xffffffffL
        );
    }


}
