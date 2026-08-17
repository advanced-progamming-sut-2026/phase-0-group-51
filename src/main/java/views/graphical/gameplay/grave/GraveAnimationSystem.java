package views.graphical.gameplay.grave;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Group;
import models.Board.Board;
import models.Board.Tile;
import models.games.ChapterTheme;
import models.games.ancientEgypt.Grave;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GraveAnimationSystem extends Group {

    public static final float DEFAULT_SCALE = 0.55f;

    private static final float GRAVE_Y_OFFSET = 1f;
    private static final float STATE_HOLD_MARGIN = 0.08f;

    private static final String EGYPT_NORMAL_PAM =
        "768/INITIAL/GRAVESTONES/EGYPT_HIEROGLYPH/EGYPT_HIEROGLYPH.PAM";

    private static final String DARK_NORMAL_PAM =
        "768/FULL/GRAVESTONES/DARK_NOOP/DARK_NOOP.PAM";

    private static final String DARK_PLANT_FOOD_PAM =
        "768/FULL/GRAVESTONES/DARK_PLANTFOOD/DARK_PLANTFOOD.PAM";

    private static final String DARK_SUN_PAM =
        "768/FULL/GRAVESTONES/DARK_SUN/DARK_SUN.PAM";

    private static final String GRAVE_BUSTER_EXPLOSION_PAM =
        "768/INITIAL/EFFECTS/GRAVEBUSTER_EXPLOSION_POTATOMINE/GRAVEBUSTER_EXPLOSION_POTATOMINE.PAM";

    private static final String STATE_UNDAMAGED = "undamaged";
    private static final String STATE_DAMAGE_1 = "damage1";
    private static final String STATE_DAMAGE_2 = "damage2";
    private static final String STATE_DAMAGE_3 = "damage3";
    private static final String STATE_DAMAGE_4 = "damage4";

    private final PamPlayer pamPlayer;
    private final BoardTransform transform;
    private final ChapterTheme theme;
    private final float scale;

    private final Map<Grave, GraveVisual> visuals =
        new IdentityHashMap<>();

    private final Set<Grave> visuallyDestroyed =
        Collections.newSetFromMap(
            new IdentityHashMap<>()
        );

    private final List<EffectVisual> effects =
        new ArrayList<>();

    private final Map<String, GraveClips> graveClipCache =
        new HashMap<>();

    private final Map<String, List<String>> effectClipCache =
        new HashMap<>();

    public GraveAnimationSystem(
        PamPlayer pamPlayer,
        BoardTransform transform,
        ChapterTheme theme
    ) {
        this(
            pamPlayer,
            transform,
            theme,
            DEFAULT_SCALE
        );
    }

    public GraveAnimationSystem(
        PamPlayer pamPlayer,
        BoardTransform transform,
        ChapterTheme theme,
        float scale
    ) {
        this.pamPlayer =
            Objects.requireNonNull(
                pamPlayer,
                "pamPlayer"
            );

        this.transform =
            Objects.requireNonNull(
                transform,
                "transform"
            );

        this.theme =
            Objects.requireNonNull(
                theme,
                "theme"
            );

        this.scale = scale;

        setTransform(false);
    }

    public void sync(Board board) {
        if (board == null) {
            clearVisuals();
            return;
        }

        Set<Grave> active =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

        Set<Grave> present =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

        for (
            int lane = 0;
            lane < board.getLaneCount();
            lane++
        ) {
            for (
                int column = 0;
                column < board.getColumnCount();
                column++
            ) {
                Tile tile =
                    board.getTile(
                        lane,
                        column
                    );

                if (tile == null
                    || !tile.hasGrave()) {
                    continue;
                }

                Grave grave =
                    tile.getGrave();

                if (grave == null) {
                    continue;
                }

                present.add(grave);

                if (grave.getHealth() <= 0) {
                    if (visuallyDestroyed.add(grave)) {
                        GraveVisual deadVisual =
                            visuals.remove(grave);

                        if (deadVisual != null) {
                            deadVisual.remove();
                        }

                    }

                    continue;
                }

                visuallyDestroyed.remove(grave);
                active.add(grave);

                String pamPath =
                    resolveGravePamPath(
                        grave
                    );

                if (pamPath == null
                    || pamPath.isBlank()) {
                    continue;
                }

                GraveVisual visual =
                    visuals.get(grave);

                if (visual == null
                    || !pamPath.equals(
                    visual.pamPath
                )) {

                    if (visual != null) {
                        visual.remove();
                    }

                    visual =
                        createGraveVisual(
                            grave,
                            pamPath,
                            lane,
                            column
                        );

                    if (visual == null) {
                        continue;
                    }

                    visuals.put(
                        grave,
                        visual
                    );
                }

                visual.lane = lane;
                visual.column = column;

                positionActor(
                    visual.actor,
                    lane,
                    column
                );

                syncDamageState(
                    grave,
                    visual
                );

            }
        }

        Iterator<Map.Entry<Grave, GraveVisual>> iterator =
            visuals
                .entrySet()
                .iterator();

        while (iterator.hasNext()) {
            Map.Entry<Grave, GraveVisual> entry =
                iterator.next();

            if (active.contains(
                entry.getKey()
            )) {
                continue;
            }

            GraveVisual visual =
                entry.getValue();

            int lane =
                visual.lane;

            int column =
                visual.column;

            visual.remove();
            iterator.remove();

        }

        visuallyDestroyed.retainAll(
            present
        );

        sortByDepth();
    }

    @Override
    public void act(float delta) {
        float safeDelta =
            Math.max(
                0f,
                delta
            );

        for (
            GraveVisual visual :
            visuals.values()
        ) {
            if (visual.stateFrozen) {
                continue;
            }

            if (visual.actor.getStateTime()
                + safeDelta
                < visual.stateHoldTime) {
                continue;
            }

            visual.actor.pauseAnimation();
            visual.stateFrozen = true;
        }

        for (
            EffectVisual effect :
            effects
        ) {
            if (effect.frozen) {
                continue;
            }

            float nextStateTime =
                effect.actor.getStateTime()
                    + safeDelta
                    * effect.actor.getPlaybackSpeed();

            if (nextStateTime
                >= effect.freezeAt) {
                effect.actor.pauseAnimation();
                effect.frozen = true;
            }
        }

        super.act(delta);

        Iterator<EffectVisual> iterator =
            effects.iterator();

        while (iterator.hasNext()) {
            EffectVisual effect =
                iterator.next();

            if (!effect.frozen
                && effect.actor.getStateTime()
                >= effect.freezeAt) {
                effect.actor.pauseAnimation();
                effect.frozen = true;
            }

            if (!effect.frozen) {
                continue;
            }

            effect.holdElapsed += safeDelta;

            if (effect.holdElapsed
                < effect.holdSeconds) {
                continue;
            }

            effect.actor.remove();
            iterator.remove();
        }
    }

    public void clearVisuals() {
        for (
            GraveVisual visual :
            visuals.values()
        ) {
            visual.remove();
        }

        visuals.clear();
        visuallyDestroyed.clear();

        for (
            EffectVisual effect :
            effects
        ) {
            effect.actor.remove();
        }

        effects.clear();
        graveClipCache.clear();
        effectClipCache.clear();
        clearChildren();
    }

    public int getVisibleGraveCount() {
        return visuals.size();
    }

    private GraveVisual createGraveVisual(
        Grave grave,
        String pamPath,
        int lane,
        int column
    ) {
        try {
            GraveClips clips =
                loadGraveClips(
                    pamPath
                );

            int maxHealth =
                Math.max(
                    1,
                    grave.getHealth()
                );

            int damageStage =
                resolveDamageStage(
                    grave.getHealth(),
                    maxHealth
                );

            String clip =
                clips.forStage(
                    damageStage
                );

            PamAnimationActor actor =
                new PamAnimationActor(
                    pamPlayer,
                    pamPath,
                    clip,
                    false
                );

            forceAllPartsVisible(
                pamPath,
                actor
            );

            actor.setScale(
                scale,
                scale
            );

            positionActor(
                actor,
                lane,
                column
            );

            addActor(actor);

            return new GraveVisual(
                grave,
                pamPath,
                clips,
                actor,
                lane,
                column,
                maxHealth,
                damageStage,
                stateHoldTime(
                    pamPath,
                    clip
                )
            );

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "GraveAnimation",
                    "Could not create grave visual for "
                        + pamPath,
                    e
                );
            }

            return null;
        }
    }

    private void syncDamageState(
        Grave grave,
        GraveVisual visual
    ) {
        int damageStage =
            resolveDamageStage(
                grave.getHealth(),
                visual.maxHealth
            );

        if (damageStage
            == visual.damageStage) {
            return;
        }

        visual.damageStage =
            damageStage;

        String clip =
            visual.clips.forStage(
                damageStage
            );

        visual.actor.resumeAnimation();

        visual.actor.play(
            clip,
            false
        );

        visual.actor.restart();

        visual.stateHoldTime =
            stateHoldTime(
                visual.pamPath,
                clip
            );

        visual.stateFrozen = false;
    }

    private static int resolveDamageStage(
        int health,
        int maxHealth
    ) {
        if (health <= 0) {
            return 4;
        }

        float ratio =
            health
                / (float) Math.max(
                1,
                maxHealth
            );

        if (ratio > 0.80f) {
            return 0;
        }

        if (ratio > 0.60f) {
            return 1;
        }

        if (ratio > 0.40f) {
            return 2;
        }

        if (ratio > 0.20f) {
            return 3;
        }

        return 4;
    }

    public void playExplosionEffect(
        int lane,
        int column
    ) {
        playOneShotEffect(
            GRAVE_BUSTER_EXPLOSION_PAM,
            lane,
            column,
            0.6f,
            "explosion",
            "explode",
            "effect",
            "attack",
            "animation",
            "anim"
        );
    }

    private void playOneShotEffect(
        String pamPath,
        int lane,
        int column,
        float fallbackDuration,
        String... clipCandidates
    ) {
        try {
            List<String> clips =
                loadEffectClips(
                    pamPath
                );

            String clip =
                findClip(
                    clips,
                    clipCandidates
                );

            if (clip == null) {
                clip = clips.get(0);
            }

            PamAnimationActor actor =
                new PamAnimationActor(
                    pamPlayer,
                    pamPath,
                    clip,
                    false
                );

            forceAllPartsVisible(
                pamPath,
                actor
            );

            actor.setScale(
                scale,
                scale
            );

            positionActor(
                actor,
                lane,
                column
            );

            addActor(actor);

            float duration =
                safeDuration(
                    pamPath,
                    clip,
                    fallbackDuration
                );

            effects.add(
                new EffectVisual(
                    actor,
                    Math.max(
                        0.01f,
                        duration - 0.03f
                    ),
                    0.15f
                )
            );

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "GraveAnimation",
                    "Could not create grave effect: "
                        + pamPath,
                    e
                );
            }
        }
    }

    private String resolveGravePamPath(
        Grave grave
    ) {
        if (theme
            == ChapterTheme.ANCIENT_EGYPT) {
            return EGYPT_NORMAL_PAM;
        }

        if (theme
            == ChapterTheme.DARK_AGES) {

            if (grave.isHasPlantFood()) {
                return DARK_PLANT_FOOD_PAM;
            }

            if (grave.isHasSun()) {
                return DARK_SUN_PAM;
            }

            return DARK_NORMAL_PAM;
        }

        return null;
    }

    private GraveClips loadGraveClips(
        String pamPath
    ) {
        GraveClips cached =
            graveClipCache.get(
                pamPath
            );

        if (cached != null) {
            return cached;
        }

        pamPlayer.loadSync(
            pamPath
        );

        List<String> available =
            pamPlayer.clips(
                pamPath
            );

        if (available == null
            || available.isEmpty()) {
            throw new IllegalStateException(
                "Grave PAM has no clips: "
                    + pamPath
            );
        }

        List<String> clips =
            List.copyOf(
                available
            );

        String undamaged =
            requireClip(
                pamPath,
                clips,
                STATE_UNDAMAGED
            );

        String damage1 =
            requireClip(
                pamPath,
                clips,
                STATE_DAMAGE_1
            );

        String damage2 =
            requireClip(
                pamPath,
                clips,
                STATE_DAMAGE_2
            );

        String damage3 =
            requireClip(
                pamPath,
                clips,
                STATE_DAMAGE_3
            );

        String damage4 =
            requireClip(
                pamPath,
                clips,
                STATE_DAMAGE_4
            );

        GraveClips result =
            new GraveClips(
                undamaged,
                damage1,
                damage2,
                damage3,
                damage4
            );

        graveClipCache.put(
            pamPath,
            result
        );

        if (Gdx.app != null) {
            Gdx.app.log(
                "GraveAnimation",
                pamPath
                    + " -> undamaged="
                    + undamaged
                    + ", damage1="
                    + damage1
                    + ", damage2="
                    + damage2
                    + ", damage3="
                    + damage3
                    + ", damage4="
                    + damage4
            );
        }

        return result;
    }

    private List<String> loadEffectClips(
        String pamPath
    ) {
        List<String> cached =
            effectClipCache.get(
                pamPath
            );

        if (cached != null) {
            return cached;
        }

        pamPlayer.loadSync(
            pamPath
        );

        List<String> available =
            pamPlayer.clips(
                pamPath
            );

        if (available == null
            || available.isEmpty()) {
            throw new IllegalStateException(
                "Effect PAM has no clips: "
                    + pamPath
            );
        }

        List<String> clips =
            List.copyOf(
                available
            );

        effectClipCache.put(
            pamPath,
            clips
        );

        if (Gdx.app != null) {
            Gdx.app.log(
                "GraveAnimation",
                pamPath
                    + " -> effect clips="
                    + clips
            );
        }

        return clips;
    }

    private static String requireClip(
        String pamPath,
        List<String> clips,
        String expected
    ) {
        String clip =
            findClip(
                clips,
                expected
            );

        if (clip != null) {
            return clip;
        }

        throw new IllegalStateException(
            "Missing grave state '"
                + expected
                + "' in "
                + pamPath
                + ". Available clips: "
                + clips
        );
    }

    private void forceAllPartsVisible(
        String pamPath,
        PamAnimationActor actor
    ) {
        try {
            PamPlayer.AnimationPart root =
                pamPlayer.getParts(
                    pamPath
                );

            if (root == null) {
                return;
            }

            forcePartTreeVisible(
                root,
                actor
            );

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "GraveAnimation",
                    "Could not force effect parts visible for "
                        + pamPath,
                    e
                );
            }
        }
    }

    private void forcePartTreeVisible(
        PamPlayer.AnimationPart part,
        PamAnimationActor actor
    ) {
        if (part == null) {
            return;
        }

        if (part.name != null
            && !part.name.isBlank()) {
            actor.getVisibilityMap().put(
                part.name,
                true
            );
        }

        if (part.children == null) {
            return;
        }

        for (
            PamPlayer.AnimationPart child :
            part.children
        ) {
            forcePartTreeVisible(
                child,
                actor
            );
        }
    }

    private float stateHoldTime(
        String pamPath,
        String clip
    ) {
        float duration =
            safeDuration(
                pamPath,
                clip,
                0.5f
            );

        float margin =
            Math.min(
                STATE_HOLD_MARGIN,
                duration * 0.2f
            );

        return Math.max(
            0.01f,
            duration - margin
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

    private static String findClip(
        List<String> clips,
        String... candidates
    ) {
        for (
            String candidate :
            candidates
        ) {
            if (candidate == null
                || candidate.isBlank()) {
                continue;
            }

            for (
                String clip :
                clips
            ) {
                if (clip.equalsIgnoreCase(
                    candidate
                )) {
                    return clip;
                }
            }
        }

        for (
            String candidate :
            candidates
        ) {
            String wanted =
                normalize(
                    candidate
                );

            if (wanted.isEmpty()) {
                continue;
            }

            for (
                String clip :
                clips
            ) {
                if (normalize(
                    clip
                ).equals(
                    wanted
                )) {
                    return clip;
                }
            }
        }

        return null;
    }

    private static String normalize(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .toLowerCase(
                Locale.ROOT
            )
            .replaceAll(
                "[^a-z0-9]",
                ""
            );
    }

    private void positionActor(
        PamAnimationActor actor,
        int lane,
        int column
    ) {
        float x =
            transform.tileX(
                column
            )
                + transform.tileWidth()
                * 0.5f;

        float y =
            transform.tileY(
                lane
            )
                + transform.tileHeight()
                * 0.5f
                + GRAVE_Y_OFFSET;

        actor.setPosition(
            x,
            y
        );
    }

    private void sortByDepth() {
        getChildren().sort(
            (first, second) ->
                Float.compare(
                    second.getY(),
                    first.getY()
                )
        );
    }

    private static final class GraveClips {
        private final String undamaged;
        private final String damage1;
        private final String damage2;
        private final String damage3;
        private final String damage4;

        private GraveClips(
            String undamaged,
            String damage1,
            String damage2,
            String damage3,
            String damage4
        ) {
            this.undamaged = undamaged;
            this.damage1 = damage1;
            this.damage2 = damage2;
            this.damage3 = damage3;
            this.damage4 = damage4;
        }

        private String forStage(
            int stage
        ) {
            return switch (stage) {
                case 1 -> damage1;
                case 2 -> damage2;
                case 3 -> damage3;
                case 4 -> damage4;
                default -> undamaged;
            };
        }
    }

    private static final class GraveVisual {
        private final Grave grave;
        private final String pamPath;
        private final GraveClips clips;
        private final PamAnimationActor actor;
        private final int maxHealth;

        private int lane;
        private int column;
        private int damageStage;
        private float stateHoldTime;
        private boolean stateFrozen;

        private GraveVisual(
            Grave grave,
            String pamPath,
            GraveClips clips,
            PamAnimationActor actor,
            int lane,
            int column,
            int maxHealth,
            int damageStage,
            float stateHoldTime
        ) {
            this.grave = grave;
            this.pamPath = pamPath;
            this.clips = clips;
            this.actor = actor;
            this.lane = lane;
            this.column = column;
            this.maxHealth = maxHealth;
            this.damageStage = damageStage;
            this.stateHoldTime = stateHoldTime;
            this.stateFrozen = false;
        }

        private void remove() {
            actor.remove();
        }
    }

    private static final class EffectVisual {
        private final PamAnimationActor actor;
        private final float freezeAt;
        private final float holdSeconds;

        private boolean frozen;
        private float holdElapsed;

        private EffectVisual(
            PamAnimationActor actor,
            float freezeAt,
            float holdSeconds
        ) {
            this.actor = actor;
            this.freezeAt = freezeAt;
            this.holdSeconds = holdSeconds;
            this.frozen = false;
            this.holdElapsed = 0f;
        }
    }
}
