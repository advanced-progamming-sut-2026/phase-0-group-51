package models.Plant;

import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public enum Explosive implements PlantType {
    POTATO_MINE(30, ExplosionMode.TRAP_SINGLE, 0.65, 15, 12),
    PRIMAL_POTATO_MINE(31, ExplosionMode.TRAP_SQUARE, 0.65, 5, 4),
    CHERRY_BOMB(32, ExplosionMode.INSTANT_SQUARE, 0, 0, 0),
    SQUASH(33, ExplosionMode.SQUASH, 1.0, 0, 0),
    GRAPESHOT(34, ExplosionMode.GRAPESHOT, 0, 0, 0),
    JALAPENO(35, ExplosionMode.INSTANT_LANE, 0, 0, 0),
    DOOM_SHROOM(36, ExplosionMode.GLOBAL_EXPLOSION, 0, 0, 0),
    TANGLE_KELP(37, ExplosionMode.TANGLE_KELP, 0.65, 0, 0),
    ICEBERG_LETTUCE(38, ExplosionMode.FREEZE_TRAP, 0.65, 0, 0),
    ICE_SHROOM(57, ExplosionMode.GLOBAL_FREEZE, 0, 0, 0),
    HOT_POTATO(59, ExplosionMode.HOT_POTATO, 0, 0, 0),
    GRAVE_BUSTER(60, ExplosionMode.GRAVE_BUSTER, 0, 0, 0);

    private static final int MINE_CLONE_COUNT = 2;
    private static final int SQUASH_PLANT_FOOD_TARGETS = 2;
    private static final double BASE_FREEZE_SECONDS = 10;
    private static final int GRAPESHOT_GRAPE_DAMAGE = 200;
    private static final int GRAPESHOT_GRAPE_LIFETIME_SECONDS = 5;
    private static final int GRAVE_BUSTER_BASE_SECONDS = 3;

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
                || mode == ExplosionMode.INSTANT_LANE
                || mode == ExplosionMode.GLOBAL_EXPLOSION
                || mode == ExplosionMode.GRAPESHOT) {
            explode(plant, state);
            if (mode == ExplosionMode.GRAPESHOT) {
                launchGrapes(plant, state);
            }
            removePlant(plant, state);
            return;
        }
        if (mode == ExplosionMode.GLOBAL_FREEZE) {
            freezeAllZombies(plant, state);
            removePlant(plant, state);
            return;
        }
        if (mode == ExplosionMode.HOT_POTATO) {
            meltHotPotatoArea(plant, state);
            if (plant.getLevel() >= 4) {
                explodeThreeByThree(plant, state);
            }
            removePlant(plant, state);
            return;
        }
        if (mode == ExplosionMode.GRAVE_BUSTER) {
            int eatSeconds = Math.max(
                    1,
                    GRAVE_BUSTER_BASE_SECONDS
                            - (plant.getLevel() >= 2 ? 1 : 0)
            );
            plant.disableFor(eatSeconds * state.getTicksPerSecond());
            return;
        }
        if (mode.isMine()) {
            float armSeconds = plant.getLevel() >= 2
                    ? upgradedArmSeconds
                    : levelOneArmSeconds;
            plant.disableFor(armSeconds * state.getTicksPerSecond());
        }
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        switch (mode) {
            case TRAP_SINGLE, TRAP_SQUARE -> tickMine(plant, state);
            case SQUASH -> tickSquash(plant, state);
            case FREEZE_TRAP -> tickIceberg(plant, state);
            case TANGLE_KELP -> tickTangleKelp(plant, state, false);
            case GRAVE_BUSTER -> finishGraveBuster(plant, state);
            default -> {
                return;
            }
        }
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (mode.isMine()) {
            plant.disableFor(0);
            dropArmedMineClones(plant, state);
        } else if (mode == ExplosionMode.SQUASH) {
            crushRandomZombies(plant, state, SQUASH_PLANT_FOOD_TARGETS);
        } else if (mode == ExplosionMode.FREEZE_TRAP) {
            freezeAllZombies(plant, state);
        } else if (mode == ExplosionMode.TANGLE_KELP) {
            tickTangleKelp(plant, state, true);
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        if (mode.isMine()) {
            onTick(plant, state);
        }
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        return mode.isMine() ? 10 : 0;
    }

    private void tickMine(Plant plant, GameState state) {
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

    private void tickSquash(Plant plant, GameState state) {
        List<Zombie> nearby = new ArrayList<>(
                state.getBoard().getZombiesInRadius(
                        plant.getPosY(),
                        plant.getPosX(),
                        triggerRadius
                )
        );
        if (nearby.isEmpty()) {
            return;
        }
        nearby.sort(Comparator.comparingDouble(zombie -> Math.hypot(
                zombie.getLane() - plant.getPosY(),
                zombie.getX() - plant.getPosX()
        )));
        int crushCount = plant.getLevel() >= 4 ? 2 : 1;
        for (int i = 0; i < Math.min(crushCount, nearby.size()); i++) {
            damageZombie(plant, state, nearby.get(i));
        }
        removePlant(plant, state);
    }

    private void tickIceberg(Plant plant, GameState state) {
        Zombie trigger = state.getBoard().getZombieNear(
                plant.getPosY(),
                plant.getPosX(),
                triggerRadius
        );
        if (trigger == null) {
            return;
        }
        applyFreeze(plant, trigger, state);
        removePlant(plant, state);
    }

    private void tickTangleKelp(Plant plant, GameState state, boolean plantFood) {
        List<Zombie> waterZombies = new ArrayList<>();
        for (Zombie zombie : state.getZombiesInTheGame()) {
            int column = (int) Math.floor(zombie.getX());
            if (!zombie.isDead() && state.getBoard().isWaterTile(zombie.getLane(), column)) {
                waterZombies.add(zombie);
            }
        }
        waterZombies.sort(Comparator.comparingDouble(zombie -> Math.hypot(
                zombie.getLane() - plant.getPosY(),
                zombie.getX() - plant.getPosX()
        )));
        int count = plantFood
                ? Math.max(3, plant.getPlantStat().targetCount() + 2)
                : Math.max(1, plant.getPlantStat().targetCount());
        if (!plantFood) {
            waterZombies.removeIf(zombie -> Math.hypot(
                    zombie.getLane() - plant.getPosY(),
                    zombie.getX() - plant.getPosX()
            ) > triggerRadius);
        }
        for (int i = 0; i < Math.min(count, waterZombies.size()); i++) {
            waterZombies.get(i).killInstantly(
                    state,
                    models.quests.QuestKillSourceType.PLANT
            );
        }
        if (!waterZombies.isEmpty()) {
            removePlant(plant, state);
        }
    }

    private void explode(Plant plant, GameState state) {
        if (mode == ExplosionMode.INSTANT_LANE) {
            state.getBoard().meltIceInLane(plant.getPosY(), state);
            for (Zombie zombie : state.getBoard().getZombiesInLane(plant.getPosY())) {
                damageZombie(plant, state, zombie);
            }
        } else if (mode == ExplosionMode.GLOBAL_EXPLOSION) {
            for (Zombie zombie : new ArrayList<>(state.getZombiesInTheGame())) {
                if (!zombie.isDead()) {
                    damageZombie(plant, state, zombie);
                }
            }
            Tile tile = state.getBoard().getTile(
                    plant.getPosY(),
                    plant.getPosX()
            );
            if (tile != null) {
                tile.setCrater(true);
            }
        } else {
            explodeThreeByThree(plant, state);
        }
    }

    private void launchGrapes(Plant plant, GameState state) {
        double[][] directions = {
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {1, 0.25}, {1, -0.25}, {-1, 0.25}, {-1, -0.25}
        };
        int hitCount = 3 + (plant.getLevel() >= 3 ? 1 : 0);
        int lifetime = GRAPESHOT_GRAPE_LIFETIME_SECONDS
                * state.getTicksPerSecond();
        for (double[] direction : directions) {
            state.getBoard().addProjectile(Projectile.bouncing(
                    GRAPESHOT_GRAPE_DAMAGE,
                    ElementType.NORMAL,
                    plant.getPlantTags(),
                    0.5,
                    plant.getPosX(),
                    plant.getPosY(),
                    direction[0],
                    direction[1],
                    hitCount,
                    0
            ).withSource(plant).withLifetime(lifetime));
        }
    }

    private void meltHotPotatoArea(Plant plant, GameState state) {
        int radius = plant.getLevel() >= 3 ? 1 : 0;
        for (int lane = Math.max(0, plant.getPosY() - radius);
             lane <= Math.min(
                     state.getBoard().getLaneCount() - 1,
                     plant.getPosY() + radius
             );
             lane++) {
            for (int column = Math.max(0, plant.getPosX() - radius);
                 column <= Math.min(
                         state.getBoard().getColumnCount() - 1,
                         plant.getPosX() + radius
                 );
                 column++) {
                Tile tile = state.getBoard().getTile(lane, column);
                tile.setIceBlocked(false);
                for (Plant frozen : tile.getPlants()) {
                    if (frozen != plant) {
                        frozen.meltCompletely(state);
                    }
                }
            }
        }
    }

    private void finishGraveBuster(Plant plant, GameState state) {
        Tile tile = state.getBoard().getTile(
                plant.getPosY(),
                plant.getPosX()
        );
        if (tile != null && tile.hasGrave()) {
            tile.removeGrave();
            state.logEvent("Grave Buster removed the grave at ("
                    + (plant.getPosX() + 1) + ", "
                    + (plant.getPosY() + 1) + ").\n");
            if (plant.getLevel() >= 4) {
                explodeThreeByThree(plant, state);
            }
        }
        removePlant(plant, state);
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

    private void crushRandomZombies(Plant plant, GameState state, int count) {
        for (Zombie zombie : state.getBoard().getRandomZombies(count)) {
            damageZombie(plant, state, zombie);
        }
    }

    private void freezeAllZombies(Plant plant, GameState state) {
        for (Zombie zombie : new ArrayList<>(
                state.getZombiesInTheGame()
        )) {
            if (zombie.isDead()) {
                continue;
            }
            if (plant.getDamage() > 0) {
                damageZombie(plant, state, zombie);
            }
            if (!zombie.isDead()) {
                applyFreeze(plant, zombie, state);
            }
        }
    }

    private void applyFreeze(Plant plant, Zombie zombie, GameState state) {
        int ticks = (int) Math.round(
                (BASE_FREEZE_SECONDS + plant.getPlantStat().freezeDuration()) * state.getTicksPerSecond());
        zombie.applyFreeze(ticks);
        state.logEvent(plant.getName() + " froze "
                + zombie.getAlias() + " for "
                + ((double) ticks / state.getTicksPerSecond())
                + " seconds.\n");
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
                    + (tile.getColumn() + 1) + ", "
                    + (tile.getLane() + 1)
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
        element.onHit(zombie, state, 0, plant);
    }

    private static void removePlant(Plant plant, GameState state) {
        plant.setMarkedForRemoval(true);
        state.getBoard().removePlant(plant);
    }

    private enum ExplosionMode {
        TRAP_SINGLE,
        TRAP_SQUARE,
        INSTANT_SQUARE,
        INSTANT_LANE,
        SQUASH,
        GLOBAL_EXPLOSION,
        FREEZE_TRAP,
        GRAPESHOT,
        TANGLE_KELP,
        GLOBAL_FREEZE,
        HOT_POTATO,
        GRAVE_BUSTER;

        private boolean isMine() {
            return this == TRAP_SINGLE || this == TRAP_SQUARE;
        }
    }
}
