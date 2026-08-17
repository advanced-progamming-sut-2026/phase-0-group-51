package views.graphical.gameplay.zombie;

import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.Zombie.Behavior.AuraBehavior;
import models.Zombie.Behavior.DamageReactionBehavior;
import models.Zombie.Behavior.DynamiteBehavior;
import models.Zombie.Behavior.ImpThrowBehavior;
import models.Zombie.Behavior.InstantKillBehavior;
import models.Zombie.Behavior.MovementBehavior;
import models.Zombie.Behavior.PushObjectBehavior;
import models.Zombie.Behavior.RangedAttackBehavior;
import models.Zombie.Behavior.SunStealBehavior;
import models.Zombie.Behavior.TransformBehavior;
import models.Zombie.Behavior.TurquoiseLaserBehavior;
import models.Zombie.Behavior.WorldEffectBehavior;
import models.games.ChapterTheme;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.EntityAnimationState;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
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

    public static final float DEFAULT_SCALE = 0.6f;

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
        Collection<Zombie> zombies
    ) {
        int compatibilityTick =
            lastObservedModelTick == Integer.MIN_VALUE
                ? 0
                : lastObservedModelTick + 1;

        update(
            delta,
            1f,
            compatibilityTick,
            zombies
        );
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

            if (modelAdvanced) {
                detectBehaviorTransitions(
                    zombie,
                    visual
                );
            }

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

            initializeBehaviorState(
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

        if (!updateSpecialClip(visual)) {
            BaseAnimation base = resolveBaseAnimation(
                zombie,
                visual
            );

            actor.play(
                base.clip,
                true
            );

            if (base.walkSpeedSynced) {
                actor.setPlaybackSpeed(
                    calculateWalkPlaybackSpeed(
                        zombie,
                        actor
                    )
                );
            } else {
                actor.setPlaybackSpeed(1f);
            }
        }

        if (zombie.isFrozen() || zombie.isButtered()) {
            actor.pauseAnimation();
        } else {
            actor.resumeAnimation();
        }
    }

    private BaseAnimation resolveBaseAnimation(
        Zombie zombie,
        ZombieVisual visual
    ) {
        EntityAnimationState fallbackState = zombie.isEating()
            ? EntityAnimationState.EAT
            : EntityAnimationState.WALK;

        String alias = zombie.getAlias();

        if (ZombieType.NEWSPAPER.getAlias().equals(alias)) {
            DamageReactionBehavior reaction =
                zombie.getBehavior(DamageReactionBehavior.class);

            if (reaction != null && !reaction.isRaged()) {
                String clip = clipOrFallback(
                    visual,
                    zombie.isEating()
                        ? "eat_newspaper"
                        : "walk_newspaper",
                    fallbackState
                );

                return new BaseAnimation(
                    clip,
                    !zombie.isEating()
                );
            }
        }

        if (ZombieType.MODERN_ALL_STAR.getAlias().equals(alias)) {
            InstantKillBehavior contact =
                zombie.getBehavior(InstantKillBehavior.class);

            if (contact != null
                && !contact.isHasKilled()
                && contact.getRunningSpeedScale() > 0f) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "run",
                        EntityAnimationState.WALK
                    ),
                    false
                );
            }
        }

        if (ZombieType.DARK_JUGGLER.getAlias().equals(alias)) {
            DamageReactionBehavior reaction =
                zombie.getBehavior(DamageReactionBehavior.class);

            if (reaction != null && reaction.isSpinning()) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "spin_walk",
                        EntityAnimationState.WALK
                    ),
                    false
                );
            }
        }

        if (ZombieType.ARCADE.getAlias().equals(alias)) {
            PushObjectBehavior push =
                zombie.getBehavior(PushObjectBehavior.class);

            if (push != null && push.hasObject()) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "push",
                        EntityAnimationState.WALK
                    ),
                    false
                );
            }
        }

        if (ZombieType.BARREL_ROLLER.getAlias().equals(alias)) {
            PushObjectBehavior push =
                zombie.getBehavior(PushObjectBehavior.class);

            if (push != null && !push.hasObject()) {
                String clip = clipOrFallback(
                    visual,
                    zombie.isEating() ? "eat2" : "walk2",
                    fallbackState
                );

                return new BaseAnimation(
                    clip,
                    false
                );
            }
        }

        if (ZombieType.ICE_AGE_TROGLOBITE.getAlias().equals(alias)) {
            PushObjectBehavior push =
                zombie.getBehavior(PushObjectBehavior.class);

            if (push != null
                && !push.getPushedFrozenZombies().isEmpty()) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "push",
                        EntityAnimationState.WALK
                    ),
                    false
                );
            }
        }

        if (ZombieType.PIANO.getAlias().equals(alias)) {
            return new BaseAnimation(
                clipOrFallback(
                    visual,
                    "play",
                    EntityAnimationState.IDLE
                ),
                false
            );
        }

        if (ZombieType.RA.getAlias().equals(alias)) {
            SunStealBehavior sunSteal =
                zombie.getBehavior(SunStealBehavior.class);

            if (sunSteal != null && sunSteal.isStealing()) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "power",
                        EntityAnimationState.SPECIAL
                    ),
                    false
                );
            }
        }

        if (ZombieType.DARK_KING.getAlias().equals(alias)) {
            return new BaseAnimation(
                visual.animations.clip(EntityAnimationState.IDLE),
                false
            );
        }

        if (ZombieType.BEACH_FISHERMAN.getAlias().equals(alias)) {
            return new BaseAnimation(
                visual.animations.clip(EntityAnimationState.IDLE),
                false
            );
        }

        if (ZombieType.CRYSTAL_SKULL.getAlias().equals(alias)) {
            TurquoiseLaserBehavior laser =
                zombie.getBehavior(TurquoiseLaserBehavior.class);

            if (laser != null && laser.suppressesMovement(zombie)) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "power",
                        EntityAnimationState.SPECIAL
                    ),
                    false
                );
            }
        }

        String clip = visual.animations.clip(fallbackState);
        return new BaseAnimation(
            clip,
            fallbackState == EntityAnimationState.WALK
        );
    }

    private void initializeBehaviorState(
        Zombie zombie,
        ZombieVisual visual
    ) {
        DamageReactionBehavior reaction =
            zombie.getBehavior(DamageReactionBehavior.class);
        if (reaction != null) {
            visual.lastRaged = reaction.isRaged();
            visual.lastSpinning = reaction.isSpinning();
        }

        InstantKillBehavior contact =
            zombie.getBehavior(InstantKillBehavior.class);
        if (contact != null) {
            visual.lastHasKilled = contact.isHasKilled();
        }

        ImpThrowBehavior summon =
            zombie.getBehavior(ImpThrowBehavior.class);
        if (summon != null) {
            visual.lastImpFired = summon.isFired();
        }

        RangedAttackBehavior ranged =
            zombie.getBehavior(RangedAttackBehavior.class);
        if (ranged != null) {
            visual.lastRangedCooldown = ranged.getCooldown();
        }

        SunStealBehavior sunSteal =
            zombie.getBehavior(SunStealBehavior.class);
        if (sunSteal != null) {
            visual.lastSunStealing = sunSteal.isStealing();
        }

        WorldEffectBehavior worldEffect =
            zombie.getBehavior(WorldEffectBehavior.class);
        if (worldEffect != null) {
            visual.lastWorldEffectCooldown = worldEffect.getCooldown();
        }

        AuraBehavior aura =
            zombie.getBehavior(AuraBehavior.class);
        if (aura != null) {
            visual.lastAuraTimer = aura.getTimer();
        }

        TransformBehavior transform =
            zombie.getBehavior(TransformBehavior.class);
        if (transform != null) {
            visual.lastTransformCooldown = transform.getCooldown();
        }

        DynamiteBehavior dynamite =
            zombie.getBehavior(DynamiteBehavior.class);
        if (dynamite != null) {
            visual.lastDynamiteExploded = dynamite.isExploded();
        }

        MovementBehavior movement =
            zombie.getBehavior(MovementBehavior.class);
        if (movement != null) {
            visual.lastDodoFly = movement.isSkipEatingThisTick();
        }

        TurquoiseLaserBehavior laser =
            zombie.getBehavior(TurquoiseLaserBehavior.class);
        if (laser != null) {
            visual.lastLaserStealing =
                laser.suppressesMovement(zombie);
        }

        visual.behaviorStateInitialized = true;
    }

    private void detectBehaviorTransitions(
        Zombie zombie,
        ZombieVisual visual
    ) {
        if (!visual.behaviorStateInitialized) {
            initializeBehaviorState(zombie, visual);
            return;
        }

        String alias = zombie.getAlias();

        DamageReactionBehavior reaction =
            zombie.getBehavior(DamageReactionBehavior.class);
        if (reaction != null) {
            if (!visual.lastRaged
                && reaction.isRaged()
                && ZombieType.NEWSPAPER.getAlias().equals(alias)) {
                enqueueSpecialClip(visual, "newspaper_defeat");
            }

            if (ZombieType.DARK_JUGGLER.getAlias().equals(alias)) {
                if (!visual.lastSpinning && reaction.isSpinning()) {
                    enqueueSpecialClip(visual, "spinup");
                } else if (visual.lastSpinning && !reaction.isSpinning()) {
                    enqueueSpecialClip(visual, "spindown");
                }
            }

            visual.lastRaged = reaction.isRaged();
            visual.lastSpinning = reaction.isSpinning();
        }

        InstantKillBehavior contact =
            zombie.getBehavior(InstantKillBehavior.class);
        if (contact != null) {
            if (!visual.lastHasKilled && contact.isHasKilled()) {
                if (ZombieType.MODERN_ALL_STAR.getAlias().equals(alias)) {
                    enqueueSpecialClip(visual, "tackle");
                } else if (ZombieType.GARGANTUAR.getAlias().equals(alias)) {
                    enqueueSpecialClip(visual, "smash_left");
                }
            }
            visual.lastHasKilled = contact.isHasKilled();
        }

        ImpThrowBehavior summon =
            zombie.getBehavior(ImpThrowBehavior.class);
        if (summon != null) {
            visual.lastImpFired = summon.isFired();
        }

        RangedAttackBehavior ranged =
            zombie.getBehavior(RangedAttackBehavior.class);
        if (ranged != null) {
            int currentCooldown = ranged.getCooldown();
            if (currentCooldown > visual.lastRangedCooldown) {
                switch (ranged.getType()) {
                    case SNOWBALL ->
                        enqueueSpecialClip(visual, "throw");
                    case OCTOPUS_NET ->
                        enqueueSpecialClip(visual, "toss");
                    case HOOK_PULL ->
                        enqueueSpecialSequence(
                            visual,
                            "cast",
                            "cast_loop",
                            "reel"
                        );
                    case LASER_BEAM ->
                        enqueueSpecialClip(visual, "attack");
                    default -> {
                    }
                }
            }
            visual.lastRangedCooldown = currentCooldown;
        }

        SunStealBehavior sunSteal =
            zombie.getBehavior(SunStealBehavior.class);
        if (sunSteal != null) {
            boolean stealing = sunSteal.isStealing();

            if (!visual.lastSunStealing && stealing) {
                enqueueSpecialClip(
                    visual,
                    "power_up"
                );
            } else if (visual.lastSunStealing && !stealing) {
                enqueueSpecialClip(
                    visual,
                    "power_down"
                );
            }

            visual.lastSunStealing = stealing;
        }

        WorldEffectBehavior worldEffect =
            zombie.getBehavior(WorldEffectBehavior.class);
        if (worldEffect != null) {
            int currentCooldown = worldEffect.getCooldown();
            if (currentCooldown > visual.lastWorldEffectCooldown
                && worldEffect.getType()
                == WorldEffectBehavior.WorldEffectType.SPAWN_TOMB) {
                enqueueSpecialClip(visual, "power");
            }
            visual.lastWorldEffectCooldown = currentCooldown;
        }

        AuraBehavior aura =
            zombie.getBehavior(AuraBehavior.class);
        if (aura != null) {
            int currentTimer = aura.getTimer();
            if (currentTimer < visual.lastAuraTimer) {
                enqueueSpecialClip(visual, "special");
            }
            visual.lastAuraTimer = currentTimer;
        }

        TransformBehavior transform =
            zombie.getBehavior(TransformBehavior.class);
        if (transform != null) {
            int currentCooldown = transform.getCooldown();
            if (currentCooldown > visual.lastTransformCooldown) {
                enqueueSpecialClip(visual, "sheep");
            }
            visual.lastTransformCooldown = currentCooldown;
        }

        DynamiteBehavior dynamite =
            zombie.getBehavior(DynamiteBehavior.class);
        if (dynamite != null) {
            if (!visual.lastDynamiteExploded
                && dynamite.isExploded()) {
                enqueueSpecialSequence(
                    visual,
                    "blastoff",
                    "fly",
                    "land"
                );
            }
            visual.lastDynamiteExploded = dynamite.isExploded();
        }

        MovementBehavior movement =
            zombie.getBehavior(MovementBehavior.class);
        if (movement != null) {
            boolean dodoFly = movement.isSkipEatingThisTick();
            if (!visual.lastDodoFly
                && dodoFly
                && movement.getType()
                == MovementBehavior.MovementType.FLY_OVER) {
                enqueueSpecialSequence(
                    visual,
                    "fly_start",
                    "fly_loop",
                    "fly_end"
                );
            }
            visual.lastDodoFly = dodoFly;
        }

        TurquoiseLaserBehavior laser =
            zombie.getBehavior(TurquoiseLaserBehavior.class);
        if (laser != null) {
            boolean stealing = laser.suppressesMovement(zombie);
            if (!visual.lastLaserStealing && stealing) {
                enqueueSpecialClip(visual, "power_up");
            } else if (visual.lastLaserStealing && !stealing) {
                enqueueSpecialSequence(
                    visual,
                    "attack",
                    "power_down"
                );
            }
            visual.lastLaserStealing = stealing;
        }
    }

    private boolean updateSpecialClip(
        ZombieVisual visual
    ) {
        if (visual.activeSpecialClip != null) {
            if (visual.actor.getStateTime()
                < visual.activeSpecialDuration) {
                return true;
            }

            visual.activeSpecialClip = null;
            visual.activeSpecialDuration = 0f;
        }

        while (!visual.specialQueue.isEmpty()) {
            String next = visual.specialQueue.removeFirst();
            if (startSpecialClip(visual, next)) {
                return true;
            }
        }

        return false;
    }

    private boolean startSpecialClip(
        ZombieVisual visual,
        String clip
    ) {
        String resolved = findAvailableClip(visual, clip);
        if (resolved == null) {
            return false;
        }

        visual.activeSpecialClip = resolved;
        visual.actor.setPlaybackSpeed(1f);
        visual.actor.play(resolved, false);
        visual.actor.restart();

        try {
            visual.activeSpecialDuration = Math.max(
                MIN_DEATH_DURATION,
                pamPlayer.clipDurationSeconds(
                    visual.animations.getPamPath(),
                    resolved
                )
            );
        } catch (RuntimeException ignored) {
            visual.activeSpecialDuration = 0.5f;
        }

        return true;
    }

    private void enqueueSpecialSequence(
        ZombieVisual visual,
        String... clips
    ) {
        for (String clip : clips) {
            enqueueSpecialClip(visual, clip);
        }
    }

    private void enqueueSpecialClip(
        ZombieVisual visual,
        String clip
    ) {
        String resolved = findAvailableClip(visual, clip);
        if (resolved == null) {
            return;
        }

        if (resolved.equals(visual.activeSpecialClip)) {
            return;
        }

        String last = visual.specialQueue.peekLast();
        if (resolved.equals(last)) {
            return;
        }

        visual.specialQueue.addLast(resolved);
    }

    private String clipOrFallback(
        ZombieVisual visual,
        String preferred,
        EntityAnimationState fallback
    ) {
        String clip = findAvailableClip(visual, preferred);
        return clip != null
            ? clip
            : visual.animations.clip(fallback);
    }

    private String findAvailableClip(
        ZombieVisual visual,
        String wanted
    ) {
        if (wanted == null || wanted.isBlank()) {
            return null;
        }

        for (String clip : visual.animations.getAvailableClips()) {
            if (clip.equalsIgnoreCase(wanted)) {
                return clip;
            }
        }

        return null;
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
        visual.specialQueue.clear();
        visual.activeSpecialClip = null;
        visual.activeSpecialDuration = 0f;

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

    private static final class BaseAnimation {
        private final String clip;
        private final boolean walkSpeedSynced;

        private BaseAnimation(
            String clip,
            boolean walkSpeedSynced
        ) {
            this.clip = clip;
            this.walkSpeedSynced = walkSpeedSynced;
        }
    }

    private static final class ZombieVisual {
        private final PamAnimationActor actor;
        private final ZombieAnimationResolver.ResolvedAnimations animations;
        private final Deque<String> specialQueue = new ArrayDeque<>();

        private float previousModelX;
        private float currentModelX;
        private boolean positionInitialized;

        private String activeSpecialClip;
        private float activeSpecialDuration;

        private boolean behaviorStateInitialized;
        private boolean lastRaged;
        private boolean lastSpinning;
        private boolean lastHasKilled;
        private boolean lastImpFired;
        private int lastRangedCooldown;
        private boolean lastSunStealing;
        private int lastWorldEffectCooldown;
        private int lastAuraTimer;
        private int lastTransformCooldown;
        private boolean lastDynamiteExploded;
        private boolean lastDodoFly;
        private boolean lastLaserStealing;

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
