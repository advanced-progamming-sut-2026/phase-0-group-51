package models.Plant;

import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;

import java.util.List;

public enum Lobber implements PlantType {
    CABBAGE_PULT(25, ElementType.NORMAL, 0, false),
    KERNEL_PULT(26, ElementType.NORMAL, 0, true),
    MELON_PULT(27, ElementType.NORMAL, 1.5, false),
    WINTER_MELON(28, ElementType.ICE, 1.5, false),
    PEPPER_PULT(29, ElementType.FIRE, 1.5, false);

    private static final int BASE_BUTTER_CHANCE_PERCENT = 25;
    private final int id;
    private final ElementType element;
    private final double areaRadius;
    private final boolean kernelPult;

    Lobber(
            int id,
            ElementType element,
            double areaRadius,
            boolean kernelPult
    ) {
        this.id = id;
        this.element = element;
        this.areaRadius = areaRadius;
        this.kernelPult = kernelPult;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        Zombie zombieTarget = state.getBoard().getFirstZombieAheadInLane(
                plant.getPosY(),
                plant.getPosX()
        );
        if (zombieTarget != null) {
            launchAtZombie(plant, state, zombieTarget);
            return;
        }

        Tile graveTarget = state.getBoard().getFirstGraveAheadInLane(
                plant.getPosY(),
                plant.getPosX()
        );
        if (graveTarget != null) {
            launchAtGrave(plant, state, graveTarget);
            return;
        }

        plant.forceReady(state);
    }

    @Override
    public void onEveryTick(Plant plant, GameState state) {
        if (this != PEPPER_PULT
                || plant.getAgeTicks() % state.getTicksPerSecond() != 0) {
            return;
        }
        state.getBoard().warmArea(
                plant.getPosY(),
                plant.getPosX(),
                1.0 + plant.getPlantStat().warmthRadius(),
                state
        );
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (this == KERNEL_PULT) {
            for (Zombie zombie : List.copyOf(
                    state.getZombiesInTheGame()
            )) {
                if (!zombie.isDead()) {
                    launchFoodProjectile(
                            plant,
                            state,
                            zombie,
                            ElementType.BUTTER,
                            Math.max(40, plant.getDamage()),
                            0
                    );
                }
            }
            return;
        }
        int damageMultiplier = this == CABBAGE_PULT ? 1 : 3;
        double foodRadius = plant.hasTag(PlantTag.AOE)
                ? Math.max(2.0, areaRadius)
                : 0;
        for (Zombie zombie : state.getBoard().getRandomZombies(3)) {
            launchFoodProjectile(
                    plant,
                    state,
                    zombie,
                    element,
                    plant.getDamage() * damageMultiplier,
                    foodRadius
            );
        }
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        return 0;
    }

    @Override
    public boolean isLobber() {
        return true;
    }

    private void launchAtZombie(
            Plant plant,
            GameState state,
            Zombie target
    ) {
        state.getBoard().addProjectile(createProjectile(
                plant,
                state,
                target.getX(),
                target.getLane()
        ));
    }

    private void launchAtGrave(
            Plant plant,
            GameState state,
            Tile target
    ) {
        state.getBoard().addProjectile(createProjectile(
                plant,
                state,
                target.getColumn(),
                target.getLane()
        ).withGraveTarget());
    }

    private Projectile createProjectile(
            Plant plant,
            GameState state,
            double targetX,
            double targetLane
    ) {
        ElementType shotElement = element;
        int damage = plant.getDamage();
        if (kernelPult && shouldLaunchButter(plant, state)) {
            shotElement = ElementType.BUTTER;
            damage = Math.max(40, damage);
        }
        double splashRadius = plant.hasTag(PlantTag.AOE) ? areaRadius : 0;
        int splashDamage = damage
                + (int) Math.round(plant.getPlantStat().aoeDamage());
        return Projectile.targeted(
                damage,
                splashDamage,
                shotElement,
                plant.getPlantTags(),
                PlantEnumSupport.projectileSpeed(plant, 0.35),
                plant.getPosX(),
                plant.getPosY(),
                targetX,
                targetLane,
                new ArcMove(),
                splashRadius
        ).withSource(plant);
    }

    private void launchFoodProjectile(
            Plant plant,
            GameState state,
            Zombie target,
            ElementType foodElement,
            int damage,
            double splashRadius
    ) {
        state.getBoard().addProjectile(Projectile.targeted(
                damage,
                damage,
                foodElement,
                plant.getPlantTags(),
                PlantEnumSupport.projectileSpeed(plant, 0.35),
                plant.getPosX(),
                plant.getPosY(),
                target.getX(),
                target.getLane(),
                new ArcMove(),
                splashRadius
        ).withSource(plant));
    }

    private boolean shouldLaunchButter(Plant plant, GameState state) {
        int chance = BASE_BUTTER_CHANCE_PERCENT;
        if (plant.getLevel() >= 2) {
            chance += 5;
        }
        return state.getBoard().getRandom().nextInt(100) < chance;
    }
}
