package views.graphical.gameplay.zombie;

import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ChapterTheme;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.EntityAnimationState;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ZombieAnimationSystem {

    public static final float DEFAULT_SCALE = 0.57f;

    private static final String GROUND_PART = "ground_swatch";

    private static final float MIN_DEATH_DURATION = 0.05f;
    private static final float MIN_STEP_DISTANCE = 0.001f;
    private static final float MIN_WALK_PLAYBACK_SPEED = 0.10f;
    private static final float MAX_WALK_PLAYBACK_SPEED = 5.00f;
    private static final float POSITION_EPSILON = 0.0001f;
    private static final float MAX_INTERPOLATION_STEP_COLUMNS = 0.75f;

    private static final String EGYPT_BASIC_PAM =
        "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";
    private static final String ICEAGE_BASIC_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_BASIC/ZOMBIE_ICEAGE_BASIC.PAM";
    private static final String BEACH_BASIC_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_BEACH_BASIC/ZOMBIE_BEACH_BASIC.PAM";
    private static final String DARK_BASIC_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM";

    private static final String EGYPT_GARGANTUAR_PAM =
        "768/INITIAL/ZOMBIE/EGYPT_GARGANTUAR/EGYPT_GARGANTUAR.PAM";
    private static final String ICEAGE_GARGANTUAR_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_GARGANTUAR/ZOMBIE_ICEAGE_GARGANTUAR.PAM";
    private static final String BEACH_GARGANTUAR_PAM =
        "768/FULL/ZOMBIE/BEACH_GARGANTUAR/BEACH_GARGANTUAR.PAM";
    private static final String DARK_GARGANTUAR_PAM =
        "768/FULL/ZOMBIE/DARK_GARGANTUAR/DARK_GARGANTUAR.PAM";

    private static final String EGYPT_IMP_PAM =
        "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_IMP/ZOMBIE_EGYPT_IMP.PAM";
    private static final String ICEAGE_IMP_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_IMP/ZOMBIE_ICEAGE_IMP.PAM";
    private static final String BEACH_IMP_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_BEACH_IMP_MERMAID/ZOMBIE_BEACH_IMP_MERMAID.PAM";
    private static final String DARK_IMP_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_MONK/ZOMBIE_DARK_IMP_MONK.PAM";

    private final ChapterTheme theme;
    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final BoardTransform boardTransform;
    private final ZombieAnimationResolver resolver;
    private final float scale;

    private final Map<Zombie, ZombieVisual> visuals =
        new IdentityHashMap<>();

    private int lastObservedModelTick = Integer.MIN_VALUE;

    public ZombieAnimationSystem(
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

    public ZombieAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme,
        float scale
    ) {
        this.pamPlayer = Objects.requireNonNull(pamPlayer, "pamPlayer");
        this.worldStage = Objects.requireNonNull(worldStage, "worldStage");
        this.boardTransform = Objects.requireNonNull(boardTransform, "boardTransform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.scale = scale;
        this.resolver = new ZombieAnimationResolver(pamPlayer);
    }

    public void update(
        float delta,
        float partialTick,
        int modelTick,
        Collection<Zombie> zombies
    ) {
        Collection<Zombie> safeZombies =
            zombies == null
                ? Collections.emptyList()
                : zombies;

        boolean firstUpdate =
            lastObservedModelTick == Integer.MIN_VALUE;

        boolean modelAdvanced =
            !firstUpdate
                && modelTick != lastObservedModelTick;

        partialTick = clamp(
            partialTick,
            0f,
            1f
        );

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

            sampleModelPosition(
                zombie,
                visual,
                modelAdvanced
            );

            updateLivingZombie(
                zombie,
                visual,
                partialTick
            );
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

        updateZombieDrawOrder();

        lastObservedModelTick = modelTick;
    }

    public void clear() {
        for (ZombieVisual visual : visuals.values()) {
            visual.actor.remove();
        }

        visuals.clear();
        resolver.clearCache();
        lastObservedModelTick = Integer.MIN_VALUE;
    }

    public int getVisibleZombieCount() {
        return visuals.size();
    }

    private void updateZombieDrawOrder() {
        List<ZombieVisual> drawOrder =
            new ArrayList<>(visuals.values());

        drawOrder.sort(
            (a, b) -> Float.compare(
                b.actor.getY(),
                a.actor.getY()
            )
        );

        for (ZombieVisual visual : drawOrder) {
            visual.actor.toFront();
        }
    }

    private ZombieVisual createVisual(Zombie zombie) {
        String alias = zombie.getAlias();
        String pamPath = resolvePamPath(theme, alias);

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

            String walkClip = animations.clip(
                EntityAnimationState.WALK
            );

            PamAnimationActor actor = new PamAnimationActor(
                pamPlayer,
                pamPath,
                walkClip,
                true
            );

            actor.setVisibleParts(
                resolveVisibleParts(pamPlayer, pamPath, alias)
            );

            if (ZombieType.EXPLORER.getAlias().equals(alias)) {
                actor.getVisibilityMap().put(
                    "zombie_egyptflag_hand_inner3b",
                    false
                );

                actor.getVisibilityMap().put(
                    "zombie_egypt_hand_inner_01",
                    false
                );

                actor.getVisibilityMap().put(
                    "zombie_cowboy_hand_inner_01",
                    false
                );

                actor.getVisibilityMap().put(
                    "zombie_hand_outer_01",
                    false
                );

                actor.getVisibilityMap().put(
                    "zombie_hand_outer_02",
                    false
                );

                actor.getVisibilityMap().put(
                    "zombie_expl_arm_outer_upper_02",
                    false
                );

                actor.getVisibilityMap().put(
                    "_particles",
                    false
                );
            }

            actor.setScale(scale, scale);
            configureGroundSwatch(alias, pamPath, walkClip, actor);

            worldStage.addActor(actor);

            ZombieVisual visual = new ZombieVisual(
                actor,
                animations
            );

            initializeModelPosition(
                zombie,
                visual
            );

            updatePosition(
                zombie,
                visual,
                1f
            );

            return visual;

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Failed to create PAM actor for "
                        + alias
                        + " ("
                        + pamPath
                        + ")",
                    e
                );
            }

            return null;
        }
    }

    public static String resolvePamPath(
        ChapterTheme theme,
        String alias
    ) {
        Objects.requireNonNull(theme, "theme");

        if (usesThemedBasicBody(alias)) {
            return switch (theme) {
                case ANCIENT_EGYPT -> EGYPT_BASIC_PAM;
                case FROSTBITE_CAVES -> ICEAGE_BASIC_PAM;
                case BIG_WAVE_BEACH -> BEACH_BASIC_PAM;
                case DARK_AGES -> DARK_BASIC_PAM;
                default -> ZombieRegistry.getIdlePamPath(alias);
            };
        }

        if (ZombieType.GARGANTUAR.getAlias().equals(alias)) {
            return switch (theme) {
                case ANCIENT_EGYPT -> EGYPT_GARGANTUAR_PAM;
                case FROSTBITE_CAVES -> ICEAGE_GARGANTUAR_PAM;
                case BIG_WAVE_BEACH -> BEACH_GARGANTUAR_PAM;
                case DARK_AGES -> DARK_GARGANTUAR_PAM;
                default -> ZombieRegistry.getIdlePamPath(alias);
            };
        }

        if (ZombieType.IMP.getAlias().equals(alias)) {
            return switch (theme) {
                case ANCIENT_EGYPT -> EGYPT_IMP_PAM;
                case FROSTBITE_CAVES -> ICEAGE_IMP_PAM;
                case BIG_WAVE_BEACH -> BEACH_IMP_PAM;
                case DARK_AGES -> DARK_IMP_PAM;
                default -> ZombieRegistry.getIdlePamPath(alias);
            };
        }

        return ZombieRegistry.getIdlePamPath(alias);
    }

    public static String resolvePamPath(
        ChapterTheme theme,
        ZombieType type
    ) {
        if (type == null) {
            return null;
        }
        return resolvePamPath(theme, type.getAlias());
    }

    private static boolean usesThemedBasicBody(String alias) {
        return ZombieType.DEFAULT.getAlias().equals(alias)
            || ZombieType.ARMOR_1.getAlias().equals(alias)
            || ZombieType.ARMOR_2.getAlias().equals(alias)
            || ZombieType.ARMOR_4.getAlias().equals(alias);
    }

    public static List<String> resolveVisibleParts(
        PamPlayer pamPlayer,
        String pamPath,
        String alias
    ) {
        LinkedHashSet<String> parts = new LinkedHashSet<>(
            ZombieRegistry.getIdleVisibleParts(alias)
        );

        int armorTier = armorTier(alias);
        if (armorTier <= 0 || pamPlayer == null || pamPath == null) {
            return List.copyOf(parts);
        }

        try {
            PamPlayer.AnimationPart root = pamPlayer.getParts(pamPath);
            if (root != null) {
                addNormalArmorBranch(root, armorTier, parts);
            }
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not resolve themed armor parts for "
                        + alias
                        + " in "
                        + pamPath,
                    e
                );
            }
        }

        if (Gdx.app != null) {
            Gdx.app.log(
                "ZombieAnimation",
                "Visible parts " + alias + " -> " + parts
            );
        }

        return List.copyOf(parts);
    }

    private static int armorTier(String alias) {
        if (ZombieType.ARMOR_1.getAlias().equals(alias)) {
            return 1;
        }
        if (ZombieType.ARMOR_2.getAlias().equals(alias)) {
            return 2;
        }
        if (ZombieType.ARMOR_4.getAlias().equals(alias)) {
            return 4;
        }
        return 0;
    }

    private static void addNormalArmorBranch(
        PamPlayer.AnimationPart root,
        int armorTier,
        Set<String> output
    ) {
        String tierToken = "armor" + armorTier;

        List<PamPlayer.AnimationPart> all = new ArrayList<>();
        flattenParts(root, all);

        PamPlayer.AnimationPart stateRoot = null;
        int bestRootScore = Integer.MIN_VALUE;

        for (PamPlayer.AnimationPart part : all) {
            String name = normalizePartName(part.name);
            if (!name.contains(tierToken)) {
                continue;
            }

            int score = 0;
            if (name.contains("states")) score += 100;
            if (!part.resource) score += 20;
            if (name.endsWith(tierToken)) score += 5;

            if (score > bestRootScore) {
                bestRootScore = score;
                stateRoot = part;
            }
        }

        if (stateRoot == null) {
            return;
        }

        if (stateRoot.name != null && !stateRoot.name.isBlank()) {
            output.add(stateRoot.name);
        }

        List<PamPlayer.AnimationPart> branch = findBestNormalBranch(stateRoot);
        for (PamPlayer.AnimationPart part : branch) {
            if (part.name != null && !part.name.isBlank()) {
                output.add(part.name);
            }
        }
    }

    private static List<PamPlayer.AnimationPart> findBestNormalBranch(
        PamPlayer.AnimationPart stateRoot
    ) {
        List<PamPlayer.AnimationPart> bestPath = List.of();
        int bestScore = Integer.MIN_VALUE;

        for (PamPlayer.AnimationPart child : stateRoot.children) {
            List<PamPlayer.AnimationPart> candidate = new ArrayList<>();
            collectBestPath(child, candidate);

            int score = 0;
            for (PamPlayer.AnimationPart part : candidate) {
                String name = normalizePartName(part.name);
                if (name.contains("norm") || name.contains("normal")) score += 100;
                if (name.contains("undamaged") || name.contains("healthy")) score += 80;
                if (name.contains("idle")) score += 20;
                if (name.contains("damage") || name.contains("broken") || name.contains("crack")) score -= 80;
            }

            if (score > bestScore) {
                bestScore = score;
                bestPath = candidate;
            }
        }

        return bestPath;
    }

    private static void collectBestPath(
        PamPlayer.AnimationPart node,
        List<PamPlayer.AnimationPart> path
    ) {
        path.add(node);

        if (node.children.isEmpty()) {
            return;
        }

        PamPlayer.AnimationPart preferred = node.children.get(0);
        int bestScore = partPreferenceScore(preferred);

        for (int i = 1; i < node.children.size(); i++) {
            PamPlayer.AnimationPart candidate = node.children.get(i);
            int score = partPreferenceScore(candidate);
            if (score > bestScore) {
                bestScore = score;
                preferred = candidate;
            }
        }

        if (!preferred.resource || preferred.children.size() > 0) {
            collectBestPath(preferred, path);
        } else {
            path.add(preferred);
        }
    }

    private static int partPreferenceScore(
        PamPlayer.AnimationPart part
    ) {
        String name = normalizePartName(part.name);
        int score = part.resource ? 0 : 10;

        if (name.contains("norm") || name.contains("normal")) score += 100;
        if (name.contains("undamaged") || name.contains("healthy")) score += 80;
        if (name.contains("idle")) score += 20;
        if (name.contains("damage") || name.contains("broken") || name.contains("crack")) score -= 80;

        return score;
    }

    private static void flattenParts(
        PamPlayer.AnimationPart part,
        List<PamPlayer.AnimationPart> output
    ) {
        output.add(part);
        for (PamPlayer.AnimationPart child : part.children) {
            flattenParts(child, output);
        }
    }

    private static String normalizePartName(String value) {
        if (value == null) {
            return "";
        }
        return value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    private void configureGroundSwatch(
        String alias,
        String pamPath,
        String walkClip,
        PamAnimationActor actor
    ) {
        try {
            ClipRef walkRef = pamPlayer.getClip(
                pamPath,
                walkClip
            );

            if (walkRef == null) {
                logGroundingUnavailable(
                    alias,
                    "walk ClipRef is null"
                );
                return;
            }

            Rectangle[] groundFrames =
                pamPlayer.partBoundsByFrame(
                    walkRef,
                    GROUND_PART
                );

            actor.setGroundingCurve(
                walkClip,
                groundFrames,
                walkRef.duration
            );

            if (!actor.hasGrounding()) {
                logGroundingUnavailable(
                    alias,
                    "part '" + GROUND_PART
                        + "' was missing or had too few usable frames"
                );
                return;
            }

            if (Gdx.app != null) {
                Gdx.app.log(
                    "ZombieAnimation",
                    "Grounding " + alias
                        + " -> part=" + GROUND_PART
                        + ", frames="
                        + actor.getGroundingFrameCount()
                        + ", nativeStep="
                        + actor.getGroundingStepDistanceCanvas()
                        + ", duration="
                        + actor.getGroundingDuration()
                );
            }

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not bake " + GROUND_PART
                        + " for " + alias,
                    e
                );
            }
        }
    }

    private void initializeModelPosition(
        Zombie zombie,
        ZombieVisual visual
    ) {
        float modelX = zombie.getX();

        visual.previousModelX = modelX;
        visual.currentModelX = modelX;
        visual.positionInitialized = true;
    }

    private void sampleModelPosition(
        Zombie zombie,
        ZombieVisual visual,
        boolean modelAdvanced
    ) {
        float modelX = zombie.getX();

        if (!visual.positionInitialized) {
            initializeModelPosition(
                zombie,
                visual
            );
            return;
        }

        if (!modelAdvanced) {
            if (Math.abs(
                modelX - visual.currentModelX
            ) > POSITION_EPSILON) {
                visual.previousModelX = modelX;
                visual.currentModelX = modelX;
            }
            return;
        }

        float movement =
            modelX - visual.currentModelX;

        if (Math.abs(movement)
            > MAX_INTERPOLATION_STEP_COLUMNS) {
            visual.previousModelX = modelX;
            visual.currentModelX = modelX;
            return;
        }

        visual.previousModelX =
            visual.currentModelX;

        visual.currentModelX = modelX;
    }

    private void updateLivingZombie(
        Zombie zombie,
        ZombieVisual visual,
        float partialTick
    ) {
        PamAnimationActor actor = visual.actor;

        updatePosition(
            zombie,
            visual,
            partialTick
        );

        EntityAnimationState state = zombie.isEating()
            ? EntityAnimationState.EAT
            : EntityAnimationState.WALK;

        actor.play(
            visual.animations.clip(state),
            true
        );

        if (state == EntityAnimationState.WALK) {
            actor.setPlaybackSpeed(
                calculateWalkPlaybackSpeed(
                    zombie,
                    actor
                )
            );
        } else {
            actor.setPlaybackSpeed(1f);
        }

        if (zombie.isFrozen() || zombie.isButtered()) {
            actor.pauseAnimation();
        } else {
            actor.resumeAnimation();
        }
    }

    private void updatePosition(
        Zombie zombie,
        ZombieVisual visual,
        float partialTick
    ) {
        PamAnimationActor actor = visual.actor;

        float renderX =
            visual.previousModelX
                + (
                visual.currentModelX
                    - visual.previousModelX
            ) * partialTick;

        float x =
            boardTransform.getArea().x()
                + (renderX + 0.5f)
                * boardTransform.tileWidth();

        float y =
            boardTransform.tileY(zombie.getLane())
                + boardTransform.tileHeight()
                * 0.5f;

        actor.setPosition(x, y);

        float scaleX =
            zombie.getDirection() >= 0
                ? scale
                : -scale;

        actor.setScale(scaleX, scale);
    }

    private float calculateWalkPlaybackSpeed(
        Zombie zombie,
        PamAnimationActor actor
    ) {
        if (!actor.hasGrounding()) {
            return zombie.isChilled() ? 0.5f : 1f;
        }

        float stepDistanceWorld =
            actor.getGroundingStepDistanceWorld();

        float duration = actor.getGroundingDuration();

        if (stepDistanceWorld <= MIN_STEP_DISTANCE
            || duration <= 0f) {
            return zombie.isChilled() ? 0.5f : 1f;
        }

        float movementSpeedColumnsPerSecond =
            Math.abs(
                zombie.getBaseSpeed()
                    * zombie.getSpeedMultiplier()
            );

        if (zombie.isChilled()) {
            movementSpeedColumnsPerSecond *= 0.5f;
        }

        float movementSpeedWorld =
            movementSpeedColumnsPerSecond
                * boardTransform.tileWidth();

        if (movementSpeedWorld <= 0f) {
            return 0f;
        }

        float playbackSpeed =
            movementSpeedWorld
                * duration
                / stepDistanceWorld;

        return clamp(
            playbackSpeed,
            MIN_WALK_PLAYBACK_SPEED,
            MAX_WALK_PLAYBACK_SPEED
        );
    }

    private void beginDeath(ZombieVisual visual) {
        visual.deathStarted = true;
        visual.deathElapsed = 0f;

        String deathClip = visual.animations.clip(
            EntityAnimationState.DEATH
        );

        visual.actor.clearGroundingKeepingVisualPosition();

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

    private static float clamp(
        float value,
        float min,
        float max
    ) {
        return Math.max(min, Math.min(max, value));
    }

    private static void logGroundingUnavailable(
        String alias,
        String reason
    ) {
        if (Gdx.app != null) {
            Gdx.app.log(
                "ZombieAnimation",
                "Grounding disabled for "
                    + alias
                    + ": "
                    + reason
            );
        }
    }

    private static final class ZombieVisual {
        private final PamAnimationActor actor;
        private final ZombieAnimationResolver.ResolvedAnimations animations;

        private float previousModelX;
        private float currentModelX;
        private boolean positionInitialized;

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
