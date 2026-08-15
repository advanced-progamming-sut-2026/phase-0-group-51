package views.graphical.gameplay.zombie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import models.Zombie.ZombieType;
import models.games.ChapterTheme;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.EntityAnimationState;
import views.graphical.animation.PamAnimationActor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class ZombieLevelPreview {


    private static final float PREVIEW_SCALE = 0.43f;


    private static final float COLUMN_X_OFFSET = -110f;


    private static final float START_Y = 400f;


    private static final float Y_SPACING = 82f;


    private static final int MAX_ROWS = 5;


    private static final float BACK_COLUMN_X_SPACING = 45f;

    private static final float BACK_COLUMN_Y_OFFSET = 8f;

    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final ChapterTheme theme;
    private final float cameraRightX;

    private final ZombieAnimationResolver resolver;

    private final List<PamAnimationActor> actors =
        new ArrayList<>();

    public ZombieLevelPreview(
        PamPlayer pamPlayer,
        Stage worldStage,
        ChapterTheme theme,
        float cameraRightX
    ) {
        this.pamPlayer =
            Objects.requireNonNull(
                pamPlayer,
                "pamPlayer"
            );

        this.worldStage =
            Objects.requireNonNull(
                worldStage,
                "worldStage"
            );

        this.theme =
            Objects.requireNonNull(
                theme,
                "theme"
            );

        this.cameraRightX = cameraRightX;

        this.resolver =
            new ZombieAnimationResolver(
                pamPlayer
            );
    }

    public void show(
        List<ZombieType> zombieTypes
    ) {
        clear();

        if (zombieTypes == null
            || zombieTypes.isEmpty()) {

            return;
        }

        List<ZombieType> uniqueTypes =
            new ArrayList<>(
                new LinkedHashSet<>(
                    zombieTypes
                )
            );

        float frontColumnX =
            cameraRightX
                + COLUMN_X_OFFSET;
        for (
            int i = uniqueTypes.size() - 1;
            i >= 0;
            i--
        ) {

            ZombieType type =
                uniqueTypes.get(i);

            int row =
                i % MAX_ROWS;
            int column =
                i / MAX_ROWS;

            float x =
                frontColumnX
                    + column
                    * BACK_COLUMN_X_SPACING;

            float y =
                START_Y
                    - row
                    * Y_SPACING
                    + column
                    * BACK_COLUMN_Y_OFFSET;

            addPreviewActor(
                type,
                x,
                y
            );
        }
    }


    public void clear() {

        for (
            PamAnimationActor actor : actors
        ) {
            actor.remove();
        }

        actors.clear();

        resolver.clearCache();
    }

    public boolean isVisible() {
        return !actors.isEmpty();
    }

    private void addPreviewActor(
        ZombieType type,
        float x,
        float y
    ) {

        String alias =
            type.getAlias();

        String pamPath =
            ZombieAnimationSystem
                .resolvePamPath(
                    theme,
                    alias
                );

        if (pamPath == null
            || pamPath.isBlank()) {

            logSkip(
                alias,
                "no PAM path"
            );

            return;
        }

        try {

            ZombieAnimationResolver
                .ResolvedAnimations animations =
                resolver.resolve(
                    alias,
                    pamPath
                );

            String idleClip =
                animations.clip(
                    EntityAnimationState.IDLE
                );

            PamAnimationActor actor =
                new PamAnimationActor(
                    pamPlayer,
                    pamPath,
                    idleClip,
                    true
                );

            actor.setVisibleParts(
                ZombieAnimationSystem
                    .resolveVisibleParts(
                        pamPlayer,
                        pamPath,
                        alias
                    )
            );

            actor.setScale(
                PREVIEW_SCALE,
                PREVIEW_SCALE
            );

            actor.setPosition(
                x,
                y
            );

            actor.setPlaybackSpeed(
                0.85f
            );

            worldStage.addActor(
                actor
            );

            actors.add(
                actor
            );

        } catch (RuntimeException e) {

            if (Gdx.app != null) {

                Gdx.app.error(
                    "ZombiePreview",
                    "Could not create preview for "
                        + alias
                        + " ("
                        + pamPath
                        + ")",
                    e
                );
            }
        }
    }

    private static void logSkip(
        String alias,
        String reason
    ) {

        if (Gdx.app != null) {

            Gdx.app.log(
                "ZombiePreview",
                "Skipped "
                    + alias
                    + ": "
                    + reason
            );
        }
    }
}
