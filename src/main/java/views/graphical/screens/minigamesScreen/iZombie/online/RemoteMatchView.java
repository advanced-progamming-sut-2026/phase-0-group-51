package views.graphical.screens.minigamesScreen.iZombie.online;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantAction;
import models.Plant.PlantFactory;
import models.Zombie.Behavior.DamageReactionBehavior;
import models.Zombie.Behavior.ImpThrowBehavior;
import models.Zombie.Behavior.InstantKillBehavior;
import models.Zombie.Behavior.RangedAttackBehavior;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.effects.VisualEffectEvent;
import models.projectile.Projectile;
import network.protocol.match.BrainNetState;
import network.protocol.match.MatchSnapshot;
import network.protocol.match.PlantNetState;
import network.protocol.match.ProjectileNetState;
import network.protocol.match.ZombieNetState;
import network.protocol.match.VisualEffectNetState;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.PlacementHighlightOverlay;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.WorldEffectManager;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Renders authoritative online snapshots through the normal-game animation
 * managers. The model objects below are render mirrors only: this class never
 * advances their game logic.
 */
public final class RemoteMatchView extends Group {

    private static final String BRAIN_ASSET =
            "IMAGE_UI_CURRENCY_VALENBRAINZ_STACK_0";
    private static final String I_ZOMBIE_SUN_PRODUCER =
            "IZombieSunProducer";
    private static final int RED_LINE_COLUMN = 6;
    private static final float SERVER_TICK_SECONDS = 0.10f;

    private final PvzGame game;
    private final BoardTransform transform;

    private final Group brainLayer = new Group();
    private final Group zombieLayer = new Group();

    private final PlacementHighlightOverlay placementHighlight;
    private final PlantViewManager plantViewManager;
    private final ProjectileViewManager projectileViewManager;
    private final WorldEffectManager worldEffectManager;
    private final ZombieAnimationSystem zombieAnimationSystem;

    private final Board remoteBoard = new Board();
    private final Map<Integer, Plant> remotePlants = new HashMap<>();
    private final Map<Integer, Long> lastPlantActionSerial = new HashMap<>();
    private final Map<Integer, Zombie> remoteZombies = new HashMap<>();
    private final Map<Integer, Projectile> remoteProjectiles = new HashMap<>();

    private final Map<Integer, Image> brainActors = new HashMap<>();

    private int latestSnapshotTick;
    private float snapshotElapsed;
    private float snapshotDuration = SERVER_TICK_SECONDS;
    private boolean hasSnapshot;

    public RemoteMatchView(
            PvzGame game,
            BoardTransform transform,
            Stage worldStage
    ) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        if (transform == null) {
            throw new IllegalArgumentException("transform cannot be null");
        }
        if (worldStage == null) {
            throw new IllegalArgumentException("worldStage cannot be null");
        }

        this.game = game;
        this.transform = transform;
        setTouchable(Touchable.disabled);

