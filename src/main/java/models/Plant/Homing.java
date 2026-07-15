package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.HomingMove;

public enum Homing implements PlantType {
    CAT_TAIL(55);

    private final int id;

    Homing(int id) {
        this.id = id;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        shootAtClosestZombie(plant, state);
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        shootAtClosestZombie(plant, state);
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
        ));
    }
}
