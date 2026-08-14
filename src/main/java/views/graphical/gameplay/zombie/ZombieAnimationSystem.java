package views.graphical.gameplay.zombie;

import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import models.Zombie.Zombie;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.EntityAnimationState;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public final class ZombieAnimationSystem {

    public static final float DEFAULT_SCALE = 0.45f;
    private static final float MIN_DEATH_DURATION = 0.05f;

    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final BoardTransform boardTransform;
    private final ZombieAnimationResolver resolver;
    private final float scale;

    private final Map<Zombie, ZombieVisual> visuals =
        new IdentityHashMap<>();

    public ZombieAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform
    ) {
        this(
            pamPlayer,
            worldStage,
            boardTransform,
            DEFAULT_SCALE
        );
    }

    public ZombieAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        float scale
    ) {
        this.pamPlayer = Objects.requireNonNull(pamPlayer, "pamPlayer");
        this.worldStage = Objects.requireNonNull(worldStage, "worldStage");
        this.boardTransform = Objects.requireNonNull(
            boardTransform,
            "boardTransform"
        );
        this.scale = scale;
        this.resolver = new ZombieAnimationResolver(pamPlayer);
    }

    public void update(
        float delta,
        Collection<Zombie> zombies
    ) {
        Collection<Zombie> safeZombies =
            zombies == null ? Collections.emptyList() : zombies;

        Set<Zombie> active = Collections.newSetFromMap(
            new IdentityHashMap<>()
        );

        for (Zombie zombie : safeZombies) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            active.add(zombie);

            ZombieVisual visual = visuals.get(zombie);
            if (visual == null) {
                visual = createVisual(zombie);
                if (visual == null) {
                    continue;
                }
                visuals.put(zombie, visual);
            }

            updateLivingZombie(zombie, visual);
        }

        Iterator<Map.Entry<Zombie, ZombieVisual>> iterator =
            visuals.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Zombie, ZombieVisual> entry = iterator.next();
            Zombie zombie = entry.getKey();
            ZombieVisual visual = entry.getValue();

            if (active.contains(zombie)) {
                continue;
            }

            if (zombie.isDead()) {
                if (!visual.deathStarted) {
                    beginDeath(visual);
                }

                visual.deathElapsed += Math.max(0f, delta);
                if (visual.deathElapsed < visual.deathDuration) {
                    continue;
                }
            }

            visual.actor.remove();
            iterator.remove();
        }
    }

    public void clear() {
        for (ZombieVisual visual : visuals.values()) {
            visual.actor.remove();
        }
        visuals.clear();
    }

    public int getVisibleZombieCount() {
        return visuals.size();
    }

    private ZombieVisual createVisual(Zombie zombie) {
        String alias = zombie.getAlias();
        String pamPath = ZombieRegistry.getIdlePamPath(alias);

        if (pamPath == null || pamPath.isBlank()) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "No PAM path configured for zombie: " + alias
                );
            }
            return null;
        }

        try {
            ZombieAnimationResolver.ResolvedAnimations animations =
                resolver.resolve(alias, pamPath);

            PamAnimationActor actor = new PamAnimationActor(
                pamPlayer,
                pamPath,
                animations.clip(EntityAnimationState.WALK),
                true
            );

            actor.setVisibleParts(
                ZombieRegistry.getIdleVisibleParts(alias)
            );
            actor.setScale(scale, scale);
            actor.setTouchable(
                com.badlogic.gdx.scenes.scene2d.Touchable.disabled
            );

            worldStage.addActor(actor);

            ZombieVisual visual = new ZombieVisual(actor, animations);
            updatePosition(zombie, actor);
            return visual;
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Failed to create PAM actor for " + alias
                        + " (" + pamPath + ")",
                    e
                );
            }
            return null;
        }
    }

    private void updateLivingZombie(
        Zombie zombie,
        ZombieVisual visual
    ) {
        updatePosition(zombie, visual.actor);

        EntityAnimationState state = zombie.isEating()
            ? EntityAnimationState.EAT
            : EntityAnimationState.WALK;

        visual.actor.play(
            visual.animations.clip(state),
            true
        );

        visual.actor.setPlaybackSpeed(
            zombie.isChilled() ? 0.5f : 1f
        );

        if (zombie.isFrozen() || zombie.isButtered()) {
            visual.actor.pauseAnimation();
        } else {
            visual.actor.resumeAnimation();
        }
    }

    private void updatePosition(
        Zombie zombie,
        PamAnimationActor actor
    ) {
        float x = boardTransform.getArea().x()
            + (zombie.getX() + 0.5f)
            * boardTransform.tileWidth();

        float y = boardTransform.tileY(zombie.getLane())
            + boardTransform.tileHeight() * 0.5f;

        actor.setPosition(x, y);
        float scaleX = zombie.getDirection() >= 0
            ? scale
            : -scale;

        actor.setScale(scaleX, scale);
    }

    private void beginDeath(ZombieVisual visual) {
        visual.deathStarted = true;
        visual.deathElapsed = 0f;

        String deathClip = visual.animations.clip(
            EntityAnimationState.DEATH
        );

        visual.actor.resumeAnimation();
        visual.actor.setPlaybackSpeed(1f);
        visual.actor.play(deathClip, false);
        visual.actor.restart();

        try {
            visual.deathDuration = Math.max(
                MIN_DEATH_DURATION,
                pamPlayer.clipDurationSeconds(
                    visual.animations.getPamPath(),
                    deathClip
                )
            );
        } catch (RuntimeException ignored) {
            visual.deathDuration = 0.5f;
        }
    }

    private static final class ZombieVisual {
        private final PamAnimationActor actor;
        private final ZombieAnimationResolver.ResolvedAnimations animations;

        private boolean deathStarted;
        private float deathElapsed;
        private float deathDuration;

        private ZombieVisual(
            PamAnimationActor actor,
            ZombieAnimationResolver.ResolvedAnimations animations
        ) {
            this.actor = actor;
            this.animations = animations;
        }
    }
}
