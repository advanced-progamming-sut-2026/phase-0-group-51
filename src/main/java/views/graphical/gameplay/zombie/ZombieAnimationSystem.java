
package views.graphical.gameplay.zombie;

import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import lombok.Getter;
import models.Zombie.Behavior.*;
import models.Board.Tile;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.Zombie.ArmorDefinition;
import models.games.ChapterTheme;
import models.games.GameState;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.EntityAnimationState;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.manager.DepthSortedEntityLayer;

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
@Getter
public final class ZombieAnimationSystem {

    public static final float DEFAULT_SCALE = 0.6f;

    private static final float DAMAGE_FLASH_DURATION = 0.15f;
    private static final float DAMAGE_FLASH_ALPHA = 0.65f;
    private static final float DAMAGE_FLASH_COOLDOWN = 0.4f;

    private static final Color PLANT_FOOD_OUTLINE_COLOR =
        Color.valueOf("58FF66");
    private static final float PLANT_FOOD_OUTLINE_THICKNESS =
        7.0f;

    private static final float PLANT_FOOD_OUTLINE_BASE_ALPHA =
        0.22f;

    private static final float PLANT_FOOD_OUTLINE_AMPLITUDE =
        0.16f;

    private static final float PLANT_FOOD_OUTLINE_PULSE_SPEED =
        2.8f;

    private static final String GROUND_PART = "ground_swatch";

    private static final float MIN_DEATH_DURATION = 0.05f;
    private static final float MIN_STEP_DISTANCE = 0.001f;
    private static final float MIN_WALK_PLAYBACK_SPEED = 0.10f;
    private static final float MAX_WALK_PLAYBACK_SPEED = 5.00f;
    private static final float POSITION_EPSILON = 0.0001f;

    private static final float DANGER_DISTANCE = 2.0f;
    private static final float MAX_DANGER_RED = 0.5f;
    private static final float MAX_INTERPOLATION_STEP_COLUMNS = 0.75f;

    // The Snorkel PAM has no dedicated submerge/surface clips.
    // Reflect the real MovementBehavior.UNDERGROUND state visually instead.
    private static final float SNORKEL_SUBMERGED_Y_OFFSET_TILES = 0.30f;
    private static final float SNORKEL_SUBMERGED_ALPHA = 0.58f;

    // How far ahead of the Arcade zombie (in the direction it's facing)
    // the cabinet prop is pushed. Positive = further out in front.
    private static final float ARCADE_CABINET_LEAD_OFFSET_TILES = 0.0f;

    private static final String PROSPECTOR_DYNAMITE_STATES =
        "_dynamite_damage_states";
    private static final String PROSPECTOR_DYNAMITE_BURNING_01 =
        "_dynamite_burning_01";
    private static final String PROSPECTOR_DYNAMITE_BURNING_02 =
        "_dynamite_burning_02";
    private static final String PROSPECTOR_DYNAMITE_BURNING_03 =
        "_dynamite_burning_03";
    private static final String PROSPECTOR_DYNAMITE_BURNT =
        "dynamite_burnt";

    private static final float PROSPECTOR_BURNING_02_START = 0.50f;
    private static final float PROSPECTOR_BURNING_03_START = 0.80f;

    private static final String PROSPECTOR_BLAST_PAM =
        "768/FULL/EFFECTS/ZOMBIE_PROSPECTOR_BLAST_OFF/ZOMBIE_PROSPECTOR_BLAST_OFF.PAM";
    private static final String PROSPECTOR_BLAST_CLIP = "animation";
    private static final float PROSPECTOR_BLAST_FALLBACK_DURATION = 0.75f;

    private static final String ZOMBIE_ASH_PAM =
        "768/INITIAL/EFFECTS/ZOMBIE_ASH/ZOMBIE_ASH.PAM";
    private static final String ZOMBIE_GARGANTUAR_ASH_PAM =
        "768/INITIAL/EFFECTS/ZOMBIE_GARGANTUAR_ASH/ZOMBIE_GARGANTUAR_ASH.PAM";
    private static final String ZOMBIE_IMP_ASH_PAM =
        "768/INITIAL/EFFECTS/ZOMBIE_IMP_ASH/ZOMBIE_IMP_ASH.PAM";
    private static final String ZOMBIE_ASH_CLIP = "animation";
    private static final float ARM_DROP_VX = 45f;
    private static final float ARM_DROP_VY0 = 130f;
    private static final float ARM_DROP_GRAVITY = 650f;
    private static final float ARM_DROP_LIFETIME = 1.5f;

    private static final float DODO_JUMP_HEIGHT_TILES = 0.6f;
    private static final float DODO_JUMP_FALLBACK_DURATION = 0.5f;

    private static final String DARK_KNIGHT_CROWN_ARMOR =
        "CrownDefault@ArmorTypes";
    private static final String DARK_KNIGHT_SHOULDER_ARMOR =
        "ShoulderArmorDefault@ArmorTypes";

    private static final String EGYPT_BASIC_PAM =
        "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";
    private static final String ICEAGE_BASIC_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_BASIC/ZOMBIE_ICEAGE_BASIC.PAM";
    private static final String BEACH_BASIC_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_BEACH_BASIC/ZOMBIE_BEACH_BASIC.PAM";
    private static final String DARK_BASIC_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM";

    private static final String FLAG_ALIAS = "ZombieFlag";
    private static final String DEFAULT_FLAG_PAM =
        "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_FLAG/ZOMBIE_TUTORIAL_FLAG.PAM";
    private static final String EGYPT_FLAG_PAM =
        "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_FLAG/ZOMBIE_EGYPT_FLAG.PAM";
    private static final String ICEAGE_FLAG_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_FLAG/ZOMBIE_ICEAGE_FLAG.PAM";
    private static final String BEACH_FLAG_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_BEACH_FLAG/ZOMBIE_BEACH_FLAG.PAM";
    private static final String DARK_FLAG_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_DARK_FLAG/ZOMBIE_DARK_FLAG.PAM";

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

    private static final String PIANIST_PAM =
        "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM";
    private static final String PIANO_PROP_PAM =
        "768/FULL/ZOMBIE/PIANO/PIANO.PAM";

    private static final String ARCADE_CABINET_PROP_PAM =
        "768/FULL/EFFECTS/80S_ARCADE_CABINET/80S_ARCADE_CABINET.PAM";

    private final ChapterTheme theme;
    private final GameState gameState;
    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final Group entityRenderLayer;
    private final BoardTransform boardTransform;
    private final ZombieAnimationResolver resolver;
    private final float scale;

    private final Map<Zombie, ZombieVisual> visuals =
        new IdentityHashMap<>();

