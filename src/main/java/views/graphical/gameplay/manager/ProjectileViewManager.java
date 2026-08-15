package views.graphical.gameplay.manager;

import com.badlogic.gdx.scenes.scene2d.Group;
import graphics.PvzGame;
import models.projectile.Projectile;
import views.graphical.gameplay.actors.ProjectileActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ProjectileViewManager extends Group {

    private final PvzGame game;
    private final BoardTransform transform;

    private final Map<Projectile, ProjectileActor>
            projectileActors =
            new IdentityHashMap<>();

    public ProjectileViewManager(
            PvzGame game,
            BoardTransform transform
    ) {
        this.game = game;
        this.transform = transform;
    }

    public void sync(
            Iterable<Projectile> projectiles
    ) {
        Set<Projectile> active =
                Collections.newSetFromMap(
                        new IdentityHashMap<>()
                );

        for (Projectile projectile : projectiles) {

            if (projectile.isMarkedForRemoval()) {
                continue;
            }

            active.add(
                    projectile
            );

            ProjectileActor actor =
                    projectileActors.get(
                            projectile
                    );

            if (actor == null) {

                actor =
                        new ProjectileActor(
                                game,
                                projectile
                        );

                projectileActors.put(
                        projectile,
                        actor
                );

                addActor(actor);
            }

            positionProjectile(
                    projectile,
                    actor
            );
        }

        removeMissingProjectiles(
                active
        );
    }

    private void positionProjectile(
            Projectile projectile,
            ProjectileActor actor
    ) {
        BoardArea area =
                transform.getArea();

        float x =
                area.x()
                        + ((float) projectile.getPosX() + 0.5f)
                        * transform.tileWidth();

        float y =
                area.y()
                        + (
                        BoardTransform.ROWS
                                - 1f
                                - (float) projectile.getPosY()
                                + 0.5f
                )
                        * transform.tileHeight();

        y +=
                (float) projectile.getVisualArcOffset()
                        * transform.tileHeight();

        actor.setProjectilePosition(
                x,
                y
        );
    }

    private void removeMissingProjectiles(
            Set<Projectile> active
    ) {
        Iterator<Map.Entry<Projectile, ProjectileActor>>
                iterator =
                projectileActors
                        .entrySet()
                        .iterator();

        while (iterator.hasNext()) {

            Map.Entry<Projectile, ProjectileActor>
                    entry =
                    iterator.next();

            if (!active.contains(
                    entry.getKey()
            )) {

                entry.getValue().remove();
                iterator.remove();
            }
        }
    }
}