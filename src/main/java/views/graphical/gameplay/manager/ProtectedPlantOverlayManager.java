package views.graphical.gameplay.manager;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import graphics.PvzGame;
import models.Board.Tile;
import models.Plant.Plant;
import models.games.GameState;
import views.graphical.gameplay.board.BoardTransform;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ProtectedPlantOverlayManager extends Group {

    private static final String TILE_BACKGROUND_REGION =
        "IMAGE_BACKGROUNDS_PROTECT_TILE_PROTECT_TILE_133X157";

    private static final String TILE_FOREGROUND_REGION =
        "IMAGE_BACKGROUNDS_PROTECT_TILE_PROTECT_TILE_112X125";

    private static final float FOREGROUND_SCALE_IN_TILE = 1f;

    private final BoardTransform transform;
    private final TextureRegion backgroundRegion;
    private final TextureRegion foregroundRegion;

    private final Map<Plant, ProtectedTileVisual> visuals =
        new IdentityHashMap<>();

    public ProtectedPlantOverlayManager(
        PvzGame game,
        BoardTransform transform
    ) {
        if (game == null) {
            throw new IllegalArgumentException(
                "game cannot be null"
            );
        }

        if (transform == null) {
            throw new IllegalArgumentException(
                "transform cannot be null"
            );
        }

        this.transform = transform;

        backgroundRegion =
            game.getTextureBank().region(
                TILE_BACKGROUND_REGION
            );

        foregroundRegion =
            game.getTextureBank().region(
                TILE_FOREGROUND_REGION
            );

        if (backgroundRegion == null) {
            throw new IllegalStateException(
                "TextureBank region was not found: "
                    + TILE_BACKGROUND_REGION
            );
        }

        if (foregroundRegion == null) {
            throw new IllegalStateException(
                "TextureBank region was not found: "
                    + TILE_FOREGROUND_REGION
            );
        }

        setTouchable(Touchable.disabled);
        setVisible(false);
    }

    public void sync(GameState state) {
        Set<Plant> visibleProtectedPlants =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

        if (state != null
            && state.isSaveOurSeedsActive()) {

            for (Plant plant : state.getProtectedPlants()) {
                if (plant == null
                    || plant.isDead()
                    || plant.isMarkedForRemoval()) {
                    continue;
                }

                Tile tile =
                    state.getBoard()
                        .getTileForPlant(plant);

                if (tile == null) {
                    continue;
                }

                visibleProtectedPlants.add(plant);

                ProtectedTileVisual visual =
                    visuals.get(plant);

                if (visual == null) {
                    visual = createVisual();
                    visuals.put(plant, visual);
                    addActor(visual);
                }

                layoutVisual(
                    visual,
                    tile.getLane(),
                    tile.getColumn()
                );
            }
        }

        Iterator<Map.Entry<Plant, ProtectedTileVisual>> iterator =
            visuals.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Plant, ProtectedTileVisual> entry =
                iterator.next();

            if (visibleProtectedPlants.contains(
                entry.getKey()
            )) {
                continue;
            }

            entry.getValue().remove();
            iterator.remove();
        }

        setVisible(!visuals.isEmpty());
    }

    private ProtectedTileVisual createVisual() {
        Image background =
            new Image(
                new TextureRegionDrawable(
                    backgroundRegion
                )
            );

        Image foreground =
            new Image(
                new TextureRegionDrawable(
                    foregroundRegion
                )
            );

        background.setTouchable(Touchable.disabled);
        foreground.setTouchable(Touchable.disabled);

        ProtectedTileVisual visual =
            new ProtectedTileVisual(
                background,
                foreground
            );

        visual.setTouchable(Touchable.disabled);
        visual.addActor(background);
        visual.addActor(foreground);

        return visual;
    }

    private void layoutVisual(
        ProtectedTileVisual visual,
        int lane,
        int column
    ) {
        float tileX =
            transform.tileX(column);

        float tileY =
            transform.tileY(lane);

        float tileWidth =
            transform.tileWidth();

        float tileHeight =
            transform.tileHeight();

        visual.setBounds(
            tileX,
            tileY,
            tileWidth,
            tileHeight
        );

        visual.background.setBounds(
            0f,
            0f,
            tileWidth,
            tileHeight
        );

        float sourceWidth =
            Math.max(
                1f,
                foregroundRegion.getRegionWidth()
            );

        float sourceHeight =
            Math.max(
                1f,
                foregroundRegion.getRegionHeight()
            );

        float maxWidth =
            tileWidth
                * FOREGROUND_SCALE_IN_TILE;

        float maxHeight =
            tileHeight
                * FOREGROUND_SCALE_IN_TILE;

        float scale =
            Math.min(
                maxWidth / sourceWidth,
                maxHeight / sourceHeight
            );

        float width =
            sourceWidth * scale;

        float height =
            sourceHeight * scale;

        float x =
            (tileWidth - width) * 0.5f;

        float y =
            (tileHeight - height) * 0.5f;

        visual.foreground.setBounds(
            x,
            y,
            width,
            height
        );
    }

    public void clearVisuals() {
        for (
            ProtectedTileVisual visual :
            visuals.values()
        ) {
            visual.remove();
        }

        visuals.clear();
        setVisible(false);
    }

    private static final class ProtectedTileVisual
        extends Group {

        private final Image background;
        private final Image foreground;

        private ProtectedTileVisual(
            Image background,
            Image foreground
        ) {
            this.background = background;
            this.foreground = foreground;
        }
    }
}
