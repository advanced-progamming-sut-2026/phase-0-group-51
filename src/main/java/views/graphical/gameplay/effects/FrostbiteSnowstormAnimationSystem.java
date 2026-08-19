package views.graphical.gameplay.effects;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import models.Zombie.Behavior.SnowstormTransportBehavior;
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

public final class FrostbiteSnowstormAnimationSystem {

    public static final float DEFAULT_SCALE = 0.6f;

    private static final String REAR_PAM =
        "768/FULL/EFFECTS/SNOWSTORM_REAR/SNOWSTORM_REAR.PAM";

    private static final String TOP_PAM =
        "768/FULL/EFFECTS/SNOWSTORM_TOP/SNOWSTORM_TOP.PAM";

    private static final String INTRO = "intro";
    private static final String LOOP = "loop";
    private static final String OUTRO = "outro";

    private static final float SNOWSTORM_OFFSET_X = 0f;
    private static final float SNOWSTORM_OFFSET_Y = 10f;

    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final ZombieAnimationSystem zombieAnimationSystem;
    private final ChapterTheme theme;
    private final float scale;

    private final Map<Zombie, SnowstormVisual> visuals =
        new IdentityHashMap<>();

    public FrostbiteSnowstormAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        ZombieAnimationSystem zombieAnimationSystem,
        ChapterTheme theme
    ) {
        this(
            pamPlayer,
            worldStage,
            zombieAnimationSystem,
            theme,
            DEFAULT_SCALE
        );
    }

    public FrostbiteSnowstormAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        ZombieAnimationSystem zombieAnimationSystem,
        ChapterTheme theme,
        float scale
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

        this.scale = scale;

        if (theme == ChapterTheme.FROSTBITE_CAVES) {
            pamPlayer.loadSync(
                REAR_PAM
            );

            pamPlayer.loadSync(
                TOP_PAM
            );
        }
    }

    public void update(
        float delta,
        Collection<Zombie> zombies
    ) {
        if (theme != ChapterTheme.FROSTBITE_CAVES) {
            clear();
            return;
        }

        Collection<Zombie> safeZombies =
            zombies == null
                ? Collections.emptyList()
                : zombies;

        Set<Zombie> present =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

        for (Zombie zombie : safeZombies) {
            if (zombie == null) {
                continue;
            }

            present.add(
                zombie
            );

            SnowstormTransportBehavior transport =
                zombie.getBehavior(
                    SnowstormTransportBehavior.class
                );

            if (transport == null) {
                continue;
            }

            SnowstormVisual visual =
                visuals.get(
                    zombie
                );

            if (transport.isActive()
                && !zombie.isDead()) {

                Actor zombieActor =
                    zombieAnimationSystem.getActor(
                        zombie
                    );

                if (zombieActor == null) {
                    continue;
                }

                if (visual == null) {
                    visual =
                        createVisual();

                    visuals.put(
                        zombie,
                        visual
                    );
                }

                visual.follow(
                    zombieActor
                );
            } else if (visual != null) {
                visual.requestStop();
            }
        }

        Iterator<Map.Entry<Zombie, SnowstormVisual>> iterator =
            visuals.entrySet()
                .iterator();

        while (iterator.hasNext()) {
            Map.Entry<Zombie, SnowstormVisual> entry =
                iterator.next();

            Zombie zombie =
                entry.getKey();

            SnowstormVisual visual =
                entry.getValue();

            if (!present.contains(
                zombie
            )
                || zombie.isDead()) {
                visual.requestStop();
            }

            Actor zombieActor =
                zombieAnimationSystem.getActor(
                    zombie
                );

            if (zombieActor != null) {
                visual.follow(
                    zombieActor
                );
            }

            visual.update(
                delta
            );

            if (visual.isFinished()) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        for (SnowstormVisual visual : visuals.values()) {
            visual.removeImmediately();
        }

        visuals.clear();
    }

    public int getVisibleCount() {
        return visuals.size();
    }

    private SnowstormVisual createVisual() {
        PamAnimationActor rear =
            new PamAnimationActor(
                pamPlayer,
                REAR_PAM,
                INTRO,
                false
            );

        PamAnimationActor top =
            new PamAnimationActor(
                pamPlayer,
                TOP_PAM,
                INTRO,
                false
            );

        rear.setTouchable(
            Touchable.disabled
        );

        top.setTouchable(
            Touchable.disabled
        );

        rear.setScale(
            scale,
            scale
        );

        top.setScale(
            scale,
            scale
        );

        worldStage.addActor(
            rear
        );

        worldStage.addActor(
            top
        );

        rear.restart();
        top.restart();

        float introDuration =
            Math.max(
                safeDuration(
                    REAR_PAM,
                    INTRO,
                    0.20f
                ),
                safeDuration(
                    TOP_PAM,
                    INTRO,
                    0.20f
                )
            );

        float outroDuration =
            Math.max(
                safeDuration(
                    REAR_PAM,
                    OUTRO,
                    0.20f
                ),
                safeDuration(
                    TOP_PAM,
                    OUTRO,
                    0.20f
                )
            );

        return new SnowstormVisual(
            rear,
            top,
            introDuration,
            outroDuration
        );
    }

    private float safeDuration(
        String pamPath,
        String clip,
        float fallback
    ) {
        try {
            return Math.max(
                0.01f,
                pamPlayer.clipDurationSeconds(
                    pamPath,
                    clip
                )
            );
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private final class SnowstormVisual {

        private final PamAnimationActor rear;
        private final PamAnimationActor top;
        private final float introDuration;
        private final float outroDuration;

        private State state =
            State.INTRO;

        private float stateTime;
        private boolean stopRequested;
        private boolean finished;

        private SnowstormVisual(
            PamAnimationActor rear,
            PamAnimationActor top,
            float introDuration,
            float outroDuration
        ) {
            this.rear = rear;
            this.top = top;
            this.introDuration =
                introDuration;
            this.outroDuration =
                outroDuration;
        }

        private void follow(
            Actor zombieActor
        ) {
            if (finished
                || zombieActor == null) {
                return;
            }

            float x =
                zombieActor.getX()
                    + SNOWSTORM_OFFSET_X;

            float y =
                zombieActor.getY()
                    + SNOWSTORM_OFFSET_Y;

            rear.setPosition(
                x,
                y
            );

            top.setPosition(
                x,
                y
            );

            updateLayering(
                zombieActor
            );
        }

        private void update(
            float delta
        ) {
            if (finished) {
                return;
            }

            stateTime +=
                Math.max(
                    0f,
                    delta
                );

            switch (state) {
                case INTRO -> {
                    if (stateTime >= introDuration) {
                        if (stopRequested) {
                            playOutro();
                        } else {
                            playLoop();
                        }
                    }
                }

                case LOOP -> {
                    if (stopRequested) {
                        playOutro();
                    }
                }

                case OUTRO -> {
                    if (stateTime >= outroDuration) {
                        finish();
                    }
                }
            }
        }

        private void requestStop() {
            if (finished
                || state == State.OUTRO) {
                return;
            }

            stopRequested = true;

            if (state == State.LOOP) {
                playOutro();
            }
        }

        private void playLoop() {
            if (finished
                || state != State.INTRO) {
                return;
            }

            state =
                State.LOOP;

            stateTime = 0f;

            rear.play(
                LOOP,
                true
            );

            top.play(
                LOOP,
                true
            );

            rear.restart();
            top.restart();
        }

        private void playOutro() {
            if (finished
                || state == State.OUTRO) {
                return;
            }

            state =
                State.OUTRO;

            stateTime = 0f;

            rear.play(
                OUTRO,
                false
            );

            top.play(
                OUTRO,
                false
            );

            rear.restart();
            top.restart();
        }

        private void updateLayering(
            Actor zombieActor
        ) {
            if (zombieActor == null
                || zombieActor.getParent() == null
                || rear.getParent()
                != zombieActor.getParent()
                || top.getParent()
                != zombieActor.getParent()) {
                return;
            }

            Group parent =
                zombieActor.getParent();

            int targetIndex =
                zombieActor.getZIndex();

            rear.setZIndex(
                Math.max(
                    0,
                    targetIndex - 1
                )
            );

            targetIndex =
                zombieActor.getZIndex();

            int maxIndex =
                parent.getChildren()
                    .size - 1;

            top.setZIndex(
                Math.min(
                    maxIndex,
                    targetIndex + 1
                )
            );
        }

        private void finish() {
            if (finished) {
                return;
            }

            finished = true;

            rear.remove();
            top.remove();
        }

        private void removeImmediately() {
            if (finished) {
                return;
            }

            finished = true;

            rear.remove();
            top.remove();
        }

        private boolean isFinished() {
            return finished;
        }
    }

    private enum State {
        INTRO,
        LOOP,
        OUTRO
    }
}
