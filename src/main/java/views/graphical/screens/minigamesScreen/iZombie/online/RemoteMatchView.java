package views.graphical.screens.minigamesScreen.iZombie.online;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ProjectileVisualData;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import com.badlogic.gdx.scenes.scene2d.ui.Image;

import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import com.badlogic.gdx.utils.Scaling;

import graphics.PvzGame;

import models.games.ChapterTheme;

import network.protocol.match.BrainNetState;
import network.protocol.match.MatchSnapshot;
import network.protocol.match.PlantNetState;
import network.protocol.match.ProjectileNetState;
import network.protocol.match.ZombieNetState;

import views.graphical.animation.EntityAnimationState;
import views.graphical.animation.PamAnimationActor;

import views.graphical.gameplay.actors.PlantActor;
import views.graphical.gameplay.actors.ProjectileActor;

import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;

import views.graphical.gameplay.zombie.ZombieAnimationResolver;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public final class RemoteMatchView
        extends Group {

    private static final String BRAIN_ASSET =
            "IMAGE_UI_CURRENCY_VALENBRAINZ_STACK_0";
    private static final String I_ZOMBIE_SUN_PRODUCER =
            "IZombieSunProducer";
    private static final int RED_LINE_COLUMN =
            6;

    private static final float SNAPSHOT_MOVE_TIME =
            0.11f;


    private final PvzGame game;

    private final BoardTransform transform;

    private final ZombieAnimationResolver
            zombieAnimationResolver;


    private final Group brainLayer =
            new Group();

    private final Group plantLayer =
            new Group();

    private final Group projectileLayer =
            new Group();

    private final Group zombieLayer =
            new Group();


    private final Map<Integer, Image>
            brainActors =
            new HashMap<>();


    private final Map<Integer, PlantActor>
            plantActors =
            new HashMap<>();

    private final Map<Integer, Integer>
            lastPlantHp =
            new HashMap<>();

    private final Map<Integer, Long>
            lastPlantActionSerial =
            new HashMap<>();


    private final Map<Integer, PamAnimationActor>
            zombieActors =
            new HashMap<>();

    private final Map<Integer, String>
            zombieAliases =
            new HashMap<>();

    private final Map<String, ZombieAnimationResolver.ResolvedAnimations>
            zombieAnimationsByAlias =
            new HashMap<>();

    private final Map<Integer, Integer>
            lastZombieRangedCooldown =
            new HashMap<>();

    private final Map<Integer, Boolean>
            zombieEatingStates =
            new HashMap<>();

    private final Map<Integer, Float>
            zombieOneShotRemaining =
            new HashMap<>();


    private final Map<Integer, ProjectileActor>
            projectileActors =
            new HashMap<>();


    private final Map<String, ProjectileVisualData>
            projectileVisualCache =
            new HashMap<>();

    private boolean firstSnapshotLogged;
    public RemoteMatchView(
            PvzGame game,
            BoardTransform transform
    ) {

        if (game == null) {

            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }


        if (transform == null) {

            throw new IllegalArgumentException(
                    "transform cannot be null"
            );
        }


        this.game =
                game;

        this.transform =
                transform;


        this.zombieAnimationResolver =
                new ZombieAnimationResolver(
                        game.getPamPlayer()
                );


        setTouchable(
                Touchable.disabled
        );

        addActor(
                brainLayer
        );

        addActor(
                plantLayer
        );

        addActor(
                projectileLayer
        );

        addActor(
                zombieLayer
        );


        addRedLine();
    }

    public void sync(
            MatchSnapshot snapshot
    ) {
        if (snapshot == null) {
            return;
        }

        if (!firstSnapshotLogged) {

            firstSnapshotLogged =
                    true;


            int plantCount =
                    snapshot.getPlants() == null
                            ? -1
                            : snapshot.getPlants().size();


            int zombieCount =
                    snapshot.getZombies() == null
                            ? -1
                            : snapshot.getZombies().size();


            System.out.println(
                    "[ONLINE SNAPSHOT] plants="
                            + plantCount
                            + " zombies="
                            + zombieCount
                            + " brains="
                            + (
                            snapshot.getBrains() == null
                                    ? -1
                                    : snapshot.getBrains().size()
                    )
            );


            if (snapshot.getPlants() != null) {

                for (PlantNetState plant :
                        snapshot.getPlants()) {

                    System.out.println(
                            "[ONLINE PLANT] "
                                    + plant.getName()
                                    + " row="
                                    + plant.getRow()
                                    + " column="
                                    + plant.getColumn()
                    );
                }
            }
        }
        syncBrains(
                snapshot
        );

        syncPlants(
                snapshot
        );

        syncProjectiles(
                snapshot
        );

        syncZombies(
                snapshot
        );
    }

    private void syncBrains(
            MatchSnapshot snapshot
    ) {

        if (snapshot.getBrains() == null) {
            return;
        }


        for (BrainNetState brain :
                snapshot.getBrains()) {

            if (brain == null) {
                continue;
            }


            int lane =
                    brain.getLane();


            if (lane < 0
                    || lane >= BoardTransform.ROWS) {

                continue;
            }


            Image actor =
                    brainActors.get(
                            lane
                    );


            if (actor == null) {

                actor =
                        createBrainActor(
                                lane
                        );


                brainActors.put(
                        lane,
                        actor
                );


                brainLayer.addActor(
                        actor
                );
            }


            actor.setVisible(
                    !brain.isEaten()
            );
        }
    }


    private Image createBrainActor(
            int lane
    ) {

        TextureRegion region =
                game.getTextureBank()
                        .region(
                                BRAIN_ASSET
                        );


        if (region == null) {

            throw new IllegalStateException(
                    "Brain asset not found: "
                            + BRAIN_ASSET
            );
        }


        Image image =
                new Image(
                        new TextureRegionDrawable(
                                region
                        )
                );


        image.setScaling(
                Scaling.fit
        );


        image.setTouchable(
                Touchable.disabled
        );


        float width =
                transform.tileWidth()
                        * 0.75f;


        float height =
                transform.tileHeight()
                        * 0.75f;


        float x =
                transform.tileX(
                        0
                )
                        + (
                        transform.tileWidth()
                                - width
                )
                        / 2f;


        float y =
                transform.tileY(
                        lane
                )
                        + (
                        transform.tileHeight()
                                - height
                )
                        / 2f;


        image.setBounds(
                x,
                y,
                width,
                height
        );


        return image;
    }

    private void syncPlants(
            MatchSnapshot snapshot
    ) {

        Set<Integer> activeIds =
                new HashSet<>();


        if (snapshot.getPlants() != null) {

            for (PlantNetState state :
                    snapshot.getPlants()) {

                if (state == null) {
                    continue;
                }


                int lane =
                        state.getRow();

                int column =
                        state.getColumn();


                if (!isValidTile(
                        lane,
                        column
                )) {

                    continue;
                }


                PlantData data =
                        resolvePlantData(
                                state.getName()
                        );


                if (data == null) {

                    continue;
                }


                int id =
                        state.getEntityId();


                activeIds.add(
                        id
                );


                PlantActor actor =
                        plantActors.get(
                                id
                        );


                if (actor == null) {

                    actor =
                            new PlantActor(
                                    game
                            );


                    actor.setPreviewMode(
                            false
                    );


                    actor.setPlant(
                            data
                    );


                    plantActors.put(
                            id,
                            actor
                    );


                    plantLayer.addActor(
                            actor
                    );

                    lastPlantActionSerial.put(
                            id,
                            state.getActionSerial()
                    );
                }

                syncPlantAction(
                        id,
                        state,
                        data,
                        actor
                );


                Integer previousHp =
                        lastPlantHp.put(
                                id,
                                state.getHp()
                        );


                if (previousHp != null
                        && state.getHp() < previousHp
                        && state.getHp() > 0) {

                    actor.flashDamage();
                }


                float x =
                        transform.tileX(
                                column
                        )
                                + transform.tileWidth()
                                / 2f;


                float y =
                        transform.tileY(
                                lane
                        )
                                + transform.tileHeight()
                                / 2f;


                actor.setPosition(
                        x,
                        y
                );
            }
        }


        Iterator<Map.Entry<Integer, PlantActor>>
                iterator =
                plantActors
                        .entrySet()
                        .iterator();


        while (iterator.hasNext()) {

            Map.Entry<Integer, PlantActor>
                    entry =
                    iterator.next();


            if (!activeIds.contains(
                    entry.getKey()
            )) {

                entry.getValue()
                        .remove();


                lastPlantHp.remove(
                        entry.getKey()
                );

                lastPlantActionSerial.remove(
                        entry.getKey()
                );


                iterator.remove();
            }
        }
    }

    private void syncPlantAction(
            int id,
            PlantNetState state,
            PlantData data,
            PlantActor actor
    ) {
        Long previousSerial = lastPlantActionSerial.put(
                id,
                state.getActionSerial()
        );

        if (previousSerial == null
                || previousSerial == state.getActionSerial()) {
            return;
        }

        String action = state.getAction();
        if (action == null) {
            return;
        }

        switch (action.trim().toUpperCase(Locale.ROOT)) {
            case "ATTACK" -> actor.playTemporaryAnimation(
                    resolveRemotePlantAttack(data)
            );
            case "PRODUCE" -> actor.playTemporaryAnimation("produce");
            case "EXPLODE" -> actor.playTerminalAnimation("attack");
            default -> {
            }
        }
    }

    private String resolveRemotePlantAttack(PlantData data) {
        if (data != null) {
            if (data.hasAnimation("attack")) {
                return "attack";
            }
            if (data.hasAnimation("attackBoth")) {
                return "attackBoth";
            }
            if (data.hasAnimation("attack1")) {
                return "attack1";
            }
        }
        return "attack";
    }

    private void syncZombies(
            MatchSnapshot snapshot
    ) {

        Set<Integer> activeIds =
                new HashSet<>();


        if (snapshot.getZombies() != null) {
            for (ZombieNetState state : snapshot.getZombies()) {
                if (state == null || state.isDead() || I_ZOMBIE_SUN_PRODUCER.equals(state.getAlias())) {
                    continue;
                }

                int lane = state.getLane();
                if (lane < 0 || lane >= BoardTransform.ROWS) {
                    continue;
                }
                int id = state.getEntityId();
                activeIds.add(id);

                PamAnimationActor actor = zombieActors.get(id);
                String oldAlias = zombieAliases.get(id);

                if (actor != null && oldAlias != null && !oldAlias.equals(state.getAlias())) {
                    actor.remove();
                    zombieActors.remove(id);
                    zombieAliases.remove(id);
                    clearZombieAnimationState(id);
                    actor = null;
                }


                if (actor == null) {
                    ZombieAnimationResolver.ResolvedAnimations animations =
                            resolveZombieAnimations(state.getAlias());

                    actor = createZombieActor(
                            state.getAlias(),
                            animations
                    );
                    if (actor == null) {
                        activeIds.remove(id);
                        continue;
                    }


                    zombieActors.put(id, actor);
                    zombieAliases.put(
                            id,
                            state.getAlias()
                    );

                    if (state.getRangedCooldown() >= 0) {
                        lastZombieRangedCooldown.put(
                                id,
                                state.getRangedCooldown()
                        );
                    }


                    zombieLayer.addActor(
                            actor
                    );


                    positionZombieImmediately(
                            actor,
                            state
                    );

                } else {

                    moveZombieSmoothly(
                            actor,
                            state
                    );
                }

                zombieEatingStates.put(
                        id,
                        state.isEating()
                );

                syncZombieAnimation(
                        id,
                        state,
                        actor
                );

                if (state.isFrozen()) {

                    actor.setColor(
                            0.68f,
                            0.86f,
                            1f,
                            1f
                    );

                } else {

                    actor.setColor(
                            Color.WHITE
                    );
                }
            }
        }


        Iterator<Map.Entry<Integer, PamAnimationActor>>
                iterator =
                zombieActors
                        .entrySet()
                        .iterator();


        while (iterator.hasNext()) {

            Map.Entry<Integer, PamAnimationActor>
                    entry =
                    iterator.next();


            if (!activeIds.contains(
                    entry.getKey()
            )) {

                entry.getValue()
                        .remove();


                zombieAliases.remove(
                        entry.getKey()
                );

                clearZombieAnimationState(
                        entry.getKey()
                );


                iterator.remove();
            }
        }
    }


    private PamAnimationActor createZombieActor(
            String alias,
            ZombieAnimationResolver.ResolvedAnimations animations
    ) {

        if (alias == null
                || alias.isBlank()) {

            return null;
        }


        try {

            if (animations == null) {
                return null;
            }

            String pamPath = animations.getPamPath();


            if (pamPath == null
                    || pamPath.isBlank()) {

                return null;
            }


            PamAnimationActor actor =
                    game.createPamActor(
                            pamPath,
                            animations.clip(
                                    EntityAnimationState.WALK
                            ),
                            0f,
                            0f,
                            true,
                            ZombieAnimationSystem
                                    .resolveVisibleParts(
                                            game.getPamPlayer(),
                                            pamPath,
                                            alias
                                    )
                    );


            actor.setTouchable(
                    Touchable.disabled
            );


            actor.setScale(
                    ZombieAnimationSystem.DEFAULT_SCALE
            );


            return actor;

        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                        "RemoteMatchView",
                        "Could not create zombie actor for alias: " + alias,
                        exception
                );
            }
            return null;
        }
    }

    private ZombieAnimationResolver.ResolvedAnimations resolveZombieAnimations(
            String alias
    ) {
        if (alias == null || alias.isBlank()) {
            return null;
        }

        if (zombieAnimationsByAlias.containsKey(alias)) {
            return zombieAnimationsByAlias.get(alias);
        }

        try {
            String pamPath = ZombieAnimationSystem.resolvePamPath(
                    ChapterTheme.MINIGAME,
                    alias
            );

            if (pamPath == null || pamPath.isBlank()) {
                return null;
            }

            ZombieAnimationResolver.ResolvedAnimations animations =
                    zombieAnimationResolver.resolve(alias, pamPath);

            zombieAnimationsByAlias.put(alias, animations);
            return animations;
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.error(
                        "RemoteMatchView",
                        "Could not resolve animations for zombie: " + alias,
                        exception
                );
            }
            return null;
        }
    }

    private void syncZombieAnimation(
            int id,
            ZombieNetState state,
            PamAnimationActor actor
    ) {
        ZombieAnimationResolver.ResolvedAnimations animations =
                resolveZombieAnimations(state.getAlias());

        if (animations == null) {
            return;
        }

        boolean rangedAttackStarted = false;

        if (state.getRangedCooldown() >= 0) {
            Integer previousCooldown = lastZombieRangedCooldown.put(
                    id,
                    state.getRangedCooldown()
            );

            rangedAttackStarted = previousCooldown != null
                    && state.getRangedCooldown() > previousCooldown;
        }

        if (rangedAttackStarted) {
            playZombieRangedAttack(
                    id,
                    state.getRangedAttackType(),
                    actor,
                    animations
            );
            return;
        }

        if (!zombieOneShotRemaining.containsKey(id)) {
            applyZombieBaseAnimation(
                    id,
                    actor,
                    animations
            );
        }
    }

    private void playZombieRangedAttack(
            int id,
            String rangedType,
            PamAnimationActor actor,
            ZombieAnimationResolver.ResolvedAnimations animations
    ) {
        String clip = findZombieRangedClip(
                rangedType,
                animations
        );

        actor.setPlaybackSpeed(1f);
        actor.play(clip, false);
        actor.restart();

        float duration = 0.6f;
        try {
            duration = Math.max(
                    0.05f,
                    game.getPamPlayer().clipDurationSeconds(
                            animations.getPamPath(),
                            clip
                    )
            );
        } catch (RuntimeException ignored) {
        }

        zombieOneShotRemaining.put(id, duration);
    }

    private String findZombieRangedClip(
            String rangedType,
            ZombieAnimationResolver.ResolvedAnimations animations
    ) {
        String normalizedType = rangedType == null
                ? ""
                : rangedType.trim().toUpperCase(Locale.ROOT);

        String[] candidates = switch (normalizedType) {
            case "SNOWBALL" -> new String[]{"throw", "attack"};
            case "OCTOPUS_NET" -> new String[]{"toss", "throw", "attack"};
            case "HOOK_PULL" -> new String[]{"cast", "attack"};
            case "LASER_BEAM" -> new String[]{"attack", "special"};
            case "JUGGLE_BALL" -> new String[]{"throw", "juggle", "attack"};
            default -> new String[]{"attack", "special"};
        };

        for (String candidate : candidates) {
            for (String available : animations.getAvailableClips()) {
                if (available != null
                        && available.equalsIgnoreCase(candidate)) {
                    return available;
                }
            }
        }

        return animations.clip(EntityAnimationState.ATTACK);
    }

    private void applyZombieBaseAnimation(
            int id,
            PamAnimationActor actor,
            ZombieAnimationResolver.ResolvedAnimations animations
    ) {
        EntityAnimationState baseState = zombieEatingStates.getOrDefault(
                id,
                false
        )
                ? EntityAnimationState.EAT
                : EntityAnimationState.WALK;

        actor.setPlaybackSpeed(1f);
        actor.play(animations.clip(baseState), true);
    }

    private void clearZombieAnimationState(int id) {
        lastZombieRangedCooldown.remove(id);
        zombieEatingStates.remove(id);
        zombieOneShotRemaining.remove(id);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        Iterator<Map.Entry<Integer, Float>> iterator =
                zombieOneShotRemaining.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, Float> entry = iterator.next();
            float remaining = entry.getValue() - Math.max(0f, delta);

            if (remaining > 0f) {
                entry.setValue(remaining);
                continue;
            }

            int id = entry.getKey();
            iterator.remove();

            PamAnimationActor actor = zombieActors.get(id);
            String alias = zombieAliases.get(id);
            ZombieAnimationResolver.ResolvedAnimations animations =
                    resolveZombieAnimations(alias);

            if (actor != null && animations != null) {
                applyZombieBaseAnimation(id, actor, animations);
            }
        }
    }


    private void positionZombieImmediately(
            PamAnimationActor actor,
            ZombieNetState state
    ) {

        float x =
                zombieX(
                        state
                );


        float y =
                zombieY(
                        state
                );


        actor.setPosition(
                x,
                y
        );
    }


    private void moveZombieSmoothly(
            PamAnimationActor actor,
            ZombieNetState state
    ) {

        float x =
                zombieX(
                        state
                );


        float y =
                zombieY(
                        state
                );


        actor.clearActions();


        actor.addAction(
                Actions.moveTo(
                        x,
                        y,
                        SNAPSHOT_MOVE_TIME,
                        Interpolation.linear
                )
        );
    }


    private float zombieX(
            ZombieNetState state
    ) {

        return transform
                .getArea()
                .x()
                + (
                state.getX()
                        + 0.5f
        )
                * transform.tileWidth();
    }


    private float zombieY(
            ZombieNetState state
    ) {

        return transform.tileY(
                state.getLane()
        )
                + transform.tileHeight()
                * 0.5f;
    }

    private void syncProjectiles(
            MatchSnapshot snapshot
    ) {

        Set<Integer> activeIds =
                new HashSet<>();


        if (snapshot.getProjectiles() != null) {

            for (ProjectileNetState state :
                    snapshot.getProjectiles()) {

                if (state == null) {
                    continue;
                }


                int id =
                        state.getEntityId();


                ProjectileVisualData visual =
                        resolveProjectileVisual(
                                state.getType()
                        );


                if (visual == null) {
                    continue;
                }


                activeIds.add(
                        id
                );


                ProjectileActor actor =
                        projectileActors.get(
                                id
                        );


                if (actor == null) {
                    actor =
                            new ProjectileActor(
                                    game,
                                    null,
                                    visual
                            );


                    projectileActors.put(
                            id,
                            actor
                    );


                    projectileLayer.addActor(
                            actor
                    );


                    actor.setProjectilePosition(
                            projectileX(
                                    state
                            ),
                            projectileY(
                                    state
                            )
                    );

                } else {

                    actor.clearActions();


                    actor.addAction(
                            Actions.moveTo(
                                    projectileX(
                                            state
                                    ),
                                    projectileY(
                                            state
                                    ),
                                    SNAPSHOT_MOVE_TIME,
                                    Interpolation.linear
                            )
                    );
                }
            }
        }


        Iterator<Map.Entry<Integer, ProjectileActor>>
                iterator =
                projectileActors
                        .entrySet()
                        .iterator();


        while (iterator.hasNext()) {

            Map.Entry<Integer, ProjectileActor>
                    entry =
                    iterator.next();


            if (!activeIds.contains(
                    entry.getKey()
            )) {

                entry.getValue()
                        .remove();

                iterator.remove();
            }
        }
    }


    private ProjectileVisualData resolveProjectileVisual(
            String projectileKey
    ) {

        if (projectileKey == null
                || projectileKey.isBlank()) {

            return null;
        }


        if (projectileVisualCache.containsKey(
                projectileKey
        )) {

            return projectileVisualCache.get(
                    projectileKey
            );
        }


        ProjectileVisualData found =
                null;


        for (PlantData data :
                PlantRegistry.getAll()) {

            if (data == null
                    || data.projectiles() == null) {

                continue;
            }


            ProjectileVisualData candidate =
                    data.projectile(
                            projectileKey
                    );


            if (candidate != null) {

                found =
                        candidate;

                break;
            }
        }


        projectileVisualCache.put(
                projectileKey,
                found
        );


        return found;
    }


    private float projectileX(
            ProjectileNetState state
    ) {

        BoardArea area =
                transform.getArea();


        return area.x()
                + (
                (float) state.getX()
                        + 0.5f
        )
                * transform.tileWidth();
    }


    private float projectileY(
            ProjectileNetState state
    ) {

        BoardArea area =
                transform.getArea();


        return area.y()
                + (
                BoardTransform.ROWS
                        - 1f
                        - (float) state.getY()
                        + 0.5f
        )
                * transform.tileHeight();
    }

    private void addRedLine() {

        Image redLine =
                new Image(
                        game.getSkin()
                                .newDrawable(
                                        "white_pixel",
                                        Color.RED
                                )
                );


        redLine.setTouchable(
                Touchable.disabled
        );


        BoardArea area =
                transform.getArea();


        float x =
                transform.tileX(
                        RED_LINE_COLUMN
                );


        redLine.setBounds(
                x - 2f,
                area.y(),
                4f,
                area.height()
        );


        addActor(
                redLine
        );


        redLine.toFront();
    }

    private boolean isValidTile(
            int lane,
            int column
    ) {

        return lane >= 0
                && lane < BoardTransform.ROWS
                && column >= 0
                && column < BoardTransform.COLUMNS;
    }
    private PlantData resolvePlantData(
            String plantName
    ) {

        if (plantName == null
                || plantName.isBlank()) {

            return null;
        }


        PlantData direct =
                PlantRegistry.getByName(
                        plantName
                );


        if (direct != null) {

            return direct;
        }


        String wanted =
                normalizePlantName(
                        plantName
                );


        for (PlantData candidate :
                PlantRegistry.getAll()) {

            if (candidate == null
                    || candidate.name() == null) {

                continue;
            }


            if (wanted.equals(
                    normalizePlantName(
                            candidate.name()
                    )
            )) {

                return candidate;
            }
        }


        if (Gdx.app != null) {

            Gdx.app.error(
                    "RemoteMatchView",
                    "Could not resolve plant from snapshot: "
                            + plantName
            );
        }


        return null;
    }


    private String normalizePlantName(
            String value
    ) {

        return value
                .trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
    }
}
