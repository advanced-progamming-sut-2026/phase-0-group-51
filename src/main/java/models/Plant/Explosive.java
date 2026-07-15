package models.Plant;

import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.util.List;

public enum Explosive implements PlantType {
    POTATO_MINE(30, ExplosionMode.TRAP_SINGLE, 0.65, 15, 12),
    PRIMAL_POTATO_MINE(31, ExplosionMode.TRAP_SQUARE, 0.65, 5, 4),
    CHERRY_BOMB(32, ExplosionMode.INSTANT_SQUARE, 0, 0, 0),
    JALAPENO(35, ExplosionMode.INSTANT_LANE, 0, 0, 0);

    private static final int MINE_CLONE_COUNT = 2;
    private final int id;
    private final ExplosionMode mode;
    private final double triggerRadius;
    private final float levelOneArmSeconds;
    private final float upgradedArmSeconds;

    Explosive(
            int id,
            ExplosionMode mode,
            double triggerRadius,
            float levelOneArmSeconds,
            float upgradedArmSeconds
    ) {
        this.id = id;
        this.mode = mode;
        this.triggerRadius = triggerRadius;
        this.levelOneArmSeconds = levelOneArmSeconds;
        this.upgradedArmSeconds = upgradedArmSeconds;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onPlanted(Plant plant, GameState state) {
        if (mode == ExplosionMode.INSTANT_SQUARE
                || mode == ExplosionMode.INSTANT_LANE) {
            explode(plant, state);
            removePlant(plant, state);
            return;
        }
        float armSeconds = plant.getLevel() >= 2
                ? upgradedArmSeconds
                : levelOneArmSeconds;
        plant.disableFor(armSeconds * state.getTicksPerSecond());
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        if (!mode.isTrap()) {
            return;
        }
        Zombie trigger = state.getBoard().getZombieNear(
                plant.getPosY(),
                plant.getPosX(),
                triggerRadius
        );
        if (trigger == null) {
            return;
        }
        if (mode == ExplosionMode.TRAP_SINGLE) {
            damageZombie(plant, state, trigger);
        } else {
            explodeThreeByThree(plant, state);
        }
        removePlant(plant, state);
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (!mode.isTrap()) {
            return;
        }
        plant.disableFor(0);
        dropArmedMineClones(plant, state);
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        onTick(plant, state);
    }

    private void explode(Plant plant, GameState state) {
        if (mode == ExplosionMode.INSTANT_LANE) {
            state.getBoard().meltIceInLane(plant.getPosY(), state);
            for (Zombie zombie : state.getBoard().getZombiesInLane(plant.getPosY())) {
                damageZombie(plant, state, zombie);
            }
        } else {
            explodeThreeByThree(plant, state);
        }
    }

    private void explodeThreeByThree(Plant plant, GameState state) {
        List<Zombie> zombies = state.getBoard().getZombiesInSquare(
                plant.getPosY(),
                plant.getPosX(),
                1,
                1
        );
        for (Zombie zombie : zombies) {
            damageZombie(plant, state, zombie);
        }
    }

    private void dropArmedMineClones(Plant source, GameState state) {
        List<Tile> destinations = state.getBoard().getRandomEmptyTiles(MINE_CLONE_COUNT);
        for (Tile tile : destinations) {
            Plant clone = createAtLevel(source.getLevel());
            clone.setPosX(tile.getColumn());
            clone.setPosY(tile.getLane());
            tile.setPlant(clone);
            clone.getPlantType().onPlanted(clone, state);
            clone.disableFor(0);
            state.logEvent(clone.getName() + " clone landed at ("
                    + (tile.getColumn() + 1) + ", " + (tile.getLane() + 1)
                    + ") and armed immediately.\n");
        }
    }

    private Plant createAtLevel(int level) {
        Plant clone = create();
        while (clone.getLevel() < level) {
            clone.levelUp();
        }
        return clone;
    }

    private void damageZombie(Plant plant, GameState state, Zombie zombie) {
        ElementType element = PlantEnumSupport.elementFromTags(plant);
        zombie.takeDamage(plant.getDamage(), element, state, plant);
        element.onHit(zombie, state);
    }

    private static void removePlant(Plant plant, GameState state) {
        plant.setMarkedForRemoval(true);
        state.getBoard().removePlant(plant.getPosY(), plant.getPosX());
    }

    private enum ExplosionMode {
        TRAP_SINGLE,
        TRAP_SQUARE,
        INSTANT_SQUARE,
        INSTANT_LANE;

        private boolean isTrap() {
            return this == TRAP_SINGLE || this == TRAP_SQUARE;
        }
    }
}
