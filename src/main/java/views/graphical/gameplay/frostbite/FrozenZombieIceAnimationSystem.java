package views.graphical.gameplay.frostbite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public final class FrozenZombieIceAnimationSystem {

    private static final String ICE_FRONT_PAM =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_ZOMBIE/"
            + "FROSTBITE_ICE_BLOCK_ZOMBIE.PAM";

    private static final String ICE_BEHIND_PAM =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_ZOMBIE_BEHIND/"
            + "FROSTBITE_ICE_BLOCK_ZOMBIE_BEHIND.PAM";

    private static final String ICE_CLIP = "idle";

    private static final float ICE_SCALE_X = 1.2f;
    private static final float ICE_SCALE_Y = 1.1f;
    private static final float ICE_OFFSET_X = 0f;
    private static final float ICE_OFFSET_Y = 0f;

    private static final float DAMAGE_FLASH_DURATION = 0.15f;
    private static final float DAMAGE_FLASH_ALPHA = 0.65f;
    private static final float DAMAGE_FLASH_COOLDOWN = 0.4f;

    private final PamPlayer pamPlayer;
    private final ZombieAnimationSystem zombieAnimationSystem;
    private final ChapterTheme theme;

    private final Map<Zombie, IceVisual> visuals =
        new IdentityHashMap<>();

    private boolean loadAttempted;
    private boolean loaded;

    public FrozenZombieIceAnimationSystem(
        PamPlayer pamPlayer,
        ZombieAnimationSystem zombieAnimationSystem,
        ChapterTheme theme
    ) {
        this.pamPlayer =
            Objects.requireNonNull(
                pamPlayer,
                "pamPlayer"
            );

        this.zombieAnimationSystem =
            Objects.requireNonNull(
                zombieAnimationSystem,
                "zombieAnimationSystem"
            );

        this.theme =
            Objects.requireNonNull(
                theme,
                "theme"
            );

        if (theme == ChapterTheme.FROSTBITE_CAVES) {
            loadPams();
        }
    }

    public void sync(
        Collection<Zombie> zombies,
        float delta
    ) {
        if (theme != ChapterTheme.FROSTBITE_CAVES) {
            clear();
            return;
        }

        if (!loadAttempted) {
            loadPams();
        }

        if (!loaded) {
            clear();
            return;
        }

        Collection<Zombie> safeZombies =
            zombies == null
                ? Collections.emptyList()
                : zombies;

        Set<Zombie> active =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

        for (Zombie zombie : safeZombies) {
            if (zombie == null
                || zombie.isDead()) {
                continue;
            }

            if (!zombie.hasIceShell()) {

                IceVisual oldVisual =
                    visuals.remove(zombie);

                if (oldVisual != null) {
                    oldVisual.remove();
                }

                continue;
            }

            PamAnimationActor zombieActor =
                zombieAnimationSystem.getActor(
                    zombie
                );

            if (zombieActor == null
                || zombieActor.getParent() == null) {
                continue;
            }

            active.add(zombie);

            IceVisual visual =
                visuals.get(
                    zombie
                );

            if (visual == null) {
                visual =
                    createVisual();

                if (visual == null) {
                    continue;
                }

                visual.lastIceShellHealth =
                    zombie.getIceShellHealth();

                visuals.put(
                    zombie,
                    visual
                );
            }

            updateDamageFlash(
                zombie,
                visual,
                delta
            );

            syncTransform(
                zombieActor,
                visual
            );

            syncLayerOrder(
                zombieActor,
                visual
            );
        }

        removeInactive(
            active
        );
    }

    public void clear() {
        for (IceVisual visual : visuals.values()) {
            visual.remove();
        }

        visuals.clear();
    }

    public int getVisibleFrozenZombieCount() {
        return visuals.size();
    }

    private void loadPams() {
        loadAttempted = true;
        loaded = false;

        try {
            pamPlayer.loadSync(
                ICE_BEHIND_PAM
            );

            pamPlayer.loadSync(
                ICE_FRONT_PAM
            );

            Rectangle behindBounds =
                pamPlayer.bounds(
                    ICE_BEHIND_PAM,
                    ICE_CLIP
                );

            Rectangle frontBounds =
                pamPlayer.bounds(
                    ICE_FRONT_PAM,
                    ICE_CLIP
                );

            if (!hasDrawableBounds(behindBounds)) {
                throw new IllegalStateException(
                    "Behind ice PAM clip '"
                        + ICE_CLIP
                        + "' has invalid bounds."
                );
            }

            if (!hasDrawableBounds(frontBounds)) {
                throw new IllegalStateException(
                    "Front ice PAM clip '"
                        + ICE_CLIP
                        + "' has invalid bounds."
                );
            }

            loaded = true;

            if (Gdx.app != null) {
                Gdx.app.log(
                    "FrozenZombieIce",
                    "Loaded frozen-zombie ice PAMs. "
                        + "behindBounds="
                        + behindBounds
                        + ", frontBounds="
                        + frontBounds
                        + ", behindClips="
                        + pamPlayer.clips(
                        ICE_BEHIND_PAM
                    )
                        + ", frontClips="
                        + pamPlayer.clips(
                        ICE_FRONT_PAM
                    )
                );
            }

        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "FrozenZombieIce",
                    "Could not load frozen-zombie ice PAMs.",
                    exception
                );
            }
        }
    }

    private IceVisual createVisual() {
        try {
            PamAnimationActor behind =
                new PamAnimationActor(
                    pamPlayer,
                    ICE_BEHIND_PAM,
                    ICE_CLIP,
                    true
                );

            PamAnimationActor front =
                new PamAnimationActor(
                    pamPlayer,
                    ICE_FRONT_PAM,
                    ICE_CLIP,
                    true
                );

            behind.setTouchable(
                Touchable.disabled
            );

            front.setTouchable(
                Touchable.disabled
            );

            behind.restart();
            front.restart();
            behind.getColor().a = 1f;
            front.getColor().a = 0.65f;

            return new IceVisual(
                behind,
                front
            );

        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "FrozenZombieIce",
                    "Could not create frozen-zombie ice actors.",
                    exception
                );
            }

            return null;
        }
    }

    private void updateDamageFlash(
        Zombie zombie,
        IceVisual visual,
        float delta
    ) {
        if (visual.damageFlashCooldownRemaining > 0f) {
            visual.damageFlashCooldownRemaining = Math.max(
                0f,
                visual.damageFlashCooldownRemaining
                    - Math.max(0f, delta)
            );
        }

        int currentIceHealth =
            zombie.getIceShellHealth();

        if (currentIceHealth < visual.lastIceShellHealth
            && currentIceHealth > 0
            && visual.damageFlashCooldownRemaining <= 0f) {
            visual.behind.flashAdditive(
                DAMAGE_FLASH_DURATION,
                DAMAGE_FLASH_ALPHA
            );

            visual.front.flashAdditive(
                DAMAGE_FLASH_DURATION,
                DAMAGE_FLASH_ALPHA
            );

            visual.damageFlashCooldownRemaining =
                DAMAGE_FLASH_COOLDOWN;
        }

        visual.lastIceShellHealth =
            currentIceHealth;
    }

    private void syncTransform(
        PamAnimationActor zombieActor,
        IceVisual visual
    ) {
        float x =
            zombieActor.getX()
                + ICE_OFFSET_X;

        float y =
            zombieActor.getY()
                + ICE_OFFSET_Y;

        float scaleX =
            zombieActor.getScaleX()
                * ICE_SCALE_X;

        float scaleY =
            zombieActor.getScaleY()
                * ICE_SCALE_Y;

        visual.behind.setPosition(
            x,
            y
        );

        visual.front.setPosition(
            x,
            y
        );

        visual.behind.setScale(
            scaleX,
            scaleY
        );

        visual.front.setScale(
            scaleX,
            scaleY
        );
    }

    private void syncLayerOrder(
        PamAnimationActor zombieActor,
        IceVisual visual
    ) {
        Group parent =
            zombieActor.getParent();

        if (parent == null) {
            return;
        }

        visual.behind.remove();
        visual.front.remove();

        int zombieIndex =
            zombieActor.getZIndex();

        parent.addActorAt(
            zombieIndex,
            visual.behind
        );

        int frontIndex =
            zombieActor.getZIndex() + 1;

        parent.addActorAt(
            frontIndex,
            visual.front
        );
    }

    private void removeInactive(
        Set<Zombie> active
    ) {
        Iterator<Map.Entry<Zombie, IceVisual>> iterator =
            visuals.entrySet()
                .iterator();

        while (iterator.hasNext()) {
            Map.Entry<Zombie, IceVisual> entry =
                iterator.next();

            if (active.contains(
                entry.getKey()
            )) {
                continue;
            }

            entry.getValue()
                .remove();

            iterator.remove();
        }
    }

    private static boolean hasDrawableBounds(
        Rectangle bounds
    ) {
        return bounds != null
            && bounds.width > 0f
            && bounds.height > 0f;
    }

    private static final class IceVisual {

        private final PamAnimationActor behind;
        private final PamAnimationActor front;
        private int lastIceShellHealth = Integer.MIN_VALUE;
        private float damageFlashCooldownRemaining;

        private IceVisual(
            PamAnimationActor behind,
            PamAnimationActor front
        ) {
            this.behind = behind;
            this.front = front;
        }

        private void remove() {
            behind.remove();
            front.remove();
        }
    }
}

