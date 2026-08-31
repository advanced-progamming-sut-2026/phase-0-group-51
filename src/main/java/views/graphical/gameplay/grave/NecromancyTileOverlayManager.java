package views.graphical.gameplay.grave;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import models.Board.Board;
import models.Board.Tile;
import models.games.ChapterFeature;
import models.games.ChapterTheme;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class NecromancyTileOverlayManager extends Group {

    private static final String BOULDER_TILE_PAM =
        "768/FULL/BACKGROUNDS/BOULDERTILE/BOULDERTILE.PAM";

    private static final String BOULDER_TILE_CLIP = "bouldertile_up";

    private static final float TILE_SCALE = 0.5f;

    private static final float MARKER_ALPHA = 0.45f;

    private static final float MARKER_Y_OFFSET = 18f;

    private final PamPlayer pamPlayer;
    private final BoardTransform transform;
    private final Group renderLayer;
    private final boolean enabled;

    private final Map<Long, PamAnimationActor> markers = new HashMap<>();

    public NecromancyTileOverlayManager(
        PamPlayer pamPlayer,
        BoardTransform transform,
        ChapterTheme theme
    ) {
        this(pamPlayer, transform, theme, null);
    }

    public NecromancyTileOverlayManager(
        PamPlayer pamPlayer,
        BoardTransform transform,
        ChapterTheme theme,
        Group renderLayer
    ) {
        this.pamPlayer = Objects.requireNonNull(pamPlayer, "pamPlayer");
        this.transform = Objects.requireNonNull(transform, "transform");
        this.renderLayer = renderLayer == null ? this : renderLayer;
        this.enabled = theme != null
            && theme.getChapterFeatures().contains(ChapterFeature.NECROMANCY);
        setTouchable(Touchable.disabled);
    }

    public void sync(Board board) {
        if (!enabled || board == null) {
            clearMarkers();
            return;
        }

        Set<Long> present = new HashSet<>();

        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                if (tile == null || !tile.isNecromancyTile()) {
                    continue;
                }
                long tileKey = key(lane, column);
                present.add(tileKey);
                if (!markers.containsKey(tileKey)) {
                    markers.put(tileKey, createMarker(lane, column));
                }
            }
        }

        Iterator<Map.Entry<Long, PamAnimationActor>> iterator =
            markers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PamAnimationActor> entry = iterator.next();
            if (!present.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }
    }

    public void clearMarkers() {
        for (PamAnimationActor actor : markers.values()) {
            actor.remove();
        }
        markers.clear();
    }

    private PamAnimationActor createMarker(int lane, int column) {
        PamAnimationActor actor = new PamAnimationActor(
            pamPlayer,
            BOULDER_TILE_PAM,
            BOULDER_TILE_CLIP,
            false
        );
        actor.setTouchable(Touchable.disabled);
        actor.setScale(TILE_SCALE, TILE_SCALE);
        actor.setColor(1f, 1f, 1f, MARKER_ALPHA);

        float x = transform.tileX(column) + transform.tileWidth() * 0.5f;
        float y = transform.tileY(lane) + transform.tileHeight() * 0.5f + MARKER_Y_OFFSET;
        actor.setPosition(x, y);

        renderLayer.addActor(actor);
        actor.restart();
        return actor;
    }

    private static long key(int lane, int column) {
        return ((long) lane << 32) | (column & 0xffffffffL);
    }
}
