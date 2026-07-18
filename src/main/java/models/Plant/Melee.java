package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public enum Melee implements PlantType {
    BONK_CHOY(39, AttackShape.SAME_LANE, 1.0, ElementType.NORMAL),
    PHAT_BEET(40, AttackShape.SQUARE_3X3, 1.0, ElementType.NORMAL),
    CHOMPER(41, AttackShape.CHOMPER, 1.0, ElementType.NORMAL),
    WASABI_WHIP(42, AttackShape.SAME_LANE, 1.0, ElementType.FIRE),
    KIWIBEAST(43, AttackShape.GROWING_AREA, 1.0, ElementType.NORMAL);

    private static final int KIWIBEAST_STAGE_TWO_SECONDS = 24;
    private static final int KIWIBEAST_STAGE_THREE_SECONDS = 72;
    private static final int KIWIBEAST_STAGE_FOUR_SECONDS = 120;
    private static final int CHOMPER_PLANT_FOOD_TARGETS = 3;

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
    public void onPlanted(Plant plant, GameState state) {
        if (this == CHOMPER) {
            plant.forceReady(state);
        } else if (this == KIWIBEAST) {
            plant.setGrowthStage(1);
        }
    }

    @Override
    public void onEveryTick(Plant plant, GameState state) {
        if (this == WASABI_WHIP
                && plant.getAgeTicks() % state.getTicksPerSecond() == 0) {
            state.getBoard().warmArea(
                    plant.getPosY(),
                    plant.getPosX(),
                    1.0,
                    state
            );
        }
        if (this != KIWIBEAST) {
            return;
        }
        int seconds = plant.getAgeTicks() / state.getTicksPerSecond();
        int maximumStage = plant.getLevel() >= 4 ? 4 : 3;
        int stage;
        if (seconds >= KIWIBEAST_STAGE_FOUR_SECONDS && maximumStage >= 4) {
            stage = 4;
        } else if (seconds >= KIWIBEAST_STAGE_THREE_SECONDS) {
            stage = 3;
        } else if (seconds >= KIWIBEAST_STAGE_TWO_SECONDS) {
            stage = 2;
        } else {
            stage = 1;
        }
        plant.setGrowthStage(stage);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        if (shape == AttackShape.CHOMPER) {
            chomp(plant, state);
            return;
        }
        int damage = effectiveDamage(plant);
        for (Zombie zombie : targets(plant, state)) {
            zombie.takeDamage(damage, element, state, plant);
            element.onHit(zombie, state, 0, plant);
        }
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (this == CHOMPER) {
            chompSeveral(plant, state, CHOMPER_PLANT_FOOD_TARGETS);
        } else if (this == KIWIBEAST) {
            int damage = effectiveDamage(plant) * 3;
            double radius = effectiveRange(plant) + 1;
            for (Zombie zombie : state.getBoard().getZombiesInRadius(
                    plant.getPosY(),
                    plant.getPosX(),
                    radius
            )) {
                zombie.takeDamage(damage, state, plant);
            }
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        if (this == CHOMPER || this == KIWIBEAST) {
            return;
        }
        int multiplier = this == PHAT_BEET ? 3 : 1;
        for (Zombie zombie : state.getBoard().getZombiesInSquare(
                plant.getPosY(),
                plant.getPosX(),
                1,
                1
        )) {
            zombie.takeDamage(
                    plant.getDamage() * multiplier,
                    element,
                    state,
                    plant
            );
            element.onHit(zombie, state, 0, plant);
        }
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        if (this == CHOMPER || this == KIWIBEAST) {
            return 0;
        }
        return 10;
    }

    private void chomp(Plant plant, GameState state) {
        Zombie target = state.getBoard().getZombiesInLane(
                plant.getPosY()
        ).stream().filter(zombie -> {
            double distance = zombie.getX() - plant.getPosX();
            return distance >= 0 && distance <= baseRange;
        }).min(Comparator.comparingDouble(Zombie::getX)).orElse(null);
        if (target == null) {
            plant.forceReady(state);
            return;
        }
        target.takeDamage(
                Integer.MAX_VALUE,
                ElementType.NORMAL,
                state,
                plant
        );
    }

    private void chompSeveral(Plant plant, GameState state, int count) {
        List<Zombie> zombies = new ArrayList<>(state.getZombiesInTheGame());
        zombies.removeIf(Zombie::isDead);
        zombies.sort(Comparator.comparingDouble(zombie -> Math.hypot(
                zombie.getLane() - plant.getPosY(),
                zombie.getX() - plant.getPosX()
        )));
        for (int i = 0; i < Math.min(count, zombies.size()); i++) {
            zombies.get(i).takeDamage(
                    Integer.MAX_VALUE,
                    ElementType.NORMAL,
                    state,
                    plant
            );
        }
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
        if (shape == AttackShape.GROWING_AREA) {
            return state.getBoard().getZombiesInRadius(
                    plant.getPosY(),
                    plant.getPosX(),
                    effectiveRange(plant)
            );
        }
        double range = effectiveRange(plant);
        return state.getBoard().getZombiesInLane(plant.getPosY()).stream()
                .filter(zombie -> Math.abs(
                        zombie.getX() - plant.getPosX()
                ) <= range)
                .toList();
    }

    private int effectiveDamage(Plant plant) {
        if (this != KIWIBEAST) {
            return plant.getDamage();
        }
        return plant.getDamage() * Math.max(1, plant.getGrowthStage());
    }

    private double effectiveRange(Plant plant) {
        if (this == WASABI_WHIP) {
            return PlantEnumSupport.upgradedRange(plant, baseRange);
        }
        if (this == KIWIBEAST) {
            return baseRange + 0.5 * Math.max(0, plant.getGrowthStage() - 1);
        }
        return baseRange;
    }

    private enum AttackShape {
        SAME_LANE,
        SQUARE_3X3,
        CHOMPER,
        GROWING_AREA
    }
}
