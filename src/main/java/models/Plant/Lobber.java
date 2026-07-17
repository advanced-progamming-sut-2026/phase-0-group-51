package models.Plant;

import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;

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
    public void onFoodTick(Plant plant, GameState state) {
        for (Zombie zombie : state.getBoard().getRandomZombies(3)) {
            launchAtZombie(plant, state, zombie);
        }
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

    private boolean shouldLaunchButter(Plant plant, GameState state) {
        int chance = BASE_BUTTER_CHANCE_PERCENT;
        if (plant.getLevel() >= 2) {
            chance += 5;
        }
        return state.getBoard().getRandom().nextInt(100) < chance;
    }
}
