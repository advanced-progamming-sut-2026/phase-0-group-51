package views.graphical.gameplay.manager;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import graphics.PvzGame;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantTag;
import views.graphical.gameplay.actors.PlantActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.*;

public class PlantViewManager extends Group {

    private final PvzGame game;
    private final BoardTransform transform;
    private final Group renderLayer;

    private final Map<Plant, PlantActor> plantActors = new IdentityHashMap<>();
    private final Map<Plant, Long> lastSeenActionSerial = new IdentityHashMap<>();
    private final Map<Plant, Long> lastSeenPlantFoodSerial = new IdentityHashMap<>();
    private final Map<Plant, Integer> lastSeenHealth = new IdentityHashMap<>();
    private final Map<Plant, Integer> lastSeenOctopusHealth = new IdentityHashMap<>();
    private final Map<Plant, String> lastSeenChargeAnimation = new IdentityHashMap<>();
    private final Map<PlantActor, Integer> actorLayers = new IdentityHashMap<>();
    private final Set<Plant> squashAnimationsStarted =
            Collections.newSetFromMap(
                    new IdentityHashMap<Plant, Boolean>()
            );

    public PlantViewManager(
            PvzGame game,
            BoardTransform transform
    ) {
        this(game, transform, null);
    }

    public PlantViewManager(
            PvzGame game,
            BoardTransform transform,
            Group renderLayer
    ) {
        this.game = game;
        this.transform = transform;
        this.renderLayer = renderLayer == null ? this : renderLayer;

        setTouchable(Touchable.disabled);
    }

