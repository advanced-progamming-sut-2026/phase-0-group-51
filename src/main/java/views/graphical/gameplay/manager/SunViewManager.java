package views.graphical.gameplay.manager;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import graphics.PvzGame;
import lombok.Setter;
import models.sun.Sun;
import views.graphical.gameplay.actors.SunActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SunViewManager extends Group {

    private static final float SKY_START_OFFSET = 60f;

    private final PvzGame game;
    private final BoardTransform transform;

    private final Map<Sun, SunActor> sunActors =
        new IdentityHashMap<>();

    @Setter
    private Consumer<Sun> onSunClicked;

    public SunViewManager(
        PvzGame game,
        BoardTransform transform
    ) {
        this.game = game;
        this.transform = transform;
    }

    public void sync(
        Iterable<Sun> suns,
        float partialTick
    ) {
        Set<Sun> activeSuns =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

        for (Sun sun : suns) {
            if (!sun.isActive()) {
                continue;
            }

            activeSuns.add(
                sun
            );

            SunActor actor =
                sunActors.get(sun);

            if (actor == null) {
                actor = new SunActor(
                    game,
                    sun,
                    this::handleSunCollected
                );

                sunActors.put(
                    sun,
                    actor
                );

                addActor(actor);
            }
            actor.syncVisualState();
            positionSun(
                sun,
                actor,
                partialTick
            );
        }

        removeMissingSuns(
            activeSuns
        );
    }

    private void positionSun(
        Sun sun,
        SunActor actor,
        float partialTick
    ) {
        int column =
            (int) sun.getX();

        float tileX =
            transform.tileX(column);

        float tileY =
            transform.tileY(
                sun.getLane()
            );

        float centerX;
        float groundY;

        if (sun.getSourcePlant() != null) {

            centerX =
                tileX
                    + transform.tileWidth()
                    * 0.78f;

            groundY =
                tileY
                    + transform.tileHeight()
                    * 0.22f;

        } else {

            centerX =
                tileX
                    + transform.tileWidth()
                    / 2f;

            groundY =
                tileY
                    + transform.tileHeight()
                    / 2f;
        }
        float centerY;

        if (sun.getSourcePlant() != null
            || sun.isGrounded()) {

            centerY = groundY;

        } else {
            BoardArea area =
                transform.getArea();

            float startY =
                area.y()
                    + area.height()
                    + SKY_START_OFFSET;

            float progress =
                sun.getFallProgress(
                    partialTick
                );

            centerY =
                Interpolation.smooth.apply(
                    startY,
                    groundY,
                    progress
                );
        }

        actor.setCenterPosition(
            centerX,
            centerY
        );
    }

    private void handleSunCollected(
        Sun sun
    ) {
        if (onSunClicked != null) {
            onSunClicked.accept(
                sun
            );
        }
    }

    private void removeMissingSuns(
        Set<Sun> activeSuns
    ) {
        Iterator<Map.Entry<Sun, SunActor>>
            iterator =
            sunActors
                .entrySet()
                .iterator();

        while (iterator.hasNext()) {

            Map.Entry<Sun, SunActor> entry =
                iterator.next();

            if (activeSuns.contains(
                entry.getKey()
            )) {
                continue;
            }

            SunActor actor =
                entry.getValue();

            if (actor.isTerminalVisual()) {

                if (actor.getParent() == null) {
                    iterator.remove();
                }

                continue;
            }

            actor.remove();
            iterator.remove();
        }
    }
}