    private final List<ProspectorBlastVisual> prospectorBlastVisuals =
        new ArrayList<>();
    private final List<FallingArmVisual> fallingArms =
        new ArrayList<>();

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
            DEFAULT_SCALE,
            null
        );
    }

    public ZombieAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme,
        float scale
    ) {
        this(
            pamPlayer,
            worldStage,
            boardTransform,
            theme,
            scale,
            null
        );
    }

    public ZombieAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme,
        float scale,
        GameState gameState
    ) {
        this(
            pamPlayer,
            worldStage,
            boardTransform,
            theme,
            scale,
            gameState,
            null
        );
    }

    public ZombieAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ChapterTheme theme,
        float scale,
        GameState gameState,
        Group entityRenderLayer
    ) {
        this.pamPlayer = Objects.requireNonNull(pamPlayer, "pamPlayer");
        this.worldStage = Objects.requireNonNull(worldStage, "worldStage");
        this.entityRenderLayer =
            entityRenderLayer == null
                ? worldStage.getRoot()
                : entityRenderLayer;
        this.boardTransform = Objects.requireNonNull(boardTransform, "boardTransform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.gameState = gameState;
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
                delta,
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
                syncDarkKnightVisual(zombie, visual);
                syncNormalArmorVisual(zombie, visual);

                if (!visual.deathStarted) {
                    beginDeath(zombie, visual);
                }

                visual.deathElapsed += Math.max(0f, delta);

                if (visual.deathElapsed < visual.deathDuration) {
                    continue;
                }
            }

            visual.actor.remove();
            if (visual.piano != null) {
                visual.piano.actor.remove();
            }
            if (visual.cabinet != null) {
                visual.cabinet.actor.remove();
            }
            iterator.remove();
        }

        updateZombieDrawOrder();
        updateProspectorBlastEffects(delta);
        updateFallingArms(delta);

        lastObservedModelTick = modelTick;
    }

    public void clear() {
        for (ZombieVisual visual : visuals.values()) {
            visual.actor.remove();
            if (visual.piano != null) {
                visual.piano.actor.remove();
            }
            if (visual.cabinet != null) {
                visual.cabinet.actor.remove();
            }
        }

        visuals.clear();

        for (ProspectorBlastVisual visual : prospectorBlastVisuals) {
            visual.actor.remove();
        }
        prospectorBlastVisuals.clear();
        for (FallingArmVisual fallingArm : fallingArms) {
            fallingArm.actor.remove();
        }
        fallingArms.clear();

        resolver.clearCache();
        lastObservedModelTick = Integer.MIN_VALUE;
    }

    public int getVisibleZombieCount() {
        return visuals.size();
    }

    public PamAnimationActor getActor(Zombie zombie) {
        if (zombie == null) {
            return null;
        }

        ZombieVisual visual = visuals.get(zombie);
        return visual == null ? null : visual.actor;
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
            if (visual.piano != null) {
                visual.piano.actor.toFront();
            }

            // Cabinet first, ZombieArcade body second: the body/hands are
            // drawn over the machine so it looks like the zombie is actually
            // leaning on and pushing the cabinet, rather than standing behind it.
            if (visual.cabinet != null) {
                visual.cabinet.actor.toFront();
            }

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

            actor.setOutline(
                zombie.isGlowing(),
                PLANT_FOOD_OUTLINE_COLOR,
                PLANT_FOOD_OUTLINE_THICKNESS
            );

            configureGroundSwatch(alias, pamPath, walkClip, actor);

            PianoVisual piano = createPianoVisual(alias);
            if (piano != null) {
                DepthSortedEntityLayer.setDepthPriority(
                    piano.actor,
                    DepthSortedEntityLayer.ZOMBIE_PRIORITY
                );
                entityRenderLayer.addActor(piano.actor);
            }
            ArcadeCabinetVisual cabinet = createArcadeCabinetVisual(alias);
            if (cabinet != null) {
                DepthSortedEntityLayer.setDepthPriority(
                    cabinet.actor,
                    DepthSortedEntityLayer.ZOMBIE_PRIORITY
                );
                entityRenderLayer.addActor(cabinet.actor);
            }
            DepthSortedEntityLayer.setDepthPriority(
                actor,
                DepthSortedEntityLayer.ZOMBIE_PRIORITY
            );
            entityRenderLayer.addActor(actor);

            ZombieVisual visual = new ZombieVisual(
                actor,
                animations,
                piano,
                cabinet
            );

            initializeExplorerTorchVisual(
                alias,
                pamPath,
                visual
            );

            initializeGargantuarImpVisual(
                alias,
                pamPath,
                visual
            );

            initializeButterVisual(
                pamPath,
                visual
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

    private PianoVisual createPianoVisual(String alias) {
        if (!ZombieType.PIANO.getAlias().equals(alias)) {
            return null;
        }

        try {
            pamPlayer.loadSync(PIANO_PROP_PAM);

            List<String> available = pamPlayer.clips(PIANO_PROP_PAM);
            if (available == null || available.isEmpty()) {
                throw new IllegalStateException(
                    "Piano PAM has no animation clips: " + PIANO_PROP_PAM
                );
            }

            List<String> clips = Collections.unmodifiableList(
                new ArrayList<>(available)
            );

            String initialClip = findClipIgnoreCase(clips, "play");
            if (initialClip == null) {
                initialClip = findClipIgnoreCase(clips, "idle");
            }
            if (initialClip == null) {
                initialClip = clips.get(0);
            }

            PamAnimationActor pianoActor = new PamAnimationActor(
                pamPlayer,
                PIANO_PROP_PAM,
                initialClip,
                true
            );
            pianoActor.setScale(scale, scale);

            return new PianoVisual(pianoActor, clips);

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Failed to create piano prop actor ("
                        + PIANO_PROP_PAM + ")",
                    e
                );
            }
            return null;
        }
    }

    private ArcadeCabinetVisual createArcadeCabinetVisual(String alias) {
        if (!ZombieType.ARCADE.getAlias().equals(alias)) {
            return null;
        }

        try {
            pamPlayer.loadSync(ARCADE_CABINET_PROP_PAM);

            List<String> available = pamPlayer.clips(ARCADE_CABINET_PROP_PAM);
            if (available == null || available.isEmpty()) {
                throw new IllegalStateException(
                    "Arcade cabinet PAM has no animation clips: "
                        + ARCADE_CABINET_PROP_PAM
                );
            }

            List<String> clips = Collections.unmodifiableList(
                new ArrayList<>(available)
            );

            String initialClip = findClipIgnoreCase(clips, "idle");
            if (initialClip == null) {
                initialClip = clips.get(0);
            }

            PamAnimationActor cabinetActor = new PamAnimationActor(
                pamPlayer,
                ARCADE_CABINET_PROP_PAM,
                initialClip,
                true
            );
            cabinetActor.setScale(scale, scale);

            return new ArcadeCabinetVisual(cabinetActor, clips);

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Failed to create arcade cabinet prop actor ("
                        + ARCADE_CABINET_PROP_PAM + ")",
                    e
                );
            }
            return null;
        }
    }

    private static String findClipIgnoreCase(
        List<String> clips,
        String wanted
    ) {
        if (clips == null || wanted == null) {
            return null;
        }

        for (String clip : clips) {
            if (clip != null && clip.equalsIgnoreCase(wanted)) {
                return clip;
            }
        }

        return null;
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

        if (FLAG_ALIAS.equals(alias)) {
            return switch (theme) {
                case ANCIENT_EGYPT -> EGYPT_FLAG_PAM;
                case FROSTBITE_CAVES -> ICEAGE_FLAG_PAM;
                case BIG_WAVE_BEACH -> BEACH_FLAG_PAM;
                case DARK_AGES -> DARK_FLAG_PAM;
                default -> DEFAULT_FLAG_PAM;
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

        if (ZombieType.PIANO.getAlias().equals(alias)) {
            return PIANIST_PAM;
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

            Gdx.app.log(
                "ZombiePartsFull",
                "========== " + alias + " =========="
            );

            for (String part : parts) {
                Gdx.app.log(
                    "ZombiePartsFull",
                    "PART = " + part
                );
            }
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
        if (ZombieType.DARK_ARMOR_3.getAlias().equals(alias)) {
            return 3;
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
        float delta,
        float partialTick
    ) {
        PamAnimationActor actor = visual.actor;

        actor.setOutline(
            zombie.isGlowing(),
            PLANT_FOOD_OUTLINE_COLOR,
            PLANT_FOOD_OUTLINE_THICKNESS
        );

        actor.setOutlinePulse(
            PLANT_FOOD_OUTLINE_BASE_ALPHA,
            PLANT_FOOD_OUTLINE_AMPLITUDE,
            PLANT_FOOD_OUTLINE_PULSE_SPEED
        );

        if (visual.dodoJumping) {
            visual.dodoJumpElapsed += Math.max(0f, delta);
            if (visual.dodoJumpElapsed >= visual.dodoJumpDuration) {
                visual.dodoJumping = false;
            }
        }

        updatePosition(
            zombie,
            visual,
            partialTick
        );

        updateDamageFlash(
            zombie,
            visual,
            delta
        );

        syncDarkKnightVisual(zombie, visual);
        syncNormalArmorVisual(zombie, visual);
        syncProspectorDynamiteVisual(zombie, visual);
        syncExplorerTorchVisual(zombie, visual);
        syncArmDamageVisual(zombie, visual);
        syncButterVisual(zombie, visual);

        if (zombie.hasIceShell()) {

            actor.play(
                visual.animations.clip(
                    EntityAnimationState.IDLE
                ),
                true
            );

            actor.setPlaybackSpeed(0f);
            actor.pauseAnimation();

        } else if (!updateSpecialClip(visual)) {

            BaseAnimation base = resolveBaseAnimation(
                zombie,
                visual
            );

            actor.play(
                base.clip,
                base.loop
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

        updateColdTint(zombie, actor);
        updateDangerTint(zombie, actor);
        updateHypnoTint(zombie, actor);
        applySnorkelSubmergedVisual(zombie, actor);

        if (zombie.isFrozen() || zombie.isButtered()) {
            actor.pauseAnimation();
        } else if (!zombie.hasIceShell()) {
            actor.resumeAnimation();
        }

        updatePianoVisual(zombie, visual);
        updateArcadeCabinetVisual(zombie, visual);
    }

    private void syncProspectorDynamiteVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        if (zombie == null
            || visual == null
            || !ZombieType.PROSPECTOR.getAlias().equals(
            zombie.getAlias()
        )) {
            return;
        }

        DynamiteBehavior dynamite =
            zombie.getBehavior(DynamiteBehavior.class);

        if (dynamite == null) {
            return;
        }

        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();

        boolean burning01 = false;
        boolean burning02 = false;
        boolean burning03 = false;
        boolean burnt = false;

        if (dynamite.isExploded() || dynamite.isExtinguished()) {
            burnt = true;
        } else {
            int totalTicks = Math.max(
                1,
                dynamite.getExplosionDelayTicks()
            );

            float progress = clamp(
                dynamite.getTimer() / (float) totalTicks,
                0f,
                1f
            );

            if (progress >= PROSPECTOR_BURNING_03_START) {
                burning03 = true;
            } else if (progress >= PROSPECTOR_BURNING_02_START) {
                burning02 = true;
            } else {
                burning01 = true;
            }
        }

        // Keep the state container available, but explicitly select only
        // one visual branch so all dynamite states cannot render together.
        visibility.put(PROSPECTOR_DYNAMITE_STATES, true);
        visibility.put(PROSPECTOR_DYNAMITE_BURNING_01, burning01);
        visibility.put(PROSPECTOR_DYNAMITE_BURNING_02, burning02);
        visibility.put(PROSPECTOR_DYNAMITE_BURNING_03, burning03);
        visibility.put(PROSPECTOR_DYNAMITE_BURNT, burnt);
    }

    private void spawnProspectorBlastEffect(
        ZombieVisual zombieVisual
    ) {
        if (zombieVisual == null || zombieVisual.actor == null) {
            return;
        }

        try {
            pamPlayer.loadSync(PROSPECTOR_BLAST_PAM);

            PamAnimationActor effect = new PamAnimationActor(
                pamPlayer,
                PROSPECTOR_BLAST_PAM,
                PROSPECTOR_BLAST_CLIP,
                false
            );

            effect.setTouchable(Touchable.disabled);
            effect.setScale(scale, scale);

            // detectBehaviorTransitions runs before updatePosition for this
            // model tick, so the actor is still at the exact blast location.
            effect.setPosition(
                zombieVisual.actor.getX(),
                zombieVisual.actor.getY()
            );

            worldStage.addActor(effect);
            effect.restart();

            float duration;
            try {
                duration = Math.max(
                    MIN_DEATH_DURATION,
                    pamPlayer.clipDurationSeconds(
                        PROSPECTOR_BLAST_PAM,
                        PROSPECTOR_BLAST_CLIP
                    )
                );
            } catch (RuntimeException ignored) {
                duration = PROSPECTOR_BLAST_FALLBACK_DURATION;
            }

            prospectorBlastVisuals.add(
                new ProspectorBlastVisual(effect, duration)
            );

        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not play Prospector blast effect: "
                        + PROSPECTOR_BLAST_PAM,
                    e
                );
            }
        }
    }

    private void updateProspectorBlastEffects(float delta) {
        Iterator<ProspectorBlastVisual> iterator =
            prospectorBlastVisuals.iterator();

        while (iterator.hasNext()) {
            ProspectorBlastVisual visual = iterator.next();

            visual.elapsed += Math.max(0f, delta);
            visual.actor.toFront();

            if (visual.elapsed >= visual.duration) {
                visual.actor.remove();
                iterator.remove();
            }
        }
    }

    /**
     * Discovers every PAM part/root whose name contains "butter" and stores
     * the actor's original visibility entry. PamPlayer hides butter parts by
     * default, so they must be explicitly enabled while the BUTTERED status
     * effect is active.
     */
    private void initializeButterVisual(
        String pamPath,
        ZombieVisual visual
    ) {
        if (visual == null || pamPath == null || pamPath.isBlank()) {
            return;
        }

        LinkedHashSet<String> butterParts =
            new LinkedHashSet<>();

        // Include any butter entry that was already present in the actor map.
        for (String part : visual.actor.getVisibilityMap().keySet()) {
            if (isButterPart(part)) {
                butterParts.add(part);
            }
        }

        try {
            PamPlayer.AnimationPart root =
                pamPlayer.getParts(pamPath);

            collectButterParts(
                root,
                butterParts
            );
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not inspect butter parts in " + pamPath,
                    e
                );
            }
        }

        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();

        for (String part : butterParts) {
            visual.butterPartBaseline.put(
                part,
                visibility.containsKey(part)
                    ? visibility.get(part)
                    : null
            );
        }

        if (Gdx.app != null && !butterParts.isEmpty()) {
            Gdx.app.log(
                "ZombieButterVisual",
                "Butter parts -> " + butterParts
            );
        }
    }

    private static void collectButterParts(
        PamPlayer.AnimationPart part,
        Collection<String> out
    ) {
        if (part == null || out == null) {
            return;
        }

        if (isButterPart(part.name)) {
            out.add(part.name);
        }

        for (PamPlayer.AnimationPart child : part.children) {
            collectButterParts(
                child,
                out
            );
        }
    }

    private static boolean isButterPart(String partName) {
        return partName != null
            && partName.toLowerCase(Locale.ROOT)
            .contains("butter");
    }

    /**
     * While the model says the zombie is buttered, force every discovered
     * butter root visible. When the effect expires, restore exactly what the
     * visibility map contained before the effect was shown.
     */
    private void syncButterVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        if (zombie == null
            || visual == null
            || visual.butterPartBaseline.isEmpty()) {
            return;
        }

        boolean buttered = zombie.isButtered();
        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();

        if (buttered) {
            // Force these on every frame so another visual-state update cannot
            // accidentally hide the butter while the status is still active.
            for (String part : visual.butterPartBaseline.keySet()) {
                visibility.put(part, true);
            }

            visual.butterVisualActive = true;
            return;
        }

        if (!visual.butterVisualActive) {
            return;
        }

        for (Map.Entry<String, Boolean> entry :
            visual.butterPartBaseline.entrySet()) {

            if (entry.getValue() == null) {
                visibility.remove(entry.getKey());
            } else {
                visibility.put(
                    entry.getKey(),
                    entry.getValue()
                );
            }
        }

        visual.butterVisualActive = false;
    }

    private void initializeExplorerTorchVisual(
        String alias,
        String pamPath,
        ZombieVisual visual
    ) {
        if (visual == null
            || !ZombieType.EXPLORER.getAlias().equals(alias)) {
            return;
        }

        LinkedHashSet<String> torchFireParts =
            new LinkedHashSet<>();

        for (String part : visual.actor.getVisibilityMap().keySet()) {
            if (isTorchFireFramePart(part)) {
                torchFireParts.add(part);
            }
        }

        try {
            PamPlayer.AnimationPart root = pamPlayer.getParts(pamPath);
            collectTorchFireFrameParts(root, torchFireParts);
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not inspect Explorer torch parts in " + pamPath,
                    e
                );
            }
        }

        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();

        for (String part : torchFireParts) {
            visual.explorerTorchFireBaseline.put(
                part,
                visibility.containsKey(part)
                    ? visibility.get(part)
                    : null
            );
        }
    }

    private static void collectTorchFireFrameParts(
        PamPlayer.AnimationPart part,
        Collection<String> out
    ) {
        if (part == null || out == null) {
            return;
        }

        if (isTorchFireFramePart(part.name)) {
            out.add(part.name);
        }

        for (PamPlayer.AnimationPart child : part.children) {
            collectTorchFireFrameParts(child, out);
        }
    }

    private static boolean isTorchFireFramePart(String partName) {
        return partName != null
            && partName.toLowerCase(Locale.ROOT)
            .contains("torch_fire_frame");
    }

    private void syncExplorerTorchVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        if (zombie == null
            || visual == null
            || !ZombieType.EXPLORER.getAlias().equals(zombie.getAlias())) {
            return;
        }

        TorchBehavior torch =
            zombie.getBehavior(TorchBehavior.class);

        boolean lit =
            torch == null || torch.isLit();

        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();

        if (!lit) {
            // Force the flame off EVERY frame while the torch is
            // extinguished. This makes the visual state authoritative even
            // if a PAM clip or another visual update changes part visibility.
            for (String part :
                visual.explorerTorchFireBaseline.keySet()) {
                visibility.put(part, false);
            }

            // Extra safety for any fire-frame key that appears dynamically
            // in the visibility map after the actor was initialized.
            for (String part :
                new ArrayList<>(visibility.keySet())) {
                if (isTorchFireFramePart(part)) {
                    visibility.put(part, false);
                }
            }

            visual.lastExplorerTorchLit = false;
            visual.explorerTorchStateInitialized = true;
            return;
        }

        // When lit, restore the PAM's original visibility only on a
        // transition back from the extinguished state.
        if (visual.explorerTorchStateInitialized
            && visual.lastExplorerTorchLit) {
            return;
        }

        for (Map.Entry<String, Boolean> entry :
            visual.explorerTorchFireBaseline.entrySet()) {

            String part = entry.getKey();
            Boolean baseline = entry.getValue();

            if (baseline == null) {
                visibility.remove(part);
            } else {
                visibility.put(part, baseline);
            }
        }

        visual.lastExplorerTorchLit = true;
        visual.explorerTorchStateInitialized = true;
    }

    private void initializeGargantuarImpVisual(
        String alias,
        String pamPath,
        ZombieVisual visual
    ) {
        if (visual == null
            || !ZombieType.GARGANTUAR.getAlias().equals(alias)) {
            return;
        }

        for (String part : visual.actor.getVisibilityMap().keySet()) {
            if (isGargantuarImpPart(part)) {
                visual.gargantuarImpParts.add(part);
            }
        }

        try {
            PamPlayer.AnimationPart root =
                pamPlayer.getParts(pamPath);

            collectGargantuarImpParts(
                root,
                visual.gargantuarImpParts
            );
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not inspect Gargantuar Imp parts in "
                        + pamPath,
                    e
                );
            }
        }
    }

    private static void collectGargantuarImpParts(
        PamPlayer.AnimationPart part,
        Collection<String> out
    ) {
        if (part == null || out == null) {
            return;
        }

        if (isGargantuarImpPart(part.name)) {
            out.add(part.name);
        }

        for (PamPlayer.AnimationPart child : part.children) {
            collectGargantuarImpParts(
                child,
                out
            );
        }
    }

    private static boolean isGargantuarImpPart(
        String partName
    ) {
        if (partName == null || partName.isBlank()) {
            return false;
        }

        String normalized =
            "_"
                + partName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                + "_";

        // Match "imp" as a real part-name token. This avoids false
        // positives such as a hypothetical name containing "simple".
        return normalized.contains("_imp_");
    }

    private void hideGargantuarImpParts(
        ZombieVisual visual
    ) {
        if (visual == null
            || visual.gargantuarImpHidden) {
            return;
        }

        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();

        for (String part : visual.gargantuarImpParts) {
            visibility.put(
                part,
                false
            );
        }

        visual.gargantuarImpHidden = true;
        visual.gargantuarHideImpAfterFire = false;
    }

    private void syncArmDamageVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        if (zombie == null || visual == null) {
            return;
        }

        boolean damaged =
            zombie.getMaxHitpoints() > 0
                && zombie.getHitpoints()
                <= zombie.getMaxHitpoints() / 2f;

        String alias = zombie.getAlias();

        if (ZombieType.IMP.getAlias().equals(alias)) {
            applyArm(visual, damaged,
                "zombie_imp_arms_outer_upper",
                "zombie_imp_arm_outer_lower",
                "zombie_imp_hand_outer");
            return;
        }

        if (usesThemedBasicBody(alias)) {
            switch (theme) {
                case ANCIENT_EGYPT -> applyArm(visual, damaged,
                    "zombie_egypt_arms_outer_upper",
                    "zombie_egypt_arm_outer_lower",
                    "zombie_egypt_hand_outer_01");
                case FROSTBITE_CAVES, BIG_WAVE_BEACH, DARK_AGES -> applyArm(visual, damaged,
                    "zombie_arms_outer_upper",
                    "zombie_arm_outer_lower",
                    "zombie_hand_outer_01");
                default -> {}
            }
        }

        // Extra zombie families with their own PAM arm parts.
        // Keep only the upper arm after HP reaches 50%.
        if (alias.toLowerCase(Locale.ROOT).contains("arcade")) {
            applyArm(visual, damaged,
                "zombie_troglobite_arm_outer_upper_bone",
                "zombie_troglobite_arm_outer_lower",
                "zombie_troglobite_hand_outer");
            return;
        }

        if (alias.toLowerCase(Locale.ROOT).contains("jane")) {
            applyArm(visual, damaged,
                "zombie_arms_outer_upper",
                "zombie_arm_outer_lower",
                "zombie_hand_outer_01_upperlayer");
            return;
        }

        if (alias.toLowerCase(Locale.ROOT).contains("crystal")
            || alias.toLowerCase(Locale.ROOT).contains("turquoise")) {
            applyArm(visual, damaged,
                "zombie_egypt_ra_arms_outer_upper",
                "zombie_egypt_ra_arm_outer_lower",
                "zombie_egypt_ra_hand_outer2");
            return;
        }

        if (alias.toLowerCase(Locale.ROOT).contains("prospector")) {
            applyArm(visual, damaged,
                "_zombie_pros_arm_outer_upper2",
                "zombie_pros_arm_outer_lower",
                "zombie_pros_hand_outer_01");
            return;
        }

        if (ZombieType.NEWSPAPER.getAlias().equals(alias)) {
            DamageReactionBehavior reaction =
                zombie.getBehavior(DamageReactionBehavior.class);

            // Newspaper's regular body arm parts should only be controlled
            // after the newspaper has been destroyed and the zombie is raged.
            if (reaction != null && reaction.isRaged()) {
                applyArm(visual, damaged,
                    "zombie_arms_outer_upper",
                    "zombie_arm_outer_lower",
                    "zombie_hand_outer_01");
            }
        }

        if (ZombieType.PIANO.getAlias().equals(alias)) {
            applyArm(visual, damaged,
                "zombie_piano_arms_outer_upper",
                "zombie_piano_arm_outer_lower",
                "zombie_piano_hand_outer");
        }
    }

    private void applyArm(
        ZombieVisual visual,
        boolean damaged,
        String upper,
        String lower,
        String hand
    ) {
        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();
        visibility.put(upper, true);
        visibility.put(lower, !damaged);
        visibility.put(hand, !damaged);

        if (damaged && !visual.armDropped) {
            visual.armDropped = true;
            spawnFallingArm(visual, lower, hand);
        }
    }

    private void spawnFallingArm(
        ZombieVisual visual,
        String lower,
        String hand
    ) {
        java.util.Set<String> targets = new java.util.HashSet<>();
        targets.add(lower);
        targets.add(hand);
        spawnFallingPart(visual, targets, "arm");
    }

    private void spawnFallingHead(ZombieVisual visual) {
        if (visual == null || visual.headDropped) {
            return;
        }
        visual.headDropped = true;

        // Detect the head parts straight from this zombie's PAM tree so the
        // effect works for every zombie (skull / jaw / head bones).
        String pamPath = visual.animations.getPamPath();
        java.util.Set<String> headParts = new java.util.HashSet<>();
        try {
            collectHeadParts(pamPlayer.getParts(pamPath), headParts);
        } catch (RuntimeException ignored) {
            return;
        }
        if (headParts.isEmpty()) {
            return;
        }

        // Hide the head parts on the dying body so it looks decapitated.
        Map<String, Boolean> bodyVis =
            visual.actor.getVisibilityMap();
        for (String head : headParts) {
            bodyVis.put(head, false);
        }

        spawnFallingPart(visual, headParts, "head");
    }

    private static void collectHeadParts(
        PamPlayer.AnimationPart part,
        java.util.Set<String> out
    ) {
        if (part == null) {
            return;
        }
        if (part.name != null) {
            String n = part.name.toLowerCase(Locale.ROOT);
            if (!n.contains("particle")
                && (n.contains("skull")
                || n.contains("jaw")
                || n.contains("head"))) {
                out.add(part.name);
            }
        }
        for (PamPlayer.AnimationPart child : part.children) {
            collectHeadParts(child, out);
        }
    }

    private void spawnFallingPart(
        ZombieVisual visual,
        java.util.Set<String> targets,
        String label
    ) {
        try {
            String pamPath = visual.animations.getPamPath();
            PamAnimationActor part = new PamAnimationActor(
                pamPlayer,
                pamPath,
                visual.animations.clip(EntityAnimationState.IDLE),
                true
            );
            part.setTouchable(Touchable.disabled);
            part.setScale(
                Math.copySign(scale, visual.actor.getScaleX()),
                scale
            );
            part.setPosition(
                visual.actor.getX(),
                visual.actor.getY()
            );

            java.util.Set<String> keep = new java.util.HashSet<>();
            markArmChain(pamPlayer.getParts(pamPath), targets, keep);
            java.util.Set<String> allParts = new java.util.HashSet<>();
            collectAllPartNames(pamPlayer.getParts(pamPath), allParts);
            Map<String, Boolean> vis = part.getVisibilityMap();
            vis.clear();
            for (String name : allParts) {
                vis.put(name, keep.contains(name));
            }
            if (Gdx.app != null) {
                Gdx.app.log(
                    "ZombieAnimation",
                    "Dropped " + label + " kept " + keep.size()
                        + " of " + allParts.size() + " parts"
                );
            }

            // If none of the target parts exist in this PAM there is
            // nothing to show, so skip spawning an empty actor.
            if (keep.isEmpty()) {
                return;
            }

            worldStage.addActor(part);
            part.restart();

            float dir = visual.actor.getScaleX() >= 0 ? 1f : -1f;
            float restY =
                visual.actor.getY() - boardTransform.tileHeight();
            fallingArms.add(new FallingArmVisual(
                part,
                dir * ARM_DROP_VX,
                ARM_DROP_VY0,
                restY
            ));
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not spawn dropped " + label,
                    e
                );
            }
        }
    }

    private static void collectAllPartNames(
        PamPlayer.AnimationPart part,
        java.util.Set<String> out
    ) {
        if (part == null) {
            return;
        }
        if (part.name != null) {
            out.add(part.name);
        }
        for (PamPlayer.AnimationPart child : part.children) {
            collectAllPartNames(child, out);
        }
    }

    private static boolean markArmChain(
        PamPlayer.AnimationPart part,
        java.util.Set<String> targets,
        java.util.Set<String> keep
    ) {
        if (part == null) {
            return false;
        }
        if (part.name != null && targets.contains(part.name)) {
            keepSubtree(part, keep);
            return true;
        }
        boolean onPath = false;
        for (PamPlayer.AnimationPart child : part.children) {
            if (markArmChain(child, targets, keep)) {
                onPath = true;
            }
        }
        if (onPath && part.name != null) {
            keep.add(part.name);
        }
        return onPath;
    }

    private static void keepSubtree(
        PamPlayer.AnimationPart part,
        java.util.Set<String> keep
    ) {
        if (part == null) {
            return;
        }
        if (part.name != null) {
            keep.add(part.name);
        }
        for (PamPlayer.AnimationPart child : part.children) {
            keepSubtree(child, keep);
        }
    }

    private void updateFallingArms(float delta) {
        float d = Math.max(0f, delta);
        Iterator<FallingArmVisual> iterator = fallingArms.iterator();
        while (iterator.hasNext()) {
            FallingArmVisual fallingArm = iterator.next();
            fallingArm.vy -= ARM_DROP_GRAVITY * d;
            fallingArm.actor.moveBy(
                fallingArm.vx * d,
                fallingArm.vy * d
            );
            // Stop after falling roughly one tile; rest in place.
            if (fallingArm.actor.getY() <= fallingArm.restY) {
                fallingArm.actor.setY(fallingArm.restY);
                fallingArm.vx = 0f;
                fallingArm.vy = 0f;
            }
            fallingArm.actor.toFront();
            fallingArm.elapsed += d;
            if (fallingArm.elapsed >= ARM_DROP_LIFETIME) {
                fallingArm.actor.remove();
                iterator.remove();
            }
        }
    }

    private void updateDamageFlash(
        Zombie zombie,
        ZombieVisual visual,
        float delta
    ) {
        if (visual.damageFlashCooldownRemaining > 0f) {
            visual.damageFlashCooldownRemaining = Math.max(
                0f,
                visual.damageFlashCooldownRemaining
                    - Math.max(0f, delta)
            );
        }

        int currentDamageHealth =
            getDamageFlashHealth(zombie);

        if (visual.lastDamageHealth == Integer.MIN_VALUE) {
            visual.lastDamageHealth = currentDamageHealth;
            return;
        }

        boolean tookDamage =
            currentDamageHealth < visual.lastDamageHealth
                && zombie.getHitpoints() > 0;

        if (tookDamage
            && visual.damageFlashCooldownRemaining <= 0f) {
            visual.actor.flashAdditive(
                DAMAGE_FLASH_DURATION,
                DAMAGE_FLASH_ALPHA
            );

            visual.damageFlashCooldownRemaining =
                DAMAGE_FLASH_COOLDOWN;
        }

        if (tookDamage
            && visual.piano != null
            && !visual.piano.damaged) {
            visual.piano.damaged = true;
            startPianoDamageTransition(visual.piano);
            visual.piano.actor.flashAdditive(
                DAMAGE_FLASH_DURATION,
                DAMAGE_FLASH_ALPHA
            );
        }

        if (tookDamage
            && visual.cabinet != null
            && visual.cabinet.actor.isVisible()) {
            visual.cabinet.actor.flashAdditive(
                DAMAGE_FLASH_DURATION,
                DAMAGE_FLASH_ALPHA
            );
        }

        visual.lastDamageHealth = currentDamageHealth;
    }

    private void startPianoDamageTransition(PianoVisual piano) {
        if (piano == null) {
            return;
        }

        String damageClip = piano.clip("damage");
        if (damageClip == null) {
            piano.damageTransitionActive = false;
            playPianoLoop(piano);
            return;
        }

        piano.damageTransitionActive = true;
        piano.actor.setPlaybackSpeed(1f);
        piano.actor.resumeAnimation();
        piano.actor.play(damageClip, false);
        piano.actor.restart();

        try {
            piano.damageDuration = Math.max(
                MIN_DEATH_DURATION,
                pamPlayer.clipDurationSeconds(
                    PIANO_PROP_PAM,
                    damageClip
                )
            );
        } catch (RuntimeException ignored) {
            piano.damageDuration = 0.5f;
        }
    }

    private void updatePianoVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        PianoVisual piano = visual.piano;
        if (piano == null) {
            return;
        }

        if (piano.damageTransitionActive) {
            if (piano.actor.getStateTime() >= piano.damageDuration) {
                piano.damageTransitionActive = false;
                playPianoLoop(piano);
            }
        } else {
            playPianoLoop(piano);
        }

        // Keep the prop visually synchronized with the pianist body.
        Color bodyColor = visual.actor.getColor();
        piano.actor.setColor(
            bodyColor.r,
            bodyColor.g,
            bodyColor.b,
            bodyColor.a
        );

        piano.actor.setOutline(
            zombie.isGlowing(),
            PLANT_FOOD_OUTLINE_COLOR,
            PLANT_FOOD_OUTLINE_THICKNESS
        );
        piano.actor.setOutlinePulse(
            PLANT_FOOD_OUTLINE_BASE_ALPHA,
            PLANT_FOOD_OUTLINE_AMPLITUDE,
            PLANT_FOOD_OUTLINE_PULSE_SPEED
        );

        if (!piano.damageTransitionActive) {
            piano.actor.setPlaybackSpeed(
                visual.actor.getPlaybackSpeed()
            );
        }

        if (zombie.isFrozen()
            || zombie.isButtered()
            || zombie.hasIceShell()) {
            piano.actor.pauseAnimation();
        } else {
            piano.actor.resumeAnimation();
        }
    }

    private void playPianoLoop(PianoVisual piano) {
        if (piano == null) {
            return;
        }

        String wanted = piano.damaged ? "play2" : "play";
        String clip = piano.clip(wanted);
        if (clip == null) {
            clip = piano.clip("idle");
        }

        if (clip != null) {
            piano.actor.play(clip, true);
        }
    }

    private void updateArcadeCabinetVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        ArcadeCabinetVisual cabinet = visual.cabinet;
        if (cabinet == null) {
            return;
        }

        PushObjectBehavior push =
            zombie.getBehavior(PushObjectBehavior.class);

        boolean hasObject =
            push != null && push.hasObject();

        // While alive, the prop follows the real PushObjectBehavior.
        // If the cabinet has been destroyed separately, do not keep drawing
        // an active machine beside a normal walking Arcade zombie.
        cabinet.actor.setVisible(hasObject);

        if (!hasObject) {
            cabinet.lastBodyPushing = false;
            return;
        }
        boolean bodyPushing =
            "push".equalsIgnoreCase(
                visual.actor.getClip()
            );

        String wanted =
            bodyPushing ? "active" : "idle";

        String cabinetClip =
            cabinet.clip(wanted);

        if (cabinetClip == null) {
            cabinetClip = cabinet.clip("idle");
        }

        if (cabinetClip != null) {
            boolean changed =
                !cabinetClip.equalsIgnoreCase(
                    cabinet.actor.getClip()
                );

            cabinet.actor.play(
                cabinetClip,
                true
            );

            // The body also enters its push clip on this update. Restarting
            // the prop only on the state transition makes both animations
            // begin from frame/time zero together instead of drifting.
            if (changed
                || bodyPushing != cabinet.lastBodyPushing) {
                cabinet.actor.restart();
            }
        }

        cabinet.lastBodyPushing = bodyPushing;

        Color bodyColor = visual.actor.getColor();
        cabinet.actor.setColor(
            bodyColor.r,
            bodyColor.g,
            bodyColor.b,
            bodyColor.a
        );

        cabinet.actor.setOutline(
            zombie.isGlowing(),
            PLANT_FOOD_OUTLINE_COLOR,
            PLANT_FOOD_OUTLINE_THICKNESS
        );
        cabinet.actor.setOutlinePulse(
            PLANT_FOOD_OUTLINE_BASE_ALPHA,
            PLANT_FOOD_OUTLINE_AMPLITUDE,
            PLANT_FOOD_OUTLINE_PULSE_SPEED
        );

        // Same playback rate as the body, just like the pianist + piano pair.
        cabinet.actor.setPlaybackSpeed(
            visual.actor.getPlaybackSpeed()
        );

        if (zombie.isFrozen()
            || zombie.isButtered()
            || zombie.hasIceShell()) {
            cabinet.actor.pauseAnimation();
        } else {
            cabinet.actor.resumeAnimation();
        }
    }

    private void playArcadeCabinetLoop(
        ArcadeCabinetVisual cabinet,
        boolean active
    ) {
        if (cabinet == null) {
            return;
        }

        String wanted = active ? "active" : "idle";
        String clip = cabinet.clip(wanted);
        if (clip == null) {
            clip = cabinet.clip("idle");
        }

        if (clip != null) {
            cabinet.actor.play(clip, true);
        }
    }

    private static int getDamageFlashHealth(Zombie zombie) {
        int health = zombie.getHitpoints();

        for (ZombieBehavior behavior : zombie.getBehaviors()) {
            if (behavior instanceof ArmorBehavior armor) {
                health += Math.max(
                    0,
                    armor.getCurrentHP()
                );
            }
        }

        return health;
    }

    private void syncNormalArmorVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        List<ArmorBehavior> armors = getArmorBehaviors(zombie);

        if (armors.isEmpty()) {
            visual.armorVisualSignature = "";
            return;
        }

        String signature = buildArmorVisualSignature(armors);
        if (signature.equals(visual.armorVisualSignature)) {
            return;
        }

        String pamPath = resolvePamPath(theme, zombie.getAlias());
        if (pamPath == null || pamPath.isBlank()) {
            return;
        }

        PamPlayer.AnimationPart root;
        try {
            root = pamPlayer.getParts(pamPath);
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieArmorVisual",
                    "Could not read PAM parts for " + zombie.getAlias(),
                    e
                );
            }
            return;
        }

        if (root == null) {
            return;
        }

        Map<String, Boolean> visibility =
            visual.actor.getVisibilityMap();

        for (ArmorBehavior armor : armors) {
            applyArmorLayerVisibility(
                zombie,
                armor,
                root,
                visibility
            );
        }

        visual.armorVisualSignature = signature;
    }



    private static List<ArmorBehavior> getArmorBehaviors(Zombie zombie) {
        List<ArmorBehavior> result = new ArrayList<>();

        for (var behavior : zombie.getBehaviors()) {
            if (behavior instanceof ArmorBehavior armor) {
                result.add(armor);
            }
        }

        return result;
    }

    private static String buildArmorVisualSignature(
        List<ArmorBehavior> armors
    ) {
        StringBuilder signature = new StringBuilder();

        for (ArmorBehavior armor : armors) {
            ArmorDefinition def = armor.getDefinition();

            if (def == null) {
                signature.append("null;");
                continue;
            }

            signature.append(def.getAlias()).append(':');

            if (armor.isGone()) {
                signature.append("gone");
            } else {
                signature.append(
                    resolveArmorLayerIndex(
                        armor,
                        def.getArmorLayers().size()
                    )
                );
            }

            signature.append(';');
        }

        return signature.toString();
    }

    private void applyArmorLayerVisibility(
        Zombie zombie,
        ArmorBehavior armor,
        PamPlayer.AnimationPart root,
        Map<String, Boolean> visibility
    ) {
        ArmorDefinition def = armor.getDefinition();
        if (def == null) {
            return;
        }

        List<String> layers = def.getArmorLayers();
        if (layers == null || layers.isEmpty()) {
            if (Gdx.app != null) {
                Gdx.app.log(
                    "ZombieArmorVisual",
                    zombie.getAlias() + " / " + def.getAlias()
                        + " has no ArmorLayers"
                );
            }
            return;
        }
        for (String layer : layers) {
            if (layer != null && !layer.isBlank()) {
                visibility.put(layer, false);
            }
        }

        if (armor.isGone()) {
            if (Gdx.app != null) {
                Gdx.app.log(
                    "ZombieArmorVisual",
                    zombie.getAlias() + " / " + def.getAlias()
                        + " hp=0/" + def.getBaseHealth()
                        + " -> hidden"
                );
            }
            return;
        }

        int layerIndex = resolveArmorLayerIndex(
            armor,
            layers.size()
        );

        if (layerIndex < 0 || layerIndex >= layers.size()) {
            return;
        }

        String targetLayer = layers.get(layerIndex);
        if (targetLayer == null || targetLayer.isBlank()) {
            return;
        }

        List<PamPlayer.AnimationPart> path =
            findPartPath(root, targetLayer);

        if (!path.isEmpty()) {
            for (PamPlayer.AnimationPart part : path) {
                if (part.name != null && !part.name.isBlank()) {
                    visibility.put(part.name, true);
                }
            }
        }

        visibility.put(targetLayer, true);

        if (Gdx.app != null) {
            Gdx.app.log(
                "ZombieArmorVisual",
                zombie.getAlias() + " / " + def.getAlias()
                    + " hp=" + armor.getCurrentHP()
                    + "/" + def.getBaseHealth()
                    + " stage=" + layerIndex
                    + " -> " + targetLayer
                    + (path.isEmpty() ? " (PAM path NOT FOUND)" : "")
            );
        }
    }

    private static int resolveArmorLayerIndex(
        ArmorBehavior armor,
        int layerCount
    ) {
        if (layerCount <= 1 || armor == null) {
            return 0;
        }

        ArmorDefinition def = armor.getDefinition();
        if (def == null) {
            return 0;
        }

        int baseHealth = Math.max(1, def.getBaseHealth());
        float healthRatio = armor.getCurrentHP() / (float) baseHealth;

        List<Float> thresholds = def.getLayerThresholds();
        if (thresholds == null || thresholds.isEmpty()) {
            thresholds = List.of(0.666f, 0.333f);
        }

        int index = 0;
        int maxThresholds = Math.min(
            thresholds.size(),
            layerCount - 1
        );

        for (int i = 0; i < maxThresholds; i++) {
            Float threshold = thresholds.get(i);
            if (threshold != null && healthRatio <= threshold) {
                index = i + 1;
            }
        }

        return Math.max(0, Math.min(index, layerCount - 1));
    }

    private static List<PamPlayer.AnimationPart> findPartPath(
        PamPlayer.AnimationPart root,
        String targetName
    ) {
        List<PamPlayer.AnimationPart> path = new ArrayList<>();

        if (root == null || targetName == null || targetName.isBlank()) {
            return path;
        }

        if (findPartPathRecursive(root, targetName, path)) {
            return path;
        }

        path.clear();
        return path;
    }

    private static boolean findPartPathRecursive(
        PamPlayer.AnimationPart current,
        String targetName,
        List<PamPlayer.AnimationPart> path
    ) {
        path.add(current);

        if (targetName.equals(current.name)) {
            return true;
        }

        for (PamPlayer.AnimationPart child : current.children) {
            if (findPartPathRecursive(child, targetName, path)) {
                return true;
            }
        }

        path.remove(path.size() - 1);
        return false;
    }

    private void syncDarkKnightVisual(
        Zombie zombie,
        ZombieVisual visual
    ) {
        if (theme != ChapterTheme.DARK_AGES
            || !ZombieType.DEFAULT.getAlias().equals(zombie.getAlias())) {
            return;
        }

        boolean knighted = hasActiveDarkKnightArmor(zombie);

        if (visual.darkKnightVisual == knighted) {
            return;
        }

        String visualAlias = knighted
            ? ZombieType.DARK_ARMOR_3.getAlias()
            : ZombieType.DEFAULT.getAlias();

        List<String> visibleParts = resolveVisibleParts(
            pamPlayer,
            DARK_BASIC_PAM,
            visualAlias
        );

        if (!visibleParts.isEmpty()) {
            visual.actor.setVisibleParts(visibleParts);
        }

        visual.darkKnightVisual = knighted;
    }

    private boolean hasActiveDarkKnightArmor(Zombie zombie) {
        for (var behavior : zombie.getBehaviors()) {
            if (!(behavior instanceof ArmorBehavior armor)
                || armor.isGone()
                || armor.getDefinition() == null) {
                continue;
            }

            String armorAlias = armor.getDefinition().getAlias();

            if (DARK_KNIGHT_CROWN_ARMOR.equals(armorAlias)
                || DARK_KNIGHT_SHOULDER_ARMOR.equals(armorAlias)) {
                return true;
            }
        }

        return false;
    }


    private void updateHypnoTint(
        Zombie zombie,
        PamAnimationActor actor
    ) {
        if (zombie == null || actor == null) {
            return;
        }
        if (zombie.isFrozen() || zombie.isChilled()) {
            return;
        }
        if (zombie.isHypnotized()) {
            actor.setColor(
                0.80f,
                0.58f,
                0.92f,
                1.00f
            );
        }
    }

    private void updateDangerTint(
        Zombie zombie,
        PamAnimationActor actor
    ) {
        if (zombie == null || actor == null) {
            return;
        }
        if (zombie.isFrozen() || zombie.isChilled()) {
            return;
        }

        float dangerDistance;

        if (gameState != null && gameState.hasDeadline()) {

            float deadlineX =
                gameState.getDeadlineColumn() - 1f;

            dangerDistance =
                Math.abs(
                    zombie.getX()
                        - deadlineX
                );

        } else if (gameState != null
            && gameState.isSaveOurSeedsActive()) {

            dangerDistance = Float.MAX_VALUE;

            for (Plant plant : gameState.getProtectedPlants()) {

                if (plant == null || plant.isDead()) {
                    continue;
                }

                Tile tile =
                    gameState.getBoard()
                        .getTileForPlant(plant);

                if (tile == null
                    || tile.getLane() != zombie.getLane()) {
                    continue;
                }

                float distance =
                    Math.abs(
                        zombie.getX()
                            - tile.getColumn()
                    );

                dangerDistance =
                    Math.min(
                        dangerDistance,
                        distance
                    );
            }

        } else {

            // Normal mode: zombies walk from right to left.
            // Mower is at column 0, so only the last two tiles are dangerous.
            float mowerX = 0f;

            dangerDistance =
                zombie.getX() - mowerX;
        }


        float danger =
            MathUtils.clamp(
                (DANGER_DISTANCE - dangerDistance)
                    / DANGER_DISTANCE,
                0f,
                1f
            );


        if (danger <= 0f) {
            actor.setColor(
                1f,
                1f,
                1f,
                1f
            );
            return;
        }


        actor.setColor(
            1f,
            1f - danger * MAX_DANGER_RED,
            1f - danger * MAX_DANGER_RED,
            1f
        );
    }

    private void updateColdTint(
        Zombie zombie,
        PamAnimationActor actor
    ) {
        if (zombie.isFrozen()) {
            actor.setColor(
                0.55f,
                0.78f,
                1.00f,
                1.00f
            );
            return;
        }

        if (zombie.isChilled()) {
            actor.setColor(
                0.72f,
                0.88f,
                1.00f,
                1.00f
            );
            return;
        }

        actor.setColor(
            1.00f,
            1.00f,
            1.00f,
            1.00f
        );
    }

    private BaseAnimation resolveBaseAnimation(
        Zombie zombie,
        ZombieVisual visual
    ) {
        SandstormTransportBehavior sandstorm =
            zombie.getBehavior(
                SandstormTransportBehavior.class
            );

        SnowstormTransportBehavior snowstorm =
            zombie.getBehavior(
                SnowstormTransportBehavior.class
            );

        boolean transported =
            sandstorm != null
                && sandstorm.isActive()
                || snowstorm != null
                && snowstorm.isActive();

        if (transported) {
            return new BaseAnimation(
                visual.animations.clip(
                    EntityAnimationState.IDLE
                ),
                false
            );
        }

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
                    true
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

        if (ZombieType.BEACH_OCTOPUS.getAlias().equals(alias)) {
            RangedAttackBehavior octopus =
                zombie.getBehavior(RangedAttackBehavior.class);
            if (octopus != null && octopus.suppressesMovement(zombie)) {
                return new BaseAnimation(
                    pickOctopusIdle(visual),
                    true
                );
            }
        }

        if (ZombieType.DARK_JUGGLER.getAlias().equals(alias)) {
            DamageReactionBehavior reaction =
                zombie.getBehavior(DamageReactionBehavior.class);

            if (zombie.isEating()) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "eat",
                        EntityAnimationState.EAT
                    ),
                    true
                );
            }

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
                    true
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

        if (ZombieType.TOMB_RAISER.getAlias().equals(alias)) {
            WorldEffectBehavior worldEffect =
                zombie.getBehavior(WorldEffectBehavior.class);

            if (worldEffect != null
                && worldEffect.getType()
                == WorldEffectBehavior.WorldEffectType.SPAWN_TOMB
                && worldEffect.isCasting()) {
                return new BaseAnimation(
                    clipOrFallback(
                        visual,
                        "power",
                        EntityAnimationState.SPECIAL
                    ),
                    false,
                    false
                );
            }
        }

        if (ZombieType.PIANO.getAlias().equals(alias)) {
            return new BaseAnimation(
                clipOrFallback(
                    visual,
                    "play",
                    EntityAnimationState.WALK
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
                || fallbackState == EntityAnimationState.EAT
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
        visual.lastAttackSerial = zombie.getAttackSerial();

        SunStealBehavior sunSteal =
            zombie.getBehavior(SunStealBehavior.class);
        if (sunSteal != null) {
            visual.lastSunStealing = sunSteal.isStealing();
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
                    if (contact.getRunningSpeedScale() > 0f) {
                        enqueueSpecialClip(visual, "kick");
                    } else {
                        enqueueSpecialClip(visual, "tackle");
                    }
                } else if (ZombieType.GARGANTUAR.getAlias().equals(alias)) {
                    enqueueSpecialClip(visual, "smash_left");
                }
            }
            visual.lastHasKilled = contact.isHasKilled();
        }

        ImpThrowBehavior summon =
            zombie.getBehavior(ImpThrowBehavior.class);
        if (summon != null) {
            boolean fired = summon.isFired();

            if (!visual.lastImpFired
                && fired
                && ZombieType.GARGANTUAR.getAlias().equals(alias)) {
                // Keep the Imp visible throughout the Gargantuar's fire
                // animation. Once that one-shot clip has fully completed,
                // all Imp-related PAM parts are hidden permanently.
                visual.gargantuarHideImpAfterFire = true;
                enqueueSpecialSequence(visual, "fire", "cannon_fire");
            }

            visual.lastImpFired = fired;
        }

        RangedAttackBehavior ranged =
            zombie.getBehavior(RangedAttackBehavior.class);
        if (ranged != null) {
            long currentAttackSerial = zombie.getAttackSerial();
            if (currentAttackSerial > visual.lastAttackSerial) {
                switch (ranged.getType()) {
                    case SNOWBALL ->
                        enqueueSpecialClip(visual, "throw");
                    case OCTOPUS_NET -> {
                        enqueueSpecialClip(visual, "toss");
                        visual.octopusIdleIndex++;
                    }
                    case HOOK_PULL ->
                        enqueueSpecialSequence(
                            visual,
                            "cast",
                            "cast_loop",
                            "reel"
                        );
                    case LASER_BEAM ->
                        enqueueSpecialClip(visual, "attack");
                    case JUGGLE_BALL ->
                        enqueueSpecialClip(visual, "throw");
                }
            }
            visual.lastAttackSerial = currentAttackSerial;
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
                if (ZombieType.PROSPECTOR.getAlias().equals(alias)) {
                    spawnProspectorBlastEffect(visual);
                }

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
                visual.dodoJumping = true;
                visual.dodoJumpFromX = movement.getLastJumpFromX();
                visual.dodoJumpElapsed = 0f;
                visual.dodoJumpDuration = dodoFlyDuration(visual);
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

            String completedSpecialClip =
                visual.activeSpecialClip;

            visual.activeSpecialClip = null;
            visual.activeSpecialDuration = 0f;

            if (visual.gargantuarHideImpAfterFire
                && completedSpecialClip.equalsIgnoreCase("fire")) {
                hideGargantuarImpParts(
                    visual
                );
            }
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

    private String pickOctopusIdle(ZombieVisual visual) {
        String[] variants = {"idle", "idle2", "idle3", "idle4", "idle5"};
        java.util.List<String> available = new java.util.ArrayList<>();
        for (String v : variants) {
            String clip = findAvailableClip(visual, v);
            if (clip != null) {
                available.add(clip);
            }
        }
        if (available.isEmpty()) {
            return visual.animations.clip(EntityAnimationState.IDLE);
        }
        int index = Math.floorMod(visual.octopusIdleIndex, available.size());
        return available.get(index);
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

    private static boolean isSnorkelSubmerged(Zombie zombie) {
        if (zombie == null
            || !ZombieType.BEACH_SNORKEL.getAlias().equals(
            zombie.getAlias()
        )) {
            return false;
        }

        MovementBehavior movement =
            zombie.getBehavior(MovementBehavior.class);

        return movement != null
            && movement.getType()
            == MovementBehavior.MovementType.UNDERGROUND
            && movement.isSubmerged();
    }

    private static void applySnorkelSubmergedVisual(
        Zombie zombie,
        PamAnimationActor actor
    ) {
        if (actor == null
            || !ZombieType.BEACH_SNORKEL.getAlias().equals(
            zombie == null ? null : zombie.getAlias()
        )) {
            return;
        }

        Color color = actor.getColor();
        float alpha = isSnorkelSubmerged(zombie)
            ? SNORKEL_SUBMERGED_ALPHA
            : 1f;

        actor.setColor(
            color.r,
            color.g,
            color.b,
            alpha
        );
    }

    private void updatePosition(
        Zombie zombie,
        ZombieVisual visual,
        float partialTick
    ) {
        PamAnimationActor actor = visual.actor;

        float renderX;
        float arcLift = 0f;

        if (visual.dodoJumping && visual.dodoJumpDuration > 0f) {
            float t = clamp(
                visual.dodoJumpElapsed / visual.dodoJumpDuration,
                0f,
                1f
            );
            renderX =
                visual.dodoJumpFromX
                    + (visual.currentModelX - visual.dodoJumpFromX) * t;
            arcLift =
                (float) Math.sin(Math.PI * t)
                    * DODO_JUMP_HEIGHT_TILES
                    * boardTransform.tileHeight();
        } else {
            renderX =
                visual.previousModelX
                    + (
                    visual.currentModelX
                        - visual.previousModelX
                ) * partialTick;
        }

        float x =
            boardTransform.getArea().x()
                + (renderX + 0.5f)
                * boardTransform.tileWidth();

        float y =
            boardTransform.tileY(zombie.getLane())
                + boardTransform.tileHeight()
                * 0.5f
                + arcLift;

        if (isSnorkelSubmerged(zombie)) {
            y -= boardTransform.tileHeight()
                * SNORKEL_SUBMERGED_Y_OFFSET_TILES;
        }

        actor.setPosition(x, y);

        float scaleX =
            zombie.getDirection() >= 0
                ? scale
                : -scale;

        actor.setScale(scaleX, scale);

        if (visual.piano != null) {
            visual.piano.actor.setPosition(x, y);
            visual.piano.actor.setScale(scaleX, scale);
        }

        if (visual.cabinet != null) {
            float cabinetLeadWorld =
                ARCADE_CABINET_LEAD_OFFSET_TILES
                    * boardTransform.tileWidth();

            float cabinetX =
                x - Math.signum(scaleX) * cabinetLeadWorld - 100;

            visual.cabinet.actor.setPosition(cabinetX, y);
            visual.cabinet.actor.setScale(scaleX, scale);
        }
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

    private float spawnZombieAshEffect(Zombie zombie, ZombieVisual visual) {
        String pam = ZOMBIE_ASH_PAM;
        String alias = zombie.getAlias();
        if (ZombieType.GARGANTUAR.getAlias().equals(alias)) {
            pam = ZOMBIE_GARGANTUAR_ASH_PAM;
        } else if (ZombieType.IMP.getAlias().equals(alias)
            || ZombieType.DARK_IMP_DRAGON.getAlias().equals(alias)) {
            pam = ZOMBIE_IMP_ASH_PAM;
        }
        try {
            pamPlayer.loadSync(pam);
            PamAnimationActor effect = new PamAnimationActor(
                pamPlayer,
                pam,
                ZOMBIE_ASH_CLIP,
                false
            );
            effect.setTouchable(Touchable.disabled);
            effect.setScale(scale, scale);
            effect.setPosition(
                visual.actor.getX(),
                visual.actor.getY()
            );
            worldStage.addActor(effect);
            effect.restart();
            float duration;
            try {
                duration = Math.max(
                    MIN_DEATH_DURATION,
                    pamPlayer.clipDurationSeconds(pam, ZOMBIE_ASH_CLIP)
                );
            } catch (RuntimeException ignored) {
                duration = 3.5f;
            }
            prospectorBlastVisuals.add(
                new ProspectorBlastVisual(effect, duration)
            );
            return duration;
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "ZombieAnimation",
                    "Could not play zombie ash effect: " + pam,
                    e
                );
            }
            return MIN_DEATH_DURATION;
        }
    }

    private void beginDeath(Zombie zombie, ZombieVisual visual) {
        visual.deathStarted = true;
        visual.deathElapsed = 0f;
        visual.specialQueue.clear();
        visual.activeSpecialClip = null;
        visual.activeSpecialDuration = 0f;

        if (zombie.isDeathByExplosion()) {
            float ashDuration = spawnZombieAshEffect(zombie, visual);
            visual.actor.setVisible(false);
            visual.deathDuration = ashDuration;
            return;
        }

        spawnFallingHead(visual);

        String deathClip = visual.animations.clip(
            EntityAnimationState.DEATH
        );

        String alternateDeathClip = findAvailableClip(visual, "die2");
        if (alternateDeathClip != null && Math.random() < 0.5) {
            deathClip = alternateDeathClip;
        }

        visual.actor.clearGroundingKeepingVisualPosition();
        visual.actor.setColor(
            1.00f,
            1.00f,
            1.00f,
            1.00f
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

        if (visual.piano != null) {
            String pianoDie = visual.piano.clip("die");
            if (pianoDie != null) {
                visual.piano.damageTransitionActive = false;
                visual.piano.actor.resumeAnimation();
                visual.piano.actor.setPlaybackSpeed(1f);
                visual.piano.actor.play(pianoDie, false);
                visual.piano.actor.restart();

                try {
                    visual.deathDuration = Math.max(
                        visual.deathDuration,
                        pamPlayer.clipDurationSeconds(
                            PIANO_PROP_PAM,
                            pianoDie
                        )
                    );
                } catch (RuntimeException ignored) {
                }
            }
        }

        if (visual.cabinet != null) {
            String cabinetDeath =
                visual.cabinet.clip("death");

            if (cabinetDeath != null) {
                visual.cabinet.actor.setVisible(true);
                visual.cabinet.actor.setColor(
                    1f, 1f, 1f, 1f
                );
                visual.cabinet.actor.resumeAnimation();
                visual.cabinet.actor.setPlaybackSpeed(1f);
                visual.cabinet.actor.play(
                    cabinetDeath,
                    false
                );
                visual.cabinet.actor.restart();

                // beginDeath() restarts the zombie body immediately above,
                // so body "die" and cabinet "death" both start at t = 0.
                // Keep both actors alive until the longer death clip ends.
                try {
                    visual.deathDuration = Math.max(
                        visual.deathDuration,
                        pamPlayer.clipDurationSeconds(
                            ARCADE_CABINET_PROP_PAM,
                            cabinetDeath
                        )
                    );
                } catch (RuntimeException ignored) {
                    // Keep the Arcade zombie's own death duration.
                }
            } else {
                visual.cabinet.actor.setVisible(false);
            }
        }
    }

    private float dodoFlyDuration(ZombieVisual visual) {
        String pam = visual.animations.getPamPath();
        float total = 0f;
        for (String clip : new String[] {
            "fly_start", "fly_loop", "fly_end"
        }) {
            try {
                total += pamPlayer.clipDurationSeconds(pam, clip);
            } catch (RuntimeException ignored) {
                // Missing clip: fall back to a fixed duration below.
            }
        }
        return total > 0.01f
            ? total
            : DODO_JUMP_FALLBACK_DURATION;
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
        private final boolean loop;

        private BaseAnimation(
            String clip,
            boolean walkSpeedSynced
        ) {
            this(
                clip,
                walkSpeedSynced,
                true
            );
        }

        private BaseAnimation(
            String clip,
            boolean walkSpeedSynced,
            boolean loop
        ) {
            this.clip = clip;
            this.walkSpeedSynced = walkSpeedSynced;
            this.loop = loop;
        }
    }

    private static final class ProspectorBlastVisual {
        private final PamAnimationActor actor;
        private final float duration;
        private float elapsed;

        private ProspectorBlastVisual(
            PamAnimationActor actor,
            float duration
        ) {
            this.actor = actor;
            this.duration = Math.max(
                MIN_DEATH_DURATION,
                duration
            );
        }
    }

    private static final class FallingArmVisual {
        private final PamAnimationActor actor;
        private float vx;
        private float vy;
        private float elapsed;
        private final float restY;

        private FallingArmVisual(
            PamAnimationActor actor,
            float vx,
            float vy,
            float restY
        ) {
            this.actor = actor;
            this.vx = vx;
            this.vy = vy;
            this.restY = restY;
        }
    }

    private static final class PianoVisual {
        private final PamAnimationActor actor;
        private final List<String> availableClips;

        private boolean damaged;
        private boolean damageTransitionActive;
        private float damageDuration;

        private PianoVisual(
            PamAnimationActor actor,
            List<String> availableClips
        ) {
            this.actor = actor;
            this.availableClips = availableClips;
        }

        private String clip(String wanted) {
            return findClipIgnoreCase(availableClips, wanted);
        }
    }

    private static final class ArcadeCabinetVisual {
        private final PamAnimationActor actor;
        private final List<String> availableClips;
        private boolean lastBodyPushing;

        private ArcadeCabinetVisual(
            PamAnimationActor actor,
            List<String> availableClips
        ) {
            this.actor = actor;
            this.availableClips = availableClips;
        }

        private String clip(String wanted) {
            return findClipIgnoreCase(availableClips, wanted);
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

        private final Set<String> gargantuarImpParts =
            new LinkedHashSet<>();
        private boolean gargantuarHideImpAfterFire;
        private boolean gargantuarImpHidden;

        private long lastAttackSerial;
        private int octopusIdleIndex;
        private boolean armDropped;
        private boolean headDropped;
        private boolean lastSunStealing;
        private int lastAuraTimer;
        private int lastTransformCooldown;
        private boolean lastDynamiteExploded;
        private boolean lastDodoFly;
        private boolean dodoJumping;
        private float dodoJumpFromX;
        private float dodoJumpElapsed;
        private float dodoJumpDuration;
        private boolean lastLaserStealing;
        private String armorVisualSignature;
        private boolean darkKnightVisual;

        private final Map<String, Boolean> butterPartBaseline =
            new java.util.LinkedHashMap<>();
        private boolean butterVisualActive;

        private final Map<String, Boolean> explorerTorchFireBaseline =
            new java.util.LinkedHashMap<>();
        private boolean explorerTorchStateInitialized;
        private boolean lastExplorerTorchLit = true;

        private int lastDamageHealth = Integer.MIN_VALUE;
        private float damageFlashCooldownRemaining;

        private final PianoVisual piano;
        private final ArcadeCabinetVisual cabinet;

        private boolean deathStarted;
        private float deathElapsed;
        private float deathDuration;

        private ZombieVisual(
            PamAnimationActor actor,
            ZombieAnimationResolver.ResolvedAnimations animations,
            PianoVisual piano,
            ArcadeCabinetVisual cabinet
        ) {
            this.actor = actor;
            this.animations = animations;
            this.piano = piano;
            this.cabinet = cabinet;
        }
    }
}
