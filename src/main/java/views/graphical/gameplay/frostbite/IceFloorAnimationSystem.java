package views.graphical.gameplay.frostbite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import models.Board.Board;
import models.Board.Tile;
import models.games.ChapterTheme;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class IceFloorAnimationSystem extends Group {

    private static final String ICE_FLOOR_PAM =
        "768/FULL/EFFECTS/ZOMBONI_TILE_ICE/ZOMBONI_TILE_ICE.PAM";

    private static final float TILE_FILL = 1.08f;
    private static final float MIN_SCALE = 0.05f;
    private static final float MAX_SCALE = 4f;

    private final PamPlayer pamPlayer;
    private final BoardTransform transform;
    private final ChapterTheme theme;

    private final Map<Long, PamAnimationActor> visuals =
        new HashMap<>();

    private String resolvedClip;
    private Rectangle resolvedBounds;
    private float resolvedScale = 1f;
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
                            column
                        );

                    if (actor == null) {
                        continue;
                    }

                    visuals.put(
                        key,
                        actor
                    );
                }

                positionActor(
                    actor,
                    lane,
                    column
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

    private void loadPam() {
        loadAttempted = true;

        try {
            pamPlayer.loadSync(
                ICE_FLOOR_PAM
            );

            resolvedClip =
                resolveDrawableClip();

            resolvedBounds =
                pamPlayer.bounds(
                    ICE_FLOOR_PAM,
                    resolvedClip
                );

            if (resolvedBounds == null
                || resolvedBounds.width <= 0f
                || resolvedBounds.height <= 0f) {
                throw new IllegalStateException(
                    "Ice-floor clip has invalid bounds: "
                        + resolvedClip
                );
            }

            resolvedScale =
                calculateScale(
                    resolvedBounds
                );

            if (Gdx.app != null) {
                Gdx.app.log(
                    "IceFloorAnimation",
                    "PAM loaded. clip="
                        + resolvedClip
                        + ", bounds="
                        + resolvedBounds
                        + ", scale="
                        + resolvedScale
                        + ", clips="
                        + pamPlayer.clips(
                        ICE_FLOOR_PAM
                    )
                );
            }

        } catch (RuntimeException exception) {
            resolvedClip = null;
            resolvedBounds = null;

            if (Gdx.app != null) {
                Gdx.app.error(
                    "IceFloorAnimation",
                    "Could not load "
                        + ICE_FLOOR_PAM,
                    exception
                );
            }
        }
    }

    private PamAnimationActor createVisual(
        int lane,
        int column
    ) {
        if (resolvedClip == null
            || resolvedBounds == null) {
            return null;
        }

        try {
            PamAnimationActor actor =
                new PamAnimationActor(
                    pamPlayer,
                    ICE_FLOOR_PAM,
                    resolvedClip,
                    true
                );

            forceAllPartsVisible(actor);

            actor.setTouchable(
                Touchable.disabled
            );

            actor.setScale(
                resolvedScale,
                resolvedScale
            );

            positionActor(
                actor,
                lane,
                column
            );

            addActor(
                actor
            );

            actor.restart();

            return actor;

        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "IceFloorAnimation",
                    "Could not create ice-floor visual at lane "
                        + (lane + 1)
                        + ", column "
                        + (column + 1)
                        + ".",
                    exception
                );
            }

            return null;
        }
    }

    private void positionActor(
        PamAnimationActor actor,
        int lane,
        int column
    ) {
        if (resolvedBounds == null) {
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
                * resolvedScale;

        float pamCenterY =
            (
                resolvedBounds.y
                    + resolvedBounds.height
                    * 0.5f
            )
                * resolvedScale;

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
        PamAnimationActor actor
    ) {
        try {
            PamPlayer.AnimationPart root =
                pamPlayer.getParts(ICE_FLOOR_PAM);

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

    private String resolveDrawableClip() {
        List<String> clips =
            pamPlayer.clips(
                ICE_FLOOR_PAM
            );

        if (clips == null
            || clips.isEmpty()) {
            throw new IllegalStateException(
                "Ice-floor PAM has no clips."
            );
        }

        String preferred =
            findClip(
                clips,
                "idle",
                "loop",
                "animation",
                "anim",
                "ice",
                "zomboni_tile_ice"
            );

        if (preferred != null
            && hasDrawableBounds(
            preferred
        )) {
            return preferred;
        }

        for (
            String clip :
            clips
        ) {
            if (hasDrawableBounds(
                clip
            )) {
                return clip;
            }
        }

        throw new IllegalStateException(
            "None of the PAM clips has drawable bounds. clips="
                + clips
        );
    }

    private boolean hasDrawableBounds(
        String clip
    ) {
        try {
            Rectangle bounds =
                pamPlayer.bounds(
                    ICE_FLOOR_PAM,
                    clip
                );

            return bounds != null
                && bounds.width > 0f
                && bounds.height > 0f;

        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String findClip(
        List<String> clips,
        String... candidates
    ) {
        for (
            String candidate :
            candidates
        ) {
            for (
                String clip :
                clips
            ) {
                if (clip != null
                    && clip.equalsIgnoreCase(
                    candidate
                )) {
                    return clip;
                }
            }
        }

        for (
            String candidate :
            candidates
        ) {
            String wanted =
                normalize(
                    candidate
                );

            for (
                String clip :
                clips
            ) {
                if (normalize(
                    clip
                ).equals(
                    wanted
                )) {
                    return clip;
                }
            }
        }

        return null;
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

    private static String normalize(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .toLowerCase(
                Locale.ROOT
            )
            .replaceAll(
                "[^a-z0-9]",
                ""
            );
    }
}
