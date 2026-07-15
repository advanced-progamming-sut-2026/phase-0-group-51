package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.util.List;

public enum Melee implements PlantType {
    BONK_CHOY(39, AttackShape.SAME_LANE, 1.0, ElementType.NORMAL),
    PHAT_BEET(40, AttackShape.SQUARE_3X3, 1.0, ElementType.NORMAL),
    WASABI_WHIP(42, AttackShape.SAME_LANE, 1.0, ElementType.FIRE);

    private final int id;
    private final AttackShape shape;
    private final double baseRange;
    private final ElementType element;

    Melee(
            int id,
            AttackShape shape,
            double baseRange,
            ElementType element
    ) {
        this.id = id;
        this.shape = shape;
        this.baseRange = baseRange;
        this.element = element;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        for (Zombie zombie : targets(plant, state)) {
            zombie.takeDamage(plant.getDamage(), element, state, plant);
            element.onHit(zombie, state);
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        onTick(plant, state);
    }

    private List<Zombie> targets(Plant plant, GameState state) {
        if (shape == AttackShape.SQUARE_3X3) {
            return state.getBoard().getZombiesInSquare(
                    plant.getPosY(),
                    plant.getPosX(),
                    1,
                    1
            );
        }
        double range = effectiveRange(plant);
        return state.getBoard().getZombiesInLane(plant.getPosY()).stream()
                .filter(zombie -> Math.abs(zombie.getX() - plant.getPosX()) <= range)
                .toList();
    }

    private double effectiveRange(Plant plant) {
        if (this == WASABI_WHIP && plant.getLevel() >= 3) {
            return baseRange + 1;
        }
        return baseRange;
    }

    private enum AttackShape {
        SAME_LANE,
        SQUARE_3X3
    }
}
