package views.graphical.gameplay.manager;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import graphics.PvzGame;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import views.graphical.gameplay.actors.PlantActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.*;

public class PlantViewManager extends Group {

    private final PvzGame game;
    private final BoardTransform transform;

    private final Map<Plant, PlantActor> plantActors = new IdentityHashMap<>();
    private final Map<Plant, Long> lastSeenActionSerial = new IdentityHashMap<>();

    public PlantViewManager(
            PvzGame game,
            BoardTransform transform
    ) {
        this.game = game;
        this.transform = transform;

        setTouchable(Touchable.disabled);
    }

    public void sync(Board board) {
        Set<Plant> plantsOnBoard = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0;
                 column < board.getColumnCount();
                 column++) {

                Tile tile = board.getTile(lane, column);

                Plant plant = tile.getTopPlant();

                if (plant == null) {
                    continue;
                }

                plantsOnBoard.add(plant);

                PlantActor actor = plantActors.get(plant);

                if (actor == null) {
                    actor = createPlantActor(plant);

                    plantActors.put(plant, actor);
                    lastSeenActionSerial.put(plant, plant.getActionSerial());
                    addActor(actor);
                }
                syncPlantBaseAnimation(plant, actor);
                syncPlantAction(plant, actor);
                positionPlant(actor, lane, column);
            }
        }

        removeMissingPlants(plantsOnBoard);
        sortPlantsByDepth();
    }
    private void syncPlantAction(Plant plant, PlantActor actor) {
        long lastSeen = lastSeenActionSerial.getOrDefault(plant, plant.getActionSerial());

        if (lastSeen == plant.getActionSerial()) {
            return;
        }

        switch (plant.getLastAction()) {
            case ATTACK -> actor.playTemporaryAnimation("attack");
            case PRODUCE -> actor.playTemporaryAnimation(resolveProduceAnimation(plant));
            case EXPLODE -> actor.playTerminalAnimation("attack");
            case NONE -> {
            }
        }
        lastSeenActionSerial.put(plant, plant.getActionSerial());
    }
    private String resolveProduceAnimation(Plant plant) {
        if (plant.getId() == 3) {
            return "produce" + plant.getGrowthStage();
        }
        return "produce";
    }
    private void sortPlantsByDepth() {
        getChildren().sort((first, second) -> Float.compare(second.getY(), first.getY()));
    }
    private void removeMissingPlants(Set<Plant> plantsOnBoard) {
        Iterator<Map.Entry<Plant, PlantActor>> iterator = plantActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Plant, PlantActor> entry = iterator.next();
            if (!plantsOnBoard.contains(entry.getKey())) {
                Plant plant = entry.getKey();
                entry.getValue().remove();
                lastSeenActionSerial.remove(plant);
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
}