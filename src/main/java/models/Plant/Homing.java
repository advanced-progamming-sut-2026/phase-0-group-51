package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.HomingMove;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public enum Homing implements PlantType {
    CAULIPOWER(14, HomingMode.HYPNOTIZE),
    ELECTRIC_BLUEBERRY(15, HomingMode.LIGHTNING),
    MAGNET_SHROOM(53, HomingMode.MAGNET),
    CAT_TAIL(55, HomingMode.PROJECTILE);

    private static final int PLANT_FOOD_TARGETS = 3;
    private static final double MAGNET_BASE_RANGE = 3;

    private final int id;
    private final HomingMode mode;

    Homing(int id, HomingMode mode) {
        this.id = id;
        this.mode = mode;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        switch (mode) {
            case PROJECTILE -> shootAtClosestZombie(plant, state);
            case HYPNOTIZE -> hypnotizeOne(plant, state);
            case LIGHTNING -> strikeOne(plant, state);
            case MAGNET -> pullOneMetallicArmor(plant, state);
        }
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        switch (mode) {
            case HYPNOTIZE -> randomEligibleZombies(state, PLANT_FOOD_TARGETS)
                    .forEach(zombie -> hypnotize(zombie, plant, state));
            case LIGHTNING -> randomEligibleZombies(state, PLANT_FOOD_TARGETS)
                    .forEach(zombie -> strike(zombie, plant, state));
            case MAGNET -> pullAllMetallicArmor(plant, state);
            case PROJECTILE -> {
                return;
            }
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        if (mode == HomingMode.PROJECTILE) {
            shootAtClosestZombie(plant, state);
        }
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        return mode == HomingMode.PROJECTILE ? 10 : 0;
    }

    private static void shootAtClosestZombie(Plant plant, GameState state) {
        Zombie target = state.getBoard().getClosestZombieAnywhere(
                plant.getPosY(),
                plant.getPosX()
        );
        if (target == null) {
            return;
        }
        state.getBoard().addProjectile(Projectile.homing(
                plant.getDamage(),
                ElementType.NORMAL,
                plant.getPlantTags(),
                PlantEnumSupport.projectileSpeed(plant, 0.45),
                plant.getPosX(),
                plant.getPosY(),
                target,
                new HomingMove()
        ).withSource(plant));
    }

    private static void hypnotizeOne(Plant plant, GameState state) {
        List<Zombie> targets = randomEligibleZombies(state, 1);
        if (!targets.isEmpty()) {
            Zombie target = targets.getFirst();
            state.getBoard().addProjectile(Projectile.homing(
                    0,
                    ElementType.HYPNOTIZE,
                    plant.getPlantTags(),
                    Math.max(
                            0.35,
                            PlantEnumSupport.projectileSpeed(plant, 0.45)
                    ),
                    plant.getPosX(),
                    plant.getPosY(),
                    target,
                    new HomingMove()
            ).withSource(plant));
        }
    }

    private static void hypnotize(
            Zombie zombie,
            Plant plant,
            GameState state
    ) {
        if (zombie.isDead() || zombie.isHypnotized()) {
            return;
        }
        zombie.setHypnotized(true);
        if (zombie.getDirection() > 0) {
            zombie.reverseDirection();
        }
        state.logEvent(plant.getName() + " hypnotized " + zombie.getAlias() + ".\n");
        System.out.printf(
                "[DEBUG][CAULIPOWER] %s hypnotized %s at row %d, x=%.2f.%n",
                plant.getName(),
                zombie.getAlias(),
                zombie.getLane() + 1,
                zombie.getX()
        );
    }

    private static void strikeOne(Plant plant, GameState state) {
        Zombie target;
        if (plant.getLevel() >= 3) {
            target = state.getZombiesInTheGame().stream()
                    .filter(zombie -> !zombie.isDead())
                    .max(Comparator.comparingInt(Zombie::getMaxHitpoints))
                    .orElse(null);
        } else {
            List<Zombie> targets = randomEligibleZombies(state, 1);
            target = targets.isEmpty() ? null : targets.getFirst();
        }
        if (target != null) {
            strike(target, plant, state);
        }
    }

    private static void strike(
            Zombie zombie,
            Plant plant,
            GameState state
    ) {
        zombie.killInstantly(
                state,
                models.quests.QuestKillSourceType.PLANT
        );
        state.logEvent(plant.getName() + " struck " + zombie.getAlias() + " with lightning.\n");
    }

    private static void pullOneMetallicArmor(
            Plant plant,
            GameState state
    ) {
        double range = PlantEnumSupport.upgradedRange(
                plant,
                MAGNET_BASE_RANGE
        );
        Zombie closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie.isDead() || !zombie.hasMetallicArmor()) {
                continue;
            }
            double distance = Math.hypot(
                    zombie.getLane() - plant.getPosY(),
                    zombie.getX() - plant.getPosX()
            );
            if (distance <= range && distance < closestDistance) {
                closest = zombie;
                closestDistance = distance;
            }
        }
        if (closest != null && closest.pullMetallicArmor()) {
            state.logEvent(plant.getName() + " removed metallic armor from "
                    + closest.getAlias() + ".\n");
        }
    }

    private static void pullAllMetallicArmor(
            Plant plant,
            GameState state
    ) {
        int removed = 0;
        for (Zombie zombie : state.getZombiesInTheGame()) {
            while (!zombie.isDead() && zombie.pullMetallicArmor()) {
                removed++;
            }
        }
        state.logEvent(plant.getName() + " plant food removed "
                + removed + " metallic armor pieces.\n");
    }

    private static List<Zombie> randomEligibleZombies(
            GameState state,
            int count
    ) {
        List<Zombie> pool = new ArrayList<>();
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                pool.add(zombie);
            }
        }
        List<Zombie> chosen = new ArrayList<>();
        int targetCount = Math.min(count, pool.size());
        for (int i = 0; i < targetCount; i++) {
            int index = state.getBoard().getRandom().nextInt(pool.size());
            chosen.add(pool.remove(index));
        }
        return chosen;
    }

    private enum HomingMode {
        PROJECTILE,
        HYPNOTIZE,
        LIGHTNING,
        MAGNET
    }
}
