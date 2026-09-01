package views.graphical.gameplay.mower;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import models.games.ChapterTheme;
import models.games.GameState;
import models.items.Mower;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MowerAnimationSystem {
    public static final float DEFAULT_SCALE = 0.50f;
    public static final float MOWER_OFFSET_X = 0f;
    public static final float MOWER_OFFSET_Y = 0f;

    private static final String EGYPT_PAM =
        "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM";

    private static final String ICEAGE_PAM =
        "768/FULL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM";

    private static final String BEACH_PAM =
        "768/FULL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM";

    private static final String DARK_PAM =
        "768/FULL/MOWERS/MOWER_DARK/MOWER_DARK.PAM";

    private static final String TUTORIAL_PAM =
        "768/INITIAL/MOWERS/MOWER_TUTORIAL/MOWER_TUTORIAL.PAM";

    private static final String IDLE = "idle";
    private static final String TRANSITION = "transition";
    private static final String ATTACK = "attack";

    private static final float FALLBACK_TRANSITION_DURATION = 0.35f;

    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final BoardTransform boardTransform;
    private final ChapterTheme theme;
    private final float scale;

    private final Map<Mower, MowerVisual> visuals =
        new IdentityHashMap<>();

    private int lastObservedModelTick = Integer.MIN_VALUE;

    public MowerAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme
    ) {
        this(
            pamPlayer,
            worldStage,
            boardTransform,
            theme,
            DEFAULT_SCALE
        );
    }

    public MowerAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme,
        float scale
    ) {
        this.pamPlayer = Objects.requireNonNull(pamPlayer);
        this.worldStage = Objects.requireNonNull(worldStage);
        this.boardTransform = Objects.requireNonNull(boardTransform);
        this.theme = Objects.requireNonNull(theme);
        this.scale = scale;
    }

    public void update(
        float delta,
        float partialTick,
        int modelTick,
        GameState gameState
    ) {
        if (gameState == null || !gameState.isMowerEnabled()) {
            clear();
            return;
        }

        boolean firstUpdate =
            lastObservedModelTick == Integer.MIN_VALUE;

        boolean modelAdvanced =
            !firstUpdate
                && modelTick != lastObservedModelTick;

        partialTick = clamp(partialTick, 0f, 1f);

        Set<Mower> active = Collections.newSetFromMap(
            new IdentityHashMap<>()
        );

        Mower[] mowers = gameState.getLawnMowers();

        if (mowers != null) {
            for (Mower mower : mowers) {
                if (mower == null || mower.isDestroyed()) {
                    continue;
                }

                active.add(mower);

                MowerVisual visual = visuals.get(mower);

                if (visual == null) {
                    visual = createVisual(mower);

                    if (visual == null) {
                        continue;
                    }

                    visuals.put(mower, visual);
                    initializeModelPosition(mower, visual);
                } else {
                    sampleModelPosition(
                        mower,
                        visual,
                        modelAdvanced
                    );
                }

                updatePosition(
                    mower,
                    visual,
                    partialTick
                );

                updateAnimation(
                    mower,
                    visual,
                    delta
                );

                visual.actor.toFront();
            }
        }

        Iterator<Map.Entry<Mower, MowerVisual>> iterator =
            visuals.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Mower, MowerVisual> entry =
                iterator.next();

            if (active.contains(entry.getKey())) {
                continue;
            }

            entry.getValue().actor.remove();
            iterator.remove();
        }

        lastObservedModelTick = modelTick;
    }

    public void clear() {
        for (MowerVisual visual : visuals.values()) {
            visual.actor.remove();
        }

        visuals.clear();
        lastObservedModelTick = Integer.MIN_VALUE;
    }

    private MowerVisual createVisual(Mower mower) {
        String pamPath = resolvePamPath(theme);

        if (pamPath == null) {
            return null;
        }

        try {
            pamPlayer.loadSync(pamPath);

            List<String> clips = pamPlayer.clips(pamPath);

            String idle = requireClip(
                pamPath,
                clips,
                IDLE
            );

            String transition = requireClip(
                pamPath,
                clips,
                TRANSITION
            );

            String attack = requireClip(
                pamPath,
                clips,
                ATTACK
            );

            PamAnimationActor actor =
                new PamAnimationActor(
                    pamPlayer,
                    pamPath,
                    idle,
                    true
                );

            actor.setScale(scale, scale);
            worldStage.addActor(actor);

            float transitionDuration = safeDuration(
                pamPath,
                transition,
                FALLBACK_TRANSITION_DURATION
            );

            return new MowerVisual(
                actor,
                idle,
                transition,
                attack,
                transitionDuration
            );
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "MowerAnimation",
                    "Could not create mower visual for row "
                        + (mower.getRowNumber() + 1),
                    exception
                );
            }

            return null;
        }
    }

    private void updateAnimation(
        Mower mower,
        MowerVisual visual,
        float delta
    ) {
        if (!mower.isActivated()) {
            visual.activationSeen = false;
            visual.transitioning = false;
            visual.transitionElapsed = 0f;
            visual.actor.play(visual.idleClip, true);
            return;
        }

        if (!visual.activationSeen) {
            visual.activationSeen = true;
            visual.transitioning = true;
            visual.transitionElapsed = 0f;

            visual.actor.play(
                visual.transitionClip,
                false
            );
            visual.actor.restart();
            return;
        }

        if (visual.transitioning) {
            visual.transitionElapsed += Math.max(0f, delta);

            if (visual.transitionElapsed
                < visual.transitionDuration) {
                return;
            }

            visual.transitioning = false;
            visual.actor.play(
                visual.attackClip,
                true
            );
            visual.actor.restart();
            return;
        }

        visual.actor.play(
            visual.attackClip,
            true
        );
    }

    private void initializeModelPosition(
        Mower mower,
        MowerVisual visual
    ) {
        float modelX = mower.getX();
        visual.previousModelX = modelX;
        visual.currentModelX = modelX;
        visual.positionInitialized = true;
    }

    private void sampleModelPosition(
        Mower mower,
        MowerVisual visual,
        boolean modelAdvanced
    ) {
        float modelX = mower.getX();

        if (!visual.positionInitialized) {
            initializeModelPosition(mower, visual);
            return;
        }

        if (!modelAdvanced) {
            if (Math.abs(modelX - visual.currentModelX) > 0.0001f) {
                visual.previousModelX = modelX;
                visual.currentModelX = modelX;
            }
            return;
        }

        visual.previousModelX = visual.currentModelX;
        visual.currentModelX = modelX;
    }

    private void updatePosition(
        Mower mower,
        MowerVisual visual,
        float partialTick
    ) {
        float renderX =
            visual.previousModelX
                + (
                visual.currentModelX
                    - visual.previousModelX
            ) * partialTick;

        float x =
            boardTransform.getArea().x()
                + (renderX + 0.5f)
                * boardTransform.tileWidth()
                + MOWER_OFFSET_X;

        float y =
            boardTransform.tileY(
                mower.getRowNumber()
            )
                + boardTransform.tileHeight()
                * 0.5f
                + MOWER_OFFSET_Y;

        visual.actor.setPosition(x, y);
    }

    private String resolvePamPath(ChapterTheme theme) {
        return switch (theme) {
            case ANCIENT_EGYPT -> EGYPT_PAM;
            case FROSTBITE_CAVES -> ICEAGE_PAM;
            case BIG_WAVE_BEACH -> BEACH_PAM;
            case DARK_AGES -> DARK_PAM;
            default -> TUTORIAL_PAM;
        };
    }

    private String requireClip(
        String pamPath,
        List<String> clips,
        String expected
    ) {
        if (clips != null) {
            for (String clip : clips) {
                if (clip != null
                    && clip.equalsIgnoreCase(expected)) {
                    return clip;
                }
            }

            String normalizedExpected = normalize(expected);

            for (String clip : clips) {
                if (clip != null
                    && normalize(clip).equals(normalizedExpected)) {
                    return clip;
                }
            }
        }

        throw new IllegalStateException(
            "Missing mower clip '"
                + expected
                + "' in "
                + pamPath
                + ". Available clips: "
                + clips
        );
    }

    private float safeDuration(
        String pamPath,
        String clip,
        float fallback
    ) {
        try {
            return Math.max(
                0.05f,
                pamPlayer.clipDurationSeconds(
                    pamPath,
                    clip
                )
            );
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    private static float clamp(
        float value,
        float min,
        float max
    ) {
        return Math.max(
            min,
            Math.min(max, value)
        );
    }

    private static final class MowerVisual {
        private final PamAnimationActor actor;
        private final String idleClip;
        private final String transitionClip;
        private final String attackClip;
        private final float transitionDuration;

        private float previousModelX;
        private float currentModelX;
        private boolean positionInitialized;

        private boolean activationSeen;
        private boolean transitioning;
        private float transitionElapsed;

        private MowerVisual(
            PamAnimationActor actor,
            String idleClip,
            String transitionClip,
            String attackClip,
            float transitionDuration
        ) {
            this.actor = actor;
            this.idleClip = idleClip;
            this.transitionClip = transitionClip;
            this.attackClip = attackClip;
            this.transitionDuration = transitionDuration;
        }
    }
}