        placementHighlight = new PlacementHighlightOverlay(game, transform);
        plantViewManager = new PlantViewManager(game, transform);
        projectileViewManager = new ProjectileViewManager(game, transform);
        worldEffectManager = new WorldEffectManager(game, transform);
        zombieAnimationSystem = new ZombieAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                transform,
                ChapterTheme.MINIGAME,
                ZombieAnimationSystem.DEFAULT_SCALE,
                null,
                zombieLayer
        );

        addActor(brainLayer);
        addActor(placementHighlight);
        addActor(plantViewManager);
        addActor(projectileViewManager);
        addActor(worldEffectManager);
        addActor(zombieLayer);
        addRedLine();
    }

    public void sync(MatchSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (hasSnapshot && snapshot.getTick() > latestSnapshotTick) {
            int tickDelta = snapshot.getTick() - latestSnapshotTick;
            snapshotDuration = Math.max(
                    SERVER_TICK_SECONDS,
                    tickDelta * SERVER_TICK_SECONDS
            );
        } else {
            snapshotDuration = SERVER_TICK_SECONDS;
        }
        snapshotElapsed = 0f;

        syncBrains(snapshot);
        syncPlants(snapshot);
        syncProjectiles(snapshot);
        syncZombies(snapshot);
        syncVisualEffects(snapshot);
        latestSnapshotTick = snapshot.getTick();
        hasSnapshot = true;
    }

    @Override
    public void act(float delta) {
        float safeDelta = Math.max(0f, delta);
        if (hasSnapshot) {
            snapshotElapsed = Math.min(
                    snapshotDuration,
                    snapshotElapsed + safeDelta
            );
            float partialTick = snapshotDuration <= 0f
                    ? 1f
                    : snapshotElapsed / snapshotDuration;

            projectileViewManager.sync(
                    remoteProjectiles.values(),
                    partialTick
            );
            zombieAnimationSystem.update(
                    safeDelta,
                    partialTick,
                    latestSnapshotTick,
                    remoteZombies.values()
            );
            worldEffectManager.toFront();
        }
        super.act(delta);
    }

    public void showPlacementHighlight(int lane, int column) {
        placementHighlight.show(lane, column);
    }

    public void hidePlacementHighlight() {
        placementHighlight.hide();
    }

    private void syncPlants(MatchSnapshot snapshot) {
        clearRemoteBoardPlants();
        Set<Integer> activeIds = new HashSet<>();

        if (snapshot.getPlants() != null) {
            for (PlantNetState state : snapshot.getPlants()) {
                if (state == null
                        || !isValidTile(state.getRow(), state.getColumn())) {
                    continue;
                }

                PlantData data = resolvePlantData(state.getName());
                if (data == null) {
                    continue;
                }

                int id = state.getEntityId();
                activeIds.add(id);
                Plant plant = remotePlants.get(id);
                boolean created = plant == null
                        || !plant.getName().equalsIgnoreCase(data.name());

                if (created) {
                    try {
                        plant = PlantFactory.create(data.name());
                    } catch (RuntimeException exception) {
                        logError(
                                "Could not create remote plant: " + data.name(),
                                exception
                        );
                        activeIds.remove(id);
                        continue;
                    }
                    remotePlants.put(id, plant);
                    lastPlantActionSerial.put(id, state.getActionSerial());
                    plant.setLastAction(parsePlantAction(state.getAction()));
                    plant.setActionSerial(state.getActionSerial());
                } else {
                    syncPlantAction(id, plant, state);
                }

                plant.setPosX(state.getColumn());
                plant.setPosY(state.getRow());
                plant.setCurrentHP(state.getHp());
                plant.setLevel(Math.max(1, state.getLevel()));
                plant.setMarkedForRemoval(false);

                Tile tile = remoteBoard.getTile(
                        state.getRow(),
                        state.getColumn()
                );
                if (tile != null) {
                    tile.setPlant(plant);
                }
            }
        }

        remotePlants.keySet().removeIf(id -> !activeIds.contains(id));
        lastPlantActionSerial.keySet().removeIf(
                id -> !activeIds.contains(id)
        );
        plantViewManager.sync(remoteBoard);
    }

    private void syncPlantAction(
            int id,
            Plant plant,
            PlantNetState state
    ) {
        Long previous = lastPlantActionSerial.put(
                id,
                state.getActionSerial()
        );
        if (previous == null || previous == state.getActionSerial()) {
            return;
        }
        plant.setLastAction(parsePlantAction(state.getAction()));
        plant.setActionSerial(state.getActionSerial());
    }

    private PlantAction parsePlantAction(String value) {
        if (value == null || value.isBlank()) {
            return PlantAction.NONE;
        }
        try {
            return PlantAction.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ignored) {
            return PlantAction.NONE;
        }
    }

    private void clearRemoteBoardPlants() {
        for (int lane = 0; lane < remoteBoard.getLaneCount(); lane++) {
            for (int column = 0;
                 column < remoteBoard.getColumnCount();
                 column++) {
                Tile tile = remoteBoard.getTile(lane, column);
                if (tile == null) {
                    continue;
                }
                for (Plant plant : tile.getPlants()) {
                    tile.removeSpecificPlant(plant);
                }
            }
        }
    }

    private void syncZombies(MatchSnapshot snapshot) {
        Set<Integer> activeIds = new HashSet<>();

        if (snapshot.getZombies() != null) {
            for (ZombieNetState state : snapshot.getZombies()) {
                if (state == null
                        || I_ZOMBIE_SUN_PRODUCER.equals(state.getAlias())
                        || state.getLane() < 0
                        || state.getLane() >= BoardTransform.ROWS) {
                    continue;
                }

                int id = state.getEntityId();
                activeIds.add(id);
                Zombie zombie = remoteZombies.get(id);

                if (zombie == null
                        || !zombie.getAlias().equals(state.getAlias())) {
                    try {
                        zombie = ZombieRegistry.spawn(state.getAlias());
                    } catch (RuntimeException exception) {
                        logError(
                                "Could not create remote zombie: "
                                        + state.getAlias(),
                                exception
                        );
                        activeIds.remove(id);
                        continue;
                    }
                    remoteZombies.put(id, zombie);
                }

                syncZombieModel(zombie, state);
            }
        }

        remoteZombies.keySet().removeIf(id -> !activeIds.contains(id));
    }

    private void syncZombieModel(
            Zombie zombie,
            ZombieNetState state
    ) {
        zombie.setLane(state.getLane());
        zombie.setX(state.getX());
        zombie.setMaxHitpoints(Math.max(1, state.getMaxHp()));
        zombie.setHitpoints(Math.max(0, state.getHp()));
        zombie.setEating(state.isEating());
        zombie.setGlowing(state.isGlowing());
        zombie.setAttackSerial(state.getAttackSerial());

        if (state.isFrozen()) {
            if (!zombie.isFrozen()) {
                zombie.applyFreeze(1);
            }
        } else if (zombie.isFrozen() || zombie.isChilled()) {
            zombie.clearColdEffects();
        }

        RangedAttackBehavior ranged =
                zombie.getBehavior(RangedAttackBehavior.class);
        if (ranged != null) {
            if (state.getRangedCooldown() >= 0) {
                ranged.setCooldown(state.getRangedCooldown());
            }
            ranged.setOctopusHasTarget(state.isRangedHasTarget());
        }

        // Mirror the authoritative behavior flags so the local animation
        // system (which is never actually ticking these render-mirror
        // zombies) can still play rage / spin / smash-tackle / imp-throw /
        // octopus-toss animations instead of only ever falling back to
        // walk or eat.
        DamageReactionBehavior reaction =
                zombie.getBehavior(DamageReactionBehavior.class);
        if (reaction != null) {
            reaction.setRaged(state.isRaged());
            reaction.setSpinning(state.isSpinning());
        }

        InstantKillBehavior contact =
                zombie.getBehavior(InstantKillBehavior.class);
        if (contact != null) {
            contact.setHasKilled(state.isHasKilled());
        }

        ImpThrowBehavior impThrow =
                zombie.getBehavior(ImpThrowBehavior.class);
        if (impThrow != null) {
            impThrow.setFired(state.isImpFired());
        }

        zombie.setDead(state.isDead());
    }

    private void syncVisualEffects(MatchSnapshot snapshot) {
        if (snapshot.getVisualEffects() == null) {
            return;
        }
        for (VisualEffectNetState state : snapshot.getVisualEffects()) {
            if (state == null || state.getType() == null) {
                continue;
            }
            try {
                VisualEffectEvent.Type type = VisualEffectEvent.Type.valueOf(
                        state.getType().trim().toUpperCase(Locale.ROOT)
                );
                worldEffectManager.play(new VisualEffectEvent(
                        type,
                        state.getX(),
                        state.getY()
                ));
            } catch (IllegalArgumentException ignored) {
                logError(
                        "Unknown remote visual effect: " + state.getType(),
                        null
                );
            }
        }
    }

    private void syncBrains(MatchSnapshot snapshot) {
        if (snapshot.getBrains() == null) {
            return;
        }
        for (BrainNetState brain : snapshot.getBrains()) {
            if (brain == null
                    || brain.getLane() < 0
                    || brain.getLane() >= BoardTransform.ROWS) {
                continue;
            }

            Image actor = brainActors.get(brain.getLane());
            if (actor == null) {
                actor = createBrainActor(brain.getLane());
                brainActors.put(brain.getLane(), actor);
                brainLayer.addActor(actor);
            }
            actor.setVisible(!brain.isEaten());
        }
    }

    private Image createBrainActor(int lane) {
        TextureRegion region = game.getTextureBank().region(BRAIN_ASSET);
        if (region == null) {
            throw new IllegalStateException(
                    "Brain asset not found: " + BRAIN_ASSET
            );
        }

        Image image = new Image(new TextureRegionDrawable(region));
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);

        float width = transform.tileWidth() * 0.75f;
        float height = transform.tileHeight() * 0.75f;
        float x = transform.tileX(0)
                + (transform.tileWidth() - width) / 2f;
        float y = transform.tileY(lane)
                + (transform.tileHeight() - height) / 2f;
        image.setBounds(x, y, width, height);
        return image;
    }

    private void syncProjectiles(MatchSnapshot snapshot) {
        Set<Integer> activeIds = new HashSet<>();

        if (snapshot.getProjectiles() != null) {
            for (ProjectileNetState state : snapshot.getProjectiles()) {
                if (state == null) {
                    continue;
                }

                int id = state.getEntityId();
                Projectile projectile = remoteProjectiles.get(id);

                if (projectile == null) {
                    Plant source = resolveProjectileSource(state);
                    if (source == null) {
                        continue;
                    }
                    projectile = Projectile.renderMirror(
                            source,
                            state.getType(),
                            state.getVisualReleaseId(),
                            state.getX(),
                            state.getY(),
                            state.getVisualArcOffset(),
                            state.isLaunched(),
                            state.getTargetX(),
                            state.getTargetY()
                    );
                    remoteProjectiles.put(id, projectile);
                } else {
                    syncProjectileSource(projectile, state);
                    projectile.syncRenderSnapshot(
                            state.getX(),
                            state.getY(),
                            state.getVisualArcOffset(),
                            state.isLaunched()
                    );
                }

                activeIds.add(id);
            }
        }

        remoteProjectiles.keySet().removeIf(id -> !activeIds.contains(id));
    }

    private Plant resolveProjectileSource(ProjectileNetState state) {
        Plant source = remotePlants.get(state.getSourcePlantEntityId());
        if (source != null) {
            syncProjectileSourcePosition(source, state);
            return source;
        }

        PlantData data = resolvePlantData(state.getSourcePlantName());
        if (data == null) {
            return null;
        }

        try {
            source = PlantFactory.create(data.name());
            syncProjectileSourcePosition(source, state);
            return source;
        } catch (RuntimeException exception) {
            logError(
                    "Could not create projectile source: " + data.name(),
                    exception
            );
            return null;
        }
    }

    private void syncProjectileSource(
            Projectile projectile,
            ProjectileNetState state
    ) {
        Plant source = projectile.getSourcePlant();
        if (source != null) {
            syncProjectileSourcePosition(source, state);
        }
    }

    private void syncProjectileSourcePosition(
            Plant source,
            ProjectileNetState state
    ) {
        if (state.getSourceColumn() >= 0) {
            source.setPosX(state.getSourceColumn());
        }
        if (state.getSourceRow() >= 0) {
            source.setPosY(state.getSourceRow());
        }
    }

    private void addRedLine() {
        Image redLine = new Image(
                game.getSkin().newDrawable("white_pixel", Color.RED)
        );
        redLine.setTouchable(Touchable.disabled);
        BoardArea area = transform.getArea();
        float x = transform.tileX(RED_LINE_COLUMN);
        redLine.setBounds(x - 2f, area.y(), 4f, area.height());
        addActor(redLine);
        redLine.toFront();
    }

    private boolean isValidTile(int lane, int column) {
        return lane >= 0
                && lane < BoardTransform.ROWS
                && column >= 0
                && column < BoardTransform.COLUMNS;
    }

    private PlantData resolvePlantData(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return null;
        }

        PlantData direct = PlantRegistry.getByName(plantName);
        if (direct != null) {
            return direct;
        }

        String wanted = normalizePlantName(plantName);
        for (PlantData candidate : PlantRegistry.getAll()) {
            if (candidate != null
                    && candidate.name() != null
                    && wanted.equals(normalizePlantName(candidate.name()))) {
                return candidate;
            }
        }

        logError(
                "Could not resolve plant from snapshot: " + plantName,
                null
        );
        return null;
    }

    private String normalizePlantName(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
    }

    private void logError(String message, RuntimeException exception) {
        if (Gdx.app == null) {
            return;
        }
        if (exception == null) {
            Gdx.app.error("RemoteMatchView", message);
        } else {
            Gdx.app.error("RemoteMatchView", message, exception);
        }
    }
}
