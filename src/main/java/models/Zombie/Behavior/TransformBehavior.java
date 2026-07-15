package models.Zombie.Behavior;

import lombok.Getter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Getter
public class TransformBehavior implements PersistableBehavior {

    private final TransformType type;
    private final int intervalTicks;
    private final int range;
    private int cooldown;

    private final List<Plant> transformedPlants = new ArrayList<>();

    public TransformBehavior(TransformType type, int intervalTicks, int range) {
        this.type = type;
        this.intervalTicks = intervalTicks;
        this.range = range;
        this.cooldown = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (--cooldown > 0) {
            return;
        }
        cooldown = intervalTicks;
        if (type != TransformType.SHEEP_TRANSFORM) {
            return;
        }
        Plant target = gs.getBoard()
            .findNearestPlantInRange(zombie.getLane(), (int) zombie.getX(), range);
        if (target != null && !target.isTransformed()) {
            target.setTransformed(true);
            transformedPlants.add(target);
        }
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return true;
    }

    @Override
    public void onDeath(Zombie zombie, GameState gs) {
        for (Plant plant : transformedPlants) {
            plant.setTransformed(false);
        }
        transformedPlants.clear();
    }


    public enum TransformType {
        SHEEP_TRANSFORM
    }

    @Override
    public String behaviorType() {
        return "TRANSFORM";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("transform_type", getType().name());
        cols.put("transform_interval", getIntervalTicks());
        cols.put("transform_range", getRange());
    }

    @Override
    public ZombieBehavior copy() {
        return new TransformBehavior(type, intervalTicks, range);
    }
}
