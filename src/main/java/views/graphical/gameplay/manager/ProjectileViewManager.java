package views.graphical.gameplay.manager;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ProjectileReleaseData;
import Data.loader.ProjectileVisualData;
import com.badlogic.gdx.scenes.scene2d.Group;
import graphics.PvzGame;
import models.Plant.Plant;
import models.projectile.Projectile;
import views.graphical.gameplay.actors.PlantActor;
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

    private final Map<Projectile, ProjectileActor> projectileActors =
            new IdentityHashMap<>();

    public ProjectileViewManager(
            PvzGame game,
            BoardTransform transform
    ) {
        this.game = game;
        this.transform = transform;
    }

    public void sync(
            Iterable<Projectile> projectiles,
            float partialTick
    ) {
        Set<Projectile> active =
                Collections.newSetFromMap(new IdentityHashMap<>());

        for (Projectile projectile : projectiles) {
            if (projectile.isMarkedForRemoval() || !projectile.isLaunched()) {
                continue;
            }

            ProjectileVisualData visual = resolveVisual(projectile);
            ProjectileReleaseData release = resolveRelease(projectile, visual);

            if (visual == null || release == null) {
                continue;
            }

            active.add(projectile);

            ProjectileActor actor = projectileActors.get(projectile);
            if (actor == null) {
                actor = new ProjectileActor(game, projectile, visual);
                projectileActors.put(projectile, actor);
                addActor(actor);
            }

            positionProjectile(
                    projectile,
                    actor,
                    release,
                    partialTick
            );
        }

        removeMissingProjectiles(active);
    }

    private ProjectileVisualData resolveVisual(Projectile projectile) {
        Plant source = projectile.getSourcePlant();
        if (source == null) {
            return null;
        }

        PlantData plantData = PlantRegistry.getById(source.getId());
        if (plantData == null) {
            plantData = PlantRegistry.getByName(source.getName());
        }
        if (plantData == null || !plantData.hasProjectiles()) {
            return null;
        }
        return plantData.resolveProjectile(
                projectile.getVisualProjectileKey()
        );
    }

    private ProjectileReleaseData resolveRelease(
            Projectile projectile,
            ProjectileVisualData visual
    ) {
        if (visual == null || visual.releases() == null || visual.releases().isEmpty()) {
            return null;
        }

        return visual.releases().stream()
                .filter(release -> release.id() == projectile.getVisualReleaseId())
                .findFirst()
                .orElse(visual.releases().getFirst());
    }

    private void positionProjectile(
            Projectile projectile,
            ProjectileActor actor,
            ProjectileReleaseData release,
            float partialTick
    ) {
        BoardArea area = transform.getArea();

        double renderPosX =
                projectile.getRenderPosX(partialTick);

        double renderPosY =
                projectile.getRenderPosY(partialTick);

        float x =
                area.x()
                        + ((float) renderPosX + 0.5f)
                        * transform.tileWidth();

        float y =
                area.y()
                        + (
                        BoardTransform.ROWS
                                - 1f
                                - (float) renderPosY
                                + 0.5f
                )
                        * transform.tileHeight();

        float offsetFactor =
                launchOffsetFactor(
                        projectile,
                        renderPosX
                );

        x += release.offsetX()
                * PlantActor.BOARD_SCALE
                * offsetFactor;

        y += release.offsetY()
                * PlantActor.BOARD_SCALE
                * offsetFactor;

        y += (float) projectile.getRenderArcOffset(partialTick)
                * transform.tileHeight();

        actor.setProjectilePosition(x, y);
    }

    private float launchOffsetFactor(
            Projectile projectile,
            double renderPosX
    ) {
        Double targetX = projectile.getTargetX();
        Plant source = projectile.getSourcePlant();

        if (targetX == null || source == null) {
            return 1f;
        }

        double totalDistance = targetX - source.getPosX();
        if (Math.abs(totalDistance) < 0.0001) {
            return 0f;
        }

        double progress =
                (renderPosX - source.getPosX())
                        / totalDistance;

        progress = Math.max(0.0, Math.min(1.0, progress));
        return (float) (1.0 - progress);
    }

    private void removeMissingProjectiles(Set<Projectile> active) {
        Iterator<Map.Entry<Projectile, ProjectileActor>> iterator =
                projectileActors.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Projectile, ProjectileActor> entry = iterator.next();

            if (!active.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }
    }
}