    public void sync(Board board) {
        Set<Plant> plantsOnBoard = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0;
                 column < board.getColumnCount();
                 column++) {

                Tile tile = board.getTile(lane, column);
                syncPlant(tile.getLilyPadPlant(), lane, column, 0, plantsOnBoard);
                syncPlant(tile.getTopPlant(), lane, column, 1, plantsOnBoard);
                syncPlant(tile.getPumpkinPlant(), lane, column, 2, plantsOnBoard);
            }
        }

        removeMissingPlants(plantsOnBoard);
        sortPlantsByDepth();
    }
    private void syncPlant(Plant plant, int lane, int column, int layer, Set<Plant> plantsOnBoard) {
        if (plant == null) {
            return;
        }

        plantsOnBoard.add(plant);

        PlantActor actor = plantActors.get(plant);

        if (actor == null) {
            actor = createPlantActor(plant);
            plantActors.put(plant, actor);
            lastSeenActionSerial.put(plant, plant.getActionSerial());
            lastSeenHealth.put(plant, plant.getCurrentHP());
            addPlantActor(actor);
        }
        actorLayers.put(actor, layer);
        syncPlantBaseAnimation(plant, actor);
        syncChargeAnimation(plant, actor);

        boolean squashAnimating = syncSquashAnimation(
                plant,
                actor,
                lane,
                column
        );

        if (!squashAnimating) {
            syncPlantAction(plant, actor);
            positionPlant(actor, lane, column);
        }

        syncPlantFoodEffect(plant, actor);
        syncPlantFoodAnimation(plant, actor);
        syncDamageFlash(plant, actor);
        syncOctopusVisual(plant, actor);
        syncFrostVisual(plant, actor);
    }
    private boolean syncSquashAnimation(
            Plant plant,
            PlantActor actor,
            int lane,
            int column
    ) {
        if (!plant.isSquashJumping()) {
            squashAnimationsStarted.remove(plant);
            return false;
        }

        if (squashAnimationsStarted.add(plant)) {
            positionPlant(actor, lane, column);

            int targetLane = Math.max(
                    0,
                    Math.min(
                            BoardTransform.ROWS - 1,
                            plant.getSquashTargetLane()
                    )
            );
            int targetColumn = Math.max(
                    0,
                    Math.min(
                            BoardTransform.COLUMNS - 1,
                            plant.getSquashTargetColumn()
                    )
            );

            float targetX = transform.tileX(targetColumn)
                    + transform.tileWidth() / 2f;
            float targetY = transform.tileY(targetLane)
                    + transform.tileHeight() / 2f;

            actor.playSquashJump(
                    targetX,
                    targetY,
                    plant::markSquashLanded,
                    plant::finishSquashJump
            );
        }

        lastSeenActionSerial.put(
                plant,
                plant.getActionSerial()
        );
        return true;
    }

    private void syncPlantAction(Plant plant, PlantActor actor) {
        long lastSeen = lastSeenActionSerial.getOrDefault(plant, plant.getActionSerial());

        if (lastSeen == plant.getActionSerial()) {
            return;
        }

        switch (plant.getLastAction()) {
            case ATTACK -> actor.playTemporaryAnimation(resolveAttackAnimation(plant));
            case PRODUCE -> actor.playTemporaryAnimation(resolveProduceAnimation(plant));
            case EXPLODE -> actor.playTerminalAnimation("attack");
            case NONE -> {
            }
        }
        lastSeenActionSerial.put(plant, plant.getActionSerial());
    }

    private void syncPlantFoodEffect(
            Plant plant,
            PlantActor actor
    ) {
        long current =
                plant.getPlantFoodVisualSerial();

        long lastSeen =
                lastSeenPlantFoodSerial.getOrDefault(
                        plant,
                        0L
                );

        if (current > lastSeen) {
            actor.playPlantFoodEffect();
            actor.playPlantFoodAnimation();
        }

        lastSeenPlantFoodSerial.put(
                plant,
                current
        );
    }
    private void syncPlantFoodAnimation(
            Plant plant,
            PlantActor actor
    ) {
        actor.syncPlantFoodAnimation(plant.isOnPlantFood());
    }

    private void syncOctopusVisual(
            Plant plant,
            PlantActor actor
    ) {
        actor.syncOctopusVisual(
                plant.hasOctopus()
        );

        int currentOctopusHealth = plant.getOctopusHP();
        int previousOctopusHealth =
                lastSeenOctopusHealth.getOrDefault(
                        plant,
                        currentOctopusHealth
                );

        if (currentOctopusHealth < previousOctopusHealth
                && currentOctopusHealth > 0) {
            actor.flashOctopusDamage();
        }

        lastSeenOctopusHealth.put(
                plant,
                currentOctopusHealth
        );
    }

    private void syncFrostVisual(
            Plant plant,
            PlantActor actor
    ) {
        actor.syncFrost(
                plant.getFrostLevel(),
                plant.getIceHealth()
        );
    }

    private void syncDamageFlash(
            Plant plant,
            PlantActor actor
    ) {
        int currentHealth = plant.getCurrentHP();
        int previousHealth = lastSeenHealth.getOrDefault(plant, currentHealth);

        if (currentHealth < previousHealth && currentHealth > 0) {
            actor.flashDamage();
        }

        lastSeenHealth.put(plant, currentHealth);
    }

    private void syncChargeAnimation(
            Plant plant,
            PlantActor actor
    ) {
        if (!plant.hasTag(PlantTag.CHARGE)) {
            return;
        }

        String animation;

        if (!plant.isChargeReady()) {
            animation = "charge";

            if (!hasAnimation(plant, animation)) {
                animation = "unarmed";
            }
        } else {
            animation = "armed";

            if (!hasAnimation(plant, animation)) {
                animation = "idle";
            }
        }

        String previous =
                lastSeenChargeAnimation.get(plant);

        if (!animation.equals(previous)) {
            actor.setBaseAnimation(animation);
            lastSeenChargeAnimation.put(
                    plant,
                    animation
            );
        }
    }

    private boolean hasAnimation(
            Plant plant,
            String animation
    ) {
        PlantData data =
                PlantRegistry.getById(
                        plant.getId()
                );

        return data != null
                && data.hasAnimation(animation);
    }

    private String resolveAttackAnimation(Plant plant) {
        PlantData data = PlantRegistry.getById(plant.getId());

        if (data == null) {
            return "attack";
        }

        if (data.hasAnimation("attack")) {
            return "attack";
        }

        String stackedAttack = "attack" + plant.getStackCount();
        if (data.hasAnimation(stackedAttack)) {
            return stackedAttack;
        }

        if (data.hasAnimation("attackBoth")) {
            return "attackBoth";
        }

        if (data.hasAnimation("attack1")) {
            return "attack1";
        }

        return "attack";
    }

    private String resolveProduceAnimation(Plant plant) {
        if (plant.getId() == 3) {
            return "produce" + plant.getGrowthStage();
        }
        return "produce";
    }
    private void addPlantActor(PlantActor actor) {
        if (actor == null) {
            return;
        }

        renderLayer.addActor(actor);
    }

    private void sortPlantsByDepth() {
        if (renderLayer instanceof DepthSortedEntityLayer depthLayer) {
            for (Map.Entry<PlantActor, Integer> entry : actorLayers.entrySet()) {
                DepthSortedEntityLayer.setDepthPriority(
                        entry.getKey(),
                        DepthSortedEntityLayer.PLANT_BASE_PRIORITY
                                + entry.getValue()
                );
            }
            depthLayer.sortNow();
            return;
        }
        getChildren().sort(
                (first, second) -> {

                    int rowOrder =
                            Float.compare(
                                    second.getY(),
                                    first.getY()
                            );

                    if (rowOrder != 0) {
                        return rowOrder;
                    }

                    int firstLayer =
                            actorLayers.getOrDefault(
                                    (PlantActor) first,
                                    1
                            );

                    int secondLayer =
                            actorLayers.getOrDefault(
                                    (PlantActor) second,
                                    1
                            );

                    return Integer.compare(
                            firstLayer,
                            secondLayer
                    );
                }
        );
    }
    private void removeMissingPlants(Set<Plant> plantsOnBoard) {
        Iterator<Map.Entry<Plant, PlantActor>> iterator = plantActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Plant, PlantActor> entry = iterator.next();
            if (!plantsOnBoard.contains(entry.getKey())) {
                Plant plant = entry.getKey();

                PlantActor actor = entry.getValue();

                actor.remove();

                actorLayers.remove(actor);

                lastSeenActionSerial.remove(plant);
                lastSeenPlantFoodSerial.remove(plant);
                lastSeenHealth.remove(plant);
                lastSeenOctopusHealth.remove(plant);
                lastSeenChargeAnimation.remove(plant);
                iterator.remove();
            }
        }
    }
    private void syncPlantBaseAnimation(Plant plant, PlantActor actor) {
        if (plant.getId() == 3) {
            actor.setBaseAnimation("idle" + plant.getGrowthStage());
        } else if (plant.getId() == 44) {
            actor.setBaseAnimation(resolveWallNutAnimation(plant));
        }
    }
    private String resolveWallNutAnimation(
            Plant plant
    ) {
        float maxHp = plant.getPlantStat().maxHp();

        if (maxHp <= 0) {
            return "idle";
        }

        float hpRatio =
                plant.getCurrentHP() / maxHp;

        if (hpRatio > 0.75f) {
            return "idle";
        }

        if (hpRatio > 0.50f) {
            return "damage1";
        }

        if (hpRatio > 0.25f) {
            return "damage2";
        }

        return "damage3";
    }

    private PlantActor createPlantActor(
            Plant plant
    ) {
        PlantData data =
                PlantRegistry.getById(
                        plant.getId()
                );

        if (data == null) {
            throw new IllegalStateException(
                    "No PlantData found for plant id: "
                            + plant.getId()
            );
        }

        PlantActor actor = new PlantActor(game);

        actor.setPreviewMode(false);
        actor.setPlant(data);

        return actor;
    }

    private void positionPlant(
            PlantActor actor,
            int lane,
            int column
    ) {
        float centerX =
                transform.tileX(column)
                        + transform.tileWidth() / 2f;

        float centerY =
                transform.tileY(lane)
                        + transform.tileHeight() / 2f;

        actor.setPosition(
                centerX,
                centerY
        );
    }
    public void animateSync(Board board) {

        Set<Plant> plantsOnBoard =
                Collections.newSetFromMap(new IdentityHashMap<>());


        for (int lane = 0; lane < board.getLaneCount(); lane++) {

            for (int column = 0;
                 column < board.getColumnCount();
                 column++) {


                Tile tile = board.getTile(lane, column);

                animatePlant(
                        tile.getLilyPadPlant(),
                        lane,
                        column,
                        0,
                        plantsOnBoard
                );

                animatePlant(
                        tile.getTopPlant(),
                        lane,
                        column,
                        1,
                        plantsOnBoard
                );

                animatePlant(
                        tile.getPumpkinPlant(),
                        lane,
                        column,
                        2,
                        plantsOnBoard
                );
            }
        }


        removeMissingPlants(plantsOnBoard);
        sortPlantsByDepth();
    }
    private void animatePlant(
            Plant plant,
            int lane,
            int column,
            int layer,
            Set<Plant> plantsOnBoard
    ) {

        if (plant == null) {
            return;
        }


        plantsOnBoard.add(plant);


        PlantActor actor =
                plantActors.get(plant);


        // اگر تازه آمده، بسازش
        if (actor == null) {

            actor = createPlantActor(plant);

            plantActors.put(plant, actor);
            lastSeenActionSerial.put(plant, plant.getActionSerial());
            lastSeenHealth.put(plant, plant.getCurrentHP());
            addPlantActor(actor);

            syncPlantBaseAnimation(
                    plant,
                    actor
            );
        }

        actorLayers.put(actor, layer);

        if (syncSquashAnimation(plant, actor, lane, column)) {
            syncPlantFoodEffect(plant, actor);
            syncPlantFoodAnimation(plant, actor);
            syncDamageFlash(plant, actor);
            syncOctopusVisual(plant, actor);
            syncFrostVisual(plant, actor);
            return;
        }


        float targetX =
                transform.tileX(column)
                        + transform.tileWidth() / 2f;


        float targetY =
                transform.tileY(lane)
                        + transform.tileHeight() / 2f;



        actor.clearActions();


        actor.addAction(
                Actions.moveTo(
                        targetX,
                        targetY,
                        0.35f,
                        Interpolation.smooth
                )
        );

        syncChargeAnimation(plant, actor);
        syncPlantAction(plant, actor);
        syncPlantFoodEffect(plant, actor);
        syncPlantFoodAnimation(plant, actor);
        syncDamageFlash(plant, actor);
        syncOctopusVisual(plant, actor);
        syncFrostVisual(plant, actor);
    }
}
